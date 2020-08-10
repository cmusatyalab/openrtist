package edu.cmu.cs.gabriel.network;

import android.app.Application;
import android.os.Handler;

import java.util.function.Consumer;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.client.comm.ServerComm;
import edu.cmu.cs.gabriel.client.results.SendSupplierResult;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;
import edu.cmu.cs.openrtist.R;

public class OpenrtistComm {
    private final ServerComm serverComm;
    private final ErrorConsumer onDisconnect;

    public static OpenrtistComm createOpenrtistComm(
            String endpoint, int port, GabrielClientActivity gabrielClientActivity,
            Handler returnMsgHandler, String tokenLimit) {
        Consumer<ResultWrapper> consumer = new ResultConsumer(
                returnMsgHandler, gabrielClientActivity);
        ErrorConsumer onDisconnect = new ErrorConsumer(returnMsgHandler, gabrielClientActivity);
        ServerComm serverComm;
        Application application = gabrielClientActivity.getApplication();
        if (tokenLimit.equals("None")) {
            serverComm = ServerComm.createServerComm(
                    consumer, endpoint, port, application, onDisconnect);
        } else {
            serverComm = ServerComm.createServerComm(
                    consumer, endpoint, port, application, onDisconnect,
                    Integer.parseInt(tokenLimit));
        }

        return new OpenrtistComm(serverComm, onDisconnect);
    }

    OpenrtistComm(ServerComm serverComm, ErrorConsumer onDisconnect) {
        this.serverComm = serverComm;
        this.onDisconnect = onDisconnect;
    }

    public void sendSupplier(FrameSupplier frameSupplier) {
        if (!this.serverComm.isRunning()) {
            return;
        }

        SendSupplierResult sendSupplierResult = this.serverComm.sendSupplier(
                frameSupplier, Const.SOURCE_NAME, /* wait */ true);
        if (sendSupplierResult == SendSupplierResult.ERROR_GETTING_TOKEN) {
            this.onDisconnect.showErrorMessage(R.string.toekn_error);
        }
    }

    public void stop() {
        this.serverComm.stop();
    }
}
