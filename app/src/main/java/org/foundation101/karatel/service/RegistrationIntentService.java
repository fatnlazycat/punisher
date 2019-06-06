package org.foundation101.karatel.service;

import android.app.IntentService;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.splunk.mint.Mint;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.MainActivity;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.scheduler.TokenExchangeJob;
import org.foundation101.karatel.utils.JobUtils;

import javax.inject.Inject;

public class RegistrationIntentService extends IntentService {
    private static final String TAG = "RegIntentService";

    @Inject KaratelPreferences preferences;

    public RegistrationIntentService() {
        super(TAG);
        KaratelApplication.dagger().inject(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");

        synchronized (KaratelPreferences.TAG) {

            String oldToken = preferences.pushToken();
            try {
                String token = obtainGCMToken();

                if (/*!oldToken.equals("") && */!oldToken.equals(token) && preferences.loggedIn()) {
                    Mint.logException(oldToken, token, new Exception("logoutToChangeToken"));

                    if (preferences.password().isEmpty()) {
                        Intent logoutIntent = new Intent(MainActivity.BROADCAST_RECEIVER_TAG);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(logoutIntent);
                        return;
                    } else {
                        //JobManager.instance().cancelAll();
                        preferences.setNewPushToken(token);
                        JobUtils.INSTANCE.schedule(TokenExchangeJob.TAG);
                    }

                } else {
                    preferences.setPushToken(token);

                /*Intent registrationComplete = new Intent(Globals.REGISTRATION_COMPLETE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);*/
                }
            } catch (Exception e) { //just ignored
                Log.d(TAG, "Failed to complete token refresh", e);
                Mint.logException(e);

                /*Intent tokenFailed = new Intent(Globals.GCM_ERROR_BROADCAST_RECEIVER_TAG);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(tokenFailed);*/

                /*JobManager.instance().cancelAll();
                JobUtils.INSTANCE.schedule(RegistrationRetryJob.TAG);*/
            }

            Intent registrationComplete = new Intent(Globals.REGISTRATION_COMPLETE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
        }
    }

    public static String obtainGCMToken() throws Exception {
        // Initially this call goes out to the network to retrieve the token, subsequent calls
        // are local.
        // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
        // See https://developers.google.com/cloud-messaging/android/start for details on this file.
        InstanceID instanceID = InstanceID.getInstance(KaratelApplication.getInstance());
        String token = instanceID.getToken(
                KaratelApplication.getInstance().getString(R.string.gcm_defaultSenderId),
                GoogleCloudMessaging.INSTANCE_ID_SCOPE, null
        );
        Log.i(TAG, "GCM Registration Token: " + token);
        return token;
    }
}


