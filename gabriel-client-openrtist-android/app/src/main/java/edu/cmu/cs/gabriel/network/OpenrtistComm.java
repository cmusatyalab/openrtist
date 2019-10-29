package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.os.Handler;

import edu.cmu.cs.gabriel.client.comm.ServerComm;

public class OpenrtistComm extends BaseComm {
    public OpenrtistComm(
            String serverIP, int port, final Activity activity, final Handler returnMsgHandler,
            String tokenLimit) {
        super(activity, returnMsgHandler);

        if (tokenLimit.equals("None")) {
            this.serverCommCore = new ServerComm(this.consumer, this.onDisconnect, serverIP, port,
                    activity.getApplication());
        } else {
            this.serverCommCore = new ServerComm(this.consumer, this.onDisconnect, serverIP, port,
                    activity.getApplication(), Integer.parseInt(tokenLimit));
        }

    }
}