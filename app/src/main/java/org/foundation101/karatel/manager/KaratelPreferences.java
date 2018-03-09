package org.foundation101.karatel.manager;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.entity.PunisherUser;

/**
 * Created by Dima on 03.11.2017.
 */

public class KaratelPreferences {
    private static final String TAG = "KARATEL_PREFERENCES";

    private static SharedPreferences instance;
    private static SharedPreferences preferences() {
        if (instance == null) {
            instance = PreferenceManager.getDefaultSharedPreferences(KaratelApplication.getInstance());
        }
        return instance;
    }


    public static void clearAll() {
        preferences().edit().clear().apply();
    }


    public static boolean loggedIn() {
        return preferences().contains(Globals.SESSION_TOKEN);
    }


    private static final String KEY_STARTED_FROM_PUSH = "KEY_STARTED_FROM_PUSH";
    public static boolean startedFromPush() {
        boolean result = preferences().getBoolean(KEY_STARTED_FROM_PUSH, false);
        Log.d(TAG, "startedFromPush=" + result);
        return result;
    }
    public static void setStartedFromPush(boolean value) {
        Log.d(TAG, "settingStartedFromPush=" + value);
        SharedPreferences.Editor editor = preferences().edit();
        editor.putBoolean(KEY_STARTED_FROM_PUSH, value);
        editor.commit();
    }


    public static void saveUserWithAvatar(String surname,
                                          String firstname,
                                          String secondname,
                                          String phone_number,
                                          String avatarFileName) {
        preferences().edit()
                .putString(Globals.USER_SURNAME, surname)
                .putString(Globals.USER_NAME, firstname)
                .putString(Globals.USER_SECOND_NAME, secondname)
                .putString(Globals.USER_PHONE, phone_number)
                .putString(Globals.USER_AVATAR, avatarFileName).apply();
    }

    public static void saveUserWithEmail(String email,
                                         String surname,
                                         String firstname,
                                         String secondname,
                                         String phone_number) {
        preferences().edit()
                .putString(Globals.USER_EMAIL, email)
                .putString(Globals.USER_SURNAME, surname)
                .putString(Globals.USER_NAME, firstname)
                .putString(Globals.USER_SECOND_NAME, secondname)
                .putString(Globals.USER_PHONE, phone_number).apply();
    }

    public static void saveUser(String token,
                                String email,
                                String surname,
                                String firstname,
                                String secondname,
                                String phone_number,
                                int id) {
        preferences().edit()
                .putString(Globals.SESSION_TOKEN, token)
                .putString(Globals.USER_EMAIL, email)
                .putString(Globals.USER_SURNAME, surname)
                .putString(Globals.USER_NAME, firstname)
                .putString(Globals.USER_SECOND_NAME, secondname)
                .putString(Globals.USER_PHONE, phone_number)
                .putInt(Globals.USER_ID, id).apply();
    }

    public static void restoreUser(){
        if (Globals.user == null) {
            SharedPreferences preferences = preferences();
            Globals.user = new PunisherUser(
                    preferences.getString(Globals.USER_EMAIL, ""),
                    "", //for password
                    preferences.getString(Globals.USER_SURNAME, ""),
                    preferences.getString(Globals.USER_NAME, ""),
                    preferences.getString(Globals.USER_SECOND_NAME, ""),
                    preferences.getString(Globals.USER_PHONE, ""));
            Globals.user.id = preferences.getInt(Globals.USER_ID, 0);
            Globals.user.avatarFileName = preferences.getString(Globals.USER_AVATAR, "");
            if (Globals.sessionToken == null) {
                Globals.sessionToken = preferences.getString(Globals.SESSION_TOKEN, "");
            }
        }
    }


    public static void setUserEmail(String email) {
        preferences().edit().putString(Globals.USER_EMAIL, email).commit();
    }
    public static void setUserAvatar(String userAvatar) {
        preferences().edit().putString(Globals.USER_AVATAR, userAvatar).commit();
    }


    public static String pushToken() {
        return preferences().getString(Globals.PUSH_TOKEN, "");
    }
    public static void setPushToken(String token) {
        preferences().edit().putString(Globals.PUSH_TOKEN, token).apply();
    }


    public static boolean appClosed() {
        boolean result = preferences().contains(Globals.APP_CLOSED);
        if (result) preferences().edit().remove(Globals.APP_CLOSED).apply();
        return result;
    }
    public static void setAppClosed() {
        preferences().edit().putBoolean(Globals.APP_CLOSED, true).apply();
    }
}
