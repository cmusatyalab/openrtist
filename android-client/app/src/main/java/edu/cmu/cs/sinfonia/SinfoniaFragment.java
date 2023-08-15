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

import static edu.cmu.cs.gabriel.Const.SOURCE_NAME;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;

import edu.cmu.cs.openrtist.databinding.SinfoniaFragmentBinding;

public class SinfoniaFragment extends Fragment {
    private static final String TAG = "OpenRTiST/SinfoniaFragment";
    private SinfoniaActivity mActivity;
    private SinfoniaFragmentBinding binding;
    private final static String KEY_LOCAL_URL = "local_url";

    public SinfoniaFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof SinfoniaActivity) {
            mActivity = (SinfoniaActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = SinfoniaFragmentBinding.inflate(inflater, container, false);
        binding.executePendingBindings();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        Log.i(TAG, "onViewStateRestored");
        if (binding == null) return;
        binding.setFragment(this);
        if (savedInstanceState != null) {
            binding.setTier1url(savedInstanceState.getString(KEY_LOCAL_URL));
        } else {
            binding.setTier1url("https://cmu.findcloudlet.org");
        }
        ArrayList<Backend> backends = new ArrayList<>();
        backends.add(new Backend(
                "Normal",
                "Intel CPU Core i9-12900",
                "737b5001-d27a-413f-9806-abf9bfce6746",
                "2.1"
        ));
        backends.add(new Backend(
                "Fast",
                "Nvidia GPU Geforce RTX 3090",
                "755e5883-0788-44da-8778-2113eddf4271",
                "2.1"
        ));
        binding.setBackends(backends);
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.i(TAG, "onSaveInstanceState");
        if (binding != null) {
            outState.putString(KEY_LOCAL_URL, binding.getTier1url());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "onDestroyView");
        binding.getBackends().clear();
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    private void onFinished() {
        // Hide the keyboard; it rarely goes away on its own.
        if (mActivity == null) return;
        View focusedView = mActivity.getCurrentFocus();
        if (focusedView != null) {
            InputMethodManager inputManager = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(
                    focusedView.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS
            );
        }
        getParentFragmentManager().popBackStackImmediate();
    }

    public void onLaunchClicked(View view, Backend backend) {
        Log.i(TAG, "onLaunchClicked");

        MaterialButton materialButton = (MaterialButton) view;
        if (!materialButton.isEnabled()) return;
        materialButton.setEnabled(false);

        Intent intent = new Intent(SinfoniaService.ACTION_START)
                .setPackage(SinfoniaService.PACKAGE_NAME)
                .putExtra("url", binding.getTier1url())
                .putExtra("applicationName", SOURCE_NAME)
                .putExtra("uuid", backend.uuid.toString())
                .putExtra("tunnelName", SOURCE_NAME + backend.name)
                .putStringArrayListExtra(
                        "application",
                        new ArrayList<>(Collections.singletonList(mActivity.getPackageName()))
                );
        mActivity.getSinfoniaService().deploy(intent);
        materialButton.setEnabled(true);
        onFinished();
    }

    public void onCleanupClick(View view) {
        Log.i(TAG, "onCloseTunnelClick");

        MaterialButton materialButton = (MaterialButton) view;
        if (!materialButton.isEnabled()) return;
        materialButton.setEnabled(false);

        Intent intent = new Intent(SinfoniaService.ACTION_START)
                .setPackage(SinfoniaService.PACKAGE_NAME)
                .putStringArrayListExtra(
                        "application",
                        new ArrayList<>(Collections.singletonList(mActivity.getPackageName()))
                );

        mActivity.getSinfoniaService().cleanup(intent);
        materialButton.setEnabled(true);
        onFinished();
    }
}