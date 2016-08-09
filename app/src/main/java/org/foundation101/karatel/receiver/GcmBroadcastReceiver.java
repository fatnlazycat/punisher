package org.foundation101.karatel.receiver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.google.android.gms.gcm.GcmReceiver;

import org.foundation101.karatel.service.MyGcmListenerService;

/**
 * Created by Dima on 29.07.2016.
 */
public class GcmBroadcastReceiver extends GcmReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("Punisher", "GcmBroadcastReceiver " + intent);
        super.onReceive(context, intent);


        /*// Explicitly specify that GcmIntentService will handle the intent.
        ComponentName comp = new ComponentName(context.getPackageName(), MyGcmListenerService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);*/
    }
}
