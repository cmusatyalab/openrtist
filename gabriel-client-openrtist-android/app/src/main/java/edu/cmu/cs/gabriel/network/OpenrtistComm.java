package edu.cmu.cs.gabriel.network;

import android.app.Activity;
import android.os.Handler;

import edu.cmu.cs.gabriel.client.comm.ServerComm;

public class OpenrtistComm extends BaseComm {
    public OpenrtistComm(
            String serverIP, int port, final Activity activity, final Handler returnMsgHandler) {
        super(activity, returnMsgHandler);

        this.serverCommCore = new ServerComm(this.consumer, this.onDisconnect, serverIP, port,
                activity.getApplication());
    }
}