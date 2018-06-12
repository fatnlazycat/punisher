package org.foundation101.karatel.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TimeUtils;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.R;
import org.foundation101.karatel.service.RegistrationIntentService;

import java.util.concurrent.TimeUnit;

public class StartActivity extends Activity {
    private static final String PREFERENCE_FIRST_RUN_FLAG = "firstRun";
    static final String TAG = "StartActivity";
    AlertDialog alertDialog;
    Dialog settingsDialog;

    private final BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive " + action);
            switch (action) {
                case Globals.REGISTRATION_COMPLETE : {
                    leaveStartPage();
                    break;
                }
                //case Globals.GCM_ERROR_BROADCAST_RECEIVER_TAG :
                case MainActivity.BROADCAST_RECEIVER_TAG : {//fallthrough - these two behave the same
                    alertDialog = showGCMErrorDialog();
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        //Toast.makeText(this, R.string.tap_to_enter, Toast.LENGTH_LONG).show();

        /*try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        //register receiver before starting the service (what if the service returns faster than the receiver is registered?)
        registerReceiver();
        Log.d(TAG, "broadcast receiver registered");

        //start push notification service
        if (checkPlayServices()) {
            startRegistrationService();
        }/* else {
            finish();
        }*/
    }

    private void startRegistrationService() {
        Log.d(TAG, "starting registrationIntentService");
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Globals.REGISTRATION_COMPLETE);
        /*intentFilter.addAction(Globals.GCM_ERROR_BROADCAST_RECEIVER_TAG);*/
        intentFilter.addAction(MainActivity.BROADCAST_RECEIVER_TAG);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(myBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(myBroadcastReceiver);
        if (alertDialog    != null) alertDialog   .dismiss();
        if (settingsDialog != null) settingsDialog.dismiss();
        super.onDestroy();
    }

    public void leaveStartPage() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (preferences.contains(PREFERENCE_FIRST_RUN_FLAG)) {
            intent.setClass(this, TipsActivity.class);
        } else {
            preferences.edit().putBoolean(PREFERENCE_FIRST_RUN_FLAG, true).apply();
            intent.setClass(this, TutorialActivity.class);
        }
        startActivity(intent);
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
            //SERVICE_INVALID leads to error dialog being shown with only one button ok that does nothing
            if (apiAvailability.isUserResolvableError(resultCode) && resultCode != ConnectionResult.SERVICE_INVALID) {
                settingsDialog = apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
                settingsDialog.setCancelable(false);
                settingsDialog.show();
            } else {
                String message = "This device is not supported.";
                Log.i(TAG, message);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        Log.d(TAG, "check play services = " + resultCode);
        return true;
    }

    public AlertDialog showGCMErrorDialog() {
        if (isFinishing()) return null;

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Помилка отримання токена")
                .setMessage("Вийдіть з програми та зайдіть знову, щоб отримувати сповіщення.")
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //KaratelPreferences.restoreUser(); //we need sessionToken to logout
                        new MainActivity.SignOutSender(StartActivity.this).execute();
                        finish();
                    }
                })
                .setCancelable(false)
                .create();
        dialog.show();
        return dialog;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PLAY_SERVICES_RESOLUTION_REQUEST && resultCode == RESULT_OK) {
            startRegistrationService();
        } else finish();
    }
}
