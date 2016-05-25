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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

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

public class SignUpActivity extends AppCompatActivity implements View.OnFocusChangeListener, View.OnClickListener {

    TextView textViewSignUpErrorMessage;
    EditText editTextEmail, editTextPassword, editTextSurname, editTextName, editTextSecondName, editTextPhone;
    String email, password, surname, name, secondName, phone;
    CheckBox checkBoxPersonalDataAgreement;
    Button signUpButton;
    FrameLayout progressBar;
    PunisherUser newUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);


        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        //getSupportActionBar().setDisplayShowTitleEnabled(true);
        //getSupportActionBar().setHomeButtonEnabled(true);

        textViewSignUpErrorMessage = (TextView)findViewById(R.id.textViewSignUpErrorMessage);

        signUpButton = (Button)findViewById(R.id.buttonRegister);

        editTextEmail = (EditText)findViewById(R.id.editTextSignUpEmail);
        editTextPassword = (EditText)findViewById(R.id.editTextSignUpPassword);
        editTextSurname = (EditText)findViewById(R.id.editTextSignUpSurname);
        editTextName = (EditText)findViewById(R.id.editTextSignUpName);
        editTextSecondName = (EditText)findViewById(R.id.editTextSignUpSecondName);
        editTextPhone = (EditText)findViewById(R.id.editTextSignUpPhone);
        checkBoxPersonalDataAgreement = (CheckBox)findViewById(R.id.checkBoxPersonalDataAgreement);

        editTextEmail.setOnFocusChangeListener(this);
        editTextPassword.setOnFocusChangeListener(this);
        editTextSurname.setOnFocusChangeListener(this);
        editTextName.setOnFocusChangeListener(this);
        editTextSecondName.setOnFocusChangeListener(this);
        editTextPhone.setOnFocusChangeListener(this);
        checkBoxPersonalDataAgreement.setOnFocusChangeListener(this);
        checkBoxPersonalDataAgreement.setOnClickListener(this);
    }

    public void signUp(View view) {
        if (textViewSignUpErrorMessage.getVisibility() == View.VISIBLE)
            textViewSignUpErrorMessage.setVisibility(View.GONE);
        if (signUpDataOK()) {
            newUser = new PunisherUser(email, password, surname, name, secondName, phone);
            new SignUpSender(this).execute(newUser);
        } else signUpButton.setEnabled(false);
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

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        signUpButton.setEnabled(signUpDataOK());
    }

    @Override
    public void onClick(View v) {
        signUpButton.setEnabled(signUpDataOK());
        //v.requestFocus();
    }

    public void empty(View view) {
        //empty method to handle click events
    }

    class SignUpSender extends AsyncTask<PunisherUser, Void, String> {

        static final String RESPONSE_INVALID_EMAIL = "Email is invalid";
        static final String MESSAGE_INVALID_EMAIL = "Перевірте правильність написання вашого email\n";
        static final String RESPONSE_INVALID_PASSWORD = "Password is too short (minimum is 6 characters)";
        static final String MESSAGE_INVALID_PASSWORD = "Пароль має бути не коротшим за 6 символів\n";
        static final String RESPONSE_INVALID_PHONE = "Phone number is an invalid number";
        static final String MESSAGE_INVALID_PHONE = "Перевірте правильність та формат телефонного номеру\n";
        static final String RESPONSE_EMAIL_ALREADY_TAKEN = "Email has already been taken";
        static final String MESSAGE_EMAIL_ALREADY_TAKEN = "Користувача з цим e-mail вже зареєстровано\n";

        Context context;

        public SignUpSender(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            editTextEmail.setBackgroundResource(R.drawable.border_for_edittext_normal);
            editTextPassword.setBackgroundResource(R.drawable.border_for_edittext_normal);
            editTextPhone.setBackgroundResource(R.drawable.border_for_edittext_normal);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(PunisherUser... params) {
            PunisherUser user = params[0];
            StringBuffer response = new StringBuffer();

            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(Globals.SERVER_URL
                        + "users").openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Accept-Encoding", "UTF-8");
                OutputStream os = urlConnection.getOutputStream();
                String request = new RequestMaker("user").makeRequest(new String[]{
                        "email", user.email,
                        "firstname", user.name,
                        "surname", user.surname,
                        "secondname", user.secondName,
                        "phone_number", user.phone,
                        "password", user.password,
                        "password_confirmation", user.password
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
            progressBar.setVisibility(View.GONE);

            try {
                JSONObject json = new JSONObject(s);
                if (json.has("id")) {
                    Toast.makeText(SignUpActivity.this, R.string.user_created, Toast.LENGTH_LONG).show();
                    newUser.id = json.getInt("id");
                    saveUser(newUser);
                    SignUpActivity.this.finish();
                }
            } catch (JSONException e) {
                textViewSignUpErrorMessage.setVisibility(View.VISIBLE);
                StringBuilder sb = new StringBuilder();
                if (s.contains(RESPONSE_INVALID_EMAIL)){
                    sb.append(MESSAGE_INVALID_EMAIL);
                    editTextEmail.setBackgroundResource(R.drawable.border_for_edittext_error);
                }
                if (s.contains(RESPONSE_INVALID_PASSWORD)){
                    sb.append(MESSAGE_INVALID_PASSWORD);
                    editTextPassword.setBackgroundResource(R.drawable.border_for_edittext_error);
                }
                if (s.contains(RESPONSE_INVALID_PHONE)){
                    sb.append(MESSAGE_INVALID_PHONE);
                    editTextPhone.setBackgroundResource(R.drawable.border_for_edittext_error);
                }
                if (s.contains(RESPONSE_EMAIL_ALREADY_TAKEN)){
                    sb.append(MESSAGE_EMAIL_ALREADY_TAKEN);
                    editTextEmail.setBackgroundResource(R.drawable.border_for_edittext_error);
                }
                textViewSignUpErrorMessage.setText(sb.toString());
            }
        }

    }
}

