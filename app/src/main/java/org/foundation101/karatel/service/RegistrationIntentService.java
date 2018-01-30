package org.foundation101.karatel.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelPreferences;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.MainActivity;

import java.io.IOException;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");

        String oldToken = KaratelPreferences.pushToken();

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Log.i(TAG, "GCM Registration Token: " + token);

            if (!oldToken.equals("") && !oldToken.equals(token)) {
                logoutToChangeToken();
            } else {
                KaratelPreferences.setPushToken(token);
                // [END get_token]

                // TODO: Implement this method to send any registration to your app's servers.
                sendRegistrationToServer(token);

                // Subscribe to topic channels
                subscribeTopics(token);

                // You should store a boolean that indicates whether the generated token has been
                // sent to your server. If the boolean is false, send the token to your server,
                // otherwise your server should have already received the token.
                //globalPreferences.edit().putBoolean(Globals.SENT_TOKEN_TO_SERVER, true).apply();
                // [END register_for_gcm]

                // Notify UI that registration has completed, so the progress indicator can be hidden.
                Intent registrationComplete = new Intent(Globals.REGISTRATION_COMPLETE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            Intent tokenFailed = new Intent(Globals.GCM_ERROR_BROADCAST_RECEIVER_TAG);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(tokenFailed);

            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
           // globalPreferences.edit().putBoolean(Globals.SENT_TOKEN_TO_SERVER, false).apply();
        }

    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/"+ topic, null);
        }
    }
    // [END subscribe_topics]

    public void logoutToChangeToken(){
        Intent logoutIntent = new Intent(MainActivity.BROADCAST_RECEIVER_TAG);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(logoutIntent);
        /*
        ((KaratelApplication)getApplication()).showOneButtonDialogFromService(
                "Увага! Змінився токен Google Cloud Messaging.",
                "Вийдіть з програми та зайдіть знову, щоб отримувати сповіщення.",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(MainActivity.BROADCAST_RECEIVER_TAG));
                    }
                }
        );*/
    }

}


