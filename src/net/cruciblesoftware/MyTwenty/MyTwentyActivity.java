package net.cruciblesoftware.MyTwenty;

import java.text.DecimalFormat;

import android.graphics.PixelFormat;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.maps.MapActivity;

public class MyTwentyActivity extends MapActivity {
    private static final String TAG = "20: " + MyTwentyActivity.class.getSimpleName();

    private DecimalFormat decimalFormatter;
    private TextView txtLat, txtLon;
    private ToggleButton btnContinuous;

    private AddressSystem address;
    private MapSystem map;
    private TwentyListener locListener;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        locListener = new TwentyListener(this);
        decimalFormatter = new DecimalFormat("0.0000000");

        setContentView(R.layout.main);
        txtLat = (TextView)(findViewById(R.id.txtLatValue));
        txtLon = (TextView)(findViewById(R.id.txtLonValue));

        // set up address and map boxes
        address = new AddressSystem(this);
        map = new MapSystem(this, address);

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
        DebugLog.log(TAG, "pausing listener");
        locListener.setContinuous(false);
        btnContinuous.setChecked(false);
        locListener.deactivateListeners();
        locListener.hideNotification();
        super.onPause();
    }

    @Override
    public void onResume() {
        DebugLog.log(TAG, "calling listener resume");
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

    void setMap(Location loc) {
        if(loc != null)
            map.updateMap(loc);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        // improves bg gradient on some phones
        this.getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}
