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
import edu.cmu.cs.gabriel.network.Protos.FromServer;
import edu.cmu.cs.gabriel.network.Protos.Engine;

import edu.cmu.cs.gabriel.token.TokenController;
import okhttp3.OkHttpClient;

public class Websocket {

    private static String TAG = "Websocket";

    private interface GabrielSocket {
        @Send
        void Send(FromClient fromClient);

        @Receive
        Stream<FromServer> Receive();

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

        webSocketInterface.Receive().start(new Stream.Observer<FromServer>() {
            @Override
            public void onNext(FromServer fromServer) {
                if (fromServer.getStatus() == FromServer.Status.SUCCESS) {
                    if (fromServer.getResultsCount() == 1) {
                        FromServer.Result result = fromServer.getResults(0);
                        if (result.getType() == FromServer.Result.ResultType.IMAGE) {
                            if (result.getEngine() == Engine.OPENRTIST) {
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
                                        "Got result from engine" + result.getEngine().name());
                            }
                        } else {
                            Log.e(TAG, "Got result of type " + result.getType().name());
                        }
                    } else {
                        Log.e(TAG, "Got " + fromServer.getResultsCount() +
                                " results in output.");
                    }
                } else {
                    Log.e(TAG, "Output status was: " + fromServer.getStatus().name());
                }

                // Refill token
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
        fromClientBuilder.setType(FromClient.Type.IMAGE);
        fromClientBuilder.setPayload(ByteString.copyFrom(data));
        fromClientBuilder.setStyle(style);

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
