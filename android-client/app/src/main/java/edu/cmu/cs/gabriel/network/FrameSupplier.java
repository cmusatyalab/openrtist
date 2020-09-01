package edu.cmu.cs.gabriel.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.util.function.Supplier;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.protocol.Protos.InputFrame;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.openrtist.Protos.Extras;

public class FrameSupplier implements Supplier<InputFrame> {
    private GabrielClientActivity gabrielClientActivity;

    public FrameSupplier(GabrielClientActivity gabrielClientActivity) {
        this.gabrielClientActivity = gabrielClientActivity;
    }

//    private static byte[] createFrameData(EngineInput engineInput) {
//        Camera.Size cameraImageSize = engineInput.getParameters().getPreviewSize();
//        YuvImage image = engineInput.getImage();
//        ByteArrayOutputStream tmpBuffer = new ByteArrayOutputStream();
//        // chooses quality 67 and it roughly matches quality 5 in avconv
//        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()),
//                67, tmpBuffer);
//        if (Const.USING_FRONT_CAMERA) {
//            byte[] newFrame = tmpBuffer.toByteArray();
//            Bitmap bitmap = BitmapFactory.decodeByteArray(newFrame, 0, newFrame.length);
//            ByteArrayOutputStream rotatedStream = new ByteArrayOutputStream();
//            Matrix matrix = new Matrix();
//            if (Const.FRONT_ROTATION) {
//                matrix.postRotate(180);
//            }
//            matrix.postScale(-1, 1);
//            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
//                    matrix, false);
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 67, rotatedStream);
//            return rotatedStream.toByteArray();
//        } else {
//            return tmpBuffer.toByteArray();
//        }
//    }

    private static InputFrame convertEngineInput(EngineInput engineInput) {
//        byte[] frame = FrameSupplier.createFrameData(engineInput);
        byte[] frame = engineInput.getFrame();

        Extras extras = Extras.newBuilder().setStyle(engineInput.getStyleType()).build();
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
