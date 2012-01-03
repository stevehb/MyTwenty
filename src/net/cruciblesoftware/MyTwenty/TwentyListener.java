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
import android.provider.Settings;
import android.widget.Toast;

class TwentyListener implements LocationListener {
    private static final String TAG = "20: " + TwentyListener.class.getSimpleName();
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private boolean isNetAvail = false;
    private boolean isNetLive = false;
    private boolean isGpsAvail = false;
    private boolean isGpsLive = false;
    private boolean isContinuous = false;
    private boolean dialogDisplayed = false;

    private ProgressDialog progressDialog;

    Location bestLocation;
    private final LocationManager locManager;
    private final MyTwentyActivity activity;

    TwentyListener(MyTwentyActivity act) {
        super();
        activity = act;
        locManager = (LocationManager)(activity.getSystemService(Context.LOCATION_SERVICE));
    }

    @Override
    public void onLocationChanged(Location loc) {
        if(loc != null) {
            DebugFile.log(TAG, "onLocationChanged(): " + loc.getProvider() + " location: (" + loc.getLatitude() + ", " +
                    loc.getLongitude() + ") accuracy=" + loc.getAccuracy() + " time=" + loc.getTime());
        }

        // first test the incoming location
        if(isBetterLocation(loc))
            bestLocation = loc;
        else
            return;

        // grab the lat/lon and set the UI components
        double lat = bestLocation.getLatitude();
        double lon = bestLocation.getLongitude();
        activity.setLatLon(lat, lon);
        activity.setMap(lat, lon, bestLocation.getAccuracy());
        activity.setAddress(bestLocation);

        // wait for gps results, unless they are not available or these are gps results
        if(!isGpsAvail || loc.getProvider().equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
            if(!isContinuous) {
                DebugFile.log(TAG, "onLocationChanged() is deactivating the listener");
                deactivateListeners();
            }
            if(dialogDisplayed) {
                DebugFile.log(TAG, "dialog canceled by LocationChange event");
                hideNotification();
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if(provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
            isNetAvail = false;
        } else if(provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
            isGpsAvail = false;
        }
        if(!isNetAvail && !isGpsAvail) {
            DebugFile.log(TAG, "onProviderDisabled() is deactivating the listener: isNetAvail=" + isNetAvail + ", isGpsAvail=" + isGpsAvail);
            deactivateListeners();
        }
        DebugFile.log(TAG, "disabled provider " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        if(provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
            isNetAvail = true;
        } else if(provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
            isGpsAvail = true;
        }
        DebugFile.log(TAG, "enabled provider " + provider);
    }

    /* I think this method is called only on Android 1.6. See
     * http://code.google.com/p/android/issues/detail?id=9433
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        DebugFile.log(TAG, "onStatusChanged(" + provider + ")");
        Toast toast;
        switch(status) {
        case LocationProvider.OUT_OF_SERVICE:
            DebugFile.log(TAG, provider + " service is out of service, isContinuous=" + isContinuous);
            if(provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
                isNetAvail = false;
                toast = Toast.makeText(activity, R.string.toast_net_unavail, Toast.LENGTH_SHORT);
                toast.show();
            } else if(provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
                isGpsAvail = false;
                toast = Toast.makeText(activity, R.string.toast_gps_unavail, Toast.LENGTH_SHORT);
                toast.show();
            }
            break;

        case LocationProvider.TEMPORARILY_UNAVAILABLE:
            DebugFile.log(TAG, provider + " service is temp unavail, isContinuous=" + isContinuous);
            break;

        case LocationProvider.AVAILABLE:
            DebugFile.log(TAG, provider + " service is now avail, isContinuous=" + isContinuous);
            if(provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
                isNetAvail = true;
                toast = Toast.makeText(activity, R.string.toast_net_avail, Toast.LENGTH_SHORT);
                toast.show();
            } else if(provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
                isGpsAvail = true;
                toast = Toast.makeText(activity, R.string.toast_gps_avail, Toast.LENGTH_SHORT);
                toast.show();
            }
            break;
        }
    }

    void activateListeners() {
        if(isNetAvail && !isNetLive) {
            DebugFile.log(TAG, "activating net listener");
            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            isNetLive = true;
        }
        if(isGpsAvail && !isGpsLive) {
            DebugFile.log(TAG, "activating gps listener");
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            isGpsLive = true;
        }
    }

    void deactivateListeners() {
        if(isGpsLive || isNetLive) {
            DebugFile.log(TAG, "now deactivating the listener");
            locManager.removeUpdates(this);
            isGpsLive = false;
            isNetLive = false;
        }
    }

    void loadLastKnown() {
        DebugFile.log(TAG, "resuming the listener");

        String provider = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        DebugFile.log(TAG, "locations providers allowed: " + provider);
        // find out who is available
        isNetAvail = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        isGpsAvail = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        DebugFile.log(TAG, "tested providers: isNetAvail=" + isNetAvail + ", isGpsAvail=" + isGpsAvail);

        // send update with possible known locations
        onLocationChanged(locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
        onLocationChanged(locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
        onLocationChanged(locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
    }

    void hideNotification() {
        if(dialogDisplayed) {
            progressDialog.dismiss();
            progressDialog = null;
            dialogDisplayed = false;
        }
    }

    void refresh() {
        activateListeners();
        if(!isContinuous)
            displayNotification();
    }

    void setContinuous(boolean continuous) {
        isContinuous = continuous;
        if(isContinuous)
            activateListeners();
        if(!isContinuous) {
            DebugFile.log(TAG, "setContinuous() is deactivating the listener");
            deactivateListeners();
        }
    }

    private void displayNotification() {
        if(dialogDisplayed)
            return;

        /* TODO
         * Update these messages dynamically to better reflect what
         * information is coming in from location providers.
         */
        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle(activity.getString(R.string.progress_dialog_title));
        progressDialog.setMessage(activity.getString(R.string.progress_dialog_message));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setOnCancelListener(
                new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        DebugFile.log(TAG, "dialog canceled by user");
                        DebugFile.log(TAG, "onCancelListener() is deactivating the listener");
                        deactivateListeners();
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
        if(location == null) {
            DebugFile.log(TAG, "rejecting new location because it is null");
            return false;
        } else if(bestLocation == null) {
            DebugFile.log(TAG, "accepting new location because no current location");
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - bestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if(isSignificantlyNewer) {
            DebugFile.log(TAG, "accepting new location because it is significantly newer");
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if(isSignificantlyOlder) {
            DebugFile.log(TAG, "rejecting new location because it is significantly older");
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
            DebugFile.log(TAG, "accepting new location because it is more accurate");
            return true;
        } else if(isNewer && !isLessAccurate) {
            DebugFile.log(TAG, "accepting new location because it is newer and equally accurate");
            return true;
        } else if(isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            DebugFile.log(TAG, "accepting new location because it is newer, not less accurate, and from same provider");
            return true;
        }
        DebugFile.log(TAG, "rejecting new location because it matched no acceptance criteria");
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
