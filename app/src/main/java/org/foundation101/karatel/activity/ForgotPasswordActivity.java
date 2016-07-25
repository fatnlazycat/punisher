package org.foundation101.karatel.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText editTextForgotPasswordEmail;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbarForgotPassword);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_grey);

        editTextForgotPasswordEmail = (EditText)findViewById(R.id.editTextForgotPasswordEmail);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //hides the software keyboard
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Globals.hideSoftKeyboard(this, event);
        return super.dispatchTouchEvent( event );
    }

    public void proceedWithPasswordRenovation(View view) {
        String email = editTextForgotPasswordEmail.getText().toString();
        new PasswordRestorator().execute(email);
    }


    class PasswordRestorator extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String request = new HttpHelper("user").makeRequestString(new String[]{"email", params[0]});
            try {
                if (HttpHelper.internetConnected(ForgotPasswordActivity.this)) {
                    return HttpHelper.proceedRequest("password", request, false);
                } else return HttpHelper.ERROR_JSON;
            } catch (final IOException e){
                ForgotPasswordActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(ForgotPasswordActivity.this, R.string.cannot_connect_server, e);
                    }
                });
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject json = new JSONObject(s);
                switch (json.getString("status")){
                    case Globals.SERVER_SUCCESS : {
                        startActivity(new Intent(ForgotPasswordActivity.this, ForgotPassword2Activity.class));
                        break;
                    }
                    case Globals.SERVER_ERROR : {
                        String message = json.has("error") ? json.getString("error") : json.getString("errors");
                        Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                        break;
                    }
                }
            } catch (JSONException e) {
                Globals.showError(ForgotPasswordActivity.this, R.string.cannot_connect_server, e);
            }
        }
    }
}
