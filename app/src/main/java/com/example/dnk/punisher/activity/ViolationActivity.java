package com.example.dnk.punisher.activity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dnk.punisher.CameraManager;
import com.example.dnk.punisher.Globals;
import com.example.dnk.punisher.DBOpenHelper;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.RequestMaker;
import com.example.dnk.punisher.Violation;
import com.example.dnk.punisher.adapter.EvidenceAdapter;
import com.example.dnk.punisher.adapter.RequisitesListAdapter;
import com.example.dnk.punisher.ViolationRequisite;
import com.example.dnk.punisher.view.ExpandedListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
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
    ProgressBar progressBar;

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

        GridView evidenceGridView = (GridView) findViewById(R.id.evidenceGridView);
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
            rowID = (long) db.update("violations_table", cv, "_id = ?", new String[]{idString});
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
            String address = cursor.getString(cursor.getColumnIndex(violation.getType() + "_address"));
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
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(Globals.SERVER_URL
                        + "/api/v1/complains").openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Authorization", Globals.token);
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                urlConnection.setRequestProperty("Accept-Encoding", "UTF-8");
                OutputStream os = urlConnection.getOutputStream();
                String request = new RequestMaker("complain").makeRequest(
                        "user_id", userId.toString(),
                        "id_number", idString,
                        "type", violation.getType(),
                        "complain_status_id", "6", //null status
                        "longitude", longitude.toString(),
                        "latitude", latitude.toString(),
                        "address", address) + "&complain[media_files][]=file.png";

                os.write(request.getBytes());
                os.flush();
                os.close();

                int responseCode = urlConnection.getResponseCode();
                InputStream is;
                if ((is = urlConnection.getInputStream()) == null) is = urlConnection.getErrorStream();
                BufferedReader reader = new BufferedReader((new InputStreamReader(is)));
                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    response.append(inputLine);
                }
                reader.close();
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
