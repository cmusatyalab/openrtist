package edu.cmu.cs.openrtist;

import android.util.Log;
import android.widget.ImageView;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.camera.ImageViewUpdater;
import edu.cmu.cs.gabriel.network.MeasurementComm;


public class MeasurementClientActivity extends GabrielClientActivity {
    private static final String TAG = "MeasureClientActivity";

    private MeasurementComm measurementComm;
    private ImageView imgView;
    private ImageView iconView;
    // Stereo views
    private ImageView stereoView1;
    private ImageView stereoView2;


    @Override
    void setupComm() {
        int port = getPort();
        imgView = super.getImageView();

        ImageViewUpdater imageViewUpdater = new ImageViewUpdater(this.imgView);

        this.measurementComm = MeasurementComm.createMeasurementComm(
                this.serverIP, port, this, this.iconView, imageViewUpdater, Const.TOKEN_LIMIT);
        this.setOpenrtistComm(this.measurementComm.getOpenrtistComm());
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "Overall FPS: " + this.measurementComm.computeOverallFps());
        super.onPause();
    }
}