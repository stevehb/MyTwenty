package net.cruciblesoftware.MyTwenty;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnLongClickListener;
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
    //private final String KEY_CONTINUOUS = "continuous";

    private EditText txtAddress;
    private TextView txtLat, txtLon;
    private MapView mapView;

    private TwentyListener locListener;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.main);
        txtAddress = (EditText)(findViewById(R.id.txtAddress));
        txtLat = (TextView)(findViewById(R.id.txtLatValue));
        txtLon = (TextView)(findViewById(R.id.txtLonValue));

        // set up edit box to be display only
        txtAddress.setFocusable(false);
        mapView = (MapView)(findViewById(R.id.map));
        mapView.setBuiltInZoomControls(false);

        // create a location listener to get GPS updates
        locListener = new TwentyListener(this);

        // The address text box
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

    void setMap(double lat, double lon) {
        MapController mc = mapView.getController();
        mc.setCenter(new GeoPoint((int)(lat * 1000000), (int)(lon * 1000000)));
        mc.setZoom(18);
    }

    //@Override
    //protected void onSaveInstanceState(Bundle outState) {
    //    super.onSaveInstanceState(outState);
    //    outState.putBoolean(KEY_CONTINUOUS, locListener.isContinuous);
    //}

    @Override
    public void onPause() {
        locListener.deactivateListener();
        locListener.hideNotification();
        super.onPause();
    }

    @Override
    public void onResume() {
        locListener.setContinuous(false);
        locListener.deactivateListener();
        locListener.hideNotification();
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
