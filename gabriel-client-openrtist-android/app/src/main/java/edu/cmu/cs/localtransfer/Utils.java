package edu.cmu.cs.localtransfer;

import android.hardware.Camera;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

public class Utils {
    // Convert to RGB using Intrinsic render script
    public static float[] convertYuvToRgb(RenderScript rs, byte[] data,
                                             Camera.Size imageSize) {

        int imageWidth = imageSize.width ;
        int imageHeight = imageSize.height ;

        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs,
                Element.RGBA_8888(rs));
//                Element.RGB_888(rs));

        // Create the input allocation  memory for Renderscript to work with
        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs))
                .setX(imageWidth)
                .setY(imageHeight)
                .setYuvFormat(android.graphics.ImageFormat.NV21);

        Allocation aIn = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
        // Set the YUV frame data into the input allocation
        aIn.copyFrom(data);

        // Create the output allocation
        Type.Builder rgbType = new Type.Builder(rs, Element.RGBA_8888(rs))
                .setX(imageWidth)
                .setY(imageHeight);

        Allocation aOut = Allocation.createTyped(rs, rgbType.create(), Allocation.USAGE_SCRIPT);

        yuvToRgbIntrinsic.setInput(aIn);
        // Run the script for every pixel on the input allocation and put the result in aOut
        yuvToRgbIntrinsic.forEach(aOut);

        // copy to rgbBuffer
        byte[] rgbaBuffer = new byte[4 * imageHeight * imageWidth];
        aOut.copyTo(rgbaBuffer);

        // rgba to rgb
        float[] rgbBuffer = new float[3 * imageHeight * imageWidth];
        int i=0, j = 0;
        while (i<rgbaBuffer.length)
        {
            byte r = rgbaBuffer[i++];
            rgbBuffer[j++] = (float) (int) (r & 0xFF); // cast byte to unsigned first and float
            byte g = rgbaBuffer[i++];
            rgbBuffer[j++] = (float) (int) (g & 0xFF);
            byte b = rgbaBuffer[i++];
            rgbBuffer[j++] = (float) (int) (b & 0xFF);
            // remove a
            byte a = rgbaBuffer[i++];
        }
        return rgbBuffer;
    }
}
