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




    @Override
    void setupComm() {
        int port = getPort();
        imgView = findViewById(R.id.guidance_image);
        iconView = findViewById(R.id.style_image);

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