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
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.content.Context;
import android.hardware.camera2.CameraManager;

import com.mobiledgex.matchingengine.MatchingEngine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;
import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;


public class ServerListActivity extends AppCompatActivity  {
    ListView listView;
    EditText serverName;
    EditText serverAddress;
    ImageView add;
    ArrayList<Server> ItemModelList;
    ServerListAdapter serverListAdapter;
    CameraManager camMan = null;
    private SharedPreferences mSharedPreferences;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 23;
    private static final String TAG = "ServerListActivity";

    private MatchingEngine matchingEngine;
    private String statusText = null;
    private AppClient.FindCloudletReply mClosestCloudlet;
    private String host;
    private int port;
    private String carrierName;
    private String appName;
    private String devName;
    private String appVersion;

    //activity menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            case R.id.settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //intent.putExtra("", faceTable);
                this.startActivity(intent);
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
        mSharedPreferences=PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> m = mSharedPreferences.getAll();
        for(Map.Entry<String,?> entry : m.entrySet()){
            Log.d("SharedPreferences",entry.getKey() + ": " +
                    entry.getValue().toString());
            Const.loadPref(this.getApplicationContext(), entry.getKey(), entry.getValue());

        }
        camMan = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        initServerList();
    }

    private void requestPermission() {
        String permissions[] = {Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
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
        //Always add MobiledgeX server so that it refreshes each time the serverlist activity is instantiated
        //but we don't need to persist it in the prefs as a result
        try {
            if(registerClient()) {
                // Now that we are registered, let's find the closest cloudlet
                findCloudlet();
            }

            // Add mex server if there are no other servers present
            Server s = new Server(String.format("%1$s (%2$s, %3$s)",getString(R.string.mex_server), mClosestCloudlet.getCloudletLocation().getLatitude(),mClosestCloudlet.getCloudletLocation().getLongitude()), mClosestCloudlet.getFqdn());
            ItemModelList.add(s);
            serverListAdapter.notifyDataSetChanged();
        } catch (ExecutionException | InterruptedException | io.grpc.StatusRuntimeException e) {
            e.printStackTrace();
            statusText = "MobiledgeX Registration Failed. Exception="+e.getLocalizedMessage();
            Log.e(TAG, statusText);
            Toast.makeText(getApplicationContext(), statusText,
                    Toast.LENGTH_LONG).show();
        }

    }

    private boolean registerClient() throws ExecutionException, InterruptedException, io.grpc.StatusRuntimeException {
        // NOTICE: In a real app, these values would be determined by the SDK, but we are reusing
        // an existing app so we don't have to create new app provisioning data for this workshop.
        appName = Const.MEX_APP;
        devName = Const.MEX_DEV;
        carrierName = Const.MEX_CARRIER;
        appVersion = Const.MEX_TAG;

        //NOTICE: A real app would request permission to enable this.
        MatchingEngine.setMatchingEngineLocationAllowed(true);

        matchingEngine = new MatchingEngine( this );
        matchingEngine.setNetworkSwitchingEnabled( false ); // Stick with wifi for workshop.
        host = Const.MEX_DME_URL; // Override host.
        port = matchingEngine .getPort(); // Keep same port.
        AppClient.RegisterClientRequest registerClientRequest = matchingEngine .createRegisterClientRequest( this ,
                devName , appName , appVersion , carrierName , null );
        AppClient.RegisterClientReply registerStatus = matchingEngine .registerClient(registerClientRequest, host, port,10000 );

        if(matchingEngine == null) {
            statusText = "registerClient call is not successfully coded. Search for TODO in code.";
            Log.e(TAG, statusText);
            Toast.makeText(getApplicationContext(), statusText,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        Log.i(TAG, "registerClientRequest="+registerClientRequest);
        Log.i(TAG, "registerStatus.getStatus()="+registerStatus.getStatus());

        if (registerStatus.getStatus() != AppClient.ReplyStatus.RS_SUCCESS) {
            statusText = "MobiledgeX Registration Failed. Error: " + registerStatus.getStatus();
            Log.e(TAG, statusText);
            Toast.makeText(getApplicationContext(), statusText,
                    Toast.LENGTH_LONG).show();
            return false;
        }

        Log.i(TAG, "SessionCookie:" + registerStatus.getSessionCookie());
        Log.i(TAG,"Client registered with MobiledgeX DME");
        // Populate app details.
        Log.i(TAG,carrierName);
        Log.i(TAG, appName);

        return true;
    }

    private double[] getGPS() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);

        Location l = null;
        double[] gps = new double[2];
        //default to CMU Campus
        gps[0] = 40.0;
        gps[1] = -80.0;
        try {
            for (int i = providers.size() - 1; i >= 0; i--) {
                l = lm.getLastKnownLocation(providers.get(i));
                if (l != null) break;
            }

            if (l != null) {
                gps[0] = l.getLatitude();
                gps[1] = l.getLongitude();
            }
        } catch(SecurityException e)  {

            gps[0] = 40.0;
            gps[1] = -80.0;
        }

        return gps;
    }

    public boolean findCloudlet() throws ExecutionException, InterruptedException {
        //(Blocking call, or use findCloudletFuture):
        Location location = new Location("MEX");
        double[] gps = getGPS();
        Log.i(TAG, String.format("GPS - Lat: %.6f Long: %.6f", gps[0], gps[1]));
        location.setLatitude(gps[0]);
        location.setLongitude(gps[1]);

        AppClient.FindCloudletRequest findCloudletRequest= matchingEngine .createFindCloudletRequest ( this, carrierName, location);
        mClosestCloudlet = matchingEngine .findCloudlet(findCloudletRequest, host, port, 10000 );

        Log.i(TAG, "mClosestCloudlet="+mClosestCloudlet.getFqdn());
        if(mClosestCloudlet == null) {
            statusText = "findCloudlet call is not successfully coded. Search for TODO in code.";
            Log.e(TAG, statusText);
            Toast.makeText(getApplicationContext(), statusText,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        if(mClosestCloudlet.getStatus() != AppClient.FindCloudletReply.FindStatus.FIND_FOUND) {
            statusText = "findCloudlet Failed. Error: " + mClosestCloudlet.getStatus();
            Log.e(TAG, statusText);
            Toast.makeText(getApplicationContext(), statusText,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        Log.i(TAG, "REQ_FIND_CLOUDLET mClosestCloudlet.uri=" + mClosestCloudlet.getFqdn());


        //Find FqdnPrefix from Port structure.
        String FqdnPrefix = "";
        List<Appcommon.AppPort> ports = mClosestCloudlet.getPortsList();
        String appPortFormat = "{Protocol: %d, FqdnPrefix: %s, Container Port: %d, External Port: %d, Public Path: '%s'}";
        for (Appcommon.AppPort aPort : ports) {
            FqdnPrefix = aPort.getFqdnPrefix();
            Log.i(TAG, String.format(Locale.getDefault(), appPortFormat,
                    aPort.getProto().getNumber(),
                    aPort.getFqdnPrefix(),
                    aPort.getInternalPort(),
                    aPort.getPublicPort(),
                    aPort.getPathPrefix()));
        }
        // Build full hostname.
        String mClosestCloudletHostName = FqdnPrefix+mClosestCloudlet.getFqdn();

        // TODO: Copy/paste the output of this log into a terminal to test latency.
        Log.i("COPY_PASTE", "ping -c 4 "+mClosestCloudletHostName);

        //verifyLocationInBackground(location);

        //getQoSPositionKpiInBackground(location);

        return true;
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
        }
    }


}
