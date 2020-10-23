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
    int getPort() {
        String[] portarr = this.serverIP.split(":");
        int port = Const.PORT;
        if (portarr.length > 1) {
            port = Integer.parseInt(portarr[1]);
        }
        return port;
    }
}
