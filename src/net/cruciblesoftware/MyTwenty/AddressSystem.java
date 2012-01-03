package net.cruciblesoftware.MyTwenty;

import java.util.Calendar;

import android.app.Activity;
import android.location.Location;
import android.os.Handler;
import android.text.ClipboardManager;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class AddressSystem {
    private static final String TAG = "20: " + AddressSystem.class.getSimpleName();
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private TextView txtLabel;
    private TextView txtAge;
    private EditText txtAddress;
    private TextView txtAccuracy;
    private final Activity activity;

    private ReverseGeocoder reverseGeocoder;
    private boolean hasAddress;

    private Handler handler;
    private String timeTemplate;
    private long latestTime;
    private Time timeFormatter;

    private String sourceTemplate;
    private int meters;

    public AddressSystem(Activity a) {
        activity = a;
        reverseGeocoder = new ReverseGeocoder(activity);

        // set up the address labels, assuming new location
        txtLabel = (TextView)activity.findViewById(R.id.txtAddressLabel);
        txtAge = (TextView)activity.findViewById(R.id.txtAddressAge);
        txtLabel.setText(R.string.address_label_new);
        txtAge.setVisibility(TextView.INVISIBLE);

        // set up the address edit box
        txtAddress = (EditText)activity.findViewById(R.id.txtAddress);
        txtAddress.setFocusable(false);
        txtAddress.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(txtAddress.toString().isEmpty())
                    return true;
                ClipboardManager clipboard = (ClipboardManager)(activity.getSystemService(activity.CLIPBOARD_SERVICE));
                clipboard.setText(txtAddress.getText().toString());
                Toast toast = Toast.makeText(activity, R.string.toast_address_copied, Toast.LENGTH_SHORT);
                toast.show();
                return true;
            }
        });

        // and set up the accuracy statement, and make it invisible for the moment
        txtAccuracy = (TextView)activity.findViewById(R.id.txtAddressAccuracy);
        txtAccuracy.setVisibility(TextView.INVISIBLE);

        // set default values for actual data
        handler = new Handler();
        hasAddress = false;
        sourceTemplate = activity.getString(R.string.address_label_accuracy);
        meters = 0;
        timeTemplate = activity.getString(R.string.address_label_age);
        latestTime = System.currentTimeMillis();
        timeFormatter = new Time(Calendar.getInstance().getTimeZone().getID());
    }

    void updateAddress(Location loc) {
        reverseGeocoder.startLookup(this, loc);
    }

    void onAddressLookupComplete(String address, final Location loc) {
        txtAddress.setText(address);
        hasAddress = reverseGeocoder.hasAddress();

        // if not a real address, just clean up and get out
        if(!reverseGeocoder.hasAddress()) {
            txtLabel.setText(R.string.address_label_new);
            txtAge.setVisibility(TextView.INVISIBLE);
            txtAccuracy.setVisibility(TextView.INVISIBLE);
            return;
        }

        // set accuracy
        String sourceStr = String.format(sourceTemplate, loc.getProvider(), (int)loc.getAccuracy());
        DebugFile.log(TAG, "formatted sourceTemplate=" + sourceStr);
        txtAccuracy.setText(sourceStr);
        txtAccuracy.setVisibility(TextView.VISIBLE);

        // reset timer
        txtLabel.setText(R.string.address_label_new);
        txtAge.setVisibility(TextView.INVISIBLE);
        latestTime = loc.getTime();

        // update the timer in two minutes
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // if last update was newer than expected, don't age the result
                if(latestTime > loc.getTime()) {
                    return;
                }

                txtLabel.setText(R.string.address_label_old);

                timeFormatter.set(loc.getTime());
                int hour = (timeFormatter.hour > 12 ? timeFormatter.hour - 12 : timeFormatter.hour);
                int min = timeFormatter.minute;
                String ampm = (timeFormatter.hour <= 12 ? "AM" : "PM");
                String timeStr = String.format(timeTemplate, hour, min, ampm);
                DebugFile.log(TAG, "at time=" + timeStr);
                txtAge.setText(timeStr);
                txtAge.setVisibility(TextView.VISIBLE);
            }
        }, TWO_MINUTES);
    }

    boolean hasAddress() {
        return hasAddress;
    }

    @Override
    public String toString() {
        return txtAddress.getText().toString();
    }
}
