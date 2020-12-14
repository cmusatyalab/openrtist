package edu.cmu.cs.gabriel.network;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.openrtist.ar.GabrielClientActivity;
import edu.cmu.cs.gabriel.protocol.Protos.InputFrame;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.openrtist.Protos.Extras;

public class FrameSupplier implements Supplier<InputFrame> {
    private GabrielClientActivity gabrielClientActivity;

    public FrameSupplier(GabrielClientActivity gabrielClientActivity) {
        this.gabrielClientActivity = gabrielClientActivity;
    }

    private static InputFrame convertEngineInput(EngineInput engineInput) {
        byte[] imageBytes = engineInput.getFrame();
        byte[] depthBytes = engineInput.getDepth_map();
        int depth_threshold = engineInput.getDepthThreshold();
        Extras extras;

        // Log.v("CHECKPOINT SUCCESS", "convertEngineInput");

        // extra includes the style type, the depth map, and the depth threshold
        extras = Extras.newBuilder().setStyle(engineInput.getStyleType())
                .setDepthMap(Extras.BytesValue.newBuilder().setValue(ByteString.copyFrom(depthBytes)))
                .setDepthThreshold(depth_threshold)
                .build();

        // TODO: Switch to this once MobilEdgeX supports protobuf-javalite:
        // fromClientBuilder.setEngineFields(Any.pack(engineFields));

        InputFrame inputFrame = InputFrame.newBuilder()
                .setPayloadType(PayloadType.IMAGE)
                .addPayloads(ByteString.copyFrom(imageBytes))
                .setExtras(FrameSupplier.pack(extras))
                .build();
        return inputFrame;
    }

    public InputFrame get() {
        EngineInput engineInput = this.gabrielClientActivity.getEngineInput();
        if (engineInput == null) {
            return null;
        }

        return FrameSupplier.convertEngineInput(engineInput);
    }

    // Based on
    // https://github.com/protocolbuffers/protobuf/blob/master/src/google/protobuf/compiler/java/java_message.cc#L1387
    private static Any pack(Extras extras) {
        return Any.newBuilder()
                .setTypeUrl("type.googleapis.com/openrtist.Extras")
                .setValue(extras.toByteString())
                .build();
    }
}

