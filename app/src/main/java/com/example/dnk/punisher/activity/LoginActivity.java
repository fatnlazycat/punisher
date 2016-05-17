package com.example.dnk.punisher.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.example.dnk.punisher.Constants;
import com.example.dnk.punisher.PunisherUser;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.RequestMaker;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextLoginEmail = (EditText)findViewById(R.id.editTextLoginEmail);
        editTextLoginPassword = (EditText)findViewById(R.id.editTextLoginPassword);
    }

    public void login(View view) {
        String email = editTextLoginEmail.getText().toString();
        String passw = editTextLoginPassword.getText().toString();
        new LoginSender(this).execute(email, passw);
    }

    public void signUp(View view) {
        startActivity(new Intent(this, SignUpActivity.class));
    }

    class LoginSender extends AsyncTask<String, Void, String> {

        Context context;

        public LoginSender(Context context){
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            String email = params[0];
            String password = params[1];
            StringBuilder response = new StringBuilder();

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(Constants.SERVER_URL
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
                String token = json.getString("token");
                startActivity(new Intent(context, MainActivity.class));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

