package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.utils.LayoutUtils;

public class PossibleValuesActivity extends AppCompatActivity {
    private final String TAG_OTHERS = "Інше";
    final static int REQUEST_CODE_CUSTOM_TEXT = 1002;

    int targetRequisiteNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_possible_values);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);

        final ListView lvPossibleValues = findViewById(R.id.lvPossibleValues);
        lvPossibleValues.addFooterView(footerForListView());

        final Intent startIntent = getIntent();
        if (startIntent != null) {
            String analyticsTag = startIntent.getStringExtra(Globals.VIOLATION_TYPE) + "-choice";
            KaratelApplication.getInstance().sendScreenName(analyticsTag);

            final String title = startIntent.getStringExtra(Globals.POSSIBLE_VALUES_HEADER);
            toolbar.setTitle(title);

            String[] data = startIntent.getStringArrayExtra(Globals.POSSIBLE_VALUES);
            targetRequisiteNumber = startIntent.getIntExtra(Globals.REQUISITE_NUMBER_FOR_POSSIBLE_VALUES, 0);
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
                        Intent customTextIntent = new Intent(PossibleValuesActivity.this, CustomTextActivity.class);
                        customTextIntent.putExtra(Globals.POSSIBLE_VALUES_HEADER, title);

                        if (startIntent.hasExtra(Globals.POSSIBLE_VALUES_TEXT))
                            customTextIntent.putExtra(Globals.POSSIBLE_VALUES_TEXT,
                                    startIntent.getCharSequenceExtra(Globals.POSSIBLE_VALUES_TEXT));

                        startActivityForResult(customTextIntent, REQUEST_CODE_CUSTOM_TEXT);
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

    private View footerForListView() {
        View footerView = new View(this);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, LayoutUtils.dpToPx(1));
        footerView.setLayoutParams(lp);
        footerView.setBackgroundColor(ContextCompat.getColor(this, R.color.green_button_disabled));
        return footerView;
    }
}
