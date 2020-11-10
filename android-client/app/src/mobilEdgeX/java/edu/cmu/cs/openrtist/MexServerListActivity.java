package edu.cmu.cs.openrtist;

import distributed_match_engine.AppClient;
import distributed_match_engine.Appcommon;
import edu.cmu.cs.gabriel.MexConst;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import com.mobiledgex.matchingengine.MatchingEngine;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MexServerListActivity extends ServerListActivity {
    private static final String TAG = "MexServerListActivity";

    private MatchingEngine matchingEngine;
    private String statusText = null;
    private AppClient.FindCloudletReply mClosestCloudlet;
    private String host;
    private int port;
    private String carrierName;
    private String appName;
    private String devName;
    private String appVersion;

    @Override
    void loadPref(Context c, String key, Object value) {
        super.loadPref(c, key, value);
        MexConst.loadPref(c, key, value);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.settings:
                Intent intent = new Intent(this, MexSettingsActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //intent.putExtra("", faceTable);
                this.startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    void requestPermission() {
        String permissions[] = {Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
        };
        this.requestPermissionHelper(permissions);
    }

    @Override
    void initServerList() {
        super.initServerList();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        } else {
            //Always add MobiledgeX server so that it refreshes each time the serverlist activity is instantiated
            //but we don't need to persist it in the prefs as a result
            try {
                if (registerClient()) {
                    // Now that we are registered, let's find the closest cloudlet
                    findCloudlet();
                }

                // Add mex server if there are no other servers present
                Server s = new Server(String.format("%1$s (%2$s, %3$s)", getString(R.string.mex_server), mClosestCloudlet.getCloudletLocation().getLatitude(), mClosestCloudlet.getCloudletLocation().getLongitude()), mClosestCloudlet.getFqdn());
                ItemModelList.add(s);
                serverListAdapter.notifyDataSetChanged();
            } catch (ExecutionException | InterruptedException | io.grpc.StatusRuntimeException e) {
                e.printStackTrace();
                statusText = "MobiledgeX Registration Failed. Exception=" + e.getLocalizedMessage();
                Log.e(TAG, statusText);
                Toast.makeText(getApplicationContext(), statusText,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean registerClient() throws ExecutionException, InterruptedException, io.grpc.StatusRuntimeException {
        // NOTICE: In a real app, these values would be determined by the SDK, but we are reusing
        // an existing app so we don't have to create new app provisioning data for this workshop.
        appName = MexConst.MEX_APP;
        devName = MexConst.MEX_DEV;
        carrierName = MexConst.MEX_CARRIER;
        appVersion = MexConst.MEX_TAG;
        Log.i(TAG,appName);
        Log.i(TAG,devName);
        Log.i(TAG, carrierName);
        Log.i(TAG, appVersion);

        //NOTICE: A real app would request permission to enable this.
        MatchingEngine.setMatchingEngineLocationAllowed(true);

        matchingEngine = new MatchingEngine( this );
        matchingEngine.setNetworkSwitchingEnabled( false ); // Stick with wifi for workshop.
        host = MexConst.MEX_DME_URL; // Override host.
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

        AppClient.FindCloudletRequest findCloudletRequest = matchingEngine.createFindCloudletRequest(this, carrierName, location);
        mClosestCloudlet = matchingEngine.findCloudlet(findCloudletRequest, host, port, 10000);

        Log.i(TAG, "mClosestCloudlet=" + mClosestCloudlet.getFqdn());
        if (mClosestCloudlet == null) {
            statusText = "findCloudlet call is not successfully coded. Search for TODO in code.";
            Log.e(TAG, statusText);
            Toast.makeText(getApplicationContext(), statusText,
                    Toast.LENGTH_LONG).show();
            return false;
        }
        if (mClosestCloudlet.getStatus() != AppClient.FindCloudletReply.FindStatus.FIND_FOUND) {
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
        String mClosestCloudletHostName = FqdnPrefix + mClosestCloudlet.getFqdn();

        // TODO: Copy/paste the output of this log into a terminal to test latency.
        Log.i("COPY_PASTE", "ping -c 4 " + mClosestCloudletHostName);

        //verifyLocationInBackground(location);

        //getQoSPositionKpiInBackground(location);

        return true;
    }
}
