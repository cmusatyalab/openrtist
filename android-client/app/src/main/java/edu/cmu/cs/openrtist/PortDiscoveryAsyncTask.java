package edu.cmu.cs.openrtist;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.AsyncTask;
import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;

public class PortDiscoveryAsyncTask extends AsyncTask<String,String, String> {
    private Context context;
    private ImageView imgConnect;
    private String endpoint;

    Class<?> gabrielClientActivityClass() {
        return GabrielClientActivity.class;
    }

    public PortDiscoveryAsyncTask(Context hostContext, ImageView imgConnect, String endpoint) {
        context = hostContext;
        this.imgConnect = imgConnect;
        this.endpoint = endpoint;
    }

    @Override
    protected String doInBackground(String... params) {
        // Method runs on a separate thread, make all the network calls you need
        // Add port discover extension for envrmnt cluster
        PortDiscoveryForEnvrmnt PD = new PortDiscoveryForEnvrmnt(context);
        return PD.portDiscovery(endpoint);
    }

    @Override
    protected void onPostExecute(String result) {
        // runs on the UI thread
        // complete setOnClickListener intents
        Const.SERVER_IP = result;
        Log.v("Port_discover server_ip", Const.SERVER_IP);

        Intent intent = new Intent(
                context, PortDiscoveryAsyncTask.this.gabrielClientActivityClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //intent.putExtra("", faceTable);
        context.startActivity(intent);
        imgConnect.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        Toast.makeText(context, R.string.connecting_toast, Toast.LENGTH_SHORT).show();
    }
}