package org.foundation101.karatel.service;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.MainActivity;
import org.foundation101.karatel.activity.TipsActivity;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";
    public static  final String REQUEST_NUMBER = "REQUEST_NUMBER";

    @Inject KaratelPreferences preferences;

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        KaratelApplication.dagger().inject(this);

        String message = data.getString("message");
        message = message == null ? "" : message;
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);

        /*if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }*/

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        sendNotification(message);
    }

    private void sendNotification(String message) {
        Class activityClass;
        if (loggedIn()){
            activityClass = MainActivity.class;
        } else {
            activityClass = TipsActivity.class;
        }

        Intent intent = new Intent(this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Pattern pattern = Pattern.compile("\\D+ (\\d{8}/\\d+) .*"); //pattern 20160101/2
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String requestTag = matcher.group(1);
            intent.putExtra(REQUEST_NUMBER, requestTag);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        final String channelId = getResources().getString(R.string.karatel);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    channelId,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(getNotificationIcon())
                .setContentTitle("Каратєль")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private int getNotificationIcon() {
        boolean use_BW_Icon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
        return use_BW_Icon ? R.drawable.ic_push : R.drawable.karatel_logo_color;
    }

    boolean loggedIn(){
        return preferences.loggedIn();
    }
}

