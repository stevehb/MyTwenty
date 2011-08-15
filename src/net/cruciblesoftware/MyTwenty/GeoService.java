package net.cruciblesoftware.MyTwenty;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

public class GeoService {

    private final Geocoder geo;
    private final Context context;

    public GeoService(Context c) {
        context = c;
        geo = new Geocoder(context, Locale.getDefault());
    }

    public String getAddress(double lat, double lon) {
        List<Address> addresses;
        try {

            addresses = geo.getFromLocation(lat, lon, 1);
            StringBuilder buff = new StringBuilder();
            if(addresses.isEmpty())
                return context.getString(R.string.geo_no_address);
            else {
                Address a = addresses.get(0);
                int maxLines = a.getMaxAddressLineIndex();
                // first build the address
                for(int i = 0; i < maxLines; i++) {
                    buff.append(a.getAddressLine(i));
                    if((i + 1) < maxLines)
                        buff.append("\n");
                }
                // then prepend the feature if it's anything interesting
                String feature = a.getFeatureName();
                if(!buff.toString().toLowerCase().contains(feature.toLowerCase()))
                    buff = new StringBuilder(feature + "\n" + buff.toString());
            }
            return buff.toString();
        } catch (IOException e) {
            return context.getString(R.string.geo_io_exception);
            /*
            try {
                HttpClient client = new DefaultHttpClient();
                URI uri;
                uri = new URI("http://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                        Double.toString(lat) + "," + Double.toString(lon) + "&sensor=true");
                HttpGet get = new HttpGet(uri);
                HttpResponse resp = client.execute(get);

                resp.getEntity().getContent().read(b)
            } catch (URISyntaxException ex) {
                return e.getMessage();
            } catch (ClientProtocolException ex) {
                return e.getMessage();
            } catch (IOException ex) {
                return e.getMessage();
            }
             */
        }
    }
}
