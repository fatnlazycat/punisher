package org.foundation101.karatel.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.R;
import org.foundation101.karatel.manager.HttpHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class ChangeEmailActivity extends AppCompatActivity {
    static final String TAG = "ChangeEmail";

    Toolbar toolbar;
    ViewGroup viewGroup;
    Button button;
    EditText emailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);

        //Google Analytics part
        ((KaratelApplication)getApplication()).sendScreenName(TAG);

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        actionBar.setTitle(R.string.change_email);


        viewGroup = (ViewGroup)findViewById(R.id.new_email);

        TextView textView = (TextView)viewGroup.getChildAt(0);
        textView.setAllCaps(true);
        textView.setText(R.string.new_email);

        button = (Button)findViewById(R.id.buttonRegister);

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
        AlertDialog dialog = dialogBuilder.setMessage(R.string.are_you_sure)
                .setNegativeButton(R.string.no, simpleListener)
                .setPositiveButton(R.string.yes, simpleListener).create();
        dialog.show();
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

    private class EmailChanger extends AsyncTask<String, Void, String> {
        String email;

        @Override
        protected String doInBackground(String... params) {
            email = params[0];
            String request = new HttpHelper("user").makeRequestString(new String[] {"email", email});
            try {
                if (HttpHelper.internetConnected(/*ChangeEmailActivity.this*/)) {
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
                    Globals.user.email = email;
                    KaratelPreferences.setUserEmail(email);

                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ChangeEmailActivity.this);
                    AlertDialog dialog = dialogBuilder.setTitle(R.string.email_changed)
                            .setMessage(R.string.check_email_to_approve)
                            .setCancelable(false)
                            .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    Intent logoutIntent = new Intent(MainActivity.BROADCAST_RECEIVER_TAG);
                                    logoutIntent.putExtra(MainActivity.TAG_JUST_LOGOUT, true);
                                    LocalBroadcastManager.getInstance(getApplicationContext())
                                            .sendBroadcast(logoutIntent);
                                    finish();
                                }
                            }).create();
                    dialog.show();
                } else {
                    if (s.equals(HttpHelper.ERROR_JSON)) {
                        message = json.getString("error");
                    } else {
                        message = ChangeEmailActivity.this.getString(R.string.invalid_email);
                    }
                    Toast.makeText(KaratelApplication.getInstance(), message, Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Globals.showError(e.getMessage(), e);
            }


        }
    }
}
