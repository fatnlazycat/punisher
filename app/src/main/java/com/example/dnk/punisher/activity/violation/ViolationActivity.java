package com.example.dnk.punisher.activity.violation;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.example.dnk.punisher.CameraManager;
import com.example.dnk.punisher.Constants;
import com.example.dnk.punisher.DBOpenHelper;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.Violation;
import com.example.dnk.punisher.adapter.EvidenceAdapter;
import com.example.dnk.punisher.adapter.RequisitesListAdapter;
import com.example.dnk.punisher.ViolationRequisite;
import com.example.dnk.punisher.view.ExpandedListView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ViolationActivity extends AppCompatActivity {

    ListView listViewRequisites;
    RequisitesListAdapter requisitesAdapter = new RequisitesListAdapter(this);
    EvidenceAdapter evidenceAdapter = new EvidenceAdapter();
    DBOpenHelper dbOpenHelper;
    Cursor cursor;
    boolean createMode;
    Integer id;
    String idString;
    Violation violation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        setContentView(R.layout.activity_violation);

        violation = (Violation)intent.getExtras().getSerializable(Constants.VIOLATION);
        requisitesAdapter.content = requisitesAdapter.makeContent(violation.type);

        int disclaimerId = getResources().getIdentifier(violation.type + "_disclaimer", "string", getPackageName());
        TextView textViewViolationDisclaimer = (TextView)findViewById(R.id.textViewViolationDisclaimer);
        textViewViolationDisclaimer.setText(disclaimerId);

        listViewRequisites = (ExpandedListView)findViewById(R.id.listViewRequisites);
        listViewRequisites.setAdapter(requisitesAdapter);

        dbOpenHelper = new DBOpenHelper(this, "violations_db", 1);

        createMode = intent.getBooleanExtra(Constants.CREATE_MODE, false);
        if (createMode) {
            if (violation.usesCamera) {//create mode means we have to capture video at start
                CameraManager cameraManager = CameraManager.getInstance(this);
                cameraManager.startCamera(CameraManager.VIDEO_CAPTURE_INTENT);
            }
        } else {//edit mode means we have to fill requisites & evidenceGridView
            id = intent.getIntExtra(Constants.ITEM_ID, 0);
            SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
            //query to data table
            String table = "violations_table";
            String[] columns = null;
            String where = "_id=?";
            idString = id.toString();
            String[] selectionArgs = {idString};
            cursor = db.query(table, columns, where, selectionArgs, null, null, null);
            cursor.moveToFirst();
            for (int i=0; i < requisitesAdapter.getCount(); i++){
                requisitesAdapter.content.get(i).value =
                        cursor.getString(cursor.getColumnIndex(requisitesAdapter.content.get(i).dbTag));
            }

            //query to media table
            table = "violations_table INNER JOIN media_table ON violations_table._id = media_table.id";
            columns = new String[]{"id", "file_name"};
            where = "id=?"; //take selectionArgs from previous query
            cursor = db.query(table, columns, where, selectionArgs, null, null, null);
            if (cursor.moveToFirst()){
                do {
                    String evidenceFileName = cursor.getString(1); //file names are in the second column of the cursor
                    evidenceAdapter.content.add(evidenceFileName);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        GridView evidenceGridView = (GridView)findViewById(R.id.evidenceGridView);
        evidenceGridView.setAdapter(evidenceAdapter);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            evidenceAdapter.content.add(CameraManager.lastCapturedFile);
            evidenceAdapter.notifyDataSetChanged();
        }
    }

    public void saveToBase(View view) {
        long rowID;

        //db actions
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("type", violation.type);

        for (int i=0; i < requisitesAdapter.getCount(); i++) {
            ViolationRequisite thisRequisite = (ViolationRequisite) requisitesAdapter.getItem(i);
            cv.put(thisRequisite.dbTag, ((EditText)((LinearLayout)listViewRequisites.getChildAt(i))
                    .getChildAt(2)).getText().toString());
        }
        if (createMode) {
            cv.put("time_stamp",new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
            rowID = db.insertOrThrow("violations_table", null, cv);
            createMode = false; //once got the time stamp & created the record we switch to edit mode
        } else{
            rowID = db.update("violations_table", cv, "_id = ?", new String[]{idString});
        }
        cv.clear();

        /*actions with the media table: if in edit mode we delete all previously written records
         *& fill the table again, because the user could add or delete something
         */
        if (!createMode) db.delete("media_table", "id = ?", new String[]{idString});
        for (int i = 0; i < evidenceAdapter.content.size(); i++){
            cv.put("id", rowID);
            cv.put("file_name", evidenceAdapter.content.get(i));
            db.insert("media_table", null, cv);
            cv.clear();
        }
    }

    public void photoVideoPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
             @Override
             public boolean onMenuItemClick(MenuItem item) {
                 return onPhotoVideoMenuItemClick(item);
             }
        });

        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.photo_video_popup_menu, popup.getMenu());
        popup.show();
    }

    public boolean onPhotoVideoMenuItemClick(MenuItem item) {
        int actionFlag = 0;
        switch (item.getItemId()) {
            case R.id.itemTakePhoto:
                actionFlag = CameraManager.IMAGE_CAPTURE_INTENT;
                break;
            case R.id.itemTakeVideo:
                actionFlag = CameraManager.VIDEO_CAPTURE_INTENT;
                break;
        }
        CameraManager.getInstance(this).startCamera(actionFlag);
        return true;
    }


    /**** Method for Setting the Height of the ListView dynamically.
     **** Hack to fix the issue of not showing all the items of the ListView
     **** when placed inside a ScrollView  ****/
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            int h = view.getMeasuredHeight();
            totalHeight += h;
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }
}
