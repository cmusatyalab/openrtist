package edu.cmu.cs.openrtist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;
import edu.cmu.cs.openrtist.ServerListActivity;

public class ServerListAdapter extends BaseAdapter {
    Context context;
    ArrayList<Server> itemModelList;
    SharedPreferences mSharedPreferences = null;

    public ServerListAdapter(Context context, ArrayList<Server> modelList) {
        this.context = context;
        this.itemModelList = modelList;
        mSharedPreferences=context.getSharedPreferences(context.getString(R.string.shared_preference_file_key),
                context.MODE_PRIVATE);
    }
    @Override
    public int getCount() {
        return itemModelList.size();
    }
    @Override
    public Object getItem(int position) {
        return itemModelList.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        convertView = null;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.list_item, null);
            TextView serverName = (TextView) convertView.findViewById(R.id.serverName);
            TextView serverAddress = (TextView) convertView.findViewById(R.id.serverAddress);
            ImageView imgRemove = (ImageView) convertView.findViewById(R.id.imgRemove);
            ImageView imgConnect = (ImageView) convertView.findViewById(R.id.imgConnect);
            Server s = itemModelList.get(position);
            serverName.setText(s.getName());
            serverAddress.setText(s.getEndpoint());
            imgConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Server s = itemModelList.get(position);
                    Const.SERVER_IP = s.getEndpoint();
                    Intent intent = new Intent(context, GabrielClientActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    //intent.putExtra("", faceTable);
                    context.startActivity(intent);

                    Toast.makeText(context, "initializing demo", Toast.LENGTH_SHORT).show();
                }
            });
            imgRemove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    Server s = itemModelList.get(position);

                    editor.remove("server:".concat(s.getName()));
                    editor.commit();
                    itemModelList.remove(position);
                    notifyDataSetChanged();
                    Toast.makeText(context, "Removed server: ".concat(s.getName()),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
        return convertView;
    }
}