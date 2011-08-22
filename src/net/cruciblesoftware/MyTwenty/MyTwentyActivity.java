package net.cruciblesoftware.MyTwenty;

import android.app.Activity;
import android.location.LocationManager;
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

public class MyTwentyActivity extends Activity {
    private final String KEY_CONTINUOUS = "continuous";

    private EditText txtAddress;
    private TextView txtLat, txtLon;

    private LocationManager locManager;
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

        // create a location listener to get GPS updates
        locManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        locListener = new TwentyListener(locManager, this);

        // The address text box
        txtAddress.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard =
                        (ClipboardManager)(getSystemService(CLIPBOARD_SERVICE));
                clipboard.setText(txtAddress.getText());
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

        if(bundle != null)
            locListener.setContinuous(bundle.getBoolean(KEY_CONTINUOUS, false));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CONTINUOUS, locListener.isContinuous);
    }

    @Override
    public void onPause() {
        locListener.deactivateListener();
        locListener.hideNotification();
        super.onPause();
    }

    @Override
    public void onStop() {
        locListener.deactivateListener();
        locListener.hideNotification();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        locListener.deactivateListener();
        super.onPause();
    }

    public void setLatLon(double lat, double lon) {
        txtLat.setText(Double.toString(lat));
        txtLon.setText(Double.toString(lon));
    }

    public void setAddress(String str) {
        txtAddress.setText(str);
    }
}
