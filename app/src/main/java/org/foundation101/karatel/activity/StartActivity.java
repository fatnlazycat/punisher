package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

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
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
        leaveStartPage(null);
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
}
