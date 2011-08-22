package net.cruciblesoftware.MyTwenty;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class TwentyListener implements LocationListener {
    public static double lat;
    public static double lon;

    boolean isGpsLive = false;
    boolean isContinuous = false;
    boolean dialogDisplayed = false;
    private ProgressDialog progressDialog;
    private final LocationManager locManager;
    private final MyTwentyActivity activity;
    private final GeoService geoService;

    public TwentyListener(LocationManager mgr, MyTwentyActivity act) {
        super();
        activity = act;
        locManager = mgr;
        geoService = new GeoService(activity);
        activateListener();     // references 'this'
        if(!isContinuous)
            displayNotification();
    }

    @Override
    public void onLocationChanged(Location loc) {
        // grab the lat/lon and release the listener
        lat = loc.getLatitude();
        lon = loc.getLongitude();
        activity.setLatLon(lat, lon);
        activity.setAddress(geoService.getAddress(lat, lon));
        Log.d("***", "location received. isContinuous=" + isContinuous + ", dialogDisplayed=" + dialogDisplayed);
        if(!isContinuous)
            deactivateListener();
        if(dialogDisplayed) {
            Log.d("***", "dialog canceled by LocationChange event");
            hideNotification();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        deactivateListener();
    }

    @Override
    public void onProviderEnabled(String provider) {
        if(isContinuous || isGpsLive)
            activateListener();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Toast toast;
        switch(status) {
        case LocationProvider.OUT_OF_SERVICE:
            Log.d("***", "gps service is unavail, isContinuous=" + isContinuous);
            toast = Toast.makeText(activity, R.string.toast_gps_unavail, Toast.LENGTH_SHORT);
            toast.show();
            deactivateListener();
            break;

        case LocationProvider.TEMPORARILY_UNAVAILABLE:
            Log.d("***", "gps service is tmp unavail, isContinuous=" + isContinuous);
            deactivateListener();
            break;

        case LocationProvider.AVAILABLE:
            Log.d("***", "gps service is totes avail, isContinuous=" + isContinuous);
            toast = Toast.makeText(activity, R.string.toast_gps_avail, Toast.LENGTH_SHORT);
            toast.show();
            if(isContinuous || isGpsLive)
                activateListener();
            break;
        }
    }

    public void deactivateListener() {
        if(isGpsLive) {
            Log.d("***", "deactivating listener");
            locManager.removeUpdates(this);
            isGpsLive = false;
        }
    }

    public void hideNotification() {
        if(dialogDisplayed) {
            progressDialog.dismiss();
            progressDialog = null;
            dialogDisplayed = false;
        }
    }

    public void refresh() {
        activateListener();
        if(!isContinuous)
            displayNotification();
    }

    public void setContinuous(boolean continuous) {
        isContinuous = continuous;
        if(isContinuous)
            activateListener();
        if(!isContinuous)
            deactivateListener();
    }

    private void activateListener() {
        if(!isGpsLive) {
            Log.d("***", "activating listener");
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            isGpsLive = true;
        }
    }

    private void displayNotification() {
        if(dialogDisplayed)
            return;
        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle(activity.getString(R.string.progress_dialog_title));
        progressDialog.setMessage(activity.getString(R.string.progress_dialog_message));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(
                new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Log.d("D", "dialog canceled by user");
                        deactivateListener();
                        dialogDisplayed = false;
                    }
                });
        progressDialog.show();
        dialogDisplayed = true;
    }
}
