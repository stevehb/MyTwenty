package net.cruciblesoftware.MyTwenty;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public class DebugLog {
    private static final String TAG = "20: " + DebugLog.class.getSimpleName();
    private static BufferedWriter writer;
    static boolean writeToFile = false;
    static boolean muzzle = false;

    static {
        try {
            if(!muzzle) {
                Log.v(TAG, "creating MyTwenty log");
            }
            if(!muzzle && writeToFile) {
                writer = new BufferedWriter(new FileWriter(Environment.getExternalStorageDirectory() + "/my20_log.txt", true));
                writer.write("Starting My20 log at " + (new Date()).toString() + "\n");
                writer.flush();
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException on file creation: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    static void log(String tag, String msg) {
        try {
            if(!muzzle) {
                Log.d(tag, msg);
            }
            if(!muzzle && writeToFile) {
                writer.write(tag + ": " + msg + "\n");
                writer.flush();
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException on file write: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}
