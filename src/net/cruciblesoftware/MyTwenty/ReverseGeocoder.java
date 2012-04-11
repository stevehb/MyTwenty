package net.cruciblesoftware.MyTwenty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;

class ReverseGeocoder {
    private static final String TAG = "20: " + ReverseGeocoder.class.getSimpleName();

    private boolean hasAddress;
    private final Context context;
    private final String NOT_AVAILABLE;
    private final String NO_ADDRESS;
    private final String GEOCODE_EXCEPTION;
    private final String GEOCODE_ERROR;

    private class GetAddressTask extends AsyncTask<Void, Void, String> {
        private final AddressSystem address;
        private final Location location;

        public GetAddressTask(AddressSystem as, Location loc) {
            address = as;
            location = loc;
        }

        @Override
        protected String doInBackground(Void... v) {
            boolean useGeocoder = Geocoder.isPresent();
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            String addressString = "";

            /* Use the built-in Geocoder service. This is the usual case, but
             * it's not available on the emulator and off-brand phones.
             */
            if(useGeocoder) {
                try {
                    List<Address> addresses;
                    Geocoder geo = new Geocoder(context, Locale.getDefault());
                    // get addresses
                    addresses = geo.getFromLocation(lat, lon, 1);
                    if(addresses == null || addresses.isEmpty()) {
                        hasAddress = false;
                        return NO_ADDRESS;
                    }

                    // build the address string
                    Address a = addresses.get(0);
                    int maxLines = a.getMaxAddressLineIndex();
                    StringBuilder addressBuilder = new StringBuilder();
                    String delim = context.getString(R.string.address_delimiter);
                    for(int i = 0; i < maxLines; i++) {
                        addressBuilder.append(a.getAddressLine(i));
                        if((i + 1) < maxLines)
                            addressBuilder.append(delim);
                    }

                    // prepend the feature if it's not already in address
                    String feature = a.getFeatureName();
                    DebugLog.log(TAG, "detected feature name: " + feature);
                    if(!addressBuilder.toString().toLowerCase().contains(feature.toLowerCase())) {
                        addressBuilder = new StringBuilder(feature + "\n" + addressBuilder.toString());
                    }
                    addressString = addressBuilder.toString();
                    hasAddress = true;
                } catch (IOException e) {
                    useGeocoder = false;
                }
            }

            /* As a back-up, and for emulator correctness, try to get the
             * address from Google's web reverse geocoder. More details at
             * https://developers.google.com/maps/documentation/geocoding/#ReverseGeocoding
             */
            if(!useGeocoder) {
                try {
                    // get the json response from Google
                    String urlReq = //
                            "http://maps.googleapis.com/maps/api/geocode/" +
                            "json?latlng=" + lat + "," + lon + "&sensor=true";
                    DebugLog.log(TAG, "using request url:\n\t" + urlReq);
                    HttpClient client = new DefaultHttpClient();
                    HttpResponse resp;
                    resp = client.execute(new HttpGet(urlReq));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
                    String line = "";
                    StringBuilder jsonBuff = new StringBuilder();
                    while((line = reader.readLine()) != null) { jsonBuff.append(line); }

                    // turn the JSON into a postal address
                    JSONObject json = new JSONObject(jsonBuff.toString());
                    addressString = json.getJSONArray("results").getJSONObject(0).getString("formatted_address");
                    hasAddress = true;
                } catch (Exception e) {
                    String msg = GEOCODE_EXCEPTION + " " + e.getLocalizedMessage();
                    DebugLog.log(TAG, msg);
                    hasAddress = false;
                    return GEOCODE_ERROR;
                }
            }
            DebugLog.log(TAG, "using this address: " + addressString);
            return addressString;
        }

        @Override
        protected void onPostExecute(String newAddress) {
            address.onAddressLookupComplete(newAddress, location);
        }
    }

    ReverseGeocoder(Context c) {
        NOT_AVAILABLE = c.getString(R.string.geo_not_available);
        NO_ADDRESS = c.getString(R.string.geo_no_address);
        GEOCODE_EXCEPTION = c.getString(R.string.geo_exception);
        GEOCODE_ERROR = c.getString(R.string.geo_error);
        hasAddress = false;
        context = c;
    }

    void startLookup(AddressSystem address, Location loc) {
        new GetAddressTask(address, loc).execute(new Void[] { });
    }

    boolean hasAddress() {
        return hasAddress;
    }
}
