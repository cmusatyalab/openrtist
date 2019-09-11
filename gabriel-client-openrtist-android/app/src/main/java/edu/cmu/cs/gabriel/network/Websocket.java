package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.tinder.scarlet.Lifecycle;
import com.tinder.scarlet.Scarlet;
import com.tinder.scarlet.ShutdownReason;
import com.tinder.scarlet.Stream;
import com.tinder.scarlet.WebSocket;
import com.tinder.scarlet.lifecycle.LifecycleRegistry;
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle;
import com.tinder.scarlet.messageadapter.protobuf.ProtobufMessageAdapter;
import com.tinder.scarlet.websocket.okhttp.OkHttpClientUtils;
import com.tinder.scarlet.ws.Receive;
import com.tinder.scarlet.ws.Send;

import java.io.ByteArrayOutputStream;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.network.Protos.FromClient;
import edu.cmu.cs.gabriel.network.Protos.ToClient;
import edu.cmu.cs.gabriel.network.Protos.ResultWrapper;
import edu.cmu.cs.gabriel.network.Protos.PayloadType;
import edu.cmu.cs.openrtist.Protos.EngineFields;
import com.google.protobuf.Any;

import edu.cmu.cs.gabriel.token.TokenController;
import okhttp3.OkHttpClient;

public class Websocket {

    private static String TAG = "Websocket";
    private static String ENGINE_NAME = "openrtist";

    private interface GabrielSocket {
        @Send
        void Send(FromClient fromClient);

        @Receive
        Stream<ToClient> Receive();

        @Receive
        Stream<WebSocket.Event> observeWebSocketEvent();
    }

    private GabrielSocket webSocketInterface;
    private Handler returnMsgHandler;
    private long frameID;
    private TokenController tokenController;
    private LifecycleRegistry lifecycleRegistry;
    private boolean connected;

    public Websocket(String serverIP, int port, Handler returnMsgHandler, Activity activity,
                     TokenController tokenController) {
        this.returnMsgHandler = returnMsgHandler;
        String url = "ws://" + serverIP + ":" + port;
        frameID = 0;
        this.tokenController = tokenController;
        this.connected = false;

        OkHttpClient okClient = new OkHttpClient();

        Lifecycle androidLifecycle = AndroidLifecycle.ofApplicationForeground(
                activity.getApplication());
        this.lifecycleRegistry = new LifecycleRegistry(0L);
        this.lifecycleRegistry.onNext(Lifecycle.State.Started.INSTANCE);

        webSocketInterface = new Scarlet.Builder()
                .webSocketFactory(OkHttpClientUtils.newWebSocketFactory(okClient, url))
                .addMessageAdapterFactory(new ProtobufMessageAdapter.Factory())
                .lifecycle(androidLifecycle.combineWith(lifecycleRegistry))
                .build().create(GabrielSocket.class);

        webSocketInterface.Receive().start(new Stream.Observer<ToClient>() {
            @Override
            public void onNext(ToClient toClient) {
                if (toClient.hasResultWrapper()) {
                    ResultWrapper resultWrapper = toClient.getResultWrapper();
                    if (resultWrapper.getStatus() == ResultWrapper.Status.SUCCESS) {
                        if (resultWrapper.getResultsCount() == 1) {
                            ResultWrapper.Result result = resultWrapper.getResults(0);
                            if (result.getPayloadType() == PayloadType.IMAGE) {
                                if (result.getEngineName().equals(ENGINE_NAME)) {
                                    ByteString dataString = result.getPayload();

                                    Bitmap imageFeedback = BitmapFactory.decodeByteArray(
                                            dataString.toByteArray(), 0, dataString.size());

                                    Message msg = Message.obtain();
                                    msg.what = NetworkProtocol.NETWORK_RET_IMAGE;
                                    msg.obj = imageFeedback;
                                    Websocket.this.returnMsgHandler.sendMessage(msg);
                                } else {
                                    Log.e(
                                            TAG,
                                            "Got result from engine " + result.getEngineName());
                                }
                            } else {
                                Log.e(TAG, "Got result of type " + result.getPayloadType().name());
                            }
                        } else {
                            Log.e(TAG, "Got " + resultWrapper.getResultsCount() +
                                    " results in output.");
                        }
                    } else {
                        Log.e(TAG, "Output status was: " + resultWrapper.getStatus().name());
                    }
                }

                // Refill Tokens
                // TODO: Ensure the number of tokens we have match toClient.getNumTokens()
                Websocket.this.tokenController.increaseTokens(1);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "onError", throwable);
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete");
            }
        });

