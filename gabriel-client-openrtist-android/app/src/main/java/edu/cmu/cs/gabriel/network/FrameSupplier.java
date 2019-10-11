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

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.protocol.Protos.FromClient;
import edu.cmu.cs.gabriel.protocol.Protos.PayloadType;
import edu.cmu.cs.openrtist.Protos.EngineFields;
import edu.cmu.cs.gabriel.client.Supplier;

public class FrameSupplier implements Supplier<FromClient.Builder> {

    private static String ENGINE_NAME = "openrtist";

    private GabrielClientActivity gabrielClientActivity;

    public FrameSupplier(GabrielClientActivity gabrielClientActivity) {
        this.gabrielClientActivity = gabrielClientActivity;
    }

    private byte[] createFrameData(EngineInput engineInput) {
        Camera.Size cameraImageSize = engineInput.parameters.getPreviewSize();
        YuvImage image = new YuvImage(engineInput.frame, engineInput.parameters.getPreviewFormat(),
                cameraImageSize.width, cameraImageSize.height, null);
        ByteArrayOutputStream tmpBuffer = new ByteArrayOutputStream();
        // chooses quality 67 and it roughly matches quality 5 in avconv
        image.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()),
                67, tmpBuffer);
        if (Const.USING_FRONT_CAMERA) {
            byte[] newFrame = tmpBuffer.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(newFrame, 0, newFrame.length);
            ByteArrayOutputStream rotatedStream = new ByteArrayOutputStream();
            Matrix matrix = new Matrix();
            if (Const.FRONT_ROTATION) {
                matrix.postRotate(180);
            }
            matrix.postScale(-1, 1);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, false);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 67, rotatedStream);
            //this.frameBuffer = tmpBuffer.toByteArray();
            return rotatedStream.toByteArray();
        } else {
            return tmpBuffer.toByteArray();
        }
    }

    private FromClient.Builder convertEngineInput(EngineInput engineInput) {
        byte[] frame = createFrameData(engineInput);

        FromClient.Builder fromClientBuilder = FromClient.newBuilder();
        fromClientBuilder.setPayloadType(PayloadType.IMAGE);
        fromClientBuilder.setEngineName(ENGINE_NAME);
        fromClientBuilder.setPayload(ByteString.copyFrom(frame));

        EngineFields.Builder engineFieldsBuilder = EngineFields.newBuilder();
        engineFieldsBuilder.setStyle(engineInput.style_type);
        EngineFields engineFields = engineFieldsBuilder.build();

        fromClientBuilder.setEngineFields(Any.pack(engineFields));
        return fromClientBuilder;
    }

    public FromClient.Builder get() {
        EngineInput engineInput = this.gabrielClientActivity.getEngineInput();
        if (engineInput != null) {
            return this.convertEngineInput(engineInput);
        }

        return null;
    }
}
