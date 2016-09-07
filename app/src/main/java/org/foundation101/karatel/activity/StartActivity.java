package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.foundation101.karatel.R;
import org.foundation101.karatel.service.RegistrationIntentService;

public class StartActivity extends Activity {
    private static final String PREFERENCE_FIRST_RUN_FLAG = "firstRun";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        //Toast.makeText(this, R.string.tap_to_enter, Toast.LENGTH_LONG).show();

        //start push notification service
        if (checkPlayServices()) {
            Log.e("Punisher", "starting registrationIntentService");
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
            leaveStartPage(null);
        }
    }


    public void leaveStartPage(View view) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        if (preferences.contains(PREFERENCE_FIRST_RUN_FLAG)) {
            startActivity(new Intent(this, TipsActivity.class));
        } else {
            preferences.edit().putBoolean(PREFERENCE_FIRST_RUN_FLAG, true).apply();
            startActivity(new Intent(this, TutorialActivity.class));

        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    static final int PLAY_SERVICES_RESOLUTION_REQUEST = 3800001;
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i("Punisher", "This device is not supported.");
                finish();
            }
            return false;
        }
        Log.e("Punisher", "check play services = " + resultCode);
        return true;
    }
}
