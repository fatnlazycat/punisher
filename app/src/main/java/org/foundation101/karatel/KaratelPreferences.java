package org.foundation101.karatel;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Dima on 03.11.2017.
 */

public class KaratelPreferences {
    private static final String TAG = "KARATEL_PREFERENCES";

    private static SharedPreferences sharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(KaratelApplication.getInstance());
    }

    private static final String KEY_LOGGED_IN = "KEY_LOGGED_IN";
    public static boolean startedFromPush() {
        boolean result = sharedPreferences().getBoolean(KEY_LOGGED_IN, false);
        Log.d(TAG, "startedFromPush=" + result);
        return result;
    }
    public static void setStartedFromPush(boolean value) {
        Log.d(TAG, "settingStartedFromPush=" + value);
        SharedPreferences.Editor editor = sharedPreferences().edit();
        editor.putBoolean(KEY_LOGGED_IN, value);
        editor.commit();
    }

}
