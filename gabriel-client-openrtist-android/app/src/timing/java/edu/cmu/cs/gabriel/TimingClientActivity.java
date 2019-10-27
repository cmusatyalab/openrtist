package edu.cmu.cs.gabriel;

import edu.cmu.cs.gabriel.network.TimingComm;

public class TimingClientActivity extends GabrielClientActivity {
    private TimingComm timingComm;

    void setupComm() {
        this.timingComm = new TimingComm(this.serverIP, Const.PORT, this,
                this.returnMsgHandler);
         this.comm = this.timingComm;
    }

    @Override
    protected void onPause() {
        this.timingComm.logAvgRtt();
        super.onPause();
    }
}
