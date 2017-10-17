package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.ComplainsBookAdapter;
import org.foundation101.karatel.entity.Violation;

public class PossibleValuesActivity extends AppCompatActivity {
    private final String TAG_OTHERS = "Інше";
    final static int REQUEST_CODE_CUSTOM_TEXT = 1002;

    int targetRequisiteNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_possible_values);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);

        final ListView lvPossibleValues = (ListView) findViewById(R.id.lvPossibleValues);

        final Intent intent = getIntent();
        if (intent != null) {
            final String title = intent.getStringExtra(Globals.POSSIBLE_VALUES_HEADER);
            toolbar.setTitle(title);

            String[] data = intent.getStringArrayExtra(Globals.POSSIBLE_VALUES);
            targetRequisiteNumber = intent.getIntExtra(Globals.REQUISITE_NUMBER_FOR_POSSIBLE_VALUES, 0);
            final int adapterItemLayout = android.R.layout.simple_list_item_1;
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, adapterItemLayout, data) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                    convertView = inflater.inflate(adapterItemLayout, parent, false);
                    if (convertView instanceof TextView) {
                        TextView tv = (TextView) convertView;
                        tv.setText(getItem(position));
                        int arrowDrawableId = TAG_OTHERS.equals(getItem(position)) ? R.drawable.ic_arrow : 0;
                        tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, arrowDrawableId, 0);
                        return convertView;
                    } else return super.getView(position, convertView, parent);
                }
            };
            lvPossibleValues.setAdapter(adapter);
            lvPossibleValues.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String text = adapter.getItem(position);
                    if (TAG_OTHERS.equals(text)) {
                        Intent intent = new Intent(PossibleValuesActivity.this, CustomTextActivity.class);
                        intent.putExtra(Globals.POSSIBLE_VALUES_HEADER, title);
                        startActivityForResult(intent, REQUEST_CODE_CUSTOM_TEXT);
                    } else publishResult(text);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_CUSTOM_TEXT) {
            if (data != null && data.getAction()!= null) {
                publishResult(data.getAction());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void publishResult(String value) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(Globals.POSSIBLE_VALUES, value);
        resultIntent.putExtra(Globals.REQUISITE_NUMBER_FOR_POSSIBLE_VALUES, targetRequisiteNumber);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
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
