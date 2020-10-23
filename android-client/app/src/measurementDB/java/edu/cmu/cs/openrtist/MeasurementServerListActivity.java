
package edu.cmu.cs.openrtist;

public class MeasurementServerListActivity extends ServerListActivity {
    ServerListAdapter createServerListAdapter() {
        return new MeasurementServerListAdapter(getApplicationContext(), ItemModelList);
    }
}