package edu.cmu.cs.gabriel.network;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.util.function.Supplier;

import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.protocol.Protos.InputFrame;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.openrtist.Protos.Extras;

public class FrameSupplier implements Supplier<InputFrame> {
    private GabrielClientActivity gabrielClientActivity;

    public FrameSupplier(GabrielClientActivity gabrielClientActivity) {
        this.gabrielClientActivity = gabrielClientActivity;
    }

    private static InputFrame convertEngineInput(EngineInput engineInput) {
        byte[] frame = engineInput.getFrame();

//        Extras extras = Extras.newBuilder().setStyle(engineInput.getStyleType()).build();

        // extra includes the style type and the depth map
        Extras extras = Extras.newBuilder().setStyle(engineInput.getStyleType())
                .setStyleImage(Extras.BytesValue.newBuilder().setValue(ByteString.copyFrom(engineInput.getDepth_map())))
                .build();

        // TODO: Switch to this once MobilEdgeX supports protobuf-javalite:
        // fromClientBuilder.setEngineFields(Any.pack(engineFields));

        InputFrame inputFrame = InputFrame.newBuilder()
                .setPayloadType(PayloadType.IMAGE)
                .addPayloads(ByteString.copyFrom(frame))
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
