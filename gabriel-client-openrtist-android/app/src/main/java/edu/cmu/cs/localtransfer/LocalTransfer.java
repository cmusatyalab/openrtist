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
import java.nio.ByteBuffer;

/**
 * A class for running style transfer locally
 * with pytorch android.
 */
public class LocalTransfer {

    private Module mModule;
    private ByteBuffer mInputTensorBuffer;
    private Tensor mInputTensor;
    private Tensor mOutputTensor;
    private int model_input_width;
    private int model_input_height;

    public LocalTransfer(int model_input_width, int model_input_height) {
        this.model_input_width = model_input_width;
        this.model_input_height = model_input_height;

    }

    private String getAssetFilePath(Context context, String modelName)
            throws FileNotFoundException {
        File file = new File(context.getFilesDir(), modelName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        } else {
            throw new FileNotFoundException(String.format("%s does not exist or is empty."));
        }
    }

    /**
     * load model
     * @param context
     * @param modelName
     * @throws FileNotFoundException
     */
    public void load(Context context, String modelName) throws FileNotFoundException {
        // get model file path
        String moduleFileAbsoluteFilePath = getAssetFilePath(context, modelName);
        // load model
        mModule = Module.load(moduleFileAbsoluteFilePath);
    }

    /**
     * run style transfer on a single image
     * @param image
     * @return
     */
    public byte[] infer(byte[] image) {
        assert mModule != null;
        mInputTensorBuffer =
                Tensor.allocateByteBuffer(3 * this.model_input_width
                        * this.model_input_height);
        // batch 1
        mInputTensor = Tensor.fromBlob(mInputTensorBuffer, new long[]{1, 3, this.model_input_height,
                this.model_input_width});

        long moduleForwardStartTime = 0;
        long moduleForwardDuration = 0;
        moduleForwardStartTime = SystemClock.elapsedRealtime();
        mOutputTensor = mModule.forward(IValue.from(mInputTensor)).toTensor();
        moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime;
        Log.d(this.getClass().getName(), String.format("forward time: %f ms",
                moduleForwardDuration));

        return mOutputTensor.getDataAsByteArray();
}
