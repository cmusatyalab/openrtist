package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.client.comm.ErrorType;
import edu.cmu.cs.gabriel.client.comm.SendSupplierResult;
import edu.cmu.cs.gabriel.client.function.Consumer;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;
import edu.cmu.cs.openrtist.Protos.EngineFields;
import edu.cmu.cs.gabriel.client.comm.ServerCommCore;
import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.openrtist.R;

public abstract class BaseComm {
    private static String TAG = "OpenrtistComm";
    private static String FILTER_PASSED = "openrtist";

    ServerCommCore serverCommCore;
    Consumer<ResultWrapper> consumer;
    Consumer<ErrorType> onDisconnect;
    private boolean shownError;
    private Handler returnMsgHandler;
    private Activity activity;

    public BaseComm(final Activity activity, final Handler returnMsgHandler) {
        this.consumer = new Consumer<ResultWrapper>() {
            @Override
            public void accept(ResultWrapper resultWrapper) {
                if (resultWrapper.getResultsCount() != 1) {
                    Log.e(TAG, "Got " + resultWrapper.getResultsCount() + " results in output.");
                    return;
                }

                if (!resultWrapper.getFilterPassed().equals(FILTER_PASSED)) {
                    Log.e(TAG, "Got result that passed filter " +
                            resultWrapper.getFilterPassed());
                    return;
                }

                ResultWrapper.Result result = resultWrapper.getResults(0);
                try {
                    EngineFields ef = EngineFields.parseFrom(resultWrapper.getExtras().getValue());
                    if (Const.DISPLAY_REFERENCE && ef.hasStyleImage()) {
                        Bitmap refImage = null;
                        if (ef.getStyleImage().getValue().toByteArray().length > 0) {
                            refImage = BitmapFactory.decodeByteArray(
                                    ef.getStyleImage().getValue().toByteArray(), 0,
                                    ef.getStyleImage().getValue().toByteArray().length);
                            if (refImage == null)
                                Log.e(TAG, String.format("decodeByteArray returned null!"));

                        }

                        Message msg = Message.obtain();
                        msg.what = Const.REFERENCE_IMAGE;
                        msg.obj = refImage;
                        returnMsgHandler.sendMessage(msg);
                    }
                    if (!Const.STYLES_RETRIEVED) {
                        if (ef.getStyleListCount() > 0) {
                            Const.STYLES_RETRIEVED = true;
                            SortedMap<String, String> styles = new TreeMap<>(ef.getStyleListMap());
                            for (Map.Entry<String, String> entry : styles.entrySet()) {
                                Log.v(TAG, String.format("style: %s, desc: [%s]", entry.getKey(), entry.getValue()));
                                ((GabrielClientActivity) activity).getStyleDescriptions().add(entry.getValue().trim());
                                ((GabrielClientActivity) activity).getStyleIds().add(entry.getKey().trim());
                            }
                        }
                    }
                }  catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }

                if (result.getPayloadType() != PayloadType.IMAGE) {
                    Log.e(TAG, "Got result of type " + result.getPayloadType().name());
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

        this.onDisconnect = new Consumer<ErrorType>() {
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
                BaseComm.this.showErrorMessage(stringId);
            }
        };

        this.shownError = false;
        this.activity = activity;
        this.returnMsgHandler = returnMsgHandler;
    }

    public void showErrorMessage(int stringId) {
        if (this.shownError) {
            return;
        }

        BaseComm.this.shownError = true;
        Log.i(TAG, "Disconnected");
        Message msg = Message.obtain();
        msg.what = NetworkProtocol.NETWORK_RET_FAILED;
        Bundle data = new Bundle();
        data.putString("message", activity.getResources().getString(stringId));
        msg.setData(data);
        returnMsgHandler.sendMessage(msg);
    }

    public void sendSupplier(FrameSupplier frameSupplier) {
        if (!this.serverCommCore.isRunning()) {
            return;
        }

        SendSupplierResult sendSupplierResult = this.serverCommCore.sendSupplier(
                frameSupplier, FILTER_PASSED);
        if (sendSupplierResult == SendSupplierResult.ERROR_GETTING_TOKEN) {
            this.showErrorMessage(R.string.toekn_error);
        }
    }

    public boolean acceptsOpenrtist() {
        return this.serverCommCore.acceptsInputForFilter(FILTER_PASSED);
    }

    public void stop() {
        this.serverCommCore.stop();
    }
}
