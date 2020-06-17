package edu.cmu.cs.gabriel;

import android.util.Log;

import edu.cmu.cs.gabriel.network.MeasurementComm;
import static edu.cmu.cs.gabriel.client.Util.ValidateEndpoint;

public class MeasurementClientActivity extends GabrielClientActivity {
    private static final String TAG = "MeasureClientActivity";

    private MeasurementComm measurementComm;

    @Override
    void setupComm() {
        String serverURL = ValidateEndpoint(this.serverIP, Const.PORT);

        this.measurementComm = new MeasurementComm(serverURL, this, this.returnMsgHandler,
                Const.TOKEN_LIMIT);
         this.comm = this.measurementComm;
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "Overall average RTT: " + this.measurementComm.getOverallAvgRtt());
        Log.i(TAG, "Overall FPS: " + this.measurementComm.getOverallFps());
        this.measurementComm.clearMeasurements();
        super.onPause();
    }
}
