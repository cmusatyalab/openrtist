package edu.cmu.cs.gabrielvr;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import edu.cmu.cs.openstylevr.R;

/**
 * Created by shilpageorge on 10/26/17.
 */

public class CustomAdapter extends BaseAdapter {
    Context context;
    //int imgid[];
    String[] style_name;
    LayoutInflater inflter;

    public CustomAdapter(Context applicationContext,String[] style_name) {
        this.context = applicationContext;
        //this.imgid = imgid;
        this.style_name = style_name;
        inflter = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return style_name.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflter.inflate(R.layout.mylist, null);
        //ImageView icon = (ImageView) view.findViewById(R.id.icon);
        TextView names = (TextView) view.findViewById(R.id.item);
        //icon.setImageResource(imgid[i]);
        names.setText(style_name[i]);
        return view;
    }
}