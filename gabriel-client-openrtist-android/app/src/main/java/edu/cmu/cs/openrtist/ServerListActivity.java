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

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.util.Patterns;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.content.Context;
import android.hardware.camera2.CameraManager;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;

import edu.cmu.cs.gabriel.Const;


public class ServerListActivity extends AppCompatActivity  {
    ListView listView;
    EditText serverName;
    EditText serverAddress;
    ImageView add;
    ArrayList<Server> ItemModelList;
    ServerListAdapter serverListAdapter;
    Switch useFrontCamera = null;
    Switch stereoEnabled = null;
    Switch showReference = null;
    Switch iterateStyles = null;
    SeekBar seekBar = null;
    TextView intervalLabel = null;
    Switch showRecorder = null;
    Switch showFPS = null;
    CameraManager camMan = null;
    private SharedPreferences mSharedPreferences;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 23;


    //activity menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cloudlet_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermission();
        
        setContentView(R.layout.activity_serverlist);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        listView = (ListView) findViewById(R.id.listServers);
        serverName = (EditText) findViewById(R.id.addServerName);
        serverAddress = (EditText) findViewById(R.id.addServerAddress);
        add = (ImageView) findViewById(R.id.imgViewAdd);
        ItemModelList = new ArrayList<Server>();
        serverListAdapter = new ServerListAdapter(getApplicationContext(), ItemModelList);
        listView.setAdapter(serverListAdapter);
        mSharedPreferences=getSharedPreferences(getString(R.string.shared_preference_file_key),
                MODE_PRIVATE);

        useFrontCamera = (Switch) findViewById(R.id.toggleCamera);
        stereoEnabled = (Switch) findViewById(R.id.toggleStereo);
        showReference = (Switch) findViewById(R.id.showReference);
        iterateStyles = (Switch) findViewById(R.id.iterateStyles);
        showRecorder = (Switch) findViewById(R.id.showRecorder);
        showFPS = (Switch) findViewById(R.id.showFPS);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        intervalLabel = (TextView) findViewById(R.id.intervalLabel);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean tracking = false;
            public void onProgressChanged(SeekBar bar, int progress, boolean what) {
                if(progress <= 0 )
                    bar.setProgress(1);

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                tracking = true;

            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                tracking = false;
                Toast.makeText(getApplicationContext(), getString(R.string.interval_set_toast, seekBar.getProgress()*5),
                        Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putInt("option:interval", seekBar.getProgress());
                editor.commit();
                Const.ITERATE_INTERVAL = 5 * seekBar.getProgress();
            }
        });
        seekBar.setProgress(mSharedPreferences.getInt("option:interval", 2));
        Const.ITERATE_INTERVAL = 5 * seekBar.getProgress();

        useFrontCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.FRONT_CAMERA_ENABLED = isChecked;
                if(isChecked)
                    stereoEnabled.setChecked(false);
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean("option:frontcam",Const.FRONT_CAMERA_ENABLED);
                editor.commit();
            }
        });
        useFrontCamera.setChecked(mSharedPreferences.getBoolean("option:frontcam", false));

        stereoEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.STEREO_ENABLED = isChecked;
                if(isChecked) {
                    showReference.setChecked(false);
                    useFrontCamera.setChecked(false);
                    showRecorder.setChecked(false);
                }

                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean("option:stereo",Const.STEREO_ENABLED);
                editor.commit();
            }
        });
        stereoEnabled.setChecked(mSharedPreferences.getBoolean("option:stereo", false));

        showReference.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.DISPLAY_REFERENCE = isChecked;
                if(isChecked)
                    stereoEnabled.setChecked(false);
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean("option:showref",isChecked);
                editor.commit();
            }
        });
        showReference.setChecked(mSharedPreferences.getBoolean("option:showref", false));

        showRecorder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.SHOW_RECORDER = isChecked;
                if(isChecked)
                    stereoEnabled.setChecked(false);
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean("option:showrec",isChecked);
                editor.commit();
            }
        });
        showRecorder.setChecked(mSharedPreferences.getBoolean("option:showrec", false));

        showFPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.SHOW_FPS = isChecked;
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean("option:showfps",isChecked);
                editor.commit();
            }
        });
        showFPS.setChecked(mSharedPreferences.getBoolean("option:showfps", false));

        iterateStyles.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.ITERATE_STYLES = isChecked;
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean("option:iterate",Const.ITERATE_STYLES);
                editor.commit();
                if(isChecked) {
                    seekBar.setVisibility(View.VISIBLE);
                    intervalLabel.setVisibility(View.VISIBLE);
                }
                else {
                    seekBar.setVisibility(View.GONE);
                    intervalLabel.setVisibility(View.GONE);
                }
            }
        });
        iterateStyles.setChecked(mSharedPreferences.getBoolean("option:iterate", false));
        camMan = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        initServerList();
    }

    private void requestPermission() {
        String permissions[] = {Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            ActivityCompat.requestPermissions(this,
                    permissions,
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }


    private void initServerList(){
        Map<String, ?> prefs = mSharedPreferences.getAll();
        for (Map.Entry<String,?> pref : prefs.entrySet())
            if(pref.getKey().startsWith("server:")) {
                Server s = new Server(pref.getKey().substring("server:".length()), pref.getValue().toString());
                ItemModelList.add(s);
                serverListAdapter.notifyDataSetChanged();
            }

        if (prefs.isEmpty()) {
            // Add demo server if there are no other servers present
            Server s = new Server(getString(R.string.demo_server), getString(R.string.demo_dns));
            ItemModelList.add(s);
            serverListAdapter.notifyDataSetChanged();
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString("server:".concat(getString(R.string.demo_server)),getString(R.string.demo_dns));
            editor.commit();
        }

    }
    /**
     * This is used to check the given URL is valid or not.
     * @param url
     * @return true if url is valid, false otherwise.
     */
    private boolean isValidUrl(String url) {
        Pattern p = Patterns.WEB_URL;
        Matcher m = p.matcher(url.toLowerCase());
        return m.matches();
    }

    public void addValue(View v) {
        String name = serverName.getText().toString();
        String endpoint = serverAddress.getText().toString();
        if (name.isEmpty() || endpoint.isEmpty()) {
            Toast.makeText(getApplicationContext(), R.string.error_empty ,
                    Toast.LENGTH_SHORT).show();
        } else if(!isValidUrl(endpoint)) {
            Toast.makeText(getApplicationContext(), R.string.error_invalidURI,
                    Toast.LENGTH_SHORT).show();
        }  else if(mSharedPreferences.contains("server:".concat(name))) {
            Toast.makeText(getApplicationContext(), R.string.error_exists,
                Toast.LENGTH_SHORT).show();
        } else {
            Server s = new Server(name, endpoint);
            ItemModelList.add(s);
            serverListAdapter.notifyDataSetChanged();
            serverName.setText("");
            serverAddress.setText("");
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString("server:".concat(name),endpoint);
            editor.commit();
            findViewById(R.id.textOptions).requestFocus();
        }
    }




}
