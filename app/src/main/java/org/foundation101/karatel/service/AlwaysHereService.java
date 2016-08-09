package org.foundation101.karatel.service;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.foundation101.karatel.receiver.GcmBroadcastReceiver;

/**
 * Created by Dima on 01.08.2016.
 */
public class AlwaysHereService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        /*GcmBroadcastReceiver receiver = new GcmBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction("com.google.android.c2dm.intent.RECEIVE");
        filter.addAction("com.google.android.c2dm.intent.REGISTRATION");
        filter.addCategory("org.foundation101.karatel");
        this.registerReceiver(receiver, filter);
        Log.e("Punisher", "receiver: " + receiver + " filter: " + filter);*/
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.e("Punisher", "AlwaysHereService Started");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
