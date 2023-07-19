package edu.cmu.cs.gabriel.network;

import android.app.Application;
import android.util.Log;
import android.widget.ImageView;

import com.google.protobuf.ByteString;

import java.util.function.Consumer;
import java.util.function.Supplier;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.MeasurementDbConsumer;
import edu.cmu.cs.gabriel.client.comm.MeasurementServerComm;
import edu.cmu.cs.gabriel.client.comm.ServerComm;
import edu.cmu.cs.gabriel.protocol.Protos;
import edu.cmu.cs.openrtist.GabrielClientActivity;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;

public class MeasurementComm {
    private MeasurementServerComm measurementServerComm;
    private final ErrorConsumer onDisconnect;
    private static OpenrtistComm openrtistComm;

    private String LOG_TAG = "MeasurementComm";

    public static MeasurementComm createMeasurementComm(
            String endpoint, int port, GabrielClientActivity gabrielClientActivity,
            ImageView referenceView, Consumer<ByteString> imageView, String tokenLimit) {
        MeasurementServerComm serverComm;
        Consumer<ResultWrapper> consumer = new ResultConsumer(
                referenceView, imageView, gabrielClientActivity);
        ErrorConsumer onDisconnect = new ErrorConsumer(gabrielClientActivity);
        MeasurementDbConsumer measurementDbconsumer = new MeasurementDbConsumer(
                gabrielClientActivity, endpoint);
        Application application = gabrielClientActivity.getApplication();
        if (tokenLimit.equals("None")) {
            serverComm = MeasurementServerComm.createMeasurementServerComm(
                    consumer, endpoint, port, application, onDisconnect,measurementDbconsumer);
        } else {
            serverComm = MeasurementServerComm.createMeasurementServerComm(
                    consumer, endpoint, port, application, onDisconnect,
                    measurementDbconsumer,Integer.parseInt(tokenLimit));
        }
        openrtistComm = new OpenrtistComm(serverComm, onDisconnect);
        return new MeasurementComm(serverComm, onDisconnect);
    }

    MeasurementComm(ServerComm serverComm, ErrorConsumer onDisconnect) {
        this.measurementServerComm = measurementServerComm;
        this.onDisconnect = onDisconnect;
    }

    public void sendSupplier(Supplier<Protos.InputFrame> supplier) {
        if (!this.measurementServerComm.isRunning()) {
            return;
        }

        this.measurementServerComm.sendSupplier(supplier, Const.SOURCE_NAME, /* wait */ false);
    }

    public void stop() {
        this.measurementServerComm.stop();
    }

    public double computeOverallFps() {
        Log.i(LOG_TAG,"ComputeOverallFps");
//        return this.measurementServerComm.computeOverallFps(Const.SOURCE_NAME);
        return 0;
    }

    public OpenrtistComm getOpenrtistComm() {
        return openrtistComm;
    }
}
