package org.foundation101.karatel.manager;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.entity.PunisherUser;
import org.foundation101.karatel.utils.TextUtils;

import javax.inject.Inject;

import dagger.Module;
import dagger.Provides;

/**
 * Created by Dima on 03.11.2017.
 */

@Module
public class KaratelPreferences {
    public static final String TAG = "KARATEL_PREFERENCES";

    @Inject public KaratelPreferences() { }

    private static SharedPreferences instance;

    private SharedPreferences preferences() {
        if (instance == null) {
            instance = PreferenceManager.getDefaultSharedPreferences(KaratelApplication.getInstance());
        }
        return instance;
    }


    public void clearAll() {
        preferences().edit().clear().apply();
    }


    public void remove(String key) {
        preferences().edit().remove(key).apply();
    }


    public boolean loggedIn() {
        return preferences().contains(Globals.SESSION_TOKEN);
    }


    public void saveUserWithAvatar(String surname,
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

    public void saveUserWithEmail(String email,
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

    /*public static void saveUser(String email,
                                     String surname,
                                     String firstname,
                                     String secondname,
                                     String phone_number,
                                     int id) {
        preferences().edit()
                .putString(Globals.USER_EMAIL, email)
                .putString(Globals.USER_SURNAME, surname)
                .putString(Globals.USER_NAME, firstname)
                .putString(Globals.USER_SECOND_NAME, secondname)
                .putString(Globals.USER_PHONE, phone_number)
                .putInt(Globals.USER_ID, id).apply();
    }*/

    @SuppressLint("ApplySharedPref")
    public void saveUser(PunisherUser user) {
        preferences().edit()
                .putString(Globals.USER_EMAIL, user.email)
                .putString(Globals.USER_SURNAME, user.surname)
                .putString(Globals.USER_NAME, user.name)
                .putString(Globals.USER_SECOND_NAME, user.secondName)
                .putString(Globals.USER_PHONE, user.phone)
                .putString(Globals.USER_AVATAR, user.avatarFileName)
                .putInt(Globals.USER_ID, user.id).commit();
    }

    public PunisherUser user() {
        SharedPreferences preferences = preferences();
        PunisherUser user = new PunisherUser(
                preferences.getString(Globals.USER_EMAIL, ""),
                "", //for password
                preferences.getString(Globals.USER_SURNAME, ""),
                preferences.getString(Globals.USER_NAME, ""),
                preferences.getString(Globals.USER_SECOND_NAME, ""),
                preferences.getString(Globals.USER_PHONE, ""));
        user.id = preferences.getInt(Globals.USER_ID, 0);
        user.avatarFileName = userAvatar();
        return user;
    }

    public void setUserId(int id) {
        preferences().edit().putInt(Globals.USER_ID, id).apply();
    }

    public int userId() {
        return preferences().getInt(Globals.USER_ID, 0);
    }

    /*public static void restoreUser(){
        if (Globals.user == null) {
            Globals.user = user();
        }
    }*/


    public void setUserEmail(String email) {
        preferences().edit().putString(Globals.USER_EMAIL, email).commit();
    }

    public String userAvatar() {
        return preferences().getString(Globals.USER_AVATAR, "");
    }
    public void setUserAvatar(String userAvatar) {
        preferences().edit().putString(Globals.USER_AVATAR, userAvatar).commit();
    }


    public String oldSessionToken() {
        return preferences().getString(Globals.OLD_SESSION_TOKEN, "");
    }
    public void setOldSessionToken(String token) {
        preferences().edit().putString(Globals.OLD_SESSION_TOKEN, token).apply();
    }


    public String sessionToken() {
        return preferences().getString(Globals.SESSION_TOKEN, "");
    }
    public void setSessionToken(String token) {
        preferences().edit().putString(Globals.SESSION_TOKEN, token).apply();
    }


    public String pushToken() {
        return preferences().getString(Globals.PUSH_TOKEN, "");
    }
    public void setPushToken(String token) {
        preferences().edit().putString(Globals.PUSH_TOKEN, token).apply();
    }


    public String newPushToken() {
        return preferences().getString(Globals.NEW_PUSH_TOKEN, "");
    }
    public void setNewPushToken(String token) {
        preferences().edit().putString(Globals.NEW_PUSH_TOKEN, token).apply();
    }


    public String oldPushToken() {
        return preferences().getString(Globals.OLD_PUSH_TOKEN, "");
    }
    public void setOldPushToken(String token) {
        preferences().edit().putString(Globals.OLD_PUSH_TOKEN, token).apply();
    }


    /*public static boolean appClosed() {
        boolean result = preferences().contains(Globals.APP_CLOSED);
        if (result) preferences().edit().remove(Globals.APP_CLOSED).apply();
        return result;
    }
    public static void setAppClosed() {
        preferences().edit().putBoolean(Globals.APP_CLOSED, true).apply();
    }*/


    public String lastLoginEmail() {
        return preferences().getString(Globals.LAST_LOGIN_EMAIL, "");
    }
    public void setLastLoginEmail(String lastLoginEmail) {
        preferences().edit().putString(Globals.LAST_LOGIN_EMAIL, lastLoginEmail).apply();
    }


    public String password() {
        String data = preferences().getString(Globals.USER_PASSWORD, "");
        return TextUtils.INSTANCE.decodeString(data);
    }
    public void setPassword(String password) {
        String data = TextUtils.INSTANCE.encodeStringWithSHA(password);
        preferences().edit().putString(Globals.USER_PASSWORD, data).apply();
    }

    public String pendingJob() {
        return preferences().getString(Globals.PENDING_JOB, "");
    }
    public void setPendingJob(String jobData) {
        preferences().edit().putString(Globals.PENDING_JOB, jobData).apply();
    }

    /**
     * preferences storing data necessary for background fb login
     */
    public String fbLoginUid() {
        return preferences().getString(Globals.BACKGROUND_FB_LOGIN_UID, "");
    }
    public void setFbLoginUid(String fbLoginUid) {
        preferences().edit().putString(Globals.BACKGROUND_FB_LOGIN_UID, fbLoginUid).apply();
    }

    public String fbLoginEmail() {
        return preferences().getString(Globals.BACKGROUND_FB_LOGIN_EMAIL, "");
    }
    public void setFbLoginEmail(String fbLoginEmail) {
        preferences().edit().putString(Globals.BACKGROUND_FB_LOGIN_EMAIL, fbLoginEmail).apply();
    }

    public String fbLoginPassword() {
        String data = preferences().getString(Globals.BACKGROUND_FB_LOGIN_PASSW, "");
        return TextUtils.INSTANCE.decodeString(data);
    }
    public void setFbLoginPassword(String password) {
        String data = TextUtils.INSTANCE.encodeStringWithSHA(password);
        preferences().edit().putString(Globals.BACKGROUND_FB_LOGIN_PASSW, data).apply();
    }
}
