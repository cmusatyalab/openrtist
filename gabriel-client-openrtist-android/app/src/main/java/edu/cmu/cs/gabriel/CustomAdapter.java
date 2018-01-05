// Copyright 2018 Carnegie Mellon University
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package edu.cmu.cs.gabriel;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import edu.cmu.cs.openrtist.R;

/**
 * Created by shilpageorge on 10/26/17.
 */

public class CustomAdapter extends BaseAdapter {
    Context context;
    int imgid[];
    String[] style_name;
    LayoutInflater inflter;

    public CustomAdapter(Context applicationContext, int[] imgid, String[] style_name) {
        this.context = applicationContext;
        this.imgid = imgid;
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
