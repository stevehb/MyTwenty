package net.cruciblesoftware.MyTwenty;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

class ReverseGeocoder {
    private static final String TAG = "20: " + ReverseGeocoder.class.getSimpleName();

    private boolean isAvail;
    private final Geocoder geo;
    private final String NOT_AVAILABLE;
    private final String NO_ADDRESS;
    private final String IO_EXCEPTION;

    ReverseGeocoder(Context c) {
        NOT_AVAILABLE = c.getString(R.string.geo_not_available);
        NO_ADDRESS = c.getString(R.string.geo_no_address);
        IO_EXCEPTION = c.getString(R.string.geo_io_exception);

        /* TODO
         * Instead of just failing on the reverse geocode, get a work around
         * to depends on Google's map API. Use a hack like this:
         * https://code.google.com/p/android/issues/detail?id=8816#c21
         * But with a service like this:
         * https://code.google.com/apis/maps/documentation/geocoding/#ReverseGeocoding
         * 
         * Note that even if a service isPresent(), it's not necessarily
         * available. Testing whether it's functional will require a real test
         * and a check for the IOException.
         */

        isAvail = Geocoder.isPresent();
        if(isAvail)
            geo = new Geocoder(c, Locale.getDefault());
        else
            geo = null;
    }

    String getAddress(double lat, double lon) {
        if(!isAvail)
            return NOT_AVAILABLE;

        List<Address> addresses;
        StringBuilder buff = new StringBuilder();
        try {
            // get addresses
            addresses = geo.getFromLocation(lat, lon, 1);
            if(addresses.isEmpty())
                return NO_ADDRESS;

            // build the address string
            Address a = addresses.get(0);
            int maxLines = a.getMaxAddressLineIndex();
            for(int i = 0; i < maxLines; i++) {
                buff.append(a.getAddressLine(i));
                if((i + 1) < maxLines)
                    buff.append("\n");
            }

            // prepend the feature if it's not already in address
            String feature = a.getFeatureName();
            Log.d(TAG, "detected feature name: " + feature);
            if(!buff.toString().toLowerCase().contains(feature.toLowerCase())) {
                buff = new StringBuilder(feature + "\n" + buff.toString());
            }

            return buff.toString();
        } catch (IOException e) {
            Log.w(TAG, "WARNING: I/O exception while reverse geocoding: " + e.getLocalizedMessage());
            if(e.getLocalizedMessage().equalsIgnoreCase("service not available")) {
                Log.d(TAG, "disabling service");
                isAvail = false;
                return NOT_AVAILABLE;
            }
            return IO_EXCEPTION + e.getLocalizedMessage();
        }
    }
}
