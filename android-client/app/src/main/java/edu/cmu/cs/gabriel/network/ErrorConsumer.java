package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.function.Consumer;

import edu.cmu.cs.gabriel.client.results.ErrorType;
import edu.cmu.cs.openrtist.R;

public class ErrorConsumer implements Consumer<ErrorType> {
    private static final String TAG = "ResultConsumer";

    private final Handler returnMsgHandler;
    private final Activity activity;
    private boolean shownError;

    public ErrorConsumer(Handler returnMsgHandler, Activity activity) {
        this.returnMsgHandler = returnMsgHandler;
        this.activity = activity;
        this.shownError = false;
    }

    @Override
    public void accept(ErrorType errorType) {
        int stringId;
        switch (errorType) {
            case SERVER_ERROR:
                stringId = R.string.server_error;
                break;
            case SERVER_DISCONNECTED:
                stringId = R.string.server_disconnected;
                break;
            case COULD_NOT_CONNECT:
                stringId = R.string.could_not_connect;
                break;
            default:
                stringId = R.string.unspecified_error;
        }
        this.showErrorMessage(stringId);
    }

    public void showErrorMessage(int stringId) {
        if (this.shownError) {
            return;
        }

        this.shownError = true;
        Log.i(TAG, "Disconnected");
        Message msg = Message.obtain();
        msg.what = NetworkProtocol.NETWORK_RET_FAILED;
        Bundle data = new Bundle();
        data.putString("message", this.activity.getResources().getString(stringId));
        msg.setData(data);
        this.returnMsgHandler.sendMessage(msg);
    }
}
