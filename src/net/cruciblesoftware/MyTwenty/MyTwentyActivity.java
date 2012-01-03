package net.cruciblesoftware.MyTwenty;

import java.text.DecimalFormat;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class MyTwentyActivity extends MapActivity {
    private static final String TAG = "20: " + MyTwentyActivity.class.getSimpleName();

    private static final int EARTH_RADIUS = 6371000;
    private static final int MAX_ZOOM = 18;
    private int zoomLevel = MAX_ZOOM;
    private DecimalFormat decimalFormatter;

    private TextView txtLat, txtLon;
    private MapView mapView;
    private ToggleButton btnContinuous;

    private AddressSystem address;
    private TwentyListener locListener;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        locListener = new TwentyListener(this);
        decimalFormatter = new DecimalFormat("0.0000000");

        setContentView(R.layout.main);
        txtLat = (TextView) (findViewById(R.id.txtLatValue));
        txtLon = (TextView) (findViewById(R.id.txtLonValue));

        // set up address box
        address = new AddressSystem(this);

        // set up the map
        mapView = (MapView) (findViewById(R.id.map));
        mapView.setBuiltInZoomControls(true);
        mapView.setSatellite(true);
        mapView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // compose the string
                String uri;
                double lat = locListener.bestLocation.getLatitude();
                double lon = locListener.bestLocation.getLongitude();
                if(address.hasAddress()) {
                    uri = "geo:0,0?q=" + lat + "," + lon + " (" + address.toString() + ")";
                } else {
                    uri = "geo:" + lat + "," + lon + "?" + "z=" + zoomLevel;
                }
                DebugFile.log(TAG, "loading map using uri=" + uri);

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                MyTwentyActivity.this.startActivity(intent);
                return false;
            }
        });

        // Refresh and continuous buttons
        Button refresh = (Button) (findViewById(R.id.btnRefresh));
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locListener.refresh();
            }
        });
        btnContinuous = (ToggleButton) (findViewById(R.id.btnContinuous));
        btnContinuous.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                locListener.setContinuous(isChecked);
            }
        });
    }

    void setLatLon(double lat, double lon) {
        txtLat.setText(decimalFormatter.format(lat));
        txtLon.setText(decimalFormatter.format(lon));
    }

    void setAddress(Location loc) {
        address.updateAddress(loc);
    }

    void setMap(double lat, double lon, double accuracy) {
        MapController mc = mapView.getController();
        mc.setCenter(new GeoPoint((int) (lat * 1000000), (int) (lon * 1000000)));

        // the calculations below don't yet work well enough
        // so short circuit that until later
        zoomLevel = MAX_ZOOM;
        mc.setZoom(zoomLevel);
        if (true)
            return;

        // otherwise calculate lat/lon radius for zoom
        // from http://www.movable-type.co.uk/scripts/latlong.html
        final double angDist = accuracy / EARTH_RADIUS;
        final double bearing = 0;
        final double lat1 = Math.toRadians(lat);
        final double lon1 = Math.toRadians(lon);
        final double cosLat1 = Math.cos(lat1);
        final double sinLat1 = Math.sin(lat1);
        final double cosAngDist = Math.cos(angDist);
        final double sinAngDist = Math.sin(angDist);
        final double lat2 = Math.asin(sinLat1 * cosAngDist + cosLat1
                * sinAngDist * Math.cos(bearing));
        final double lon2 = lon1
                + Math.atan2(Math.sin(bearing) * sinAngDist * cosLat1,
                        cosAngDist - sinLat1 * Math.sin(lat2));

        final double latSpan = Math.abs(lat2 - lat1) * 1200;
        final double lonSpan = Math.abs(lon2 - lon1) * 1200;
        mc.zoomToSpan((int) (latSpan * 1000000), (int) (lonSpan * 1000000));
        zoomLevel = mapView.getZoomLevel();
        if (zoomLevel > MAX_ZOOM) {
            zoomLevel = MAX_ZOOM;
            mc.setZoom(zoomLevel);
        }

        // check against max zoom
        if (accuracy <= 1.0) {
            zoomLevel = MAX_ZOOM;
            mc.setZoom(zoomLevel);
        }

        DebugFile.log(TAG, "point (" + lat1 + "," + lon1 + ") with accuracy="
                + accuracy + " gives latSpan=" + latSpan + ", lonSpan="
                + lonSpan + ", zoomLevel=" + zoomLevel);
    }

    @Override
    public void onPause() {
        DebugFile.log(TAG, "pausing listener");
        locListener.setContinuous(false);
        btnContinuous.setChecked(false);
        locListener.deactivateListeners();
        locListener.hideNotification();
        super.onPause();
    }

    @Override
    public void onResume() {
        DebugFile.log(TAG, "calling listener resume");
        locListener.loadLastKnown();
        super.onResume();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        // improve background gradient quality on some phones
        this.getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
