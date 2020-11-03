package edu.cmu.cs.gabriel.network;

import android.util.Log;
import android.widget.ImageView;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.function.Consumer;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.camera.ImageViewUpdater;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.openrtist.Protos.Extras;

public class ResultConsumer implements Consumer<ResultWrapper> {
    private static final String TAG = "ResultConsumer";

    private final ImageViewUpdater referenceViewUpdater;
    private final Consumer<ByteString> imageViewUpdater;
    private final GabrielClientActivity gabrielClientActivity;

    public ResultConsumer(
            ImageView referenceView, Consumer<ByteString> imageViewUpdater,
            GabrielClientActivity gabrielClientActivity) {
        this.referenceViewUpdater = new ImageViewUpdater(referenceView);
        this.imageViewUpdater = imageViewUpdater;
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
                if (extras.getStyleImage().getValue().toByteArray().length > 0) {
                    this.referenceViewUpdater.accept(extras.getStyleImage().getValue());
                }
            }

            if (!Const.STYLES_RETRIEVED && (extras.getStyleListCount() > 0)) {
                Const.STYLES_RETRIEVED = true;
                this.gabrielClientActivity.addStyles(extras.getStyleListMap().entrySet());
            }
        }  catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "Protobuf Error", e);
        }

        if (result.getPayloadType() != PayloadType.IMAGE) {
            Log.e(TAG, "Got result of type " + result.getPayloadType().name());
            return;
        }

        this.imageViewUpdater.accept(result.getPayload());
        this.gabrielClientActivity.addFrameProcessed();
    }
}

