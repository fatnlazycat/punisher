package com.example.dnk.punisher.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.example.dnk.punisher.Globals;
import com.example.dnk.punisher.PunisherUser;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.RequestMaker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SignUpActivity extends AppCompatActivity {

    TextView textViewSignUpErrorMessage;
    EditText editTextEmail, editTextPassword, editTextSurname, editTextName, editTextSecondName, editTextPhone;
    String email, password, surname, name, secondName, phone;
    CheckBox checkBoxPersonalDataAgreement;
    PunisherUser newUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        //getSupportActionBar().setDisplayShowTitleEnabled(true);
        //getSupportActionBar().setHomeButtonEnabled(true);

        textViewSignUpErrorMessage = (TextView)findViewById(R.id.textViewSignUpErrorMessage);

        editTextEmail = (EditText)findViewById(R.id.editTextSignUpEmail);
        editTextPassword = (EditText)findViewById(R.id.editTextSignUpPassword);
        editTextSurname = (EditText)findViewById(R.id.editTextSignUpSurname);
        editTextName = (EditText)findViewById(R.id.editTextSignUpName);
        editTextSecondName = (EditText)findViewById(R.id.editTextSignUpSecondName);
        editTextPhone = (EditText)findViewById(R.id.editTextSignUpPhone);
        checkBoxPersonalDataAgreement = (CheckBox)findViewById(R.id.checkBoxPersonalDataAgreement);
    }

    public void signUp(View view) {
        if (textViewSignUpErrorMessage.getVisibility() == View.VISIBLE)
            textViewSignUpErrorMessage.setVisibility(View.GONE);
        if (signUpDataOK()) {
            newUser = new PunisherUser(email, password, surname, name, secondName, phone);
            new SignUpSender(this).execute(newUser);
        }
    }

    private boolean signUpDataOK(){
        boolean result = true;
        result = checkBoxPersonalDataAgreement.isChecked() && result;

        result = !(email = editTextEmail.getText().toString()).isEmpty() && result;
        result = !(password = editTextPassword.getText().toString()).isEmpty() && result;
        result = !(surname = editTextSurname.getText().toString()).isEmpty() && result;
        result = !(name = editTextName.getText().toString()).isEmpty() && result;
        result = !(secondName = editTextSecondName.getText().toString()).isEmpty() && result;
        result = !(phone = editTextPhone.getText().toString()).isEmpty() && result;

        return result;
    }


    public void saveUser(PunisherUser user){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Globals.USER_EMAIL, user.email);
        editor.putString(Globals.USER_PASSWORD, user.password);
        editor.putString(Globals.USER_SURNAME, user.surname);
        editor.putString(Globals.USER_NAME, user.name);
        editor.putString(Globals.USER_SECOND_NAME, user.secondName);
        editor.putString(Globals.USER_PHONE, user.phone);
        editor.putInt(Globals.USER_ID, user.id);
        boolean b = editor.commit();
    }

    class SignUpSender extends AsyncTask<PunisherUser, Void, String> {

        Context context;

        public SignUpSender(Context context){
            this.context = context;
        }

        @Override
        protected String doInBackground(PunisherUser... params) {
            PunisherUser user = params[0];
            StringBuffer response = new StringBuffer();

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(Globals.SERVER_URL
                        + "api/v1/users").openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Accept-Encoding", "UTF-8");
                OutputStream os = urlConnection.getOutputStream();
                String request = new RequestMaker("user").makeRequest(
                        "email", user.email,
                        "firstname", user.name,
                        "surname", user.surname,
                        "secondname", user.secondName,
                        "phone_number", user.phone,
                        "password", user.password,
                        "password_confirmation", user.password);

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
            textViewSignUpErrorMessage.setVisibility(View.VISIBLE);
            textViewSignUpErrorMessage.setText(s);
            try {
                JSONObject json = new JSONObject(s);
                newUser.id = json.getInt("id");
                saveUser(newUser);
            } catch (JSONException e) {
                Log.e("Punisher-JSON", e.getMessage());
            }
        }

    }
}

