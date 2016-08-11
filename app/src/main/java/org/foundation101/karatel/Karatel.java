package org.foundation101.karatel;

import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.splunk.mint.Mint;

//import org.acra.*;
//import org.acra.annotation.*;

//@ReportsCrashes(formUri = "http://www.yourselectedbackend.com/reportpath")
public class Karatel extends Application {

    //Google Analytics part
    private Tracker mTracker;
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }
    public void sendScreenName(Class screenName){
        Tracker thisTracker = getDefaultTracker();
        thisTracker.setScreenName(screenName.getSimpleName());
        thisTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Mint.initAndStartSession(this, "c609df56");
        //ACRA.init(this);
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        Runtime.getRuntime().gc();
    }

    public void restoreUserFromPreferences(){
        if (Globals.user == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            Globals.user = new PunisherUser(
                    preferences.getString(Globals.USER_EMAIL, ""),
                    "", //for password
                    preferences.getString(Globals.USER_SURNAME, ""),
                    preferences.getString(Globals.USER_NAME, ""),
                    preferences.getString(Globals.USER_SECOND_NAME, ""),
                    preferences.getString(Globals.USER_PHONE, ""));
            Globals.user.id = preferences.getInt(Globals.USER_ID, 0);
            Globals.user.avatarFileName = preferences.getString(Globals.USER_AVATAR, "");
            if (Globals.sessionToken == null) {
                Globals.sessionToken = preferences.getString(Globals.SESSION_TOKEN, "");
            }
        }
    }

    public void showOneButtonDialogFromService(String title, String message, DialogInterface.OnClickListener action) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppTheme);
        dialogBuilder.setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.ok, action);
        final AlertDialog dialog = dialogBuilder.create();
        final Window dialogWindow = dialog.getWindow();
        final WindowManager.LayoutParams dialogWindowAttributes = dialogWindow.getAttributes();

        // Set fixed width (280dp) and WRAP_CONTENT height
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialogWindowAttributes);
        lp.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 280, getResources().getDisplayMetrics());
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialogWindow.setAttributes(lp);

        // Set to TYPE_SYSTEM_ALERT so that the Service can display it
        dialogWindow.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    public boolean locationIsMock(Location location){
        boolean isMock = false;
        if (android.os.Build.VERSION.SDK_INT >= 18) {
            isMock = (location==null || location.isFromMockProvider());
        } else {
            isMock = !Settings.Secure.getString(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
        }

        /*
        //debug block
        String message = isMock ? "mock" : "good!";
        Toast.makeText(this, "georesult = "+message, Toast.LENGTH_LONG).show();
        Log.e("Punisher","georesult = "+message);
        */

        return isMock;
    }
}
