package org.foundation101.thepunisher.activity;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;

import org.foundation101.thepunisher.R;

public class ForgotPassword2Activity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password2);
    }

    public void proceedWithPasswordRenovation(View view) {
        finishAffinity();
    }
}
