package com.example.dnk.punisher.activity;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;

import com.example.dnk.punisher.R;

public class ForgotPassword2Activity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password2);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void proceedWithPasswordRenovation(View view) {

    }
}
