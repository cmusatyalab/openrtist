/*
 * Copyright 2023 Carnegie Mellon University
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.cmu.cs.sinfonia;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class SinfoniaActivity extends AppCompatActivity {
    private static final String TAG = "OpenRTiST/SinfoniaActivity";
    private static final int REQUEST_CODE_PERMISSION = 44;
    private SinfoniaService sinfoniaService;
    private boolean isServiceBound = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "onServiceConnected");
            SinfoniaService.MyBinder binder = (SinfoniaService.MyBinder) iBinder;
            sinfoniaService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "onServiceDisconnected");
            sinfoniaService = null;
            isServiceBound = false;
        }

        @Override
        public void onBindingDied(ComponentName componentName) {
            Log.i(TAG, "onBindingDied");
            isServiceBound = false;
        }

        @Override
        public void onNullBinding(ComponentName componentName) {
            Log.i(TAG, "onNullBinding");
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        requestPermissions();

        Intent intent = new Intent(this, SinfoniaService.class)
                .setAction(SinfoniaService.ACTION_BIND);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SinfoniaFragment())
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        if (isServiceBound) unbindService(serviceConnection);
        super.onDestroy();
    }

    public SinfoniaService getSinfoniaService() {
        return sinfoniaService;
    }

//    public void requestPermissions() {
//        SinfoniaService.Companion.requestPermissions(this, REQUEST_CODE_PERMISSION);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(
//            int requestCode,
//            @NonNull String[] permissions,
//            @NonNull int[] grantResults
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_CODE_PERMISSION) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                return;
//            }
//
//        }
//    }
}
