package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.os.Handler;

import edu.cmu.cs.gabriel.client.comm.LogRttFpsConsumer;
import edu.cmu.cs.gabriel.client.comm.MeasurementServerComm;
import edu.cmu.cs.gabriel.client.comm.RttFps;
import edu.cmu.cs.gabriel.client.function.Consumer;

public class MeasurementComm extends BaseComm {
    MeasurementServerComm measurementServerComm;

    public MeasurementComm(String serverURL, final Activity activity,
                           final Handler returnMsgHandler, String tokenLimit) {
        super(activity, returnMsgHandler);

        Consumer<RttFps> intervalReporter = new LogRttFpsConsumer();

        if (tokenLimit.equals("None")) {
            this.measurementServerComm = new MeasurementServerComm(this.consumer, this.onDisconnect,
                    serverURL, activity.getApplication(), intervalReporter);
        } else {
            this.measurementServerComm = new MeasurementServerComm(
                    this.consumer, this.onDisconnect, serverURL, activity.getApplication(),
                    intervalReporter, Integer.parseInt(tokenLimit));
        }

        this.serverCommCore = measurementServerComm;
    }

    public double getOverallAvgRtt() {
        return this.measurementServerComm.getOverallAvgRtt();
    }

    public double getOverallFps() {
        return this.measurementServerComm.getOverallFps();
    }

    public void clearMeasurements() {
        this.measurementServerComm.clearMeasurements();
    }
}
