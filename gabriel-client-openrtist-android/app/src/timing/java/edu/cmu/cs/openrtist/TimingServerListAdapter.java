package edu.cmu.cs.openrtist;

import android.content.Context;

import java.util.ArrayList;

import edu.cmu.cs.gabriel.TimingClientActivity;

public class TimingServerListAdapter extends ServerListAdapter {
    public TimingServerListAdapter(Context context, ArrayList<Server> modelList) {
        super(context, modelList);
    }

    @Override
    Class<?> gabrielClientActivityClass() {
        return TimingClientActivity.class;
    }
}
