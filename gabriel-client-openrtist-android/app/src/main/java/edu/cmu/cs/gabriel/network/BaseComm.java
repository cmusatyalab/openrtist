package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.protobuf.ByteString;

import edu.cmu.cs.gabriel.client.function.Consumer;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;
import edu.cmu.cs.gabriel.client.comm.ServerCommCore;
import edu.cmu.cs.openrtist.R;

public class BaseComm {
    private static String TAG = "OpenrtistComm";
    private static String ENGINE_NAME = "openrtist";

    ServerCommCore serverCommCore;
    Consumer<ResultWrapper> consumer;
    Runnable onDisconnect;
    private boolean shownError;

    public BaseComm(final Activity activity, final Handler returnMsgHandler) {

        this.consumer = new Consumer<ResultWrapper>() {
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

        this.onDisconnect = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Disconnected");
                String message = BaseComm.this.serverCommCore.isRunning()
                        ? activity.getResources().getString(R.string.server_disconnected)
                        : activity.getResources().getString(R.string.could_not_connect);

                if (BaseComm.this.shownError) {
                    return;
                }

                BaseComm.this.shownError = true;

                Message msg = Message.obtain();
                msg.what = NetworkProtocol.NETWORK_RET_FAILED;
                Bundle data = new Bundle();
                data.putString("message", message);
                msg.setData(data);
                returnMsgHandler.sendMessage(msg);
            }
        };

        this.shownError = false;
    }

    public void sendSupplier(FrameSupplier frameSupplier) {
        this.serverCommCore.sendSupplier(frameSupplier);
    }

    public void stop() {
        this.serverCommCore.stop();
    }
}
