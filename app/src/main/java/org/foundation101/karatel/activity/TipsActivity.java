package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
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
import org.foundation101.karatel.PunisherUser;
import org.foundation101.karatel.R;
import org.foundation101.karatel.HttpHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class TipsActivity extends Activity {
    EditText editTextLoginEmail, editTextLoginPassword;
    FrameLayout progressBar;
    SharedPreferences preferences;
    // facebook part
    Button facebookLoginButton;
    //private LoginButton loginButton;
    private CallbackManager fbCallbackManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //check connectivity
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()){
            Toast.makeText(this, "no internet connection", Toast.LENGTH_LONG).show();
        }

        // facebook part
        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.activity_login);

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);

        editTextLoginEmail = (EditText)findViewById(R.id.editTextLoginEmail);
        editTextLoginPassword = (EditText)findViewById(R.id.editTextLoginPassword);

        preferences = getPreferences(MODE_PRIVATE);
        if (preferences.contains(Globals.LAST_LOGIN_EMAIL)) {
            editTextLoginEmail.setText(preferences.getString(Globals.LAST_LOGIN_EMAIL, ""));
            editTextLoginPassword.requestFocus();
        }
    }

    public void login(View view) {
        String email = editTextLoginEmail.getText().toString();
        String passw = editTextLoginPassword.getText().toString();
        String gcmToken = Globals.pushToken == null ? "" : Globals.pushToken;
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
        List<String> permissionNeeds= Arrays.asList("user_photos", "email", "user_birthday", "user_friends");
        fbCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().logInWithReadPermissions(this,permissionNeeds);
        LoginManager.getInstance().registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                final AccessToken accessToken = loginResult.getAccessToken();
                String uid = accessToken.getUserId();
                String request = new HttpHelper("session").makeRequestString(new String[] {"uid", uid});
                new LoginSender(TipsActivity.this ,true).execute(request);
            }

            @Override
            public void onCancel() {
                LoginManager.getInstance().logOut();
            }

            @Override
            public void onError(FacebookException e) {
                String message = e.getMessage();
                Toast.makeText(TipsActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
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
            String api = viaFacebook ? "signin?provider=facebook" : "signin";
            return HttpHelper.proceedRequest(api, params[0], false);
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
                    String avatarUrl = userJSON.getJSONObject("avatar").getString("url");
                    if (avatarUrl != null) {
                        new AvatarGetter().execute(avatarUrl);
                    }
                    startActivity(new Intent(context, MainActivity.class));
                } else if (json.getString("status").equals("error")){
                    Toast.makeText(TipsActivity.this, json.getString("error"), Toast.LENGTH_LONG).show();
                }
                /*//check if user preferences are filled in, fill if not.
                //required when the app was newly installed
                SharedPreferences globalPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                if (!globalPreferences.contains(Globals.USER_ID)) {//no preferences saved, so do it
                    JSONObject userJSON = json.getJSONObject("user");
                    PunisherUser user = new PunisherUser(
                            userJSON.getString("email"),
                            "", //for password
                            userJSON.getString("surname"),
                            userJSON.getString("firstname"),
                            userJSON.getString("secondname"),
                            userJSON.getString("phone_number"));
                    user.id = Integer.parseInt(userJSON.getString("id"));
                    boolean b = globalPreferences.edit()
                            .putString(Globals.USER_EMAIL, user.email)
                            .putString(Globals.USER_PASSWORD, user.password)
                            .putString(Globals.USER_SURNAME, user.surname)
                            .putString(Globals.USER_NAME, user.name)
                            .putString(Globals.USER_SECOND_NAME, user.secondName)
                            .putString(Globals.USER_PHONE, user.phone)
                            .putInt(Globals.USER_ID, user.id).commit();
                    int i =0;
                }*/

            } catch (JSONException e) {
                Toast.makeText(TipsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
            progressBar.setVisibility(View.GONE);
        }
    }

    class AvatarGetter extends AsyncTask<String, Void, Void>{
        @Override
        protected Void doInBackground(String... params) {
            try {
                Globals.user.avatarFileName = TipsActivity.this.getFilesDir() + "avatar" + Globals.user.id + CameraManager.PNG;
                URL url = new URL(Globals.SERVER_URL.replace("/api/v1/", "") + params[0]);
                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                FileOutputStream fos = new FileOutputStream(Globals.user.avatarFileName);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            } catch (Exception e){
                Globals.user.avatarFileName = "";
                Log.e("Punisher", e.getMessage());
            }
            return null;
        }
    }
}

