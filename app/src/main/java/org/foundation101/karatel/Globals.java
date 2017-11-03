package org.foundation101.karatel;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.foundation101.karatel.entity.PunisherUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Dima on 11.05.2016.
 */
public class Globals {

    //intent extras
    public static final String ITEM_ID = "PUNISHER_ITEM_ID";
    public static final String REQUEST_JSON = "PUNISHER_REQUEST_JSON";
    public static final String VIOLATION_ACTIVITY_MODE = "PUNISHER_VIOLATION_ACTIVITY_MODE";
    public static final String VIOLATION = "org.foundation101.karatel.entity.Violation";
    public static final String MEDIA_FILE = "PUNISHER_MEDIA_FILE";
    public static final String NEWS_ITEM = "PUNISHER_NEWS_ITEM";
    public static final String NEWS_TITLE = "PUNISHER_NEWS_TITLE";
    public static final String DOC_TO_VIEW = "PUNISHER_DOC_TO_VIEW";
    public static final String DOC_TO_VIEW_TITLE = "PUNISHER_DOC_TO_VIEW_TITLE";
    public static final String REGISTRATION_COMPLETE = "PUNISHER_REGISTARTION_COMPLETE";
    public static final String GCM_ERROR_BROADCAST_RECEIVER_TAG = "PUNISHER_GCM_ERROR_BROADCAST_RECEIVER_TAG";
    public static final String MAIN_ACTIVITY_SAVED_INSTANCE_STATE = "PUNISHER_MAIN_ACTIVITY_SAVED_INSTANCE_STATE";
    public static final String POSSIBLE_VALUES = "PUNISHER_POSSIBLE_VALUES";
    public static final String POSSIBLE_VALUES_HEADER = "PUNISHER_POSSIBLE_VALUES_HEADER";
    public static final String REQUISITE_NUMBER_FOR_POSSIBLE_VALUES = "PUNISHER_REQUISITE_NUMBER_FOR_POSSIBLE_VALUES";
    public static final String VIOLATION_TYPE = "PUNISHER_VIOLATION_TYPE";

    //Bundle keys
    public static final String REQUISITES_VALUES = "REQUISITES_VALUES";
    public static final String EVIDENCES = "EVIDENCES";
    public static final String DELETED_EVIDENCES = "DELETED_EVIDENCES";

    //fragment tags for MainActivity
    public static final int MAIN_ACTIVITY_PUNISH_FRAGMENT = 1;
    public static final int MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT = 2;
    public static final int MAIN_ACTIVITY_COMPLAINS_BOOK_FRAGMENT = 3;
    public static final int MAIN_ACTIVITY_VIDEO_LIST_FRAGMENT = 4;
    public static final int MAIN_ACTIVITY_DONATE = 5;
    public static final int MAIN_ACTIVITY_ABOUT_FRAGMENT = 6;
    public static final int MAIN_ACTIVITY_PARTNERS_FRAGMENT = 7;
    public static final int MAIN_ACTIVITY_NEWS_FRAGMENT = 8;
    public static final int MAIN_ACTIVITY_CONTACTS_FRAGMENT = 9;
    public static final int MAIN_ACTIVITY_PROFILE_FRAGMENT = 10;
    public static final int MAIN_ACTIVITY_EXIT = 11;
    //now go fragments that won't be called from the drawer
    public static final int MAIN_ACTIVITY_COMPLAIN_DRAFTS = 100;

    public static final String PUSH_TOKEN = "PUSH_TOKEN";

    //user data tags
    public static final String LAST_LOGIN_EMAIL = "LAST_LOGIN_EMAIL";
    public static final String SESSION_TOKEN = "SESSION_TOKEN";
    public static final String USER_EMAIL = "USER_EMAIL";
    public static final String USER_PASSWORD = "USER_PASSWORD";
    public static final String USER_SURNAME = "USER_SURNAME";
    public static final String USER_NAME = "USER_NAME";
    public static final String USER_SECOND_NAME = "USER_SECOND_NAME";
    public static final String USER_PHONE = "USER_PHONE";
    public static final String USER_ID = "USER_ID";
    public static final String USER_AVATAR = "USER_AVATAR";

    //interaction with server api
    public static final String SERVER_URL = "https://karatel-test.foundation101.org/api/v1/"; //-api -test
    public static final int MAX_SERVER_REQUEST_SIZE = 100 * 1024 * 1024; //100 mb
    public static final String SERVER_SUCCESS = "success";
    public static final String SERVER_ERROR = "error";
    public static final String APP_CLOSED = "APP_CLOSED";/*used in ChangePasswordActivity to close it after exiting
        the matter is that finishAffinity() is called in MainActivity -> doesn't affect Activities AFTER MainActivity
        So need to close them in onResume() using this tag
        */

    public static String sessionToken;
    public static PunisherUser user;

    //preferences
    public static final String SENT_TOKEN_TO_SERVER = "SENT_TOKEN_TO_SERVER";

    //map of complain statuses: key-indexes on server, value-index in array in apk resources
    public static HashMap<Integer, Integer> statusesMap = new HashMap<>();

    static String[] monthsNames = {
            " січня ",    " лютого ",
            " березня ",  " квітня ",    " травня ",
            " червня ",   " липня ",    " серпня ",
            " вересня ",  " жовтня ",   " листопада ",
            " грудня "
    };

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


    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        View v = activity.getCurrentFocus();
        if (v != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }


    public static String translateDate(String inFormat, String outFormat, String inDate){
        String outDate = "";
        SimpleDateFormat inFormatter = new SimpleDateFormat(inFormat, Locale.US);
        SimpleDateFormat outFormatter = new SimpleDateFormat(outFormat, new Locale("uk", "UA"));
        try {
            Date date = inFormatter.parse(inDate);
            outDate = outFormatter.format(date);

            Pattern p = Pattern. compile("(\\d+\\s*)+"); //digits and spaces -> no letters
            Matcher m = p.matcher(outDate);
            if (m.matches()) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                Integer monthNumber = calendar.get(Calendar.MONTH);
                String month = monthsNames.length > monthNumber ? monthsNames[monthNumber] : monthNumber.toString();
                outDate = "" +
                        calendar.get(Calendar.DAY_OF_MONTH) +
                        month +
                        calendar.get(Calendar.YEAR);
            }
        } catch (Exception e) {
            Log.e("Punisher", e.getMessage());
        }

        //now check if the result contains month as letters, if not - use month from array
        return outDate;
    }

    public static void showError(Context context, int errorMessageResId, Exception e){
        if (context != null) Toast.makeText(context, errorMessageResId, Toast.LENGTH_LONG).show();
        if (e != null) {
            //String logMessage = (e.getMessage() == null) ? e.toString() : e.getMessage();
            Log.d("Punisher error", "", e);
        }
    }

    public static void showError(Context context, String errorMessage, Exception e){
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
        if (e != null) {
            //String logMessage = (e.getMessage() == null) ? e.toString() : e.getMessage();
            Log.d("Punisher error", "", e);
        }
    }
}
