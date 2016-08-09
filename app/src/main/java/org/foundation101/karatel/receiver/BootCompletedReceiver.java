package org.foundation101.karatel.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.foundation101.karatel.service.AlwaysHereService;
import org.foundation101.karatel.service.RegistrationIntentService;

/**
 * Created by Dima on 29.07.2016.
 */
public class BootCompletedReceiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent registrationIntent = new Intent(context, RegistrationIntentService.class);
        context.startService(registrationIntent);
        Intent alwaysHereIntent = new Intent(context, AlwaysHereService.class);
        context.startService(alwaysHereIntent);
        Log.e("Punisher", "boot completed");
    }
}