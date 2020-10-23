package edu.cmu.cs.measurementDb;
import android.app.Activity;
import android.location.LocationListener;
import android.os.Bundle;

public class LocationHelper extends Activity implements LocationListener {
    public void onCreate() {
        ;
    }

    @Override
    public void onLocationChanged(android.location.Location location) {
        double latitude=location.getLatitude();
        double longitude=location.getLongitude();
        String msg="New Latitude: "+latitude + " New Longitude: "+longitude;
//        Log.i("LOCATION CHANGED HELPER", msg);
        ;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}