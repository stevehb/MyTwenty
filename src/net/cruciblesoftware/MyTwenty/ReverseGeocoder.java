package net.cruciblesoftware.MyTwenty;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;

class ReverseGeocoder {
    private static final String TAG = "20: " + ReverseGeocoder.class.getSimpleName();

    private boolean isAvail;
    private boolean hasAddress;
    private final Geocoder geo;
    private final String NOT_AVAILABLE;
    private final String NO_ADDRESS;
    private final String IO_EXCEPTION;

    private class GetAddressTask extends AsyncTask<Double, Void, String> {
        private final MyTwentyActivity my20;

        public GetAddressTask(MyTwentyActivity activity) {
            my20 = activity;
        }

        @Override
        protected String doInBackground(Double... latlon) {
            double lat = latlon[0];
            double lon = latlon[1];
            List<Address> addresses;
            StringBuilder buff = new StringBuilder();
            try {
                // get addresses
                addresses = geo.getFromLocation(lat, lon, 1);
                if(addresses.isEmpty()) {
                    hasAddress = false;
                    return NO_ADDRESS;
                }

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
                DebugFile.log(TAG, "detected feature name: " + feature);
                if(!buff.toString().toLowerCase().contains(feature.toLowerCase())) {
                    buff = new StringBuilder(feature + "\n" + buff.toString());
                }

                hasAddress = true;
                return buff.toString();
            } catch (IOException e) {
                DebugFile.log(TAG, IO_EXCEPTION + " " + e.getLocalizedMessage());
                if(e.getLocalizedMessage().equalsIgnoreCase("service not available")) {
                    DebugFile.log(TAG, "disabling service");
                    isAvail = false;
                    return NOT_AVAILABLE;
                }
                hasAddress = false;
                return IO_EXCEPTION + e.getLocalizedMessage();
            }
        }

        @Override
        protected void onPostExecute(String newAddress) {
            // update address back on UI thread
            my20.setAddress(newAddress);
        }
    }

    ReverseGeocoder(Context c) {
        NOT_AVAILABLE = c.getString(R.string.geo_not_available);
        NO_ADDRESS = c.getString(R.string.geo_no_address);
        IO_EXCEPTION = c.getString(R.string.geo_io_exception);
        hasAddress = false;

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

    void setAddress(MyTwentyActivity my20, double lat, double lon) {
        if(!isAvail) {
            hasAddress = false;
            my20.setAddress(NOT_AVAILABLE);
            return;
        }
        new GetAddressTask(my20).execute(new Double[] { lat, lon });
    }

    boolean hasAddress() {
        return hasAddress;
    }
}
