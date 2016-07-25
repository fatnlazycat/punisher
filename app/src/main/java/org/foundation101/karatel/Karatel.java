package org.foundation101.karatel;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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
}
