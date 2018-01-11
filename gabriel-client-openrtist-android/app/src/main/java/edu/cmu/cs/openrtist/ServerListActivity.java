package edu.cmu.cs.openrtist;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ImageView;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.util.Patterns;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Map;

import edu.cmu.cs.gabriel.Const;


public class ServerListActivity extends AppCompatActivity  {
    ListView listView;
    EditText serverName;
    EditText serverAddress;
    ImageView add;
    ArrayList<Server> ItemModelList;
    ServerListAdapter serverListAdapter;
    Switch stereoEnabled = null;
    Switch showReference = null;
    Switch iterateStyles = null;
    private SharedPreferences mSharedPreferences;

    //activity menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cloudlet_demo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.about:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.about_message)
                        .setTitle(R.string.about_title);
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            default:
                return false;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serverlist);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        listView = (ListView) findViewById(R.id.listServers);
        serverName = (EditText) findViewById(R.id.addServerName);
        serverAddress = (EditText) findViewById(R.id.addServerAddress);
        add = (ImageView) findViewById(R.id.imgViewAdd);
        ItemModelList = new ArrayList<Server>();
        serverListAdapter = new ServerListAdapter(getApplicationContext(), ItemModelList);
        listView.setAdapter(serverListAdapter);
        mSharedPreferences=getSharedPreferences(getString(R.string.shared_preference_file_key),
                MODE_PRIVATE);

        stereoEnabled = (Switch) findViewById(R.id.toggleStereo);
        showReference = (Switch) findViewById(R.id.showReference);
        iterateStyles = (Switch) findViewById(R.id.iterateStyles);
        stereoEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.STEREO_ENABLED = isChecked;
                if(isChecked)
                    showReference.setChecked(false);

                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean("option:stereo",Const.STEREO_ENABLED);
                editor.commit();
            }
        });
        stereoEnabled.setChecked(mSharedPreferences.getBoolean("option:stereo", false));

        showReference.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.DISPLAY_REFERENCE = isChecked;
                if(isChecked)
                    stereoEnabled.setChecked(false);
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean("option:showref",isChecked);
                editor.commit();
            }
        });
        showReference.setChecked(mSharedPreferences.getBoolean("option:showref", false));

        iterateStyles.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Const.ITERATE_STYLES = isChecked;
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean("option:iterate",Const.ITERATE_STYLES);
                editor.commit();
            }
        });
        iterateStyles.setChecked(mSharedPreferences.getBoolean("option:iterate", false));


        initServerList();
    }

    private void initServerList(){
        Map<String, ?> prefs = mSharedPreferences.getAll();
        for (Map.Entry<String,?> pref : prefs.entrySet())
            if(pref.getKey().startsWith("server:")) {
                Server s = new Server(pref.getKey().substring("server:".length()), pref.getValue().toString());
                ItemModelList.add(s);
                serverListAdapter.notifyDataSetChanged();
            }
    }
    /**
     * This is used to check the given URL is valid or not.
     * @param url
     * @return true if url is valid, false otherwise.
     */
    private boolean isValidUrl(String url) {
        Pattern p = Patterns.WEB_URL;
        Matcher m = p.matcher(url.toLowerCase());
        return m.matches();
    }

    public void addValue(View v) {
        String name = serverName.getText().toString();
        String endpoint = serverAddress.getText().toString();
        if (name.isEmpty() || endpoint.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Name and address are required.",
                    Toast.LENGTH_SHORT).show();
        } else if(!isValidUrl(endpoint)) {
            Toast.makeText(getApplicationContext(), "Server address is an invalid URI.",
                    Toast.LENGTH_SHORT).show();
        }  else if(mSharedPreferences.contains("server:".concat(name))) {
            Toast.makeText(getApplicationContext(), "A server by that name already exists.",
                Toast.LENGTH_SHORT).show();
        } else {
            Server s = new Server(name, endpoint);
            ItemModelList.add(s);
            serverListAdapter.notifyDataSetChanged();
            serverName.setText("");
            serverAddress.setText("");
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString("server:".concat(name),endpoint);
            editor.commit();
            findViewById(R.id.textOptions).requestFocus();
        }
    }




}
