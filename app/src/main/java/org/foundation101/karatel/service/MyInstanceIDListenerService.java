package org.foundation101.karatel.service;

import android.content.Intent;

import com.google.android.gms.iid.InstanceIDListenerService;
import com.splunk.mint.Mint;
import com.splunk.mint.MintLogLevel;


public class MyInstanceIDListenerService extends InstanceIDListenerService {

    private static final String TAG = "MyInstanceIDLS";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    @Override
    public void onTokenRefresh() {
        Mint.logException("my custom exception", "", new Exception(TAG));
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);

        /* Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);*/
    }

}

