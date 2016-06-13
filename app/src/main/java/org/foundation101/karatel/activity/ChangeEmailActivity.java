package org.foundation101.karatel.activity;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.foundation101.karatel.R;
import org.foundation101.karatel.HttpHelper;
import org.json.JSONException;
import org.json.JSONObject;

public class ChangeEmailActivity extends AppCompatActivity {
    Toolbar toolbar;
    ViewGroup viewGroup;
    Button button;
    EditText emailEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_email);

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

        viewGroup.getChildAt(1).setVisibility(View.GONE);

        button = (Button)findViewById(R.id.buttonRegister);
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

        @Override
        protected String doInBackground(String... params) {
            String email = params[0];
            String request = new HttpHelper("user").makeRequestString(new String[] {"email", email});
            return HttpHelper.proceedRequest("email", request, true);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String message;
            try {
                JSONObject json = new JSONObject(s);
                message = json.getString("status");
            } catch (JSONException e) {
                e.printStackTrace();
                message = s;
            }
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ChangeEmailActivity.this);
            AlertDialog dialog = dialogBuilder.setTitle(R.string.email_change)
                    .setNegativeButton(R.string.ok, simpleListener).create();
            dialog.show();
        }
    }
}
