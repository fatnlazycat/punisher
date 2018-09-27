package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.foundation101.karatel.Const;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.PunisherUser;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.utils.FileUtils;
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
    RelativeLayout progressBar;

    // facebook part
    private CallbackManager fbCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // facebook part
        //FacebookSdk.sdkInitialize(getApplicationContext());
        fbCallbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_login);

        ((KaratelApplication)getApplication()).sendScreenName(TAG);

        progressBar = findViewById(R.id.rlProgress);

        editTextLoginEmail = findViewById(R.id.editTextLoginEmail);
        editTextLoginPassword = findViewById(R.id.editTextLoginPassword);

        if (KaratelPreferences.loggedIn()){
            startActivity(new Intent(this, MainActivity.class));
        }

        String lastLoginEmail = KaratelPreferences.lastLoginEmail();
        if (!lastLoginEmail.isEmpty()) {
            editTextLoginEmail.setText(lastLoginEmail);
            editTextLoginPassword.requestFocus();
        }
    }

    public void login(View view) {
        String email = editTextLoginEmail.getText().toString();
        String passw = editTextLoginPassword.getText().toString();
        String gcmToken = KaratelPreferences.pushToken();

        new LoginSender(this, email, passw, gcmToken).execute();
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
        if (HttpHelper.internetConnected()) {
            List<String> permissionNeeds = Arrays.asList(/*"user_photos", */"email"/*, "user_birthday", "user_friends"*/);
            //fbCallbackManager = CallbackManager.Factory.create();
            LoginManager.getInstance().logInWithReadPermissions(this, permissionNeeds);
            LoginManager.getInstance().registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    final AccessToken accessToken = loginResult.getAccessToken();
                    String gcmToken = KaratelPreferences.pushToken();
                    String uid = accessToken.getUserId();

                    new LoginSender(TipsActivity.this, uid, gcmToken).execute();
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
            Globals.showMessage(R.string.no_internet_connection);
        }
    }

    public static class LoginSender extends AsyncTask<Void, Void, String> {
        Activity activity;
        View progressBar;
        boolean viaFacebook;
        String email = null;
        String password = null;
        String gcmToken;
        String uid = null;

        //email+password version
        public LoginSender(Activity activity, String email, String password, String gcmToken){
            this.activity = activity;
            viaFacebook   = false;
            this.email    = email;
            this.password = password;
            this.gcmToken = gcmToken;

            if (activity != null && activity instanceof TipsActivity)
                this.progressBar = ((TipsActivity) activity).progressBar;
        }

        //facebook version
        public LoginSender(Activity activity, String uid, String gcmToken){
            this.activity = activity;
            viaFacebook   = true;
            this.gcmToken = gcmToken;
            this.uid      = uid;

            if (activity != null && activity instanceof TipsActivity)
                this.progressBar = ((TipsActivity) activity).progressBar;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... params) {
            if (HttpHelper.internetConnected()) {
                try {
                    return performLoginRequest(this);
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
                    //if we've got token without catching an exception -> login successful!
                    if (viaFacebook) {
                        KaratelPreferences.setPassword(uid);
                        KaratelPreferences.setLastLoginEmail(null);
                    } else {
                        KaratelPreferences.setPassword(password);
                        KaratelPreferences.setLastLoginEmail(email);
                    }

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
                        new AvatarGetter(activity).execute(avatarUrl);
                    }
                    activity.startActivity(new Intent(activity, MainActivity.class));
                } else {
                    String errorMessage;
                    if (json.getString("status").equals("error")){
                        errorMessage = json.getString("error");
                    } else {
                        errorMessage = s;
                    }
                    Globals.showMessage(errorMessage);
                }
            } catch (JSONException e) {
                Globals.showError(R.string.error, e);
            }
            progressBar.setVisibility(View.GONE);
            //Toast.makeText(TipsActivity.this, "end of LoginSender", Toast.LENGTH_SHORT).show();
        }

        public static String performLoginRequest(TipsActivity.LoginSender loginSender) throws IOException {
            String request = loginSender.viaFacebook ?
                    new HttpHelper("session").makeRequestString(new String[]{
                            "uid", loginSender.uid,
                            "token", loginSender.gcmToken,
                            "platform", "android"
                    }) :
                    new HttpHelper("session").makeRequestString(new String[] {
                            "email", loginSender.email,
                            "password", loginSender.password,
                            "token", loginSender.gcmToken,
                            "platform", "android"
                    });

            String api = loginSender.viaFacebook ? "signin?provider=facebook" : "signin";

            return HttpHelper.proceedRequest(api, request, false);
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
            String avatarFileName = FileUtils.INSTANCE.avatarFileName(false);
            try {
                URL url = new URL(Const.SERVER_URL.replace("/api/v1/", "") + params[0]);
                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                FileOutputStream fos = new FileOutputStream(avatarFileName);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            } catch (final Exception e){
                avatarFileName = "";
                Globals.showError(R.string.cannot_connect_server, e);
            }
            KaratelPreferences.setUserAvatar(avatarFileName);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (context instanceof  MainActivity && viewToSet != null)
                ((MainActivity) context).setAvatarImageView(viewToSet, KaratelPreferences.userAvatar());
        }
    }
}

