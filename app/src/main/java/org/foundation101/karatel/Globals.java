package org.foundation101.karatel;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.text.Html;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Dima on 11.05.2016.
 */
public class Globals {

    //intent extras
    public static final String ITEM_ID = "PUNISHER_ITEM_ID";
    public static final String REQUEST_JSON = "PUNISHER_REQUEST_JSON";
    public static final String VIOLATION_ACTIVITY_MODE = "PUNISHER_VIOLATION_ACTIVITY_MODE";
    public static final String VIOLATION = "com.example.dnk.punisher.Violation";
    public static final String MEDIA_FILE = "PUNISHER_MEDIA_FILE";
    public static final String NEWS_ITEM = "PUNISHER_NEWS_ITEM";
    public static final String NEWS_TITLE = "PUNISHER_NEWS_TITLE";
    public static final String REGISTRATION_COMPLETE = "PUNISHER_REGISTARTION_COMPLETE";
    public static final String MAIN_ACTIVITY_SAVED_INSTANCE_STATE = "PUNISHER_MAIN_ACTIVITY_SAVED_INSTANCE_STATE";
    public static final String THUMBNAIL_URL = "PUNISHER_THUMBNAIL_URL";

    //fragment tags for MainActivity
    public static final int MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT = 2;
    public static final int MAIN_ACTIVITY_NEWS_FRAGMENT = 4;
    public static final int MAIN_ACTIVITY_PROFILE_FRAGMENT = 6;

    //user data tags
    public static final String LAST_LOGIN_EMAIL = "LAST_LOGIN_EMAIL";
    public static final String USER_EMAIL = "USER_EMAIL";
    public static final String USER_PASSWORD = "USER_PASSWORD";
    public static final String USER_SURNAME = "USER_SURNAME";
    public static final String USER_NAME = "USER_NAME";
    public static final String USER_SECOND_NAME = "USER_SECOND_NAME";
    public static final String USER_PHONE = "USER_PHONE";
    public static final String USER_ID = "USER_ID";

    //interaction with server api
    public static final String SERVER_URL = "https://karatel-test.foundation101.org/api/v1/";
    public static final String SERVER_SUCCESS = "success";

    static final String GOOGLE_SENDER_ID = "301781387946";

    public static String sessionToken, pushToken;
    public static PunisherUser user;

    //preferences
    public static final String SENT_TOKEN_TO_SERVER = "SENT_TOKEN_TO_SERVER";


    //hides the software keyboard
    public static void hideSoftKeyboard(Activity activity, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = activity.getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
    }

    public static String translateDate(String inFormat, String outFormat, String inDate){
        String outDate = "";
        SimpleDateFormat inFormatter = new SimpleDateFormat(inFormat, Locale.US);
        SimpleDateFormat outFormatter = new SimpleDateFormat(outFormat, new Locale("uk", "UA"));
        try {
            Date date = inFormatter.parse(inDate);
            outDate = outFormatter.format(date);

        } catch (ParseException e) {
            Log.e("Punisher", e.getMessage());
        }
        return outDate;
    }
}
