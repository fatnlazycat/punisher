package org.foundation101.karatel.activity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.MultipartUtility;
import org.foundation101.karatel.R;
import org.foundation101.karatel.Request;
import org.foundation101.karatel.Violation;
import org.foundation101.karatel.adapter.RequisitesListAdapter;
import org.foundation101.karatel.view.ExpandedGridView;
import org.foundation101.karatel.view.ExpandedListView;
import org.foundation101.karatel.ViolationRequisite;
import org.foundation101.karatel.adapter.EvidenceAdapter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViolationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    final int RQS_GooglePlayServices = 1000;

    public static final int MODE_CREATE = -1;
    public static final int MODE_EDIT = 0;

    ListView listViewRequisites;
    RequisitesListAdapter requisitesAdapter;
    EvidenceAdapter evidenceAdapter = new EvidenceAdapter(this);
    DBHelper dbHelper;
    Cursor cursor;
    int createMode;
    Integer id, status, idOnServer;
    String idString, time_stamp;
    String id_number_server = "";
    Violation violation = new Violation();
    FrameLayout progressBar;
    TabHost tabs;
    Button punishButton, saveButton;
    public Double latitude, longitude;

    public GoogleApiClient googleApiClient;
    Location l;

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
        validatePunishButton();
        validateSaveButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGooglePlayServices();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        setContentView(R.layout.activity_violation);

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);
        punishButton = (Button) findViewById(R.id.punishButton);
        saveButton = (Button) findViewById(R.id.saveButton);

        if (checkGooglePlayServices()) buildGoogleApiClient();

        //initializing tabs
        tabs=(TabHost)findViewById(android.R.id.tabhost);
        tabs.setup();
        setupTab(R.id.tabInfo, getString(R.string.information));
        tabs.setCurrentTab(0);
        TabWidget tabWidget = (TabWidget)findViewById(android.R.id.tabs);

        /*tabs.setOnTabChangedListener(new TabHost.OnTabChangeListener(){
            @Override
            public void onTabChanged(String tabId) {
                switch (tabs.getCurrentTab()){
                    case 0: {

                        break;
                    }
                    case 1: {
                        break;
                    }
                }
            }
        });*/
        dbHelper = new DBHelper(this, DBHelper.DATABASE, 1);
        requisitesAdapter = new RequisitesListAdapter(this);

        createMode = intent.getIntExtra(Globals.VIOLATION_ACTIVITY_MODE, MODE_EDIT);
        if (createMode == MODE_CREATE) {

            idOnServer = 0;
            violation = (Violation) intent.getExtras().getSerializable(Globals.VIOLATION);
            status = 0; //status = draft
            requisitesAdapter.content = requisitesAdapter.makeContent(violation.type);
            if (violation.usesCamera) {//create mode means we have to capture video at start
                CameraManager cameraManager = CameraManager.getInstance(this);
                cameraManager.startCamera(CameraManager.VIDEO_CAPTURE_INTENT);
            }
        } else {//edit or view mode means we have to fill requisites & evidenceGridView
            id = intent.getIntExtra(Globals.ITEM_ID, 0);
            idString = id.toString();
            if (createMode == MODE_EDIT) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                //query to data table
                String table = "violations_table";
                String[] columns = null;
                String where = "_id=?";
                String[] selectionArgs = {idString};
                cursor = db.query(table, columns, where, selectionArgs, null, null, null);
                cursor.moveToFirst();
                idOnServer = cursor.getInt(cursor.getColumnIndex(DBHelper.ID_SERVER));
                id_number_server = cursor.getString(cursor.getColumnIndex(DBHelper.ID_NUMBER_SERVER));
                latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                time_stamp = cursor.getString(cursor.getColumnIndex(DBHelper.TIME_STAMP));
                status = cursor.getInt(cursor.getColumnIndex("status"));
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
            } else {//if neither create nor edit mode then it's loaded from server
                Request request = (Request)intent.getExtras().getSerializable(Globals.REQUEST_JSON);

                idOnServer = request.id;
                id_number_server = request.id_number;
                latitude = request.latitude;
                longitude = request.longitude;
                time_stamp = request.created_at;
                status = request.complain_status_id;
                violation.setType(request.type);
                requisitesAdapter.content = requisitesAdapter.makeContent(violation.type);
                for (int i = 0; i < requisitesAdapter.getCount(); i++) {
                    try {
                        String fieldName = requisitesAdapter.content.get(i).dbTag.replace(violation.getType() + "_", "");
                        requisitesAdapter.content.get(i).value = (String)Request.class.getField(fieldName).get(request);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        int disclaimerId = getResources().getIdentifier(violation.type + "_disclaimer", "string", getPackageName());
        TextView textViewViolationDisclaimer = (TextView) findViewById(R.id.textViewViolationDisclaimer);
        if (getResources().getString(disclaimerId).isEmpty()) textViewViolationDisclaimer.setVisibility(View.GONE);
            else textViewViolationDisclaimer.setText(disclaimerId);

        listViewRequisites = (ExpandedListView) findViewById(R.id.listViewRequisites);
        listViewRequisites.setAdapter(requisitesAdapter);
        listViewRequisites.setFocusable(false);

        ExpandedGridView evidenceGridView = (ExpandedGridView) findViewById(R.id.evidenceGridView);
        evidenceGridView.setAdapter(evidenceAdapter);
        evidenceGridView.setFocusable(false);

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        //actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        actionBar.setTitle(Violation.getViolationNameFromType(this, violation.getType()));

        //check if the request is already filed - > no edit anymore
        if (createMode > MODE_EDIT){
            punishButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.GONE);
            requisitesAdapter.setEditTrigger(false);
            evidenceAdapter.setEditTrigger(false);
            toolbar.setSubtitle("Заявка № " + id_number_server);

            setupTab(R.id.tabStatus, getString(R.string.request_status));
            tabWidget.setVisibility(View.VISIBLE);

            ImageButton imageButtonAddEvidence = (ImageButton)findViewById(R.id.imageButtonAddEvidence);
            imageButtonAddEvidence.setVisibility(View.GONE);
            TextView addedPhotoVideoTextView = (TextView)findViewById(R.id.addedPhotoVideoTextView);
            addedPhotoVideoTextView.setText(getString(R.string.addedPhotoVideo));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            evidenceAdapter.content.add(CameraManager.lastCapturedFile);
            evidenceAdapter.notifyDataSetChanged();
        }
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sort_items_menu, menu);
        return true;
    }*/

    //called from MainActivity.onCreate to set the tabs
    private void setupTab(final int contentViewId, String tag){
        /*LayoutInflater inflater= LayoutInflater.from(tabs.getContext());
        View customTab=inflater.inflate(R.layout.layout_custom_tabs, null, false);
        TextView textView=(TextView)customTab.findViewById(R.id.oneOfCustomTabs);
        textView.setText(tag);*/

        TabHost.TabSpec tabBuilder=tabs.newTabSpec(tag);
        //tabBuilder.setIndicator(customTab);
        tabBuilder.setIndicator(tag);
        tabBuilder.setContent(new TabHost.TabContentFactory(){
            public View createTabContent(String tag) {return findViewById(contentViewId);}
        });
        tabs.addTab(tabBuilder);
    }

    public void saveToBase(View view) {
        Long rowID;

        //db actions
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("type", violation.type);
        cv.put("status", status);
        cv.put(DBHelper.ID_SERVER, idOnServer);
        cv.put(DBHelper.ID_NUMBER_SERVER, id_number_server);
        cv.put(DBHelper.USER_ID, Globals.user.id);

        for (int i = 0; i < requisitesAdapter.getCount(); i++) {
            ViolationRequisite thisRequisite = (ViolationRequisite) requisitesAdapter.getItem(i);
            cv.put(thisRequisite.dbTag, ((EditText) ((LinearLayout) listViewRequisites.getChildAt(i))
                    .getChildAt(2)).getText().toString());
        }
        if (createMode == MODE_CREATE) {
            cv.put(DBHelper.LONGITUDE, longitude);
            cv.put(DBHelper.LATITUDE, latitude);
            cv.put(DBHelper.TIME_STAMP, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));
            rowID = db.insertOrThrow("violations_table", null, cv);
            id = new BigDecimal(rowID).intValue();
            createMode = MODE_EDIT; //once created the record we switch to edit mode
        } else {
            cv.put("_id", id);
            cv.put(DBHelper.LONGITUDE, longitude);
            cv.put(DBHelper.LATITUDE, latitude);
            cv.put(DBHelper.TIME_STAMP, time_stamp);
            rowID = db.replace("violations_table", null, cv);
        }
        cv.clear();

        /*actions with the media table: if in edit mode - we delete all previously written records
         *& fill the table again, because the user could add or delete something
         */
        if (createMode != MODE_CREATE) db.delete("media_table", "id = ?", new String[]{idString});
            else createMode = MODE_EDIT; //once created the record we switch to edit mode
        for (int i = 0; i < evidenceAdapter.content.size(); i++) {
            cv.put("id", rowID);
            cv.put("file_name", evidenceAdapter.content.get(i));
            db.insert("media_table", null, cv);
            cv.clear();
        }
        db.close();

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

    public void punish(View view) {
        saveToBase(null);
        new ViolationSender(this).execute(violation);
    }

    public void empty(View view) {
        //empty method to handle click events
    }

    public void validateSaveButton(){
        saveButton.setEnabled(!evidenceAdapter.isEmpty());
    }

    public void validatePunishButton(){
        punishButton.setEnabled(allDataEntered());
    }

    public boolean allDataEntered(){
        boolean result = !evidenceAdapter.isEmpty();

        /*if the inflation of the view is not conducted yet -> we have just opened the activity * do not need to
         * check EditText values, but check adapter's content values instead
         */
        if (listViewRequisites.getChildAt(0) == null) {
            for (ViolationRequisite requisite : requisitesAdapter.content) {
                if (!result) return false; //to speed up the calculation - if anyone of the fields is empty no need to check others
                if (requisite.value != null) result = !requisite.value.isEmpty();
                else return false;
            }
        } else {//inflation is conducted, check the values in EditTexts
            for (int i = 0; i < requisitesAdapter.getCount(); i++) {
                if (!result) return false; //to speed up the calculation - if anyone of the fields is empty no need to check others
                EditText et = ((EditText) ((LinearLayout) listViewRequisites.getChildAt(i)).getChildAt(2));
                String text = et.getText().toString();
                result = !text.isEmpty();
            }
        }
        return result;
    }

    /**
     * @return the last known best location
     */
    private Location getLastLocation() {
        Location locationNet = null;
        //permission check required by the system
        /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }*/
        locationNet = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (locationNet != null) {
        } else {
            Toast.makeText(this, R.string.location_data_not_available, Toast.LENGTH_LONG).show();
        }
        return locationNet;
    }

    private boolean checkGooglePlayServices(){
        GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
        int resultCode = gaa.isGooglePlayServicesAvailable(getApplicationContext());
        if (resultCode == ConnectionResult.SUCCESS){
            Toast.makeText(getApplicationContext(), "isGooglePlayServicesAvailable SUCCESS", Toast.LENGTH_LONG).show();
            return true;
        }else{
            if (gaa.isUserResolvableError(resultCode)) {
                gaa.getErrorDialog(this, resultCode, RQS_GooglePlayServices).show();
            } else {
                Toast.makeText(getApplicationContext(), "This device is not supported.", Toast.LENGTH_LONG).show();
            }
            return false;
        }
    }

    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (createMode == MODE_CREATE) {
            l = getLastLocation();
            latitude = l.getLatitude();
            longitude = l.getLongitude();
            requisitesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e("Punisher", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override //Activity method
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
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
            StringBuffer response = new StringBuffer();

            SQLiteDatabase db = dbHelper.getReadableDatabase();
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
                            requestParameters.add(Globals.user.id.toString()); break;
                        }
                        case "id_number" : {
                            requestParameters.add(idString); break;
                        }
                        case "type" : {
                            requestParameters.add(violation.getType()); break;
                        }
                        case "complain_status_id" : {
                            requestParameters.add("1"); break;
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
            try {
                JSONObject json = new JSONObject(s).getJSONObject("data");
                idOnServer = json.getInt("id");
                id_number_server = json.getString("id_number");
                ViolationActivity.this.saveToBase(null);
            } catch (JSONException e){
                Toast.makeText(context, e.getMessage()  , Toast.LENGTH_LONG).show();
            }
        }
    }
}