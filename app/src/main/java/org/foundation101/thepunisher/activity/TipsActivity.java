package org.foundation101.thepunisher.activity;

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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.foundation101.thepunisher.CameraManager;
import org.foundation101.thepunisher.Globals;
import org.foundation101.thepunisher.PunisherUser;
import org.foundation101.thepunisher.R;
import org.foundation101.thepunisher.RequestMaker;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class TipsActivity extends Activity {
    EditText editTextLoginEmail, editTextLoginPassword;
    FrameLayout progressBar;
    SharedPreferences preferences;
    /* facebook part
    private LoginButton loginButton;
    private CallbackManager callbackManager;
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //check connectivity
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        if (!networkInfo.isConnected()){
            Toast.makeText(this, "no internet connection", Toast.LENGTH_LONG).show();
        }

        /* facebook part
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        */
        setContentView(R.layout.activity_login);

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);

        editTextLoginEmail = (EditText)findViewById(R.id.editTextLoginEmail);
        editTextLoginPassword = (EditText)findViewById(R.id.editTextLoginPassword);

        preferences = getPreferences(MODE_PRIVATE);
        if (preferences.contains(Globals.LAST_LOGIN_EMAIL)) {
            editTextLoginEmail.setText(preferences.getString(Globals.LAST_LOGIN_EMAIL, ""));
            editTextLoginPassword.requestFocus();
        }

        /* facebook part
        loginButton = (LoginButton)findViewById(R.id.login_button);
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Profile fbProfile = Profile.getCurrentProfile();
                AccessToken accessToken = loginResult.getAccessToken();
                GraphRequest request = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.v("LoginActivity", response.toString());
                        try {
                            // Application code
                            String email = object.getString("email");
                            //new LoginSender(TipsActivity.this).execute(email, passw);
                        } catch (JSONException e){
                            Log.e("Punisher", e.getMessage());
                        }
                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender,birthday");
                request.setParameters(parameters);
                request.executeAsync();
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
        */
    }

    public void login(View view) {
        String email = editTextLoginEmail.getText().toString();
        String passw = editTextLoginPassword.getText().toString();
        new LoginSender(this).execute(email, passw);
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
        /* facebook part
        callbackManager.onActivityResult(requestCode, resultCode, data);
        */
    }

    class LoginSender extends AsyncTask<String, Void, String> {

        private static final String RESPONSE_INVALID_PASSWORD = "Invalid pasword";
        private static final String MESSAGE_INVALID_PASSWORD = "Неправільний email та/або пароль";
        Context context;

        public LoginSender(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            String email = params[0];
            String password = params[1];
            StringBuilder response = new StringBuilder();

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(Globals.SERVER_URL
                        + "signin").openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Accept-Encoding", "UTF-8");
                OutputStream os = urlConnection.getOutputStream();
                String request = new RequestMaker("session").makeRequest(new String[] {
                        "email", email,
                        "password", password
                });

                os.write(request.getBytes());
                os.flush();
                os.close();

                int responseCode = urlConnection.getResponseCode();
                InputStream is;
                is = (responseCode == HttpURLConnection.HTTP_OK) ?
                        urlConnection.getInputStream(): urlConnection.getErrorStream();
                BufferedReader reader = new BufferedReader((new InputStreamReader(is)));
                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    response.append(inputLine);
                }
                reader.close();
            } catch (IOException e) {
                Log.e("Punisher error", e.getMessage());
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject json = new JSONObject(s).getJSONObject("data");
                Globals.token = json.getString("token");
                //if we've got token without catching an exception -> login successful!
                preferences.edit().putString(Globals.LAST_LOGIN_EMAIL, editTextLoginEmail.getText().toString()).apply();

                //get user data
                JSONObject userJSON = json.getJSONObject("user");
                Globals.user = new PunisherUser(
                        userJSON.getString("email"),
                        "", //for password
                        userJSON.getString("surname"),
                        userJSON.getString("firstname"),
                        userJSON.getString("secondname"),
                        userJSON.getString("phone_number"));
                Globals.user.id = userJSON.getInt("id");
                String avatarUrl = userJSON.getJSONObject("avatar").getString("url");
                if (avatarUrl != null){
                    new AvatarGetter().execute(avatarUrl);
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
                startActivity(new Intent(context, MainActivity.class));
            } catch (JSONException e) {
                if (s.contains(RESPONSE_INVALID_PASSWORD)){
                    Toast.makeText(TipsActivity.this, MESSAGE_INVALID_PASSWORD, Toast.LENGTH_LONG).show();
                }
            }
            progressBar.setVisibility(View.GONE);
        }
    }

    class AvatarGetter extends AsyncTask<String, Void, Void>{
        @Override
        protected Void doInBackground(String... params) {
            try {
                Globals.user.avatarFileName = TipsActivity.this.getFilesDir() + "avatar" + Globals.user.id + CameraManager.PNG;
                URL url = new URL(Globals.SERVER_URL.substring(0, Globals.SERVER_URL.length() - 1) + params[0]);
                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                FileOutputStream fos = new FileOutputStream(Globals.user.avatarFileName);
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            } catch (Exception e){
                Log.e("Punisher", e.getMessage());
            }
            return null;
        }
    }
}

