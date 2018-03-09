package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
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

import org.foundation101.karatel.manager.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.entity.PunisherUser;
import org.foundation101.karatel.R;
import org.foundation101.karatel.manager.HttpHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class TipsActivity extends Activity {
    static final String TAG = "LoginActivity";
    EditText editTextLoginEmail, editTextLoginPassword;
    FrameLayout progressBar;
    SharedPreferences preferences;

    // facebook part
    private CallbackManager fbCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // facebook part
        FacebookSdk.sdkInitialize(getApplicationContext());
        fbCallbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_login);

        ((KaratelApplication)getApplication()).sendScreenName(TAG);

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);

        editTextLoginEmail = (EditText)findViewById(R.id.editTextLoginEmail);
        editTextLoginPassword = (EditText)findViewById(R.id.editTextLoginPassword);

        if (KaratelPreferences.loggedIn()){
            startActivity(new Intent(this, MainActivity.class));
        }

        preferences = getPreferences(MODE_PRIVATE);
        if (preferences.contains(Globals.LAST_LOGIN_EMAIL)) {
            editTextLoginEmail.setText(preferences.getString(Globals.LAST_LOGIN_EMAIL, ""));
            editTextLoginPassword.requestFocus();
        }
    }

    public void login(View view) {
        String email = editTextLoginEmail.getText().toString();
        String passw = editTextLoginPassword.getText().toString();
        String gcmToken = KaratelPreferences.pushToken();
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
        if (HttpHelper.internetConnected(/*this*/)) {
            List<String> permissionNeeds = Arrays.asList("user_photos", "email", "user_birthday", "user_friends");
            //fbCallbackManager = CallbackManager.Factory.create();
            LoginManager.getInstance().logInWithReadPermissions(this, permissionNeeds);
            LoginManager.getInstance().registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    final AccessToken accessToken = loginResult.getAccessToken();
                    String gcmToken = KaratelPreferences.pushToken();
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
                    Globals.showError(R.string.cannot_connect_server, e);
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
            if (HttpHelper.internetConnected(/*context*/)) {
                String api = viaFacebook ? "signin?provider=facebook" : "signin";
                try {
                    return HttpHelper.proceedRequest(api, params[0], false);
                } catch (final IOException e) {
                    Globals.showError(R.string.cannot_connect_server, e);
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

                    KaratelPreferences.saveUser(
                            dataJSON.getString("token"),
                            userJSON.getString("email"),
                            userJSON.getString("surname"),
                            userJSON.getString("firstname"),
                            userJSON.getString("secondname"),
                            userJSON.getString("phone_number"),
                            userJSON.getInt("id"));

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
                Globals.showError(R.string.error, e);
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
                Globals.showError(R.string.cannot_connect_server, e);
            }
            KaratelPreferences.setUserAvatar(Globals.user.avatarFileName);
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