        webSocketInterface.observeWebSocketEvent().start(new Stream.Observer<WebSocket.Event>() {
            @Override
            public void onNext(WebSocket.Event receivedUpdate) {
                Log.i(TAG, receivedUpdate.toString());

                if (receivedUpdate instanceof WebSocket.Event.OnConnectionOpened) {
                    Websocket.this.connected = true;
                } else if (receivedUpdate instanceof WebSocket.Event.OnConnectionClosing ||
                        receivedUpdate instanceof WebSocket.Event.OnConnectionFailed) {
                    if (Websocket.this.tokenController != null) {
                        Websocket.this.tokenController.reset();
                    }

                    if (Websocket.this.connected) {
                        Message msg = Message.obtain();
                        msg.what = NetworkProtocol.NETWORK_RET_FAILED;
                        Bundle data = new Bundle();
                        data.putString("message", "Connection Error");
                        msg.setData(data);
                        Websocket.this.returnMsgHandler.sendMessage(msg);
                    }
                    Websocket.this.connected = false;
                }
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(TAG, "event onError", throwable);

            }

            @Override
            public void onComplete() {
                Log.i(TAG, "event onComplete");

            }
        });
    }

    public void sendFrame(byte[] frame, Camera.Parameters parameters, String style) {
        byte[] data;

        Camera.Size cameraImageSize = parameters.getPreviewSize();
        YuvImage image = new YuvImage(frame, parameters.getPreviewFormat(), cameraImageSize.width,
                cameraImageSize.height, null);
        ByteArrayOutputStream tmpBuffer = new ByteArrayOutputStream();
        // chooses quality 67 and it roughly matches quality 5 in avconv
        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()),
                67, tmpBuffer);
        if (Const.USING_FRONT_CAMERA) {
            byte[] newFrame = tmpBuffer.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(newFrame, 0, newFrame.length);
            ByteArrayOutputStream rotatedStream = new ByteArrayOutputStream();
            Matrix matrix = new Matrix();
            if (Const.FRONT_ROTATION) {
                matrix.postRotate(180);
            }
            matrix.postScale(-1, 1);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 67, rotatedStream);
            //this.frameBuffer = tmpBuffer.toByteArray();
            data = rotatedStream.toByteArray();
        } else {
            data = tmpBuffer.toByteArray();
        }
        this.frameID++;

        FromClient.Builder fromClientBuilder = FromClient.newBuilder();
        fromClientBuilder.setFrameId(this.frameID);
        fromClientBuilder.setPayloadType(PayloadType.IMAGE);
        fromClientBuilder.setEngineName(ENGINE_NAME);
        fromClientBuilder.setPayload(ByteString.copyFrom(data));

        EngineFields.Builder engineFieldsBuilder = EngineFields.newBuilder();
        engineFieldsBuilder.setStyle(style);
        EngineFields engineFields = engineFieldsBuilder.build();

        fromClientBuilder.setEngineFields(Any.pack(engineFields));

        FromClient fromClient = fromClientBuilder.build();

        webSocketInterface.Send(fromClient);
    }

    public void stop() {
        lifecycleRegistry.onNext(new Lifecycle.State.Stopped.WithReason(ShutdownReason.GRACEFUL));
    }

    public boolean isConnected() {
        return connected;
    }
}
