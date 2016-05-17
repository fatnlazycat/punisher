package com.example.dnk.punisher.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.example.dnk.punisher.R;

public class StartActivity extends Activity {
    private static final String PREFERENCE_FIRST_RUN_FLAG = "firstRun";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }


    public void leaveStartPage(View view) {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        if (preferences.contains(PREFERENCE_FIRST_RUN_FLAG)) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            preferences.edit().putBoolean(PREFERENCE_FIRST_RUN_FLAG, true).apply();
            startActivity(new Intent(this, TutorialActivity.class));

        }
    }
}
