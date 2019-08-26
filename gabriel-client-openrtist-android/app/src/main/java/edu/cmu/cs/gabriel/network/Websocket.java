package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.tinder.scarlet.Scarlet;
import com.tinder.scarlet.Stream;
import com.tinder.scarlet.WebSocket;
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle;
import com.tinder.scarlet.messageadapter.protobuf.ProtobufMessageAdapter;
import com.tinder.scarlet.websocket.okhttp.OkHttpClientUtils;
import com.tinder.scarlet.ws.Receive;
import com.tinder.scarlet.ws.Send;

import java.io.ByteArrayOutputStream;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.network.Protos.Input;
import edu.cmu.cs.gabriel.network.Protos.Result;

import okhttp3.OkHttpClient;

public class Websocket {

    private static String TAG = "Websocket";

    private interface GabrielSocket {
        @Send
        void Send(Input input);

        @Receive
        Stream<Result> Receive();

        @Receive
        Stream<WebSocket.Event> observeWebSocketEvent();
    }

    private GabrielSocket webSocketInterface;
    private Handler returnMsgHandler;
    private long frameID;

    public Websocket(String serverIP, int port, Handler returnMsgHandler, Activity activity) {
        this.returnMsgHandler = returnMsgHandler;
        String url = "ws://" + serverIP + ":" + port;
        frameID = 0;

        OkHttpClient okClient = new OkHttpClient();

        webSocketInterface = new Scarlet.Builder()
                .webSocketFactory(OkHttpClientUtils.newWebSocketFactory(okClient, url))
                .addMessageAdapterFactory(new ProtobufMessageAdapter.Factory())
                .lifecycle(AndroidLifecycle.ofApplicationForeground(activity.getApplication()))
                .build().create(GabrielSocket.class);

        webSocketInterface.Receive().start(new Stream.Observer<Result>() {
            @Override
            public void onNext(Result result) {
                System.out.println("Received");
                String imageFeedbackString = result.getData();
                byte[] data = Base64.decode(imageFeedbackString.getBytes(), Base64.DEFAULT);
                Bitmap imageFeedback = BitmapFactory.decodeByteArray(data,0,data.length);

                Message msg = Message.obtain();
                msg.what = NetworkProtocol.NETWORK_RET_IMAGE;
                msg.obj = imageFeedback;
                Websocket.this.returnMsgHandler.sendMessage(msg);
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
        System.out.println("Sending");
        byte[] data;

        Camera.Size cameraImageSize = parameters.getPreviewSize();
        YuvImage image = new YuvImage(frame, parameters.getPreviewFormat(), cameraImageSize.width,
                cameraImageSize.height, null);
        ByteArrayOutputStream tmpBuffer = new ByteArrayOutputStream();
        // chooses quality 67 and it roughly matches quality 5 in avconv
        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 67, tmpBuffer);
        if (Const.USING_FRONT_CAMERA) {
            byte[] newFrame = tmpBuffer.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(newFrame, 0, newFrame.length);
            ByteArrayOutputStream rotatedStream = new ByteArrayOutputStream();
            Matrix matrix = new Matrix();
            if (Const.FRONT_ROTATION) {
                matrix.postRotate(180);
            }
            matrix.postScale(-1, 1);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 67, rotatedStream);
            //this.frameBuffer = tmpBuffer.toByteArray();
            data = rotatedStream.toByteArray();
        } else {
            data = tmpBuffer.toByteArray();
        }
        this.frameID++;

        Input.Builder inputBuilder = Input.newBuilder();
        inputBuilder.setUpdateId(Long.toString(this.frameID));
        inputBuilder.setType(Input.Type.IMAGE);
        inputBuilder.setData(ByteString.copyFrom(data));
        inputBuilder.setStyle(style);

        Input input = inputBuilder.build();

        webSocketInterface.Send(input);
    }
}
