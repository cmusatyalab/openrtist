package edu.cmu.cs.gabriel;

import android.content.Context;

public class MexConst {
    //These MEX DME parameters are case-sensitive
    public static String MEX_APP = "openrtist";
    public static String MEX_DEV = "CMU";
    public static String MEX_CARRIER = "CMU";
    public static String MEX_TAG = "latest";
    public static String MEX_DME_URL = "us-mexdemo.dme.mobiledgex.net";

    public static void loadPref(Context c, String key, Object value) {
        switch(key) {
            case "mex_app":
                MexConst.MEX_APP = value.toString();
                break;
            case "mex_dev":
                MexConst.MEX_DEV = value.toString();
                break;
            case "mex_tag":
                MexConst.MEX_TAG = value.toString();
                break;
            case "mex_carrier":
                MexConst.MEX_CARRIER = value.toString();
                break;
            case "mex_dme_url":
                MexConst.MEX_DME_URL = value.toString();
                break;
        }
    }
}
