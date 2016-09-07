package org.foundation101.karatel.service;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.iid.InstanceIDListenerService;

import org.foundation101.karatel.Karatel;
import org.foundation101.karatel.activity.MainActivity;


public class MyInstanceIDListenerService extends InstanceIDListenerService {

    private static final String TAG = "MyInstanceIDLS";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);

        /* Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);*/
    }
    // [END refresh_token]

}

