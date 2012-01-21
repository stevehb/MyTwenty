package net.cruciblesoftware.MyTwenty;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

class MapSystem {
    private static final String TAG = "20: " + MapSystem.class.getSimpleName();
    private static final int EARTH_RADIUS = 6371000;
    private final static double BEARING = 0.0;

    private final Activity activity;
    private final MapView mapView;
    private Location location;
    private int zoomLevel = 18;


    //    private class PositionOverlay extends ItemizedOverlay<OverlayItem> {
    //        private final OverlayItem marker;
    //        private OverlayItem longTouchItem;
    //
    //        PositionOverlay(Drawable defaultMarker) {
    //            super(boundCenterBottom(defaultMarker));
    //            marker = createItem(0);
    //            marker.setMarker(boundCenterBottom(defaultMarker));
    //            populate();
    //        }
    //
    //        @Override
    //        protected OverlayItem createItem(int i) {
    //            switch(i) {
    //            case 0:
    //                return marker;
    //            case 1:
    //                return longTouchItem;
    //            default:
    //                return null;
    //            }
    //        }
    //
    //        @Override
    //        public int size() {
    //            return 1;
    //        }
    //
    //        @Override
    //        protected boolean onTap(int index) {
    //            OverlayItem item = createItem(index);
    //            AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
    //            dialog.setTitle(item.getTitle());
    //            dialog.setMessage(item.getSnippet());
    //            dialog.show();
    //            return true;
    //        }
    //    }

    MapSystem(Activity a, final AddressSystem address) {
        activity = a;

        // set up the map
        mapView = (MapView)(activity.findViewById(R.id.map));
        mapView.setBuiltInZoomControls(true);
        mapView.setSatellite(true);
        mapView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() != MotionEvent.ACTION_DOWN)
                    return false;

                // show confirmation dialog for launching maps
                new AlertDialog.Builder(activity)
                .setIcon(android.R.drawable.ic_dialog_map)
                .setMessage(R.string.map_launch_dialog_message)
                .setTitle(R.string.map_launch_dialog_title)
                .setNegativeButton(R.string.map_launch_dialog_negative, null)
                .setPositiveButton(R.string.map_launch_dialog_positive,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // compose the string and launch the geo intent
                        String uri;
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        if(address.hasAddress())
                            uri = "geo:0,0?q=" + lat + "," + lon + " (" + address.toString() + ")";
                        else
                            uri = "geo:" + lat + "," + lon + "?" + "z=" + zoomLevel;
                        DebugLog.log(TAG, "loading map using uri=" + uri);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        activity.startActivity(intent);
                    }
                }).show();
                return false;
            }
        });
    }

    void updateMap(Location loc) {
        location = loc;
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        double accuracy = location.getAccuracy();

        MapController mc = mapView.getController();
        mc.setCenter(new GeoPoint((int) (lat * 1000000), (int) (lon * 1000000)));

        // determine optimal range to show in mapview
        if(accuracy < 1000) accuracy = accuracy * 2;
        if(accuracy < 150) accuracy = 150;

        // calculate lat/lon radius for zoom
        // from http://www.movable-type.co.uk/scripts/latlong.html
        final double angDist = accuracy / EARTH_RADIUS;
        final double lat1 = Math.toRadians(lat);
        final double lon1 = Math.toRadians(lon);
        final double cosLat1 = Math.cos(lat1);
        final double sinLat1 = Math.sin(lat1);
        final double cosAngDist = Math.cos(angDist);
        final double sinAngDist = Math.sin(angDist);
        final double lat2 = Math.asin(sinLat1 * cosAngDist + cosLat1 * sinAngDist * Math.cos(BEARING));
        final double lon2 = lon1 + Math.atan2(Math.sin(BEARING) * sinAngDist * cosLat1, cosAngDist - sinLat1 * Math.sin(lat2));
        final double latSpan = Math.toDegrees(Math.abs(lat2 - lat1));
        final double lonSpan = Math.toDegrees(Math.abs(lon2 - lon1));
        mc.zoomToSpan((int) (latSpan * 1000000), (int) (lonSpan * 1000000));
        zoomLevel = mapView.getZoomLevel();

        DebugLog.log(TAG, "point (" + lat + "," + lon + ") with accuracy="
                + accuracy + " gives latSpan=" + latSpan + ", lonSpan="
                + lonSpan + ", zoomLevel=" + zoomLevel);
    }
}
