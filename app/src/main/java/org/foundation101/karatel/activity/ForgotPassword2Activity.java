package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.foundation101.karatel.R;
import org.foundation101.karatel.manager.KaratelPreferences;

public class ForgotPassword2Activity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password2);
    }

    public void proceedWithPasswordRenovation(View view) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(intent, getString(R.string.chooseEmailClient)));
        }

        if (KaratelPreferences.loggedIn()){
            new MainActivity.SignOutSender(ForgotPassword2Activity.this).execute();
            /*KaratelPreferences.setAppClosed();

            Intent logoutIntent = new Intent(MainActivity.BROADCAST_RECEIVER_TAG);
            logoutIntent.putExtra(MainActivity.TAG_JUST_LOGOUT, true);
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(logoutIntent);
            finish();*/
        } else {
            finishAffinity();
        }
    }
}
