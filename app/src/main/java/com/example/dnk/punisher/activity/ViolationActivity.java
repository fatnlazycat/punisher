package com.example.dnk.punisher.activity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dnk.punisher.CameraManager;
import com.example.dnk.punisher.DBOpenHelper;
import com.example.dnk.punisher.Globals;
import com.example.dnk.punisher.MultipartUtility;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.Violation;
import com.example.dnk.punisher.ViolationRequisite;
import com.example.dnk.punisher.adapter.EvidenceAdapter;
import com.example.dnk.punisher.adapter.RequisitesListAdapter;
import com.example.dnk.punisher.view.ExpandedGridView;
import com.example.dnk.punisher.view.ExpandedListView;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViolationActivity extends AppCompatActivity {

    ListView listViewRequisites;
    RequisitesListAdapter requisitesAdapter = new RequisitesListAdapter(this);
    EvidenceAdapter evidenceAdapter = new EvidenceAdapter();
    DBOpenHelper dbOpenHelper;
    Cursor cursor;
    boolean createMode;
    Integer id;
    String idString, time_stamp;
    Violation violation = new Violation();
    ProgressBar progressBar;
    Double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        setContentView(R.layout.activity_violation);

        dbOpenHelper = new DBOpenHelper(this, "violations_db", 1);

        createMode = intent.getBooleanExtra(Globals.CREATE_MODE, false);
        if (createMode) {
            violation = (Violation) intent.getExtras().getSerializable(Globals.VIOLATION);
            requisitesAdapter.content = requisitesAdapter.makeContent(violation.type);
            if (violation.usesCamera) {//create mode means we have to capture video at start
                CameraManager cameraManager = CameraManager.getInstance(this);
                cameraManager.startCamera(CameraManager.VIDEO_CAPTURE_INTENT);
            }
        } else {//edit mode means we have to fill requisites & evidenceGridView
            id = intent.getIntExtra(Globals.ITEM_ID, 0);
            SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
            //query to data table
            String table = "violations_table";
            String[] columns = null;
            String where = "_id=?";
            idString = id.toString();
            String[] selectionArgs = {idString};
            cursor = db.query(table, columns, where, selectionArgs, null, null, null);
            cursor.moveToFirst();
            latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
            longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
            time_stamp = cursor.getString(cursor.getColumnIndex("time_stamp"));
            violation.setType(cursor.getString(cursor.getColumnIndex("type")));
            requisitesAdapter.content = requisitesAdapter.makeContent(violation.type);
            for (int i = 0; i < requisitesAdapter.getCount(); i++) {
                requisitesAdapter.content.get(i).value =
                        cursor.getString(cursor.getColumnIndex(requisitesAdapter.content.get(i).dbTag));
            }

            //query to media table
            table = "violations_table INNER JOIN media_table ON violations_table._id = media_table.id";
            columns = new String[]{"id", "file_name"};
            where = "id=?"; //take selectionArgs from previous query
            cursor = db.query(table, columns, where, selectionArgs, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String evidenceFileName = cursor.getString(1); //file names are in the second column of the cursor
                    evidenceAdapter.content.add(evidenceFileName);
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.close();
        }

        int disclaimerId = getResources().getIdentifier(violation.type + "_disclaimer", "string", getPackageName());
        TextView textViewViolationDisclaimer = (TextView) findViewById(R.id.textViewViolationDisclaimer);
        textViewViolationDisclaimer.setText(disclaimerId);

        listViewRequisites = (ExpandedListView) findViewById(R.id.listViewRequisites);
        listViewRequisites.setAdapter(requisitesAdapter);

        ExpandedGridView evidenceGridView = (ExpandedGridView) findViewById(R.id.evidenceGridView);
        evidenceGridView.setAdapter(evidenceAdapter);

        progressBar = (ProgressBar)findViewById(R.id.progressBarPunish);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            evidenceAdapter.content.add(CameraManager.lastCapturedFile);
            evidenceAdapter.notifyDataSetChanged();
        }
    }

    public void saveToBase(View view) {
        Long rowID;

        //db actions
        SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("type", violation.type);

        for (int i = 0; i < requisitesAdapter.getCount(); i++) {
            ViolationRequisite thisRequisite = (ViolationRequisite) requisitesAdapter.getItem(i);
            cv.put(thisRequisite.dbTag, ((EditText) ((LinearLayout) listViewRequisites.getChildAt(i))
                    .getChildAt(2)).getText().toString());
        }
        if (createMode) {
            Location l = getLastLocation();
            cv.put("longitude", l==null ? 0 : l.getLongitude());
            cv.put("latitude", l==null ? 0 : l.getLatitude());
            cv.put("time_stamp", new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
            rowID = db.insertOrThrow("violations_table", null, cv);
            id = new BigDecimal(rowID).intValue();
        } else {
            cv.put("_id", id);
            cv.put("longitude", longitude);
            cv.put("latitude", latitude);
            cv.put("time_stamp", time_stamp);
            rowID = db.replace("violations_table", null, cv);
        }
        cv.clear();

        /*actions with the media table: if in edit mode we delete all previously written records
         *& fill the table again, because the user could add or delete something
         */
        if (!createMode) db.delete("media_table", "id = ?", new String[]{idString});
        for (int i = 0; i < evidenceAdapter.content.size(); i++) {
            cv.put("id", rowID);
            cv.put("file_name", evidenceAdapter.content.get(i));
            db.insert("media_table", null, cv);
            cv.clear();
        }
        db.close();
        createMode = false; //once created the record we switch to edit mode

        //delete the evidence files removed by the user from filesystem
        for (String fileToDelete : evidenceAdapter.filesDeletedDuringSession){
            new File(fileToDelete).delete();
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
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            int h = view.getMeasuredHeight();
            totalHeight += h;
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    public void punish(View view) {
        saveToBase(view);
        new ViolationSender(this).execute(violation);
    }

    /**
     * @return the last known best location
     */
    private Location getLastLocation() {
        //working with geodata
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location locationNet = null;
        //permission check required by the system
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        return locationNet;

    }

    class ViolationSender extends AsyncTask<Violation, Void, String> {

        Context context;

        public ViolationSender(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Violation... params) {
            Violation violation = params[0];
            Integer userId = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getInt(Globals.USER_ID, 0);
            StringBuffer response = new StringBuffer();

            SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
            //query to data table
            String table = "violations_table";
            String[] columns = null;
            String where = "_id=?";
            idString = id.toString();
            String[] selectionArgs = {idString};
            cursor = db.query(table, columns, where, selectionArgs, null, null, null);
            cursor.moveToFirst();
            Double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
            Double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
            Map<String, String> dbRowData= new HashMap<>();
            for (int i = 0; i < requisitesAdapter.getCount(); i++) {
                String columnName = requisitesAdapter.content.get(i).dbTag;
                String columnValue = cursor.getString(cursor.getColumnIndex(columnName));
                dbRowData.put(columnName, columnValue==null ? "" : columnValue);
            }
            cursor.close();
            db.close();
            try {
                //get resources
                Resources res = context.getResources();
                int resId = res.getIdentifier(violation.getType() + "_server", "string", context.getPackageName());
                String typeServerSuffix = res.getString(resId);
                //also need to remove trailing 's' from type server suffix
                String typeServerSuffixNoS = typeServerSuffix.substring(0, typeServerSuffix.length()-1);

                String requestUrl = Globals.SERVER_URL + typeServerSuffix;

                //prepare the request parameters
                ArrayList<String> requestParameters = new ArrayList<>();
                String[] keysForRequestParameters = new String[0];
                switch (violation.getType()){
                    case "WrongParking" : {
                        keysForRequestParameters = new String[] {"user_id", "id_number", "complain_status_id",
                                "address", "longitude", "latitude", "vehicle_number"}; break;
                    }
                    case "PitRoad" : {
                        keysForRequestParameters = new String[] {"user_id", "id_number", "complain_status_id",
                                "address", "longitude", "latitude", "the_closest_landmark", "type"}; break;
                    }
                    case "NoSmoking" : {
                        keysForRequestParameters = new String[] {"user_id", "id_number", "complain_status_id",
                                "address", "longitude", "latitude", "name_of_institution", "name_of_entity", "type"}; break;
                    }
                    case "Damage" : {
                        keysForRequestParameters = new String[] {"user_id", "id_number", "complain_status_id",
                                "address", "longitude", "latitude", "the_closest_landmark", "type"}; break;
                    }
                    case "SaleOfGood" : {
                        keysForRequestParameters = new String[] {"user_id", "id_number", "complain_status_id",
                                "address", "longitude", "latitude", "name_of_authority", "name_of_institution",
                                "name_of_entity", "type"}; break;
                    }
                    case "Insult" : {
                        keysForRequestParameters = new String[] {"user_id", "id_number", "complain_status_id",
                                "address", "longitude", "latitude", "description", "all_name", "position",
                                "name_of_authority", "name_of_institution", "name_of_entity", "type"}; break;
                    }
                    case "Bribe" : {
                        keysForRequestParameters = new String[] {"user_id", "id_number", "complain_status_id",
                                "address", "longitude", "latitude", "description", "all_name", "position",
                                "name_of_authority", "type"}; break;
                    }
                }
                for (String str : keysForRequestParameters){
                    requestParameters.add(str);
                    switch (str) {
                        case "user_id" : {
                            requestParameters.add(userId.toString()); break;
                        }
                        case "id_number" : {
                            requestParameters.add(idString); break;
                        }
                        case "type" : {
                            requestParameters.add(violation.getType()); break;
                        }
                        case "complain_status_id" : {
                            requestParameters.add("6"); break;
                        }
                        case "longitude" : {
                            requestParameters.add(longitude.toString()); break;
                        }
                        case "latitude" : {
                            requestParameters.add(latitude.toString()); break;
                        }
                        default : {
                            String adaptedKey = violation.getType() + "_" + str;
                            requestParameters.add(dbRowData.get(adaptedKey));
                        }
                    }
                }

                MultipartUtility multipart = new MultipartUtility(requestUrl, "UTF-8");
                int i=0;
                String[] requestParametersArray = requestParameters.toArray(new String[0]);
                while (i < requestParametersArray.length) {
                    multipart.addFormField(typeServerSuffixNoS +"[" + requestParametersArray[i++] + "]",
                            requestParametersArray[i++]);
                }
                i=0;
                for (String mediaFileName : evidenceAdapter.content) {
                    multipart.addFilePart(typeServerSuffixNoS + "[media_files][]", new File(mediaFileName));
                }
                List<String> responseList = multipart.finish();
                Log.e("Punisher", "SERVER REPLIED:");
                for (String line : responseList) {
                    Log.e("Punisher", "Upload Files Response:::" + line);
                    response.append(line);
                    // get your server response here.
                }

            } catch (IOException e) {
                Log.e("Punisher error", e.getMessage());
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(context, s, Toast.LENGTH_LONG).show();

        }
    }
}
