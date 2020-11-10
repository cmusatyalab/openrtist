package edu.cmu.cs.gabriel;

import android.util.Log;

import edu.cmu.cs.gabriel.network.MeasurementComm;

public class MeasurementClientActivity extends GabrielClientActivity {
    private static final String TAG = "MeasureClientActivity";

    private MeasurementComm measurementComm;

    @Override
    void setupComm() {
        int port = getPort();
        this.measurementComm = new MeasurementComm(
                this.serverIP, port, this, this.returnMsgHandler, Const.TOKEN_LIMIT);
        this.setOpenrtistComm(this.measurementComm.getOpenrtistComm());
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "Overall FPS: " + this.measurementComm.computeOverallFps());
        super.onPause();
    }
}
