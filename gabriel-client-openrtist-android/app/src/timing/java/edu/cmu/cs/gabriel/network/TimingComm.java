package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.os.Handler;

import edu.cmu.cs.gabriel.client.comm.TimingServerComm;

public class TimingComm extends BaseComm {
    TimingServerComm timingServerComm;

    public TimingComm(String serverIP, int port, final Activity activity,
                      final Handler returnMsgHandler, String tokenLimit) {
        super(activity, returnMsgHandler);

        if (tokenLimit.equals("None")) {
            this.timingServerComm = new TimingServerComm(this.consumer, this.onDisconnect, serverIP,
                    port, activity.getApplication());
        } else {
            this.timingServerComm = new TimingServerComm(this.consumer, this.onDisconnect, serverIP,
                    port, activity.getApplication(), Integer.parseInt(tokenLimit));
        }

        this.serverCommCore = timingServerComm;
    }

    public void logAvgRtt() {
        this.timingServerComm.logAvgRtt();
    }
}
