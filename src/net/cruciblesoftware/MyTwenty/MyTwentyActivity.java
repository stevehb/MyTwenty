package net.cruciblesoftware.MyTwenty;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class MyTwentyActivity extends MapActivity {
    private static final String TAG = "20: " + MyTwentyActivity.class.getSimpleName();

    private int zoomLevel = 18;

    private EditText txtAddress;
    private TextView txtLat, txtLon;
    private MapView mapView;

    private TwentyListener locListener;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        /* TODO
         * Add views to layout to display accuracy and maybe satellite count.
         */

        setContentView(R.layout.main);
        txtLat = (TextView)(findViewById(R.id.txtLatValue));
        txtLon = (TextView)(findViewById(R.id.txtLonValue));

        // set up address box
        txtAddress = (EditText)(findViewById(R.id.txtAddress));
        txtAddress.setFocusable(false);
        txtAddress.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(txtAddress.toString().isEmpty())
                    return true;
                ClipboardManager clipboard =
                        (ClipboardManager)(getSystemService(CLIPBOARD_SERVICE));
                clipboard.setText(txtAddress.toString());
                Toast toast = Toast.makeText(MyTwentyActivity.super,
                        R.string.toast_address_copied, Toast.LENGTH_SHORT);
                toast.show();
                return true;
            }
        });

        // set up the map
        mapView = (MapView)(findViewById(R.id.map));
        mapView.setBuiltInZoomControls(true);
        mapView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() != MotionEvent.ACTION_DOWN)
                    return true;

                // compose the string
                String uri;
                if(txtAddress.toString().isEmpty()) {
                    uri = "geo:" + locListener.lat + "," + locListener.lon + "?" + "z=" + zoomLevel;
                } else {
                    uri = "geo:0,0?q=" + locListener.lat + "," + locListener.lon + "(" + txtAddress.toString() + ")";
                }
                DebugFile.log(TAG, "loading map using uri=" + uri);

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                MyTwentyActivity.this.startActivity(intent);
                return false;
            }
        });

        // Refresh and continuous buttons
        Button refresh = (Button)(findViewById(R.id.btnRefresh));
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locListener.refresh();
            }
        });
        ToggleButton continuous = (ToggleButton)(findViewById(R.id.btnContinuous));
        continuous.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                locListener.setContinuous(isChecked);
            }
        });

        // create a location listener to get GPS updates
        locListener = new TwentyListener(this);

        //if(bundle != null)
        //    locListener.setContinuous(bundle.getBoolean(KEY_CONTINUOUS, false));
    }


    void setLatLon(double lat, double lon) {
        txtLat.setText(Double.toString(lat));
        txtLon.setText(Double.toString(lon));
    }

    void setAddress(String str) {
        txtAddress.setText(str);
    }

    void setMap(float accuracy) {
        int lat = (int)(locListener.lat * 1000000);
        int lon = (int)(locListener.lon * 1000000);
        zoomLevel = 18;
        MapController mc = mapView.getController();
        mc.setCenter(new GeoPoint(lat, lon));

        /* TODO
         * Instead of a static zoom level, calculate a lat/lon range, given
         * that the accuracy parameter is in meters. The zoom in should
         * probably be capped at about 18.
         * Use the destination point calculation from
         * http://www.movable-type.co.uk/scripts/latlong.html
         * and call mc.zoomToSpan(latSpanE6, lonSpanE6). Also, get the zoom
         * level from mapView.getZoomLevel() and set the member variable in
         * case the map link needs it later.
         */
        mc.setZoom(zoomLevel);
    }

    //@Override
    //protected void onSaveInstanceState(Bundle outState) {
    //    super.onSaveInstanceState(outState);
    //    outState.putBoolean(KEY_CONTINUOUS, locListener.isContinuous);
    //}

    @Override
    public void onPause() {
        DebugFile.log(TAG, "pausing listener");
        locListener.setContinuous(false);
        locListener.deactivateListeners();
        locListener.hideNotification();
        super.onPause();
    }

    @Override
    public void onResume() {
        DebugFile.log(TAG, "calling listener resume");
        locListener.resume();
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
