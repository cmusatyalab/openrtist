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

package edu.cmu.cs.openrtist;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import android.app.AlertDialog;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.GabrielConfigurationAsyncTask;
import edu.cmu.cs.utils.UIUtils;


import static android.content.Context.MODE_PRIVATE;
import static edu.cmu.cs.CustomExceptions.CustomExceptions.notifyError;
import static edu.cmu.cs.utils.NetworkUtils.checkOnline;
import static edu.cmu.cs.utils.NetworkUtils.isOnline;


public class CloudletDemoActivity extends AppCompatActivity implements
        GabrielConfigurationAsyncTask.AsyncResponse{



    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 23;


    private Toolbar toolbar;
    private ViewPager viewPager;
    private static final String TAG = "cloudletDemoActivity";

    private static final int DLG_EXAMPLE1 = 0;
    private static final int TEXT_ID = 999;
    public String inputDialogResult;
    private CloudletFragment childFragment;
    private EditText dialogInputTextEdit;

    public SharedPreferences mSharedPreferences = null;

    private byte[] asyncResponseExtra = null;
    public String currentServerIp = null;
    private Activity mActivity = null;

    //fix the bug for load_state, and onresume race for sending
    public boolean onResumeFromLoadState = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestPersmission();

        if(false){
        //if (!isOnline(this)) {
            notifyError(Const.CONNECTIVITY_NOT_AVAILABLE, true, this);
        } else {
            setContentView(R.layout.activity_cloudlet_demo);

            toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            childFragment =
                    (CloudletFragment) getSupportFragmentManager().findFragmentById(R.id.demofragment);
            mSharedPreferences = getSharedPreferences(getString(R.string.shared_preference_file_key),
                    MODE_PRIVATE);
        }
        Log.d(TAG, "on create");
//        mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addApi(Drive.API)
//                .addScope(Drive.SCOPE_FILE)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .build();
        mActivity = this;
    }

    private void requestPersmission() {
        Log.d(TAG, "request permission");
        String permissions[] = {Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            ActivityCompat.requestPermissions(this,
                    permissions,
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "camera permission approved");
                } else {
                    Log.d(TAG, "camera permission denied");
                }
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "on start");
//        mGoogleApiClient.connect();
    }

    //activity menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cloudlet_demo, menu);
        return true;
    }

//    @Override
//    public void onDialogEditTextResult(String result) {
//        inputDialogResult=result;
//    }

    /**
     * callback when gabriel configuration async task finished
     * @param action
     * @param success
     * @param extra
     */
    @Override
    public void onGabrielConfigurationAsyncTaskFinish(String action,
                                                      boolean success,
                                                      byte[] extra) {

        if (action.equals(Const.GABRIEL_CONFIGURATION_RESET_STATE)){
            if (!success){
                String errorMsg=
                        "No Gabriel Server Found. \n" +
                                "Please define a valid Gabriel Server IP ";

                notifyError(errorMsg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                },this);
            }
        }
}

    public void actionUploadStateByteArray(byte[] stateData){
        if (stateData!=null){
            onResumeFromLoadState=true;
            //sendOpenFaceLoadStateRequest(stateData);
        } else {
            Log.e(TAG, "wrong file format");
            Toast.makeText(this, "wrong file format", Toast.LENGTH_LONG).show();
        }
    }



    public void sendOpenFaceResetRequest(String remoteIP) {
        boolean online = isOnline(this);
        if (online){
            GabrielConfigurationAsyncTask task =
                    new GabrielConfigurationAsyncTask(this,
                            remoteIP,
                            Const.VIDEO_STREAM_PORT,
                            Const.RESULT_RECEIVING_PORT,
                            Const.GABRIEL_CONFIGURATION_RESET_STATE,
                            this);
            task.execute();
            Log.d(TAG, "send reset openface server request to " + currentServerIp);
        } else {
            notifyError(Const.CONNECTIVITY_NOT_AVAILABLE, false, this);
        }
    }

    /**
     * get all avaiable servers names
     * @return
     */
    private CharSequence[] getAllServerNames(){
        String[] dictNames=getResources().getStringArray(R.array.shared_preference_ip_dict_names);
        List<String> allNames=new ArrayList<String>();
        for (int idx=0; idx<dictNames.length;idx++){
            String sharedPreferenceIpDictName=dictNames[idx];
            Set<String> existingNames =
                    mSharedPreferences.getStringSet(sharedPreferenceIpDictName,
                            new HashSet<String>());
            String prefix=getResources().
                    getStringArray(R.array.add_ip_places_spinner_array)[idx]+
                    SelectServerAlertDialog.IP_NAME_PREFIX_DELIMITER;
            for (String name:existingNames){
                allNames.add(prefix+name);
            }
        }
        CharSequence[] result = allNames.toArray(new CharSequence[allNames.size()]);
        return result;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.manage_servers:
                Intent i = new Intent(this, IPSettingActivity.class);
                startActivity(i);
                return true;
            case R.id.about:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.about_message)
                        .setTitle(R.string.about_title);
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            default:
                return false;
        }
    }

//            case R.id.setting_copy_server_state:
//                //TODO: alertdialog let user select which server to copy from
//                AlertDialog dg =SelectServerAlertDialog.createDialog(
//                        mActivity,
//                        "Pick a Server",
//                        getAllServerNames(),
//                        launchCopyStateAsyncTaskAction,
//                        SelectServerAlertDialog.cancelAction,
//                        true);
//                dg.show();
//                return true;

//            case R.id.setting_load_state:
//                return actionUploadStateFromLocalFile();




    @Override
    protected void onResume() {
        Log.i(TAG, "on resume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "on pause");

        super.onPause();
    }

}


