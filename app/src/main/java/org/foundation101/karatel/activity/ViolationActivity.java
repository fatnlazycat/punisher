package org.foundation101.karatel.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import org.codehaus.jackson.map.ObjectMapper;
import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.MultipartUtility;
import org.foundation101.karatel.R;
import org.foundation101.karatel.Request;
import org.foundation101.karatel.UpdateEntity;
import org.foundation101.karatel.Violation;
import org.foundation101.karatel.ViolationRequisite;
import org.foundation101.karatel.adapter.EvidenceAdapter;
import org.foundation101.karatel.adapter.HistoryAdapter;
import org.foundation101.karatel.adapter.RequestListAdapter;
import org.foundation101.karatel.adapter.RequisitesListAdapter;
import org.foundation101.karatel.fragment.RequestListFragment;
import org.foundation101.karatel.view.ExpandedGridView;
import org.foundation101.karatel.view.ExpandedListView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    public static final String STATUS_REFUSED = "Відмовлено в розгляді";

    ListView listViewRequisites;
    RequisitesListAdapter requisitesAdapter;
    EvidenceAdapter evidenceAdapter = new EvidenceAdapter(this);
    HistoryAdapter historyAdapter;
    ListView historyListView;
    DBHelper dbHelper;
    Cursor cursor;
    int mode, thumbDimension;
    Long rowID;

    public int getMode() {
        return mode;
    }

    public Request request = null;
    Integer id, status, idOnServer;
    String idInDbString, time_stamp;
    String id_number_server = "";
    Violation violation = new Violation();
    public Double latitude, longitude;
    boolean statusTabFirstShow = true;

    FrameLayout progressBar;
    TabHost tabs;
    Button punishButton, saveButton;


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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_violation);

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);
        punishButton = (Button) findViewById(R.id.punishButton);
        saveButton = (Button) findViewById(R.id.saveButton);

        if (checkGooglePlayServices()) buildGoogleApiClient();

        //dimension of the thumbnail - the thumbnail should have the same size as MediaStore.Video.Thumbnails.MICRO_KIND
        thumbDimension = getResources().getDimensionPixelOffset(R.dimen.thumbnail_size);

        //initializing tab view with only one tab - the second will be initialized later
        tabs=(TabHost)findViewById(android.R.id.tabhost);
        tabs.setup();
        setupTab(R.id.tabInfo, getString(R.string.information));
        tabs.setCurrentTab(0);
        tabs.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (statusTabFirstShow) {
                    historyListView = (ListView)findViewById(R.id.historyListView_TabStatus);
                    FrameLayout.LayoutParams mParam = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT);
                    historyListView.setLayoutParams(mParam);
                    historyListView.setAdapter(historyAdapter);
                    historyAdapter.notifyDataSetChanged();
                    statusTabFirstShow = false;
                }
            }
        });

        dbHelper = new DBHelper(this, DBHelper.DATABASE, 1);
        requisitesAdapter = new RequisitesListAdapter(this);

        Intent intent = this.getIntent();
        mode = intent.getIntExtra(Globals.VIOLATION_ACTIVITY_MODE, MODE_EDIT);
        if (mode == MODE_CREATE) {
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
            idInDbString = id.toString();
            if (mode == MODE_EDIT) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                //query to data table
                String table = "violations_table";
                String[] columns = null;
                String where = "_id=?";
                String[] selectionArgs = {idInDbString};
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
                cursor.close();
                db.close();
            } else {//if neither create nor edit mode then it's loaded from server
                request = (Request)intent.getExtras().getSerializable(Globals.REQUEST_JSON);

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
                        Globals.showError(this, R.string.error, e);
                    }
                }

                historyAdapter = new HistoryAdapter(this);
                new RequestFetcher().execute(idOnServer);
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
        makeEvidenceAdapterContent(mode, request);
        evidenceGridView.setAdapter(evidenceAdapter);
        evidenceGridView.setFocusable(false);

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        actionBar.setTitle(Violation.getViolationNameFromType(this, violation.getType()));

        if (mode > MODE_EDIT){
            //first check if the status is "refused" which means we can edit the data & re-send the request
            if (mode == calculateRefusedStatus()){
                saveButton.setVisibility(View.GONE);
            } else { //the request is already filed & not refused - > no edit anymore
                blockButtons(true);
                requisitesAdapter.setEditTrigger(false);
            }

            evidenceAdapter.setEditTrigger(false);
            ImageButton imageButtonAddEvidence = (ImageButton)findViewById(R.id.imageButtonAddEvidence);
            imageButtonAddEvidence.setVisibility(View.GONE);
            TextView addedPhotoVideoTextView = (TextView)findViewById(R.id.addedPhotoVideoTextView);
            addedPhotoVideoTextView.setText(getString(R.string.addedPhotoVideo));

            toolbar.setSubtitle("Заявка № " + id_number_server);

            setupTab(R.id.historyListView_TabStatus, getString(R.string.request_status));
            TabWidget tabWidget = (TabWidget)findViewById(android.R.id.tabs);
            tabWidget.setVisibility(View.VISIBLE);
            tabs.setCurrentTab(0);

            findViewById(R.id.textViewViolationHeader).setVisibility(View.GONE);
        }
    }

    int calculateRefusedStatus(){
        int result = -100;
        String[] violationStatuses = getResources().getStringArray(R.array.violationStatuses);
        int refusedStatusIndex = Arrays.asList(violationStatuses).indexOf(STATUS_REFUSED);
        if (Globals.statusesMap != null){
            for (int key : Globals.statusesMap.keySet()){
                if (Globals.statusesMap.get(key) == refusedStatusIndex)
                    result = key;
            }
        }
        return result;
    }

    void makeEvidenceAdapterContent(int mode, Request request){
        if (mode == MODE_EDIT) {
            SQLiteDatabase _db = dbHelper.getReadableDatabase();
            //query to media table
            String table = DBHelper.VIOLATIONS_TABLE + " INNER JOIN " + DBHelper.MEDIA_TABLE + " ON "
                    + DBHelper.VIOLATIONS_TABLE + "._id = " + DBHelper.MEDIA_TABLE + ".id";
            String[] columns = new String[]{DBHelper.ID, DBHelper.FILE_NAME};
            String where = "id=?";
            String[] selectionArgs = {idInDbString};
            Cursor _cursor = _db.query(table, columns, where, selectionArgs, null, null, null);
            if (_cursor.moveToFirst()) {
                do try {
                    String evidenceFileName = _cursor.getString(_cursor.getColumnIndex(DBHelper.FILE_NAME));
                    Bitmap thumbnail;
                    if (evidenceFileName.endsWith(CameraManager.JPG)) {
                        thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(evidenceFileName),
                                thumbDimension, thumbDimension);
                    } else { //it's video
                        thumbnail = ThumbnailUtils.createVideoThumbnail(evidenceFileName, MediaStore.Video.Thumbnails.MICRO_KIND);
                    }
                    evidenceAdapter.content.add(evidenceFileName);
                    evidenceAdapter.mediaContent.add(thumbnail);
                } catch (Exception e){//we read files so need to catch exceptions
                    Globals.showError(this, R.string.error, e);
                } while (_cursor.moveToNext());
            }
            _cursor.close();
            _db.close();
        } else if (mode > MODE_EDIT){
            String serverUrl = Globals.SERVER_URL.replace("/api/v1/", "");
            for (int i = 0; i < request.images.length; i++) {
                String evidenceFileName = serverUrl + request.images[i].thumb.url;
                new ThumbnailFetcher().execute(evidenceFileName);
                //when done with the thumbnail we get the full image name
                evidenceFileName = serverUrl + request.images[i].url;
                evidenceAdapter.content.add(evidenceFileName);

            }
            for (int i = 0; i < request.videos.length; i++) {
                String evidenceFileName = serverUrl + request.videos[i].thumb.url;
                new ThumbnailFetcher().execute(evidenceFileName);
                //when done with the thumbnail we get the full image name
                evidenceFileName = serverUrl + request.videos[i].url;
                evidenceAdapter.content.add(evidenceFileName);

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK &&
                (requestCode == CameraManager.IMAGE_CAPTURE_INTENT || requestCode == CameraManager.VIDEO_CAPTURE_INTENT)) {
            try {
                evidenceAdapter.content.add(CameraManager.lastCapturedFile);
                Bitmap bmp;
                if (CameraManager.lastCapturedFile.endsWith(CameraManager.JPG)) {
                    bmp = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(CameraManager.lastCapturedFile),
                            thumbDimension, thumbDimension);
                } else {
                    bmp = ThumbnailUtils.createVideoThumbnail(CameraManager.lastCapturedFile, MediaStore.Video.Thumbnails.MICRO_KIND);
                }
                evidenceAdapter.mediaContent.add(bmp);
                evidenceAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Globals.showError(this, R.string.error, e);
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
        // Fighting with java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
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

    //called from onCreate to set the tabs
    private void setupTab(final int contentViewId, String tag){
        TabHost.TabSpec tabBuilder=tabs.newTabSpec(tag);
        tabBuilder.setIndicator(tag);
        tabBuilder.setContent(new TabHost.TabContentFactory(){
            public View createTabContent(String tag) {return findViewById(contentViewId);}
        });
        tabs.addTab(tabBuilder);
    }

    public void saveToBase(View view) throws Exception {
        //first check if location is available - show error dialog if not
        if (checkLocation()) {

            //db actions
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();

            cv.put("type", violation.type);
            cv.put("status", status);
            cv.put(DBHelper.ID_SERVER, idOnServer);
            cv.put(DBHelper.ID_NUMBER_SERVER, id_number_server);
            cv.put(DBHelper.USER_ID, Globals.user.id);
            cv.put(DBHelper.LONGITUDE, longitude);
            cv.put(DBHelper.LATITUDE, latitude);

            for (int i = 0; i < requisitesAdapter.getCount(); i++) {
                ViolationRequisite thisRequisite = (ViolationRequisite) requisitesAdapter.getItem(i);
                cv.put(thisRequisite.dbTag, ((EditText) ((LinearLayout) listViewRequisites.getChildAt(i))
                        .getChildAt(2)).getText().toString());
            }
            switch (mode) {
                case MODE_CREATE: {
                    String createdTimeStamp = new SimpleDateFormat(RequestListAdapter.INPUT_DATE_FORMAT, Locale.US)
                            .format(new Date());
                    if (createdTimeStamp == null) {
                        throw new Exception("my exception: Date = null");
                    } //TODO check why this happens
                    cv.put(DBHelper.TIME_STAMP, createdTimeStamp);
                    rowID = db.insertOrThrow(DBHelper.VIOLATIONS_TABLE, null, cv);
                    id = new BigDecimal(rowID).intValue();
                    mode = MODE_EDIT; //once created the record we switch to edit mode
                    break;
                }
                case MODE_EDIT :{
                    cv.put(DBHelper._ID, id);
                    cv.put(DBHelper.TIME_STAMP, time_stamp);
                    rowID = db.replace(DBHelper.VIOLATIONS_TABLE, null, cv);
                    break;
                }
                default : {//re-send mode
                    cv.put(DBHelper.TIME_STAMP, time_stamp);
                    rowID = db.insertOrThrow(DBHelper.VIOLATIONS_TABLE, null, cv);
                }
            }
            cv.clear();

        /*actions with the media table: if in edit mode - we delete all previously written records
         *& fill the table again, because the user could add or delete something
         */
            if (mode == MODE_EDIT) //this condition works only in edit mode - delete all records previously added to db
                db.delete(DBHelper.MEDIA_TABLE, "id = ?", new String[]{idInDbString});
            for (int i = 0; i < evidenceAdapter.content.size(); i++) {
                cv.put(DBHelper.ID, rowID);
                cv.put(DBHelper.FILE_NAME, evidenceAdapter.content.get(i));
                db.insert(DBHelper.MEDIA_TABLE, null, cv);
                cv.clear();
            }
            db.close();

            //delete the evidence files removed by the user from filesystem
            try {
                for (String fileToDelete : evidenceAdapter.filesDeletedDuringSession) {
                    new File(fileToDelete).delete();
                }
            } catch (Exception e) {
                Globals.showError(this, R.string.cannot_write_file, e);
            }
            Toast.makeText(this, R.string.requestSaved, Toast.LENGTH_LONG).show();
        }
    }

    boolean checkLocation(){
        if (latitude == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setTitle(R.string.cannot_define_location).setNegativeButton(R.string.ok, null).create();
            dialog.show();
            return false;
        } else return true;
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
        try {
            RequestListFragment.punishPerformed = true;
            saveToBase(null);
            new ViolationSender(this).execute(violation);
        } catch (Exception e){
            Globals.showError(this,  R.string.error, e);
        }
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

        /*if the inflation of the view is not conducted yet -> we have just opened the activity & do not need to
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

    public void blockButtons(boolean block){
        int visibility = block ? View.GONE : View.VISIBLE;
        punishButton.setVisibility(visibility);
        saveButton.setVisibility((visibility));
    }

    /**
     * @return the last known best location
     */
    private Location getLastLocation() throws NullPointerException {
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
        Location locationGoogle = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        return locationGoogle;
    }

    public Location getOldAndroidLocation() throws NullPointerException {
        //old Android package location code
        LocationManager lm = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        String best = lm.getBestProvider(new Criteria(), true);
        Location locationAndroid = lm.getLastKnownLocation(best);
        return locationAndroid;
    }

    private boolean checkGooglePlayServices(){
        GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
        int resultCode = gaa.isGooglePlayServicesAvailable(getApplicationContext());
        if (resultCode == ConnectionResult.SUCCESS){
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
        if (mode == MODE_CREATE) {
            l = getLastLocation();
            if (l != null) {
                latitude = l.getLatitude();
                longitude = l.getLongitude();
                requisitesAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e("Punisher", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
        if (mode == MODE_CREATE) {
            l = getOldAndroidLocation();
            if (l != null) {
                latitude = l.getLatitude();
                longitude = l.getLongitude();
                requisitesAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override //Activity method
    protected void onStop() {
        if (googleApiClient != null) googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mode == MODE_CREATE){
            boolean deletedSuccessfully = true;
            //delete the evidence files if the request was not saved
            for (String fileToDelete : evidenceAdapter.content) {
                deletedSuccessfully = new File(fileToDelete).delete() && deletedSuccessfully;
            }
            for (String fileToDelete : evidenceAdapter.filesDeletedDuringSession) {
                deletedSuccessfully = new File(fileToDelete).delete() && deletedSuccessfully;
            }
            Log.e("Punisher", "files deleted successfully " + deletedSuccessfully);
        }
        super.onDestroy();
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
            String table = DBHelper.VIOLATIONS_TABLE;
            String[] columns = null;
            String where = "_id=?";
            idInDbString = new BigDecimal(rowID).toString();
            String[] selectionArgs = {idInDbString};
            cursor = db.query(table, columns, where, selectionArgs, null, null, null);
            cursor.moveToFirst();
            Double latitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.LATITUDE));
            Double longitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.LONGITUDE));
            Map<String, String> dbRowData = new HashMap<>();
            for (int i = 0; i < requisitesAdapter.getCount(); i++) {
                String columnName = requisitesAdapter.content.get(i).dbTag;
                String columnValue = cursor.getString(cursor.getColumnIndex(columnName));
                dbRowData.put(columnName, columnValue == null ? "" : columnValue);
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
                String[] keysForRequestParameters = ViolationRequisite.getRequisites(ViolationActivity.this,
                        violation.getType());
                for (String str : keysForRequestParameters){
                    requestParameters.add(str);
                    switch (str) {
                        case "user_id" : {
                            requestParameters.add(Globals.user.id.toString()); break;
                        }
                        case "id_number" : {
                            requestParameters.add(idInDbString); break;
                        }
                        case "type" : {
                            requestParameters.add(violation.getType()); break;
                        }
                        case "complain_status_id" : {
                            requestParameters.add(Integer.valueOf(mode).toString()); break;
                        }
                        case "longitude" : {
                            requestParameters.add(longitude.toString()); break;
                        }
                        case "latitude" : {
                            requestParameters.add(latitude.toString()); break;
                        }
                        case "create_in_the_device" : {
                            requestParameters.add(time_stamp); break;
                        }
                        default : {
                            String adaptedKey = violation.getType() + "_" + str;
                            requestParameters.add(dbRowData.get(adaptedKey));
                        }
                    }
                }
                String[] requestParametersArray = requestParameters.toArray(new String[0]);

                if (mode == calculateRefusedStatus()){
                    String request = new HttpHelper(typeServerSuffixNoS).makeRequestString(requestParametersArray);
                    return HttpHelper.proceedRequest(typeServerSuffix + "/" + id.toString(), "PUT", request, true);
                } else {
                    MultipartUtility multipart = new MultipartUtility(requestUrl, "UTF-8");
                    int i = 0;
                    while (i < requestParametersArray.length) {
                        multipart.addFormField(typeServerSuffixNoS + "[" + requestParametersArray[i++] + "]",
                                requestParametersArray[i++]);
                    }
                    for (String mediaFileName : evidenceAdapter.content) {
                        String requestTag = mediaFileName.endsWith(CameraManager.JPG) ? "images" : "videos";
                        multipart.addFilePart(typeServerSuffixNoS + "[" + requestTag + "][]", new File(mediaFileName));
                    }
                    List<String> responseList = multipart.finish();
                    Log.e("Punisher", "SERVER REPLIED:");
                    for (String line : responseList) {
                        Log.e("Punisher", "Upload Files Response:::" + line);
                        response.append(line);
                    }
                }

            } catch (final IOException e){
                ViolationActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(ViolationActivity.this, R.string.cannot_connect_server, e);
                    }
                });
                return "";
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.GONE);
            if (mode == calculateRefusedStatus()){
                DBHelper.deleteRequest(dbHelper.getWritableDatabase(), new BigDecimal(rowID).intValue());
            }
            try {
                JSONObject fullAnswer = new JSONObject(s);
                if (fullAnswer.getString("status").equals(Globals.SERVER_SUCCESS)) {
                    if (mode != calculateRefusedStatus()) {
                        DBHelper.deleteRequest(dbHelper.getWritableDatabase(), id);
                    }
                    Toast.makeText(context, R.string.requestSent, Toast.LENGTH_LONG).show();
                    finish();
                }
            } catch (JSONException e){
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    class ThumbnailFetcher extends AsyncTask<String, Void, Exception>{

        @Override
        protected Exception doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                ViolationActivity.this.evidenceAdapter.mediaContent.add(bmp);
            } catch (Exception e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception e) {
            ViolationActivity.this.evidenceAdapter.notifyDataSetChanged();
            super.onPostExecute(e);
            if (e != null)
                Globals.showError(ViolationActivity.this, R.string.cannot_connect_server, e);
        }
    }

    class RequestFetcher extends AsyncTask<Integer, Void, String>{

        @Override
        protected String doInBackground(Integer... params) {
            String result = null;
            try {
                result = HttpHelper.proceedRequest("complains/" + params[0], "GET", "", true);
            } catch (final IOException e) {
                ViolationActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(ViolationActivity.this, R.string.cannot_connect_server, e);
                    }
                });
                return "";
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONArray updatesJSON = new JSONObject(s).getJSONArray("data").getJSONObject(1).getJSONArray("updates");
                ObjectMapper objectMapper = new ObjectMapper();
                UpdateEntity[] updates = new UpdateEntity[updatesJSON.length()];
                for (int i = 0; i < updatesJSON.length(); i++){
                    updates[i] = objectMapper.readValue(updatesJSON.get(i).toString(), UpdateEntity.class);
                }
                historyAdapter.setContent(updates);
            } catch (JSONException | IOException e) {
                Globals.showError(ViolationActivity.this, R.string.cannot_connect_server, e);
            }
        }
    }
}
