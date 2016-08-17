package org.foundation101.karatel.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ChangePasswordActivity extends AppCompatActivity {
    Toolbar toolbar;
    ViewGroup viewGroupPassword, viewGroupNewPassword;
    EditText oldPassword, newPassword;
    ImageButton showOldPassword, showNewPassword;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        actionBar.setTitle(R.string.change_password);

        button = (Button)findViewById(R.id.buttonChange);

        ChangePasswordTextWatcher textWatcher = new ChangePasswordTextWatcher();

        viewGroupPassword = (ViewGroup)findViewById(R.id.old_password);
        TextView textViewPassword = (TextView)viewGroupPassword.getChildAt(0);
        textViewPassword.setAllCaps(true);
        textViewPassword.setText(R.string.current_password);
        viewGroupPassword.getChildAt(1).setVisibility(View.GONE);
        oldPassword = (EditText)viewGroupPassword.getChildAt(2);
        oldPassword.setHint(R.string.enter_current_password);
        oldPassword.addTextChangedListener(textWatcher);
        oldPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        viewGroupNewPassword = (ViewGroup)findViewById(R.id.new_password);
        TextView textViewNewPassword = (TextView) viewGroupNewPassword.getChildAt(0);
        textViewNewPassword.setAllCaps(true);
        textViewNewPassword.setText(R.string.new_password);
        viewGroupNewPassword.getChildAt(1).setVisibility(View.GONE);
        newPassword = (EditText)viewGroupNewPassword.getChildAt(2);
        newPassword.setHint(R.string.enter_new_password);
        newPassword.addTextChangedListener(textWatcher);
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        showOldPassword = (ImageButton)findViewById(R.id.showOldPasswordButton);
        showOldPassword.setOnTouchListener(new ShowPasswordOnTouchListener(oldPassword));
        showNewPassword = (ImageButton)findViewById(R.id.showNewPasswordButton);
        showNewPassword.setOnTouchListener(new ShowPasswordOnTouchListener(newPassword));
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

    public void startPasswordRenovation(View view) {
        startActivity(new Intent(this, ForgotPasswordActivity.class));
    }

    public void changePassword(View view) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        AlertDialog dialog = dialogBuilder.setMessage(R.string.are_you_sure)
                .setNegativeButton(R.string.no, simpleListener)
                .setPositiveButton(R.string.yes, simpleListener).create();
        dialog.show();
    }

    DialogInterface.OnClickListener simpleListener = new DialogInterface.OnClickListener(){
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                new PasswordChanger().execute(oldPassword.getText().toString(), newPassword.getText().toString());
            }
            dialog.dismiss();
        }
    };

    void validateButton(){
        boolean check = !(oldPassword.getText().toString().isEmpty() || newPassword.getText().toString().isEmpty());
        button.setEnabled(check);
    }

    class ChangePasswordTextWatcher implements TextWatcher{
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) {
            validateButton();
        }
    }

    class ShowPasswordOnTouchListener implements View.OnTouchListener{
        EditText editText;

        ShowPasswordOnTouchListener(EditText toShow){
           editText = toShow;
        }
        public boolean onTouch(View v, MotionEvent event) {
           switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
               editText.setInputType(InputType.TYPE_CLASS_TEXT);
            break;
            case MotionEvent.ACTION_UP:
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            break;
            }
            return true;
        }
    }
    private class PasswordChanger extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String oldPass = params[0];
            String newPass = params[1];
            String request = new HttpHelper("user").makeRequestString(new String[]
                    {"password", oldPass, "new_user_password", newPass});
            try {
                if (HttpHelper.internetConnected(ChangePasswordActivity.this)) {
                    return HttpHelper.proceedRequest("change_password", request, true);
                } else return HttpHelper.ERROR_JSON;
            } catch (final IOException e) {
                ChangePasswordActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(ChangePasswordActivity.this, R.string.cannot_connect_server, e);
                    }
                });
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String message;
            try {
                JSONObject json = new JSONObject(s);
                message = json.getString("status");
                switch (message) {
                    case Globals.SERVER_SUCCESS : {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ChangePasswordActivity.this);
                        AlertDialog dialog = dialogBuilder.setTitle(R.string.password_change)
                                .setMessage(R.string.your_password_is_changed_successfully)
                                .setNegativeButton(R.string.ok, simpleListener).create();
                        dialog.show();
                        break;
                    }
                    case Globals.SERVER_ERROR : {
                        JSONObject errorsJSON = json.optJSONObject("errors");
                        if (errorsJSON != null && errorsJSON.has("password")) {
                            message = errorsJSON.getJSONArray("password").getString(0);
                        } else {
                            message = json.getString("errors");
                        }
                        Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_LONG).show();
                        break;
                    }
                }
            } catch (JSONException e) {
                Globals.showError(ChangePasswordActivity.this, R.string.cannot_connect_server, e);
            }
        }
    }
}
