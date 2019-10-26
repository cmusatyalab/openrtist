// Copyright 2018 Carnegie Mellon University
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package edu.cmu.cs.localtransfer;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;

/**
 * A class for running style transfer locally
 * with pytorch android.
 */
public class LocalTransfer {

    private Module mModule;
    private FloatBuffer mInputTensorBuffer;
    private Tensor mInputTensor;
    private Tensor mOutputTensor;
    private int model_input_width;
    private int model_input_height;

    public LocalTransfer(int model_input_width, int model_input_height) {
        mModule=null;
        this.model_input_width = model_input_width;
        this.model_input_height = model_input_height;
        this.mInputTensorBuffer = Tensor.allocateFloatBuffer(3 * this.model_input_width
                * this.model_input_height);

    }

    private String getAssetFilePath(Context context, String modelName)
            throws FileNotFoundException {
        File file = new File(context.getFilesDir(), modelName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        // android asset files are not unzipped from apk by default
        // need to read it out in order to treat it as a normal file
        try (InputStream is = context.getAssets().open(modelName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(this.getClass().getName(), String.format("Error processing asset: %s",
                    modelName));
        }
        Log.e(this.getClass().getName(), String.format("%s does not exist or is empty.",
                file.getAbsolutePath()));
        throw new FileNotFoundException(String.format("%s does not exist or is empty.",
                file.getAbsolutePath()));
    }

    /**
     * load model
     * @param context
     * @param modelName
     * @throws FileNotFoundException
     */
    synchronized public void load(Context context, String modelName) throws FileNotFoundException {
        // get model file path
        String moduleFileAbsoluteFilePath = getAssetFilePath(context, modelName);
        // load model
        mModule = Module.load(moduleFileAbsoluteFilePath);
        Log.d(this.getClass().getName(), String.format("loaded style: %s", modelName));
    }

    /**
     * run style transfer on a single image
     * @param image
     * @return
     */
    synchronized public int[] infer(float[] image) {
        // change data layout from <height, width, channel> to <channel, height, width>
        long st = SystemClock.elapsedRealtime();
        float[] imageCHW = Utils.dataLayoutHWCtoCHW(image);
        Log.d(this.getClass().getName(), String.format("input data " +
                        "preprocessing (HWC to CHW) takes %d ms",
                SystemClock.elapsedRealtime() - st));

        // inference
        st = SystemClock.elapsedRealtime();
        this.mInputTensor = Tensor.fromBlob(imageCHW,
                new long[]{1, 3, this.model_input_height, this.model_input_width});
        mOutputTensor = mModule.forward(IValue.from(mInputTensor)).toTensor();
        Log.d(this.getClass().getName(), String.format("model inference time: %d ms",
                SystemClock.elapsedRealtime() - st));

        // output. change data layout and type
        st = SystemClock.elapsedRealtime();
        float[] output = mOutputTensor.getDataAsFloatArray();
        float[] outputHWC = Utils.dataLayoutCHWtoHWC(output);
        int[] converted = Utils.convertFloatArrayToImageIntArray(outputHWC);
        Log.d(this.getClass().getName(), String.format("output data " +
                        "postprocessing (CHWToHWC, float rgb to int argb) takes %d ms",
                SystemClock.elapsedRealtime() - st));

        return converted;
    }
}
