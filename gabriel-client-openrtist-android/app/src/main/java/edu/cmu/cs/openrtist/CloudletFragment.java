package edu.cmu.cs.openrtist;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import android.widget.Switch;
import android.widget.CompoundButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.cs.gabriel.Const;
import edu.cmu.cs.gabriel.GabrielClientActivity;


public class CloudletFragment extends Fragment {
    private final int LAUNCHCODE = 0;
    private static final int DLG_EXAMPLE1 = 0;
    private static final int TEXT_ID = 1000;
    public String inputDialogResult=null;
    private static final String TAG = "OpenStyleFragment";


    protected Button cloudletRunDemoButton;
    protected RadioGroup typeRadioGroup;
    protected RadioButton cloudletRadioButton;
    protected RadioButton cloudRadioButton;
    protected Spinner selectServerSpinner;
    protected Switch stereoEnabled;
    protected View view;
    protected List<String> spinnerList;

    private static final String LOG_TAG = "fragment";


    private CloudletDemoActivity getMyAcitivty() {
        CloudletDemoActivity a = (CloudletDemoActivity) getActivity();
        return a;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.spinnerList = new ArrayList<String>();
    }

    Spinner.OnItemSelectedListener spinnerSelectedListener = new Spinner.OnItemSelectedListener(){
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            updateCurrentServerIp();

        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // do nothing
        }
    };

    private void updateCurrentServerIp(){
        String currentIpName=selectServerSpinner.getSelectedItem().toString();
        getMyAcitivty().currentServerIp=getMyAcitivty().mSharedPreferences.getString(currentIpName,
                getMyAcitivty().currentServerIp);
        //getMyAcitivty().sendOpenFaceGetPersonRequest(getMyAcitivty().currentServerIp);
        //update PersonUIRow
        Log.d(TAG, "current ip changed to: " + getMyAcitivty().currentServerIp);
//        Toast.makeText(getContext(),
//                "current ip: "+getMyAcitivty().currentServerIp,
//                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.d(TAG, "on resume");
        if (getMyAcitivty().onResumeFromLoadState){
            Log.d(TAG, "on resume from load state. don't refresh yet");
            getMyAcitivty().onResumeFromLoadState=false;
        } else {
            populateSelectServerSpinner();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view=inflater.inflate(R.layout.unified_fragment,container,false);

        typeRadioGroup=(RadioGroup)view.findViewById(R.id.type_radiogroup);
        cloudletRadioButton=(RadioButton)view.findViewById(R.id.radio_cloudlet);
        cloudRadioButton=(RadioButton)view.findViewById(R.id.radio_cloud);
        typeRadioGroup.check(R.id.radio_cloudlet);

        selectServerSpinner=(Spinner) view.findViewById(R.id.select_server_spinner);
        cloudletRunDemoButton =(Button)view.findViewById(R.id.cloudletRunDemoButton);
        typeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                populateSelectServerSpinner();
            }
        });

        stereoEnabled = (Switch) view.findViewById(R.id.stereoMode);
        stereoEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Const.STEREO_ENABLED = true;
                } else {
                    Const.STEREO_ENABLED = false;
                }
            }
        });
        cloudletRunDemoButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                Const.SERVER_IP = getMyAcitivty().currentServerIp;
                Intent intent = new Intent(getContext(), GabrielClientActivity.class);
                //intent.putExtra("", faceTable);
                startActivity(intent);
                Toast.makeText(getContext(), "initializing demo", Toast.LENGTH_SHORT).show();
            }
        });


        return view;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void populateSelectServerSpinner(){
        //initilize spinner
        int checkedId=typeRadioGroup.getCheckedRadioButtonId();
        RadioButton rb= (RadioButton) view.findViewById(checkedId);
        String type=rb.getText().toString();
        List<String> spinnerItems=getIpNamesByType(type);
        selectServerSpinner.setOnItemSelectedListener(spinnerSelectedListener);
        selectServerSpinner.setAdapter(createSpinnerAdapterFromList(spinnerItems));
//        updateCurrentServerIp();
    }

    private SpinnerAdapter createSpinnerAdapterFromList(List<String> items){
        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<String>(getContext(),
                        android.R.layout.simple_spinner_item, items);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return spinnerAdapter;
    }

    /**
     * return ip names by type (cloudlet or cloud)
     * @param type
     * @return
     */
    private List<String> getIpNamesByType(String type){
        SharedPreferences mSharedPreferences=getMyAcitivty().mSharedPreferences;
        Set<String> ipNameSet = new HashSet<String>();
        if (type.equals(getString(R.string.type_cloudlet))){
            ipNameSet=mSharedPreferences.getStringSet(
                    getString(R.string.shared_preference_cloudlet_ip_dict),
                    new HashSet<String>());
        } else if (type.equals(getString(R.string.type_cloud))) {
            ipNameSet=mSharedPreferences.getStringSet(
                    getString(R.string.shared_preference_cloud_ip_dict),
                    new HashSet<String>());
        } else {
            Log.e(TAG,"invalid type selected");
        }
        //add set to spinner
        List<String> ipNames=new ArrayList<String>();
        ipNames.addAll(ipNameSet);
        return ipNames;
    }


    private void startGabrielActivityForTraining(String name, String ip) {
        //TODO: how to handle sync faces between cloud and cloudlet?
        Const.SERVER_IP = ip;
        Intent intent = new Intent(getContext(), GabrielClientActivity.class);
        intent.putExtra("name", name);
        startActivityForResult(intent, LAUNCHCODE);
        Toast.makeText(getContext(), "training", Toast.LENGTH_SHORT).show();
    }
}
