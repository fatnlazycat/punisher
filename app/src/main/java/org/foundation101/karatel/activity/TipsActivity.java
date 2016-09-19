package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.Karatel;
import org.foundation101.karatel.PunisherUser;
import org.foundation101.karatel.R;
import org.foundation101.karatel.HttpHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class TipsActivity extends Activity {
    static final String TAG = "Login";
    EditText editTextLoginEmail, editTextLoginPassword;
    FrameLayout progressBar;
    SharedPreferences preferences, globalPreferences;

    // facebook part
    private CallbackManager fbCallbackManager;

    private final BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ((Karatel)getApplication()).showOneButtonDialogFromService(
                    "Помилка отримання токена",
                    "Вийдіть з програми та зайдіть знову, щоб отримувати сповіщення.",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            finishAffinity();
                        }
                    }
            );
        }
    };
    public static final String BROADCAST_RECEIVER_TAG = "myBroadcastReceiver_TipsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // facebook part
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_login);

        ((Karatel)getApplication()).sendScreenName(TAG);

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);

        editTextLoginEmail = (EditText)findViewById(R.id.editTextLoginEmail);
        editTextLoginPassword = (EditText)findViewById(R.id.editTextLoginPassword);

        globalPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (globalPreferences.contains(Globals.SESSION_TOKEN)){
            startActivity(new Intent(this, MainActivity.class));
        }

        preferences = getPreferences(MODE_PRIVATE);
        if (preferences.contains(Globals.LAST_LOGIN_EMAIL)) {
            editTextLoginEmail.setText(preferences.getString(Globals.LAST_LOGIN_EMAIL, ""));
            editTextLoginPassword.requestFocus();
        }

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(myBroadcastReceiver, new IntentFilter(BROADCAST_RECEIVER_TAG));
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(myBroadcastReceiver);
        super.onDestroy();
    }

    public void login(View view) {
        String email = editTextLoginEmail.getText().toString();
        String passw = editTextLoginPassword.getText().toString();
        String gcmToken = globalPreferences.contains(Globals.PUSH_TOKEN) ?
                globalPreferences.getString(Globals.PUSH_TOKEN, "") : "";
        String request = new HttpHelper("session").makeRequestString(new String[] {
                "email", email,
                "password", passw,
                "token", gcmToken,
                "platform", "android"
        });
        new LoginSender(this, false).execute(request);
    }

    public void signUp(View view) {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    public void startPasswordRenovation(View view) {
        startActivity(new Intent(this, ForgotPasswordActivity.class));
    }

    public void empty(View view) {
        //empty method to handle click events
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
         //facebook part
        fbCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    //hides the software keyboard
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Globals.hideSoftKeyboard(this, event);
        return super.dispatchTouchEvent( event );
    }

    public void facebookLogin(View view) {
        if (HttpHelper.internetConnected(this)) {
            List<String> permissionNeeds = Arrays.asList("user_photos", "email", "user_birthday", "user_friends");
            fbCallbackManager = CallbackManager.Factory.create();
            LoginManager.getInstance().logInWithReadPermissions(this, permissionNeeds);
            LoginManager.getInstance().registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    final AccessToken accessToken = loginResult.getAccessToken();
                    String gcmToken = globalPreferences.contains(Globals.PUSH_TOKEN) ?
                            globalPreferences.getString(Globals.PUSH_TOKEN, "") : "";
                    String uid = accessToken.getUserId();
                    String request = new HttpHelper("session").makeRequestString(new String[]{
                            "uid", uid,
                            "token", gcmToken,
                            "platform", "android"
                    });
                    new LoginSender(TipsActivity.this, true).execute(request);
                }

                @Override
                public void onCancel() {
                    LoginManager.getInstance().logOut();
                }

                @Override
                public void onError(FacebookException e) {
                    Globals.showError(TipsActivity.this, R.string.cannot_connect_server, e);
                }
            });
        } else {
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
        }
    }

    class LoginSender extends AsyncTask<String, Void, String> {

        //private static final String RESPONSE_INVALID_PASSWORD = "Invalid pasword";
        //private static final String MESSAGE_INVALID_PASSWORD = "Неправільний email та/або пароль";
        Context context;
        boolean viaFacebook;

        public LoginSender(Context context, boolean facebookFlag){
            this.context = context;
            viaFacebook = facebookFlag;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            if (HttpHelper.internetConnected(context)) {
                String api = viaFacebook ? "signin?provider=facebook" : "signin";
                try {
                    return HttpHelper.proceedRequest(api, params[0], false);
                } catch (final IOException e) {
                    TipsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Globals.showError(TipsActivity.this, R.string.cannot_connect_server, e);
                        }
                    });
                    return "";
                }
            } else return HttpHelper.ERROR_JSON;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals("success")) {
                    JSONObject dataJSON = json.getJSONObject("data");
                    Globals.sessionToken = dataJSON.getString("token");
                    //if we've got token without catching an exception -> login successful!
                    preferences.edit().putString(Globals.LAST_LOGIN_EMAIL, editTextLoginEmail.getText().toString()).apply();

                    //get user data
                    JSONObject userJSON = dataJSON.getJSONObject("user");
                    Globals.user = new PunisherUser(
                            userJSON.getString("email"),
                            "", //for password
                            userJSON.getString("surname"),
                            userJSON.getString("firstname"),
                            userJSON.getString("secondname"),
                            userJSON.getString("phone_number"));
                    Globals.user.id = userJSON.getInt("id");

                    globalPreferences.edit()
                            .putString(Globals.SESSION_TOKEN, dataJSON.getString("token"))
                            .putString(Globals.USER_EMAIL, userJSON.getString("email"))
                            .putString(Globals.USER_SURNAME, userJSON.getString("surname"))
                            .putString(Globals.USER_NAME, userJSON.getString("firstname"))
                            .putString(Globals.USER_SECOND_NAME, userJSON.getString("secondname"))
                            .putString(Globals.USER_PHONE, userJSON.getString("phone_number"))
                            .putInt(Globals.USER_ID, userJSON.getInt("id")).apply();

                    String avatarUrl = userJSON.getJSONObject("avatar").getString("url");
                    if (avatarUrl != null && !avatarUrl.equals("null")) {
                        new AvatarGetter(TipsActivity.this).execute(avatarUrl);
                    }
                    startActivity(new Intent(context, MainActivity.class));
                } else {
                    String errorMessage;
                    if (json.getString("status").equals("error")){
                        errorMessage = json.getString("error");
                    } else {
                        errorMessage = s;
                    }
                    Toast.makeText(TipsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Globals.showError(TipsActivity.this, R.string.error, e);
            }
            progressBar.setVisibility(View.GONE);
            //Toast.makeText(TipsActivity.this, "end of LoginSender", Toast.LENGTH_SHORT).show();
        }
    }

    public static class AvatarGetter extends AsyncTask<String, Void, Void>{

        Activity context;

        public void setViewToSet(ImageView viewToSet) {
            this.viewToSet = viewToSet;
        }

        ImageView viewToSet;

        public AvatarGetter(Activity context){
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //Toast.makeText(TipsActivity.this, "start of AvaterGetter", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                Globals.user.avatarFileName = context.getFilesDir() + "avatar" + Globals.user.id + CameraManager.PNG;
                URL url = new URL(Globals.SERVER_URL.replace("/api/v1/", "") + params[0]);
                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                FileOutputStream fos = new FileOutputStream(Globals.user.avatarFileName);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            } catch (final Exception e){
                Globals.user.avatarFileName = "";
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(context, R.string.cannot_connect_server, e);
                    }
                });
            }
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString(Globals.USER_AVATAR, Globals.user.avatarFileName).apply();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (context instanceof  MainActivity && viewToSet != null)
                ((MainActivity) context).setAvatarImageView(viewToSet);
            //Toast.makeText(TipsActivity.this, "end of AvatarGetter", Toast.LENGTH_SHORT).show();
        }
    }
}

