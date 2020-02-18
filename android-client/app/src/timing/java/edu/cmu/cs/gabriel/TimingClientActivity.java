package edu.cmu.cs.gabriel;

import edu.cmu.cs.gabriel.network.TimingComm;

import static edu.cmu.cs.gabriel.client.Util.ValidateEndpoint;

public class TimingClientActivity extends GabrielClientActivity {
    private TimingComm timingComm;

    @Override
    void setupComm() {
        String serverURL = ValidateEndpoint(this.serverIP, Const.PORT);

        this.timingComm = new TimingComm(serverURL, this, this.returnMsgHandler,
                Const.TOKEN_LIMIT);
         this.comm = this.timingComm;
    }

    @Override
    protected void onPause() {
        this.timingComm.logAvgRtt();
        super.onPause();
    }
}
