package edu.cmu.cs.gabriel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Consumer;

import edu.cmu.cs.gabriel.client.observer.IntervalMeasurement;
import edu.cmu.cs.openrtist.GabrielClientActivity;
import edu.cmu.cs.measurementDb.InfluxDBHelper;
import edu.cmu.cs.measurementDb.*;
import edu.cmu.cs.openrtist.R;


import static androidx.core.app.ActivityCompat.requestPermissions;

public class MeasurementDbConsumer implements Consumer<IntervalMeasurement > {
    private static final String TAG = "MeasurementDbConsumer";

    private GabrielClientActivity gabrielClientActivity;
    private TelephonyManager teleMan;
    private LocationManager locMan;
    private WifiManager wifiMan;

    private final String MANUFACTURER = Build.MANUFACTURER;
    private final String MODEL = Build.MODEL;

    private String serverURL;
    private String connect_type = "NA";
    private String carrier_name = "NA";
    private String country_name = "NA";
    private String phone_type;
    private String influxhost = "192.168.8.153";// set for real in preferences
    private int influxport = 8086; // set for real in preferences

    private static final int MEASURMEMENT_DB_PERMISSION_REQUEST_CODE = 31001;

    InfluxDBHelper influxhelper;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    public MeasurementDbConsumer(GabrielClientActivity pgabrielClientActivity, String endpoint) {
        gabrielClientActivity = pgabrielClientActivity;
        serverURL = endpoint;
        this.checkPermissions();
        SharedPreferences settings = fetchSettings(pgabrielClientActivity.getApplicationContext());
        influxhost = settings.getString("influxdb_ip",influxhost);
//        influxport = settings.getInt("influxdb_port",influxport);
        // Get the Connection Information
        ConnectivityManager conMan =
                (ConnectivityManager) this.gabrielClientActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            connect_type = conMan.getActiveNetworkInfo().getTypeName();
        } catch (Exception e) {
            Log.e(TAG, "NO ACTIVE CELLULAR NETWORK");
        }

        // Get the Telephony Information
        teleMan = (TelephonyManager) this.gabrielClientActivity.getSystemService(Context.TELEPHONY_SERVICE);
        phone_type = this.getPhoneType(teleMan);

        // Get WiFi Information
        wifiMan = (WifiManager) this.gabrielClientActivity.getSystemService(Context.WIFI_SERVICE);

        // Get the Subscription Information
        SubscriptionManager subMan = (SubscriptionManager) this.gabrielClientActivity.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        List<SubscriptionInfo> subInfo = subMan.getActiveSubscriptionInfoList();
        if (subInfo.size() > 0) {
            carrier_name = (String) subInfo.get(0).getCarrierName();
            country_name = subInfo.get(0).getCountryIso();
        }

        // Get the Location Information
        locMan = (LocationManager) gabrielClientActivity.getSystemService(Context.LOCATION_SERVICE);
        LocationHelper locHelper = new LocationHelper();
        try {
            locMan.requestLocationUpdates("gps", 1000, (float) 1, locHelper);
        } catch (Exception SecurityException) {
            Log.e(TAG, "No Location Permission");
        }

