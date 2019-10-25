package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.protobuf.ByteString;

import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.client.Consumer;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;
import edu.cmu.cs.gabriel.client.ServerComm;
import edu.cmu.cs.openrtist.R;

public class OpenrtistComm {
    private static String TAG = "OpenrtistComm";
    private static String ENGINE_NAME = "openrtist";

    private ServerComm serverComm;
    private boolean shownError;

    public OpenrtistComm(
            String serverIP, int port, final Activity activity, final Handler returnMsgHandler) {

        Consumer<ResultWrapper> consumer = new Consumer<ResultWrapper>() {
            @Override
            public void accept(ResultWrapper resultWrapper) {
                if (resultWrapper.getResultsCount() != 1) {
                    Log.e(TAG, "Got " + resultWrapper.getResultsCount() + " results in output.");
                    return;
                }

                ResultWrapper.Result result = resultWrapper.getResults(0);

                if (result.getPayloadType() != PayloadType.IMAGE) {
                    Log.e(TAG, "Got result of type " + result.getPayloadType().name());
                    return;
                }

                if (!result.getEngineName().equals(ENGINE_NAME)) {
                    Log.e(TAG, "Got result from engine " + result.getEngineName());
                    return;
                }

                ByteString dataString = result.getPayload();

                Bitmap imageFeedback = BitmapFactory.decodeByteArray(
                        dataString.toByteArray(), 0, dataString.size());

                Message msg = Message.obtain();
                msg.what = NetworkProtocol.NETWORK_RET_IMAGE;
                msg.obj = imageFeedback;
                returnMsgHandler.sendMessage(msg);
            }
        };

        Runnable onDisconnect = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Disconnected");
                String message = OpenrtistComm.this.serverComm.isRunning()
                        ? activity.getResources().getString(R.string.server_disconnected)
                        : activity.getResources().getString(R.string.could_not_connect);

                if (OpenrtistComm.this.shownError) {
                    return;
                }

                OpenrtistComm.this.shownError = true;

                Message msg = Message.obtain();
                msg.what = NetworkProtocol.NETWORK_RET_FAILED;
                Bundle data = new Bundle();
                data.putString("message", message);
                msg.setData(data);
                returnMsgHandler.sendMessage(msg);
            }
        };

        this.serverComm = new ServerComm(consumer, onDisconnect, serverIP, port,
                activity.getApplication());

        this.shownError = false;
    }

    public void sendSupplier(FrameSupplier frameSupplier) {
        this.serverComm.sendSupplier(frameSupplier);
    }

    public void stop() {
        this.serverComm.stop();
    }
}
