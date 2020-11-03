package edu.cmu.cs.gabriel.network;

import java.util.function.Consumer;

import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.gabriel.client.results.ErrorType;
import edu.cmu.cs.openrtist.R;

public class ErrorConsumer implements Consumer<ErrorType> {
    private final GabrielClientActivity gabrielClientActivity;
    private boolean shownError;

    public ErrorConsumer(GabrielClientActivity gabrielClientActivity) {
        this.gabrielClientActivity = gabrielClientActivity;
        this.shownError = false;
    }

    @Override
    public void accept(ErrorType errorType) {
        int stringId;
        switch (errorType) {
            case SERVER_ERROR:
                stringId = R.string.server_error;
                break;
            case SERVER_DISCONNECTED:
                stringId = R.string.server_disconnected;
                break;
            case COULD_NOT_CONNECT:
                stringId = R.string.could_not_connect;
                break;
            default:
                stringId = R.string.unspecified_error;
        }
        this.showErrorMessage(stringId);
    }

    public void showErrorMessage(int stringId) {
        if (this.shownError) {
            return;
        }

        this.shownError = true;
        this.gabrielClientActivity.showNetworkErrorMessage(
                this.gabrielClientActivity.getResources().getString(stringId));
    }
}
