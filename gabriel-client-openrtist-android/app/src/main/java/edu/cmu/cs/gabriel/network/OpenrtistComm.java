package edu.cmu.cs.gabriel.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.protobuf.ByteString;

import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabrielclient.Protos.PayloadType;
import edu.cmu.cs.gabrielclient.Protos.ResultWrapper;
import edu.cmu.cs.gabrielclient.ServerComm;
import edu.cmu.cs.openrtist.R;

public class OpenrtistComm extends ServerComm {
    private static String TAG = "OpenrtistComm";
    private static String ENGINE_NAME = "openrtist";

    private Handler returnMsgHandler;
    private boolean shownError;
    private GabrielClientActivity gabrielClientActivity;

    public OpenrtistComm(String serverIP, int port, GabrielClientActivity gabrielClientActivity,
                         Handler returnMsgHandler) {
        super(serverIP, port, gabrielClientActivity.getApplication());
        this.returnMsgHandler = returnMsgHandler;
        this.gabrielClientActivity = gabrielClientActivity;
        this.shownError = false;

    }

    protected void handleResults(ResultWrapper resultWrapper) {
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
                    this.returnMsgHandler.sendMessage(msg);
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
    }

    protected void handleDisconnect() {
        Log.i(TAG, "Disconnected");
        String message = this.isConnected()
                ? this.gabrielClientActivity.getResources().getString(R.string.server_disconnected)
                : this.gabrielClientActivity.getResources().getString(R.string.could_not_connect);

        if (!shownError) {
            shownError = true;

            Message msg = Message.obtain();
            msg.what = NetworkProtocol.NETWORK_RET_FAILED;
            Bundle data = new Bundle();
            data.putString("message", message);
            msg.setData(data);
            this.returnMsgHandler.sendMessage(msg);
        }
    }
}
