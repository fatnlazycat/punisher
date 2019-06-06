package org.foundation101.karatel.activity;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import javax.inject.Inject;


public class ChangeEmailActivity extends AppCompatActivity {
    static final String TAG = "ChangeEmail";
    static final String EMAIL_CHANGED = "EMAIL_CHANGED";

    Toolbar toolbar;
    ViewGroup viewGroup;
    Button button;
    EditText emailEditText;
    AlertDialog promptDialog  = null;
    AlertDialog successDialog = null;

    @Inject KaratelPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);
        KaratelApplication.dagger().inject(this);

        //Google Analytics part
        ((KaratelApplication)getApplication()).sendScreenName(TAG);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        actionBar.setTitle(R.string.change_email);


        viewGroup = findViewById(R.id.new_email);

        TextView textView = (TextView)viewGroup.getChildAt(0);
        textView.setAllCaps(true);
        textView.setText(R.string.new_email);

        button = findViewById(R.id.buttonRegister);

        emailEditText = (EditText)viewGroup.getChildAt(2);
        emailEditText.setHint(R.string.enter_new_email);
        emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                validateButton(s.toString());
            }
        });
        emailEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Globals.hideSoftKeyboard(ChangeEmailActivity.this);
                return true;
            }
        });

        viewGroup.getChildAt(1).setVisibility(View.GONE);

        if (savedInstanceState != null && savedInstanceState.getBoolean(EMAIL_CHANGED, false))
            showSuccessDialog();
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

    void validateButton(String s){
        button.setEnabled(!s.isEmpty());
    }

    public void changeEmail(View view) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        promptDialog = dialogBuilder.setMessage(R.string.are_you_sure)
                .setNegativeButton(R.string.no, simpleListener)
                .setPositiveButton(R.string.yes, simpleListener).create();
        promptDialog.show();
    }

    DialogInterface.OnClickListener simpleListener = new DialogInterface.OnClickListener(){
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                    new EmailChanger().execute(emailEditText.getText().toString());
                }
            dialog.dismiss();
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        boolean successDialogShown = successDialog != null && successDialog.isShowing();
        outState.putBoolean(EMAIL_CHANGED, successDialogShown);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (promptDialog  != null) promptDialog .dismiss();
        if (successDialog != null) successDialog.dismiss();
        super.onDestroy();
    }

    private void showSuccessDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ChangeEmailActivity.this);
        successDialog = dialogBuilder.setTitle(R.string.email_changed)
                .setMessage(R.string.check_email_to_approve)
                .setCancelable(false)
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        new MainActivity.SignOutSender(ChangeEmailActivity.this).execute();
                        /*Intent logoutIntent = new Intent(MainActivity.BROADCAST_RECEIVER_TAG);
                        logoutIntent.putExtra(MainActivity.TAG_JUST_LOGOUT, true);
                        LocalBroadcastManager.getInstance(getApplicationContext())
                                .sendBroadcast(logoutIntent);
                        finish();*/
                    }
                }).create();
        successDialog.show();
    }

    private class EmailChanger extends AsyncTask<String, Void, String> {
        String email;

        @Override
        protected String doInBackground(String... params) {
            email = params[0];
            String request = new HttpHelper("user").makeRequestString(new String[] {"email", email});
            try {
                if (HttpHelper.internetConnected()) {
                    return HttpHelper.proceedRequest("email", request, true);
                } else return HttpHelper.ERROR_JSON;
            } catch (final IOException e){
                Globals.showError(R.string.cannot_connect_server, e);
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String message;
            try {
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals(Globals.SERVER_SUCCESS)){
                    preferences.setUserEmail(email);
                    showSuccessDialog();
                } else {
                    if (s.equals(HttpHelper.ERROR_JSON)) {
                        message = json.getString("error");
                    } else {
                        message = ChangeEmailActivity.this.getString(R.string.invalid_email);
                    }
                    Globals.showMessage(message);
                }
            } catch (JSONException e) {
                Globals.showError(e.getMessage(), e);
            }


        }
    }
}
