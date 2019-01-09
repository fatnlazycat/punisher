package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;

public class CustomTextActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_text);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);

        EditText etCustomText = findViewById(R.id.etCustomText);

        final Intent intent = getIntent();
        if (intent != null) {
            final String title = intent.getStringExtra(Globals.POSSIBLE_VALUES_HEADER);
            toolbar.setTitle(title);

            if (intent.hasExtra(Globals.POSSIBLE_VALUES_TEXT))
                etCustomText.setText(intent.getCharSequenceExtra(Globals.POSSIBLE_VALUES_TEXT));
        }

        etCustomText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String text = "" + v.getText();
                    Intent resultIntent = new Intent(text);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                    return true;
                }
                return false;
            }
        });
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
}
