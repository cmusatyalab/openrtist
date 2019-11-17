package edu.cmu.cs.openrtist;

public class TimingServerListActivity extends ServerListActivity {
    ServerListAdapter createServerListAdapter() {
        return new TimingServerListAdapter(getApplicationContext(), ItemModelList);
    }
}
