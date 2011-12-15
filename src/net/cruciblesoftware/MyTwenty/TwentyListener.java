package net.cruciblesoftware.MyTwenty;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

class TwentyListener implements LocationListener {
    private static final String TAG = "20: " + TwentyListener.class.getSimpleName();
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private boolean isGpsLive = false;
    private boolean isContinuous = false;
    private boolean dialogDisplayed = false;

    private Location bestLocation;

    private ProgressDialog progressDialog;

    private final LocationManager locManager;
    private final MyTwentyActivity activity;
    private final ReverseGeocoder geoService;

    TwentyListener(MyTwentyActivity act) {
        super();
        activity = act;
        locManager = (LocationManager)(activity.getSystemService(Context.LOCATION_SERVICE));
        geoService = new ReverseGeocoder(activity);

        Location passLoc = locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        Location netLoc = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location gpsLoc= locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(isBetterLocation(passLoc))
            bestLocation = passLoc;
        if(isBetterLocation(netLoc))
            bestLocation = netLoc;
        if(isBetterLocation(gpsLoc))
            bestLocation = gpsLoc;
        onLocationChanged(bestLocation);

        activateListener();     // references 'this'!
        if(!isContinuous)
            displayNotification();
    }

    @Override
    public void onLocationChanged(Location loc) {
        Log.d(TAG, "got " + loc.getProvider() + " location: (" + loc.getLatitude() + ", " +
                loc.getLongitude() + ") accuracy=" + loc.getAccuracy() + " time=" + loc.getTime());
        // first test the incoming location
        if(isBetterLocation(loc))
            bestLocation = loc;
        else
            return;

        // grab the lat/lon and set the UI components
        double lat = bestLocation.getLatitude();
        double lon = bestLocation.getLongitude();
        activity.setLatLon(lat, lon);
        activity.setAddress(geoService.getAddress(lat, lon));
        activity.setMap(lat, lon);

        if(!isContinuous)
            deactivateListener();
        if(dialogDisplayed) {
            Log.d(TAG, "dialog canceled by LocationChange event");
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
            Log.d(TAG, "gps service is unavail, isContinuous=" + isContinuous);
            toast = Toast.makeText(activity, R.string.toast_gps_unavail, Toast.LENGTH_SHORT);
            toast.show();
            deactivateListener();
            break;

        case LocationProvider.TEMPORARILY_UNAVAILABLE:
            Log.d(TAG, "gps service is tmp unavail, isContinuous=" + isContinuous);
            deactivateListener();
            break;

        case LocationProvider.AVAILABLE:
            Log.d(TAG, "gps service is totes avail, isContinuous=" + isContinuous);
            toast = Toast.makeText(activity, R.string.toast_gps_avail, Toast.LENGTH_SHORT);
            toast.show();
            if(isContinuous || isGpsLive)
                activateListener();
            break;
        }
    }

    void deactivateListener() {
        if(isGpsLive) {
            Log.d(TAG, "deactivating listener");
            locManager.removeUpdates(this);
            isGpsLive = false;
        }
    }

    void hideNotification() {
        if(dialogDisplayed) {
            progressDialog.dismiss();
            progressDialog = null;
            dialogDisplayed = false;
        }
    }

    void refresh() {
        activateListener();
        if(!isContinuous)
            displayNotification();
    }

    void setContinuous(boolean continuous) {
        isContinuous = continuous;
        if(isContinuous)
            activateListener();
        if(!isContinuous)
            deactivateListener();
    }

    void activateListener() {
        if(!isGpsLive) {
            Log.d(TAG, "activating listener");
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            //locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
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
                        Log.d(TAG, "dialog canceled by user");
                        deactivateListener();
                        dialogDisplayed = false;
                    }
                });
        progressDialog.show();
        dialogDisplayed = true;
    }

    /* Logic copied from
     * http://developer.android.com/guide/topics/location/obtaining-user-location.html
     */
    private boolean isBetterLocation(Location location) {
        Log.d(TAG, "comparing " + location.getProvider() + " location: (" + location.getLatitude() + ", " +
                location.getLongitude() + ") accuracy=" + location.getAccuracy() + " time=" + location.getTime());

        if(bestLocation == null) {
            Log.d(TAG, "accepting new location because no current location");
            return true;
        }
        else if(location == null) {
            Log.d(TAG, "rejecting new location because it is null");
            return false;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - bestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if(isSignificantlyNewer) {
            Log.d(TAG, "accepting new location because it is significantly newer");
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if(isSignificantlyOlder) {
            Log.d(TAG, "rejecting new location because it is significantly older");
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - bestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                bestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if(isMoreAccurate) {
            Log.d(TAG, "accepting new location because it is more accurate");
            return true;
        } else if(isNewer && !isLessAccurate) {
            Log.d(TAG, "accepting new location because it is newer and equally accurate");
            return true;
        } else if(isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            Log.d(TAG, "accepting new location because it is newer, not less accurate, and from same provider");
            return true;
        }
        Log.d(TAG, "rejecting new location because it matched no acceptance criteria");
        return false;
    }

    // Check whether two providers are the same
    private boolean isSameProvider(String provider1, String provider2) {
        if(provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
