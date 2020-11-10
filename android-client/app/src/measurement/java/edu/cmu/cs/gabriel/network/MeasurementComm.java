package edu.cmu.cs.gabriel.network;

import android.app.Application;
import android.os.Handler;

import java.util.function.Consumer;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.client.comm.MeasurementServerComm;
import edu.cmu.cs.gabriel.client.consumer.LogMeasurementConsumer;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;

public class MeasurementComm {
    private final MeasurementServerComm measurementServerComm;
    private final OpenrtistComm openrtistComm;

    public MeasurementComm(
            String endpoint, int port, GabrielClientActivity gabrielClientActivity,
            Handler returnMsgHandler, String tokenLimit) {
        Consumer<ResultWrapper> consumer = new ResultConsumer(
                returnMsgHandler, gabrielClientActivity);
        ErrorConsumer onDisconnect = new ErrorConsumer(returnMsgHandler, gabrielClientActivity);
        LogMeasurementConsumer logMeasurementConsumer = new LogMeasurementConsumer();
        Application application = gabrielClientActivity.getApplication();
        if (tokenLimit.equals("None")) {
            this.measurementServerComm = MeasurementServerComm.createMeasurementServerComm(
                    consumer, endpoint, port, application, onDisconnect, logMeasurementConsumer);
        } else {
            this.measurementServerComm = MeasurementServerComm.createMeasurementServerComm(
                    consumer, endpoint, port, application, onDisconnect, logMeasurementConsumer,
                    Integer.parseInt(tokenLimit));
        }

        this.openrtistComm = new OpenrtistComm(this.measurementServerComm, onDisconnect);
    }

    public OpenrtistComm getOpenrtistComm() {
        return openrtistComm;
    }

    public double computeOverallFps() {
        return this.measurementServerComm.computeOverallFps(Const.SOURCE_NAME);
    }
}
