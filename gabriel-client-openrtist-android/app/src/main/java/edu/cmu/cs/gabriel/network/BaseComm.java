package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.graphics.ImageDecoder;
import android.graphics.ImageDecoder.Source;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.client.function.Consumer;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;
import edu.cmu.cs.openrtist.Protos.EngineFields;
import edu.cmu.cs.gabriel.client.comm.ServerCommCore;
import edu.cmu.cs.openrtist.R;
import edu.cmu.cs.gabriel.Const;

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
                try {
                    EngineFields ef = EngineFields.parseFrom(resultWrapper.getEngineFields().getValue());
                    if (Const.DISPLAY_REFERENCE && ef.hasStyleImage()) {
                        Bitmap refImage = null;
                        if (ef.getStyleImage().getValue().toByteArray().length > 0) {
                            refImage = BitmapFactory.decodeByteArray(ef.getStyleImage().getValue().toByteArray(), 0, ef.getStyleImage().getValue().toByteArray().length);
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
                                ((GabrielClientActivity) activity).getStyleDescriptions().add(entry.getValue());
                                ((GabrielClientActivity) activity).getStyleIds().add(entry.getKey());
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
