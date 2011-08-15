package net.cruciblesoftware.MyTwenty;

import android.app.Activity;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MyTwentyActivity extends Activity {
    private static EditText txtAddress;
    private static TextView txtLat, txtLon;

    private LocationManager locManager;
    private TwentyListener locListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        txtAddress = (EditText)(findViewById(R.id.txtAddress));
        txtLat = (TextView)(findViewById(R.id.txtLatValue));
        txtLon = (TextView)(findViewById(R.id.txtLonValue));

        // set up edit box to be display only
        txtAddress.setFocusable(false);

        // create a location listener to get GPS updates
        locManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        locListener = new TwentyListener(locManager, this);

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
    }

    @Override
    public void onPause() {
        locListener.deactivateListener();
        super.onPause();
    }

    @Override
    public void onStop() {
        locListener.deactivateListener();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        locListener.deactivateListener();
        super.onPause();
    }

    public static void setLatLon(double lat, double lon) {
        txtLat.setText(Double.toString(lat));
        txtLon.setText(Double.toString(lon));
    }

    public static void setAddress(String str) {
        txtAddress.setText(str);
    }
}
