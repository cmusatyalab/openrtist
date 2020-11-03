package edu.cmu.cs.gabriel.network;

import android.app.Application;
import android.widget.ImageView;

import java.util.function.Consumer;
import java.util.function.Supplier;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.client.comm.ServerComm;
import edu.cmu.cs.gabriel.protocol.Protos.InputFrame;
import edu.cmu.cs.gabriel.protocol.Protos.ResultWrapper;

public class OpenrtistComm {
    private final ServerComm serverComm;
    private final ErrorConsumer onDisconnect;

    public static OpenrtistComm createOpenrtistComm(
            String endpoint, int port, GabrielClientActivity gabrielClientActivity,
            ImageView referenceView, ImageView imageView, String tokenLimit) {
        Consumer<ResultWrapper> consumer = new ResultConsumer(
                referenceView, imageView, gabrielClientActivity);
        ErrorConsumer onDisconnect = new ErrorConsumer(gabrielClientActivity);
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

    public void sendSupplier(Supplier<InputFrame> supplier) {
        if (!this.serverComm.isRunning()) {
            return;
        }

        this.serverComm.sendSupplier(supplier, Const.SOURCE_NAME, /* wait */ false);
    }

    public void stop() {
        this.serverComm.stop();
    }
}
