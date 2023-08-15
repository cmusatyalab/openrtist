/*
 * Copyright 2023 Carnegie Mellon University
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.cmu.cs.sinfonia;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import edu.cmu.cs.openrtist.databinding.BackendDetailBinding;

public class BindingAdapters {
    private static final String TAG = "OpenRTiST/BindingAdapters";

    @BindingAdapter({"items", "fragment", "layout"})
    public static <E> void setItems(
            LinearLayout view,
            ArrayList<E> items,
            Fragment fragment,
            int layoutResId
    ) {
        Log.i(TAG, "setItems");
        if (fragment instanceof SinfoniaFragment) {
            SinfoniaFragment sinfoniaFragment = (SinfoniaFragment) fragment;
            view.removeAllViews();
            if (items != null) {
                LayoutInflater inflater = LayoutInflater.from(view.getContext());
                for (E item : items) {
                    BackendDetailBinding binding = DataBindingUtil.inflate(inflater, layoutResId, view, false);
                    Backend backend = (Backend) item;
                    binding.setItem(backend);
                    binding.setFragment(sinfoniaFragment);
                    view.addView(binding.getRoot());
                }
            }
        }
    }
}
