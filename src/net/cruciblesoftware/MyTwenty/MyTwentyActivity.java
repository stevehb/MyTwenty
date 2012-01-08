package net.cruciblesoftware.MyTwenty;

import java.text.DecimalFormat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
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
    private int zoomLevel = 18;
    private DecimalFormat decimalFormatter;
    private boolean launchMaps;

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
        mapView = (MapView)(findViewById(R.id.map));
        mapView.setBuiltInZoomControls(true);
        mapView.setSatellite(true);
        mapView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                DebugFile.log(TAG, "in the mapview touch");

                if(event.getAction() != MotionEvent.ACTION_DOWN)
                    return false;

                launchMaps = false;
                // show confirmation dialog for launching maps
                new AlertDialog.Builder(MyTwentyActivity.this)
                .setIcon(android.R.drawable.ic_dialog_map)
                .setMessage(R.string.map_launch_dialog_message)
                .setTitle("Go to the maps???")
                .setNegativeButton(R.string.map_launch_dialog_negative, null)
                .setPositiveButton(R.string.map_launch_dialog_positive,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        launchMaps = true;
                    }
                }).show();
                if(!launchMaps)
                    return false;

                // compose the string and launch the geo intent
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

    void setLatLon(double lat, double lon) {
        txtLat.setText(decimalFormatter.format(lat));
        txtLon.setText(decimalFormatter.format(lon));
    }

    void setAddress(Location loc) {
        if(loc != null)
            address.updateAddress(loc);
    }

    void setMap(double lat, double lon, double accuracy) {
        MapController mc = mapView.getController();
        mc.setCenter(new GeoPoint((int) (lat * 1000000), (int) (lon * 1000000)));

        // determine optimal range to show in mapview
        if(accuracy < 1000) accuracy = accuracy * 2;
        if(accuracy < 150) accuracy = 150;

        // calculate lat/lon radius for zoom
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

        final double latSpan = Math.toDegrees(Math.abs(lat2 - lat1));
        final double lonSpan = Math.toDegrees(Math.abs(lon2 - lon1));
        mc.zoomToSpan((int) (latSpan * 1000000), (int) (lonSpan * 1000000));
        zoomLevel = mapView.getZoomLevel();

        DebugFile.log(TAG, "point (" + lat + "," + lon + ") with accuracy="
                + accuracy + " gives latSpan=" + latSpan + ", lonSpan="
                + lonSpan + ", zoomLevel=" + zoomLevel);
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
