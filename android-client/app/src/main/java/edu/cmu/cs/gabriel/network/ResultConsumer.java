package edu.cmu.cs.gabriel.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.openrtist.Protos.Extras;

public class ResultConsumer implements Consumer<ResultWrapper> {
    private static final String TAG = "ResultConsumer";

    private final Handler returnMsgHandler;
    private final GabrielClientActivity gabrielClientActivity;

    public ResultConsumer(Handler returnMsgHandler, GabrielClientActivity gabrielClientActivity) {
        this.returnMsgHandler = returnMsgHandler;
        this.gabrielClientActivity = gabrielClientActivity;
    }

    @Override
    public void accept(ResultWrapper resultWrapper) {
        if (resultWrapper.getResultsCount() != 1) {
            Log.e(TAG, "Got " + resultWrapper.getResultsCount() + " results in output.");
            return;
        }

        ResultWrapper.Result result = resultWrapper.getResults(0);
        try {
            Extras extras = Extras.parseFrom(resultWrapper.getExtras().getValue());
            if (Const.DISPLAY_REFERENCE && extras.hasStyleImage()) {
                Bitmap refImage = null;
                if (extras.getStyleImage().getValue().toByteArray().length > 0) {
                    refImage = BitmapFactory.decodeByteArray(
                            extras.getStyleImage().getValue().toByteArray(), 0,
                            extras.getStyleImage().getValue().toByteArray().length);
                    if (refImage == null)
                        Log.e(TAG, "decodeByteArray returned null!");

                }

                Message msg = Message.obtain();
                msg.what = Const.REFERENCE_IMAGE;
                msg.obj = refImage;
                returnMsgHandler.sendMessage(msg);
            }
            if (!Const.STYLES_RETRIEVED && (extras.getStyleListCount() > 0)) {
                Const.STYLES_RETRIEVED = true;
                Map<String, String> styles = new TreeMap<>(extras.getStyleListMap());
                for (Map.Entry<String, String> entry : styles.entrySet()) {
                    Log.v(TAG, "style: " + entry.getKey() + ", desc: " + entry.getValue());
                    gabrielClientActivity.getStyleDescriptions().add(entry.getValue().trim());
                    gabrielClientActivity.getStyleIds().add(entry.getKey().trim());
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
}

