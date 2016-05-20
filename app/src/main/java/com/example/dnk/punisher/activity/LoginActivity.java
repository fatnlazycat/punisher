package com.example.dnk.punisher.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.example.dnk.punisher.Globals;
import com.example.dnk.punisher.PunisherUser;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.RequestMaker;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends Activity {
    EditText editTextLoginEmail, editTextLoginPassword;
    FrameLayout progressBar;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgressLogin);

        editTextLoginEmail = (EditText)findViewById(R.id.editTextLoginEmail);
        editTextLoginPassword = (EditText)findViewById(R.id.editTextLoginPassword);

        preferences = getPreferences(MODE_PRIVATE);
        if (preferences.contains(Globals.LAST_LOGIN_EMAIL)) {
            editTextLoginEmail.setText(preferences.getString(Globals.LAST_LOGIN_EMAIL, ""));
            editTextLoginPassword.requestFocus();
        }

        //FacebookSdk.sdkInitialize(getApplicationContext());
        //AppEventsLogger.activateApp(this);

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
        //empty method to catch onClick events
    }

    class LoginSender extends AsyncTask<String, Void, String> {

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
                        + "api/v1/signin").openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Accept-Encoding", "UTF-8");
                OutputStream os = urlConnection.getOutputStream();
                String request = new RequestMaker("session").makeRequest(
                        "email", email,
                        "password", password);

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
            //Toast.makeText(context, s, Toast.LENGTH_LONG).show();
            try {
                JSONObject json = new JSONObject(s);
                Globals.token = json.getString("token");
                //if we've got token without catching an exception -> login successful!
                preferences.edit().putString(Globals.LAST_LOGIN_EMAIL, editTextLoginEmail.getText().toString()).apply();

                //check if user preferences are filled in, fill if not.
                //required when the app was newly installed
                SharedPreferences globalPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                if (globalPreferences.contains(Globals.USER_ID)) {//no preferences saved, so do it
                    JSONObject userJSON = json.getJSONObject("user");
                    PunisherUser user = new PunisherUser(
                            userJSON.getString("email"),
                            "", //for password
                            userJSON.getString("surname"),
                            userJSON.getString("firstname"),
                            userJSON.getString("secondname"),
                            userJSON.getString("phone_number"));
                    globalPreferences.edit()
                            .putString(Globals.USER_EMAIL, user.email)
                            .putString(Globals.USER_PASSWORD, user.password)
                            .putString(Globals.USER_SURNAME, user.surname)
                            .putString(Globals.USER_NAME, user.name)
                            .putString(Globals.USER_SECOND_NAME, user.secondName)
                            .putString(Globals.USER_PHONE, user.phone)
                            .putInt(Globals.USER_ID, user.id).apply();
                }
                startActivity(new Intent(context, MainActivity.class));
            } catch (JSONException e) {
                Log.e("Punisher-JSON", e.getMessage());
            }
            progressBar.setVisibility(View.GONE);
        }
    }
}

