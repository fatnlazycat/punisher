package org.foundation101.karatel;

import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.splunk.mint.Mint;

import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

//import org.acra.*;
//import org.acra.annotation.*;

//@ReportsCrashes(formUri = "http://www.yourselectedbackend.com/reportpath")
public class Karatel extends Application {
    public static boolean MAIN_ACTIVITY_FROM_PUSH = false;

    private static Retrofit retrofit = null;

    //Google Analytics part
    private Tracker mTracker;
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }
    public void sendScreenName(Class screenName){
        Tracker thisTracker = getDefaultTracker();
        thisTracker.setScreenName(screenName.getSimpleName());
        thisTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Mint.initAndStartSession(this, "c609df56");
        //ACRA.init(this);
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        Runtime.getRuntime().gc();
    }

    public void restoreUserFromPreferences(){
        if (Globals.user == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
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

    public void showOneButtonDialogFromService(String title, String message, DialogInterface.OnClickListener action) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppTheme);
        dialogBuilder.setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.ok, action)
            .setCancelable(false);
        final AlertDialog dialog = dialogBuilder.create();
        final Window dialogWindow = dialog.getWindow();
        final WindowManager.LayoutParams dialogWindowAttributes = dialogWindow.getAttributes();

        // Set fixed width (280dp) and WRAP_CONTENT height
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialogWindowAttributes);
        lp.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 280, getResources().getDisplayMetrics());
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialogWindow.setAttributes(lp);

        // Set to TYPE_SYSTEM_ALERT so that the Service can display it
        dialogWindow.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    public boolean locationIsMock(Location location){
        boolean isMock = !Settings.Secure.getString(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
        /*if (android.os.Build.VERSION.SDK_INT >= 18) {
            isMock = (location==null || location.isFromMockProvider());
        } else {
            isMock = !Settings.Secure.getString(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
        }*/

        /*
        //debug block
        String message = isMock ? "mock" : "good!";
        Toast.makeText(this, "georesult = "+message, Toast.LENGTH_LONG).show();
        Log.e("Punisher","georesult = "+message);
        */

        return isMock;
    }


    public static Retrofit getClient() {
        if (retrofit==null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(Globals.SERVER_URL)
                    .addConverterFactory(JacksonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    //by default the camera rotates the image so we need to rotate it back
    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case 180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case 90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            case 270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getOrientation(Context context, Uri photoUri) {
        /* it's on the external media. */
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor == null || cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        int result = cursor.getInt(0);
        cursor.close();
        return result;
    }
}
