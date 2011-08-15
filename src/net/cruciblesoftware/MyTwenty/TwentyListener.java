package net.cruciblesoftware.MyTwenty;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class TwentyListener implements LocationListener {
    public static double lat;
    public static double lon;

    private boolean isGpsLive = false;
    private boolean isContinuous = false;
    private boolean dialogDisplayed = false;
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
        MyTwentyActivity.setLatLon(lat, lon);
        MyTwentyActivity.setAddress(geoService.getAddress(lat, lon));
        if(!isContinuous)
            deactivateListener();
        if(dialogDisplayed) {
            progressDialog.dismiss();
            dialogDisplayed = false;
        }

    }

    @Override
    public void onProviderDisabled(String provider) {
        deactivateListener();
    }

    @Override
    public void onProviderEnabled(String provider) {
        activateListener();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        /*
        switch(status) {
        case LocationProvider.OUT_OF_SERVICE:
        case LocationProvider.TEMPORARILY_UNAVAILABLE:
            deactivateListener();
            break;

        case LocationProvider.AVAILABLE:
            activateListener();
            break;
        }
         */
    }

    public void deactivateListener() {
        if(isGpsLive) {
            locManager.removeUpdates(this);
            isGpsLive = false;
        }
    }

    public void refresh() {
        activateListener();
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
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            isGpsLive = true;
        }
    }

    private void displayNotification() {
        progressDialog = ProgressDialog.show(activity,
                activity.getString(R.string.progress_dialog_title),
                activity.getString(R.string.progress_dialog_message), true, true,
                new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                deactivateListener();
                dialogDisplayed = false;
            }
        });
    }
}
