package org.foundation101.karatel.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.entity.PunisherUser;
import org.foundation101.karatel.R;
import org.foundation101.karatel.manager.HttpHelper;

import org.foundation101.karatel.manager.KaratelPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    static final String TAG = "Registration";

    TextView textViewSignUpErrorMessage;
    EditText editTextEmail, editTextPassword, editTextSurname, editTextName, editTextSecondName, editTextPhone;
    String email, password, surname, name, secondName, phone;
    CheckBox checkBoxPersonalDataAgreement;
    Button signUpButton;
    View progressBar;
    PunisherUser newUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        ((KaratelApplication)getApplication()).sendScreenName(TAG);

        progressBar = findViewById(R.id.rlProgress);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);

        textViewSignUpErrorMessage    = findViewById(R.id.textViewSignUpErrorMessage);

        signUpButton                  = findViewById(R.id.buttonRegister);

        editTextEmail                 = findViewById(R.id.etSignUpEmail);
        editTextPassword              = findViewById(R.id.etSignUpPassword);
        editTextSurname               = findViewById(R.id.etSignUpSurname);
        editTextName                  = findViewById(R.id.etSignUpName);
        editTextSecondName            = findViewById(R.id.etSignUpSecondName);
        editTextPhone                 = findViewById(R.id.etSignUpPhone);
        checkBoxPersonalDataAgreement = findViewById(R.id.checkBoxPersonalDataAgreement);

        SignUpTextWatcher textWatcher = new SignUpTextWatcher();

        editTextEmail                .addTextChangedListener(textWatcher);
        editTextPassword             .addTextChangedListener(textWatcher);
        editTextSurname              .addTextChangedListener(textWatcher);
        editTextName                 .addTextChangedListener(textWatcher);
        editTextSecondName           .addTextChangedListener(textWatcher);
        editTextPhone                .addTextChangedListener(textWatcher);
        checkBoxPersonalDataAgreement.setOnClickListener(this);

        if (!KaratelPreferences.fbLoginUid().isEmpty()) findViewById(R.id.tvFb).setVisibility(View.VISIBLE);
    }

    public void signUp(View view) {
        if (textViewSignUpErrorMessage.getVisibility() == View.VISIBLE)
            textViewSignUpErrorMessage.setVisibility(View.GONE);
        newUser = new PunisherUser(email, password, surname, name, secondName, phone);
        if (HttpHelper.internetConnected()) {
            new SignUpSender(this).execute(newUser);
        } else {
            Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
        }
    }

    void validateButton(){
        signUpButton.setEnabled(signUpDataOK());
    }

    private boolean signUpDataOK(){
        boolean result = checkBoxPersonalDataAgreement.isChecked();

        result = !(email = editTextEmail.getText().toString().replace(" ", "")).isEmpty() && result;
        result = !(password = editTextPassword.getText().toString().replace(" ", "")).isEmpty() && result;
        result = !(surname = editTextSurname.getText().toString().replace(" ", "")).isEmpty() && result;
        result = !(name = editTextName.getText().toString().replace(" ", "")).isEmpty() && result;
        result = !(secondName = editTextSecondName.getText().toString().replace(" ", "")).isEmpty() && result;
        result = !(phone = editTextPhone.getText().toString().replace(" ", "")).isEmpty() && result;

        return result;
    }

    class SignUpTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            validateButton();
        }
    }

    @Override
    public void onClick(View v) {
        validateButton();
    }

    public void empty(View view) {
        //empty method to handle click events
    }

    //hides the software keyboard
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Globals.hideSoftKeyboard(this, event);
        return super.dispatchTouchEvent( event );
    }

    class SignUpSender extends AsyncTask<PunisherUser, Void, String> {
        Context context;
        PunisherUser user;

        SignUpSender(Context context){
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
            user = params[0];
            String request = new HttpHelper("user").makeRequestString(new String[]{
                    "email", user.email,
                    "firstname", user.name,
                    "surname", user.surname,
                    "secondname", user.secondName,
                    "phone_number", user.phone,
                    "password", user.password,
                    "password_confirmation", user.password
            });
            try {
                return HttpHelper.proceedRequest("users", request, false);
            } catch (final IOException e) {
                Globals.showError(R.string.cannot_connect_server, e);
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.GONE);

            try {
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals("success")) {
                    JSONObject dataJSON = json.getJSONObject("data");

                    if (!KaratelPreferences.fbLoginUid().isEmpty()) {
                        KaratelPreferences.setFbLoginEmail(user.email);
                        KaratelPreferences.setFbLoginPassword(user.password);
                    }

                    if (dataJSON.has("id")) {
                        Toast.makeText(SignUpActivity.this, R.string.user_created, Toast.LENGTH_LONG).show();
                        SignUpActivity.this.finish();
                    }
                } else if (json.getString("status").equals("error")) {
                    textViewSignUpErrorMessage.setVisibility(View.VISIBLE);
                    StringBuilder sb = new StringBuilder();

                    JSONObject errorJSON = json.getJSONObject("error");
                    JSONArray errorNames = errorJSON.names();
                    for (int i = 0; i < errorNames.length(); i++){
                        EditText toMarkRed = null;
                        switch (errorNames.getString(i)){
                            case ("email") : toMarkRed = editTextEmail; break;
                            case ("password") : toMarkRed = editTextPassword; break;
                            case ("phone_number") : toMarkRed = editTextPhone; break;
                        }

                        //read only the first message in the array for each error type
                        String errorMessage = errorJSON.getJSONArray(errorNames.getString(i)).getString(0);

                        sb.append(errorMessage + "\n");
                        if (toMarkRed != null) {
                            toMarkRed.setBackgroundResource(R.drawable.border_for_edittext_error);
                        }
                    }
                    textViewSignUpErrorMessage.setText(sb.toString());
                }
            } catch (JSONException e) {
                Globals.showError(R.string.error, e);
                if (e.getMessage() != null) {
                    textViewSignUpErrorMessage.setVisibility(View.VISIBLE);
                    textViewSignUpErrorMessage.setText(e.getMessage());
                }
            }
        }
    }
}