        // Set up the Database
//        String serverBase = serverURL.split(":")[0]; // Assumes InfluxDB on same server as OpenRTIST
//        influxhelper = new InfluxDBHelper(serverBase, this.getServerPort());
        influxhelper = new InfluxDBHelper(influxhost, influxport);
        influxhelper.setSessionID();
        // Run Traceroute
//        String typestr = "traceroute";
//        MeasurementFactory.Measurement tmeasure =
//                new MeasurementFactory.Measurement(typestr, MANUFACTURER, MODEL,
//                        serverURL, connect_type, phone_type, carrier_name, country_name,
//                        "NULL1", "NULL2");
//        influxhelper.AsyncWritePoint(tmeasure);
    }

    @Override
    @SuppressLint({"MissingPermission", "DefaultLocale"})
    public void accept(IntervalMeasurement intervalMeasurement) {
        // Framerate

        double ifps = round(intervalMeasurement.getIntervalFps(), 1);
        double ofps = round(intervalMeasurement.getOverallFps(), 1); // TODO
        String msg = String.format("FRAMERATE: INTERVALFPS = %.1f OVERALLFPS = %.1f", ifps, ofps);
        Log.i(TAG, msg);
        String typestr = "framerate";
        MeasurementFactory.Measurement tmeasure =
                new MeasurementFactory.Measurement(typestr, MANUFACTURER, MODEL,
                        serverURL, connect_type, phone_type, carrier_name, country_name,
                        "INTERVALFPS", "OVERALLFPS");
        tmeasure.setVar0(ifps);
        tmeasure.setVar1(ofps);
        influxhelper.AsyncWritePoint(tmeasure);

        typestr = "allmeasure";
        MeasurementFactory.Measurement tmeasureall =
                new MeasurementFactory.Measurement(typestr, MANUFACTURER, MODEL,
                        serverURL, connect_type, phone_type, carrier_name, country_name,
                        "INTERVALFPS", "OVERALLFPS");
        tmeasureall.setVar0(ifps);
        tmeasureall.setVar1(ofps);

        // Round Trip Time
        double irtt = round(intervalMeasurement.getIntervalRtt(), 1);
        double ortt = round(intervalMeasurement.getOverallRtt(), 1); // TODO
        msg = String.format("ROUND TRIP TIME: INTERVALRTT = %.1f OVERALLRTT =%.1f", irtt, ortt);
        Log.i(TAG, msg);
        typestr = "roundtriptime";
        tmeasure = new MeasurementFactory.Measurement(typestr, MANUFACTURER, MODEL,
                serverURL, connect_type, phone_type, carrier_name, country_name,
                "INTERVALRTT", "OVERALLRTT");
        tmeasure.setVar0(irtt);
        tmeasure.setVar1(ortt);
        influxhelper.AsyncWritePoint(tmeasure);
        tmeasureall.setFieldMap("INTERVALRTT",(float) irtt);
        tmeasureall.setFieldMap("OVERALLRTT",(float) ortt);


        // Location
        Location locationGPS = null;
        try {
            locationGPS = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) { Log.e(TAG,"NO LOCATION PERMISSION"); } // Do Nothing
        double lat;
        double lng;
        if (locationGPS != null) {
            lng = round(locationGPS.getLongitude(), 6);
            lat = round(locationGPS.getLatitude(), 6);
            msg = String.format("LOCATION: LONGITUDE = %.6f LATITUDE = %.6f", lng, lat);
            typestr = "location";
            tmeasure = new MeasurementFactory.Measurement(typestr, MANUFACTURER, MODEL,
                    serverURL, connect_type, phone_type, carrier_name, country_name,
                    "LAT", "LNG");
            tmeasure.setVar0(lat);
            tmeasure.setVar1(lng);
            tmeasure.setTagMap("latitude",Double.toString(lat));
            tmeasure.setTagMap("longitude",Double.toString(lng));
            influxhelper.AsyncWritePoint(tmeasure);

            tmeasureall.setFieldMap("LAT",(float) lat);
            tmeasureall.setFieldMap("LNG",(float) lng);
            tmeasureall.setTagMap("latitude",Double.toString(lat));
            tmeasureall.setTagMap("longitude",Double.toString(lng));
        } else {
            msg = "CURRENT GPS LOCATION UNAVAILABLE";
        }
        Log.i(TAG, msg);


        // Signal Strength
        double cellSignalStrength = -9999; int cellID = 0;
        try {
            List<CellInfo> cellInfo = teleMan.getAllCellInfo();
            cellSignalStrength = getCellSignalStrength(cellInfo);
            cellID = this.getCellID(cellInfo);
        } catch (Exception SecurityException) {
            Log.e(TAG, "NO CELL PERMISSION");
        }

        List<ScanResult> wifiScanList = wifiMan.getScanResults();
        double wifiSignalStrength = this.getWiFiSignalStrength(wifiScanList);

        if (cellSignalStrength != -9999 || wifiSignalStrength != -9999) {
            msg = String.format("SIGNAL STRENGTH: WIFI = %.1f Dbm CELLID %d  CELLULAR = %.1f Dbm",
                    wifiSignalStrength, cellID, cellSignalStrength);
            Log.i(TAG, msg);
            typestr = "signal";
            tmeasure = new MeasurementFactory.Measurement(typestr, MANUFACTURER, MODEL,
                    serverURL, connect_type, phone_type, carrier_name, country_name,
                    "WIFISIGNAL", "CELLSIGNAL");
            tmeasure.setVar0(wifiSignalStrength);
            tmeasure.setVar1(cellSignalStrength);
            tmeasure.setTagMap("CELLID", Integer.toString(cellID));
            influxhelper.AsyncWritePoint(tmeasure);

            tmeasureall.setFieldMap("WIFISIGNAL",(float) wifiSignalStrength);
            tmeasureall.setFieldMap("CELLSIGNAL",(float) cellSignalStrength);
            tmeasureall.setTagMap("CELLID", Integer.toString(cellID));
        } else {
            Log.e(TAG, "CURRENT SIGNAL STRENGTH UNAVAILABLE");
        }
        influxhelper.AsyncWritePoint(tmeasureall);

    }

    private int getCellID(List<CellInfo> cellInfos) {
        int cellID = -9999;
        if (cellInfos != null) {
            for (int i = 0; i < cellInfos.size(); i++) {
                if (cellInfos.get(i).isRegistered()) {
                    if (cellInfos.get(i) instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                        CellIdentityLte identityLte = cellInfoLte.getCellIdentity();
                        cellID = identityLte.getCi();
                    }
                }
            }
        }
        return cellID;
    }

    private String getPhoneType(TelephonyManager teleMan) {
        int phoneTypeInt = teleMan.getPhoneType();
        String phoneType = null;
        phoneType = phoneTypeInt == TelephonyManager.PHONE_TYPE_GSM ? "gsm" : phoneType;
        phoneType = phoneTypeInt == TelephonyManager.PHONE_TYPE_CDMA ? "cdma" : phoneType;
        return phoneType;
    }

    private static double getCellSignalStrength(List<CellInfo> cellInfos) throws SecurityException {
        double strength = -9999;
        if(cellInfos != null) {
            for (int i = 0 ; i < cellInfos.size() ; i++) {
                if (cellInfos.get(i).isRegistered()) {
                    if(cellInfos.get(i) instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                        CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                        strength = cellSignalStrengthLte.getDbm();
                    }
                    // Assume LTE Network for now
//                    } else if (cellInfos.get(i) instanceof CellInfoWcdma) {
//                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
//                        CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
//                        strength = cellSignalStrengthWcdma.getDbm();
//                    } else if (cellInfos.get(i) instanceof CellInfoGsm) {
//                        CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
//                        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
//                        strength = cellSignalStrengthGsm.getDbm();
//                    } else if (cellInfos.get(i) instanceof CellInfoCdma) {
//                        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
//                        CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
//                        strength = cellSignalStrengthCdma.getDbm();
//                    }
                }
            }
        }
        return MeasurementDbConsumer.round(strength,1);
    }
    private double getWiFiSignalStrength(List<ScanResult> wifiScanList) {
        double nn = wifiScanList.size();
        double cummsignalLevel = 0;
        for (ScanResult result : wifiScanList) {
            cummsignalLevel += result.level;
        }
        return MeasurementDbConsumer.round(cummsignalLevel/nn,1);
    }

    private int getServerPort()  { return InfluxDBHelper.getInfluxDBPort(); }

    public static double round(double value, int places) {
        BigDecimal bd;
        double retdoub = -9999;
        if (places < 0) throw new IllegalArgumentException();
        try {
            bd = BigDecimal.valueOf(value);
            bd = bd.setScale(places, RoundingMode.HALF_UP);
            retdoub = bd.doubleValue();
        } catch (Exception ignored) { } // Do Nothing
        return retdoub;
    }


    // Deal with the incremental permissions -- only want to request these permissions for the measurment collection
    // So, don't take the easy way and put them in ServerListActivity.

    private void checkPermissions() {
        // Try a few times to get permissions -- then leave out fields that depend on denied perms
        if (!checkPermAttemptCount()) {
            Log.e(TAG, "Exceeded Permission Attempt Count");
            return;
        }
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
        };
        boolean haverequested = false;
        for (String perm:permissions) {
            String [] permlst = {perm};
            int permState = ContextCompat.checkSelfPermission(gabrielClientActivity, perm);
            switch (permState){
                case PackageManager.PERMISSION_GRANTED:
                    continue;
                case PackageManager.PERMISSION_DENIED:
                    Log.e(TAG, String.format("%s denied",perm));
                default:
                    requestPermissions(gabrielClientActivity,
                            permlst, MEASURMEMENT_DB_PERMISSION_REQUEST_CODE);
                    haverequested = true;
            }
        }
        if (haverequested) {
            incrementPermAttempts();
        }
    }
    private boolean checkPermAttemptCount() {
        final int MAX_PERM_ATTEMPTS = 10;
        SharedPreferences permPref = getPermPrefs();
        int permAttempts = permPref.getInt("attemptcount",0);
        if (permAttempts >= MAX_PERM_ATTEMPTS) {
            Log.i(TAG,String.format("Permission Attempts Exceeded: %d of %d" ,permAttempts,MAX_PERM_ATTEMPTS));
            return false;
        } else {
            Log.i(TAG,String.format("Permission Attempts OK: %d of %d" ,permAttempts,MAX_PERM_ATTEMPTS));
            return true;
        }
    }

    public SharedPreferences fetchSettings(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
    @SuppressLint("ApplySharedPref")
    private void incrementPermAttempts () {
        SharedPreferences permPref = getPermPrefs();
        int permAttempts = permPref.getInt("attemptcount",0);
        SharedPreferences.Editor editor = permPref.edit();
        editor.putInt("attemptcount", ++permAttempts);
        editor.commit();
    }
    private SharedPreferences getPermPrefs() {
        String spname = "permissionattempts";
        return gabrielClientActivity.getSharedPreferences(spname, Context.MODE_PRIVATE);
    }
}