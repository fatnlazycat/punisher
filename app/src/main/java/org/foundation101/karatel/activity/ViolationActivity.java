package org.foundation101.karatel.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.codehaus.jackson.map.ObjectMapper;
import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.CreationResponse;
import org.foundation101.karatel.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.Karatel;
import org.foundation101.karatel.R;
import org.foundation101.karatel.Request;
import org.foundation101.karatel.Violation;
import org.foundation101.karatel.ViolationRequisite;
import org.foundation101.karatel.adapter.DrawerAdapter;
import org.foundation101.karatel.adapter.EvidenceAdapter;
import org.foundation101.karatel.adapter.HistoryAdapter;
import org.foundation101.karatel.adapter.RequestListAdapter;
import org.foundation101.karatel.adapter.RequisitesListAdapter;
import org.foundation101.karatel.fragment.RequestListFragment;
import org.foundation101.karatel.retrofit.RetrofitDownloader;
import org.foundation101.karatel.retrofit.RetrofitMultipartUploader;
import org.foundation101.karatel.view.ExpandedGridView;
import org.foundation101.karatel.view.MyScrollView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ViolationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    final int RQS_GooglePlayServices = 1000;

    public static final int MODE_CREATE = -1;
    public static final int MODE_EDIT = 0;
    public static final String STATUS_REFUSED = "Відмовлено в розгляді";
    public static final String TAG = "ViolationActivity";

    RequisitesListAdapter requisitesAdapter;
    LinearLayout requisitesList;
    RelativeLayout tabStatus;
    EvidenceAdapter evidenceAdapter = new EvidenceAdapter(this);
    HistoryAdapter historyAdapter;
    ListView historyListView;
    DBHelper dbHelper;
    Cursor cursor;
    int mode, thumbDimension;
    Long rowID;

    FragmentManager fm;
    SupportMapFragment supportMapFragment;
    final Double REFRESH_ACCURACY = 0.001;
    boolean mockLocationDialogShown = false;

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

    public FrameLayout progressBar;
    TabHost tabs;
    Button punishButton, saveButton;

    public GoogleApiClient googleApiClient;
    LocationManager locationManager;
    LocationRequest locationRequest;
    android.location.LocationListener locationListener;
    static final int REQUEST_CHECK_SETTINGS = 2000;

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
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (requisitesAdapter != null) requisitesAdapter.getMapDataFromBundle(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_violation);

        requisitesList = (LinearLayout) findViewById(R.id.requisitesList);

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);
        punishButton = (Button) findViewById(R.id.punishButton);
        saveButton = (Button) findViewById(R.id.saveButton);

        if (checkGooglePlayServices()) { buildGoogleApiClient(); }
        initOldAndroidLocation();

        //check location availability
        /*if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) && !HttpHelper.internetConnected(this)) {
            DialogFragment dialog = new OpenSettingsFragment();
            dialog.show(getSupportFragmentManager(), "openSettingsFragment");
        }*/

        ((Karatel)getApplication()).restoreUserFromPreferences();

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
                    tabStatus = (RelativeLayout)findViewById((R.id.tabStatus));
                    historyListView = (ListView)findViewById(R.id.historyListView);
                    FrameLayout.LayoutParams mParam = new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT);
                    tabStatus.setLayoutParams(mParam);
                    historyListView.setAdapter(historyAdapter);
                    historyListView.setEmptyView(findViewById(R.id.layoutNoRequests));
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
                latitude = request.address_lat;
                longitude = request.address_lon;
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

        ExpandedGridView evidenceGridView = (ExpandedGridView) findViewById(R.id.evidenceGridView);
        //evidenceGridView.setHorizontalSpacing(DrawerAdapter.dpToPx(getApplicationContext(), 10));
        makeEvidenceAdapterContent(mode, request);
        evidenceGridView.setAdapter(evidenceAdapter);
        evidenceGridView.setEmptyView(findViewById(R.id.emptyView));
        evidenceGridView.setFocusable(false);

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        actionBar.setTitle(Violation.getViolationNameFromType(this, violation.getType()));

        //setting views for appropriate mode
        if (mode > MODE_EDIT){
            //first check if the status is "refused" which means we can edit the data & re-send the request
            if (mode == calculateRefusedStatus()){
                saveButton.setVisibility(View.GONE);
                punishButton.setText(R.string.create_new_bases_on_this);
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

            setupTab(R.id.tabStatus, getString(R.string.request_status));
            TabWidget tabWidget = (TabWidget)findViewById(android.R.id.tabs);
            tabWidget.setVisibility(View.VISIBLE);
            tabs.setCurrentTab(0);

            findViewById(R.id.textViewViolationHeader).setVisibility(View.GONE);
            findViewById(R.id.textViewViolationDisclaimer).setVisibility(View.GONE);
        } else {
            int disclaimerId = getResources().getIdentifier(violation.type + "_disclaimer", "string", getPackageName());
            TextView textViewViolationDisclaimer = (TextView) findViewById(R.id.textViewViolationDisclaimer);
            if (getResources().getString(disclaimerId).isEmpty()) textViewViolationDisclaimer.setVisibility(View.GONE);
            else textViewViolationDisclaimer.setText(disclaimerId);
        }

        makeRequisitesViews();

        ((Karatel)getApplication()).sendScreenName(violation.type);
    }

    void makeRequisitesViews(){
        fm = getSupportFragmentManager();
        supportMapFragment =  SupportMapFragment.newInstance(/*new GoogleMapOptions().liteMode(true)*/);
        for (int i=0; i<requisitesAdapter.getCount(); i++){
            RequisitesListAdapter.ViewHolder holder = new RequisitesListAdapter.ViewHolder();

            View v = LayoutInflater.from(this).inflate(R.layout.item_violation_requisite, requisitesList, false);

            ViolationRequisite thisRequisite = requisitesAdapter.content.get(i);

            holder.textViewRequisiteHeader=(TextView)v.findViewById(R.id.textViewRequisiteHeader);
            holder.textViewRequisiteDescription=(TextView)v.findViewById(R.id.textViewRequisiteDescription);
            holder.mapContainer = (FrameLayout) v.findViewById(R.id.mapContainer);
            holder.editTextRequisite = (AutoCompleteTextView)v.findViewById(R.id.editTextRequisite);

            holder.textViewRequisiteHeader.setText(thisRequisite.name);

            if (thisRequisite.description.isEmpty()) {
                holder.textViewRequisiteDescription.setVisibility(View.GONE);
            } else {
                holder.textViewRequisiteDescription.setVisibility(View.VISIBLE);
                holder.textViewRequisiteDescription.setText(thisRequisite.description);
            }

            if (thisRequisite.dbTag.endsWith("_address")){
                fm.beginTransaction().replace(R.id.mapContainer, supportMapFragment).commitAllowingStateLoss();// allowingStateLoss - Fighting with java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
                supportMapFragment.getMapAsync(requisitesAdapter);
                holder.mapContainer.setVisibility(View.VISIBLE);

                ((MyScrollView)findViewById(R.id.tabInfo)).addInterceptScrollView(holder.mapContainer);

                holder.editTextRequisite.setThreshold(0);
                holder.editTextRequisite.setAdapter(requisitesAdapter.addressAdapter);
                holder.editTextRequisite.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (requisitesAdapter.mMap != null) {
                            requisitesAdapter.mMap.clear();
                            RequisitesListAdapter.PlaceLikelihoodHolder placeHolder =
                                    (RequisitesListAdapter.PlaceLikelihoodHolder) parent.getAdapter().getItem(position);
                            LatLng place = placeHolder.field.getPlace().getLatLng();
                            MarkerOptions marker = new MarkerOptions().position(place);
                            requisitesAdapter.mMap.addMarker(marker);
                            requisitesAdapter.hasMarker = true;
                            requisitesAdapter.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place, RequisitesListAdapter.DEFAULT_ZOOM));
                        }
                    }
                });
                requisitesAdapter.addressEditText = holder.editTextRequisite;
            } else { //it's not autocomplete - ordinary EditText, hence - no map
                holder.mapContainer.setVisibility(View.GONE);
            }
            if ((thisRequisite.value == null) || (thisRequisite.value.isEmpty())){
                if (requisitesAdapter.editTrigger) {
                    holder.editTextRequisite.setHint(thisRequisite.hint);
                } else holder.editTextRequisite.setText("");
            } else {
                holder.editTextRequisite.setText(thisRequisite.value);
            }
            holder.editTextRequisite.setEnabled(requisitesAdapter.editTrigger);
            holder.editTextRequisite.setFocusable(requisitesAdapter.editTrigger);
            if (requisitesAdapter.editTrigger){
                holder.editTextRequisite.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        validatePunishButton();
                    }
                });
            }

            requisitesAdapter.holders.add(holder);
            requisitesList.addView(v);
        }
    }

    int calculateRefusedStatus(){
        int result = -100;

        /*commented out to cancel special tratment of refused requests - they stay refused forever
        String[] violationStatuses = getResources().getStringArray(R.array.violationStatuses);
        int refusedStatusIndex = Arrays.asList(violationStatuses).indexOf(STATUS_REFUSED);
        if (Globals.statusesMap != null){
            for (int key : Globals.statusesMap.keySet()){
                if (Globals.statusesMap.get(key) == refusedStatusIndex)
                    result = key;
            }
        }*/
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
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 4;
                        int orientation = Karatel.getOrientation(evidenceFileName);
                        thumbnail = Karatel.rotateBitmap(
                                ThumbnailUtils.extractThumbnail(
                                        BitmapFactory.decodeFile(evidenceFileName, options)
                                        , thumbDimension, thumbDimension
                                )
                                , orientation
                        );
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
                Bitmap bmp;
                if (CameraManager.lastCapturedFile.endsWith(CameraManager.JPG)) {
                    int orientation = Karatel.getOrientation(CameraManager.lastCapturedFile);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    bmp = Karatel.rotateBitmap(
                            ThumbnailUtils.extractThumbnail(
                                    BitmapFactory.decodeFile(CameraManager.lastCapturedFile, options)
                                    , thumbDimension, thumbDimension
                            )
                            , orientation
                    );
                } else {
                    bmp = ThumbnailUtils.createVideoThumbnail(CameraManager.lastCapturedFile, MediaStore.Video.Thumbnails.MICRO_KIND);
                }
                evidenceAdapter.content.add(CameraManager.lastCapturedFile);
                evidenceAdapter.mediaContent.add(bmp);
                evidenceAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Globals.showError(this, R.string.error, e);
            }
        }

        if (requestCode == REQUEST_CHECK_SETTINGS){

            /*switch (resultCode){
                case RESULT_OK : {*/
            if (googleApiClient != null && googleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            }
                    /*break;
                }
            }*/
        }
        super.onActivityResult(requestCode, resultCode, intent);
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
        /*first check if location is available - show error dialog if not
        this is made to prevent saving request with zero LatLng -> leads to crash.
         */
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
                cv.put(thisRequisite.dbTag, (requisitesAdapter.holders.get(i).editTextRequisite.getText().toString()));
            }
            switch (mode) {
                case MODE_CREATE: {
                    time_stamp = new SimpleDateFormat(RequestListAdapter.INPUT_DATE_FORMAT, Locale.US)
                            .format(new Date());
                    cv.put(DBHelper.TIME_STAMP, time_stamp);
                    rowID = db.insertOrThrow(DBHelper.VIOLATIONS_TABLE, null, cv);
                    id = new BigDecimal(rowID).intValue();
                    idInDbString = rowID.toString();
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
            if (mode == MODE_EDIT) {//this condition works only in edit mode - delete all records previously added to db
                int i = db.delete(DBHelper.MEDIA_TABLE, "id = ?", new String[]{idInDbString});
            }
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

            //show toast only if the method is called by Save button
            if (view != null ) Toast.makeText(this, R.string.requestSaved, Toast.LENGTH_SHORT).show();
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
            saveToBase(null); //we pass null to point that it's called from punish()
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

        int i=0;
        while (i < requisitesAdapter.getCount() && result){
            Editable text = requisitesAdapter.holders.get(i).editTextRequisite.getText();
            if (requisitesAdapter.content.get(i).necessary) result = (text != null) && (!text.toString().isEmpty());
            i++;
        }

        return result;
    }

    public void blockButtons(boolean block){
        int visibility = block ? View.GONE : View.VISIBLE;
        punishButton.setVisibility(visibility);
        saveButton.setVisibility((visibility));
    }

    /*private Location getLastLocation() throws NullPointerException {
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
        }
        Location locationGoogle = null;
        if (googleApiClient != null) {
            locationGoogle = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }

        if (((Karatel)getApplicationContext()).locationIsMock(locationGoogle)) {
            return null;
        } else {
            return locationGoogle;
        }
    }*/

    /*
    old Android package location code
    */
    void initOldAndroidLocation(){
        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyOldAndroidLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    class MyOldAndroidLocationListener implements android.location.LocationListener{
        @Override
        public void onLocationChanged(Location location) {
            if (latitude == null || latitude == 0) {//use this only if no result from FusedLocationAPI

                if (((Karatel)getApplicationContext()).locationIsMock(location)){
                    if (!mockLocationDialogShown) {
                        mockLocationDialogShown = true;
                        AlertDialog.Builder builder = new AlertDialog.Builder(ViolationActivity.this);
                        AlertDialog dialog = builder
                                .setTitle(R.string.turn_off_mock_locations)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                                        if (intent.resolveActivity(getPackageManager()) != null) {
                                            startActivity(intent);
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null).create();
                        dialog.show();
                    }
                } else if (mode == MODE_CREATE) {
                    Double latToCheck = latitude != null ? latitude : 0;
                    Double lonToCheck = longitude != null ? longitude : 0;
                    Double absLat = Math.abs(latToCheck - location.getLatitude());
                    Double absLon = Math.abs(lonToCheck - location.getLongitude());
                    if (absLat > REFRESH_ACCURACY || absLon > REFRESH_ACCURACY) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        requisitesAdapter.nullSavedLatLng();
                        requisitesAdapter.reclaimMap();
                    }
                }
            }
            locationManager.removeUpdates(locationListener);
            //locationListener = null;
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
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
            locationRequest = LocationRequest.create()
                    .setNumUpdates(3)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(1);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);
            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());

            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    //final LocationSettingsStates states = result.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can initialize location requests here.
                            if (googleApiClient != null && googleApiClient.isConnected()) {
                                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                                        locationRequest, ViolationActivity.this);
                            }
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(ViolationActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            Toast.makeText(ViolationActivity.this,  "location settings not available", Toast.LENGTH_LONG).show();
                            ViolationActivity.this.finish();
                            break;
                    }
                }
            });
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
        if (mode == MODE_CREATE && !((Karatel)getApplicationContext()).locationIsMock(location)){
            Double latToCheck = latitude!=null ? latitude : 0;
            Double lonToCheck = longitude!=null ? longitude : 0;
            Double absLat = Math.abs(latToCheck - location.getLatitude());
            Double absLon = Math.abs(lonToCheck - location.getLongitude());
            if (absLat > REFRESH_ACCURACY || absLon > REFRESH_ACCURACY) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                requisitesAdapter.nullSavedLatLng();
                requisitesAdapter.reclaimMap();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        requisitesAdapter.putMapDataToBundle(outState);
        super.onSaveInstanceState(outState);
    }

    @Override //Activity method
    protected void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
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

            if (locationListener != null) {
                locationManager.removeUpdates(locationListener);
                //locationListener = null;
            }
        }

        requisitesAdapter.releaseMap();

        super.onDestroy();
    }

    class ViolationSender extends AsyncTask<Violation, Void, CreationResponse> {

        Context context;

        public ViolationSender(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            Globals.hideSoftKeyboard(ViolationActivity.this);
        }

        @Override
        protected CreationResponse doInBackground(Violation... params) {
            if (!HttpHelper.internetConnected(context)) return null;

            Violation violation = params[0];
            CreationResponse result;

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

                // this part is to update existing declined request instead of creating new one
                if (mode == calculateRefusedStatus()){
                    String request = new HttpHelper(typeServerSuffixNoS).makeRequestString(requestParametersArray);
                    String response = HttpHelper.proceedRequest(typeServerSuffix + "/" + id.toString(), "PUT", request, true);
                    result = new ObjectMapper().readValue(response, CreationResponse.class);
                } else {

                    //Retrofit request
                    MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                    int i = 0;
                    while (i < requestParametersArray.length) {
                        requestBodyBuilder.addFormDataPart(typeServerSuffixNoS + "[" + requestParametersArray[i++] + "]",
                                requestParametersArray[i++]);
                    }
                    for (String mediaFileName : evidenceAdapter.content) {
                        String requestTag, mimeType;
                        if (mediaFileName.endsWith(CameraManager.JPG)){
                            requestTag = "images";
                            mimeType = "image/jpeg";
                        } else {
                            requestTag = "videos";
                            mimeType = "video/mp4";
                        }
                        requestBodyBuilder.addFormDataPart(typeServerSuffixNoS + "[" + requestTag + "][]",
                                mediaFileName,
                                RequestBody.create(MediaType.parse(mimeType), new File(mediaFileName)));
                    }

                    RetrofitMultipartUploader api = Karatel.getClient().create(RetrofitMultipartUploader.class);
                    Call<CreationResponse> call = api.upload(Globals.sessionToken, typeServerSuffix, requestBodyBuilder.build());
                    Response<CreationResponse> json = call.execute();
                    if (json.isSuccessful()) {
                        result = json.body();
                    } else {
                        ResponseBody errorBody = json.errorBody();
                        result = new ObjectMapper().readValue(errorBody.string(), CreationResponse.class);
                        errorBody.close();
                    }

                    //request based on HttpUrlConnection
                    /*
                    int tries = 0; final int MAX_TRIES = 2;
                    while (tries++ < MAX_TRIES) try {
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
                        return response.toString();
                    } catch (IOException e) {
                        if (tries == MAX_TRIES) {
                            throw e;
                        } else {
                            Log.e("Punisher", "try # " + tries + " : " + e.toString());
                        }
                    }*/
                }

            } catch (final IOException e){
                ViolationActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(ViolationActivity.this, R.string.cannot_connect_server, e);
                    }
                });
                return null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(CreationResponse answer) {
            super.onPostExecute(answer);
            progressBar.setVisibility(View.GONE);

            if (answer == null) {
                try {
                    answer = new ObjectMapper().readValue(HttpHelper.ERROR_JSON, CreationResponse.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            if (mode == calculateRefusedStatus()){
                DBHelper.deleteRequest(db, new BigDecimal(rowID).intValue());
            }
            switch (answer.status) {
                case Globals.SERVER_SUCCESS: {
                    if (mode != calculateRefusedStatus()) {
                        DBHelper.deleteRequest(db, id);
                    }
                    //Toast.makeText(context, R.string.requestSent, Toast.LENGTH_LONG).show();
                    finish();
                    break;
                }
                case Globals.SERVER_ERROR: {
                    Toast.makeText(context, answer.error, Toast.LENGTH_LONG).show();
                    break;
                }
            }

            db.close();
        }
    }

    class ThumbnailFetcher extends AsyncTask<String, Void, Exception>{

        @Override
        protected Exception doInBackground(String... params) {
            try {
                String filePath = getExternalFilesDir(null) + File.separator + TAG + CameraManager.JPG;
                File file  = new File(filePath);
                String baseUrl = Globals.SERVER_URL.replace("/api/v1/", "");

                RetrofitDownloader downloader = new Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .build().create(RetrofitDownloader.class);
                Call<ResponseBody> call = downloader.downloadFileWithDynamicUrl(params[0].replace(baseUrl, ""));
                Response<ResponseBody> response = call.execute();
                if (response.isSuccessful()) {
                    Log.d(TAG, "server contacted and has file");
                    boolean writtenToDisk = ShowMediaActivity.writeResponseBodyToDisk(response.body(), file);
                    Log.d(TAG, "file download was a success? " + writtenToDisk);
                } else {
                    Log.d(TAG, "server contact failed");
                }

                int orientation = Karatel.getOrientation(filePath);

                Bitmap picture = Karatel.rotateBitmap(BitmapFactory.decodeFile(filePath, null), orientation);
                if (picture != null) {
                    int width = getResources().getDimensionPixelSize(R.dimen.evidence_width);
                    int height = getResources().getDimensionPixelSize(R.dimen.evidence_height);
                    Bitmap fitBitmap = Bitmap.createScaledBitmap(picture, width, height, false);
                    ViolationActivity.this.evidenceAdapter.mediaContent.add(fitBitmap);
                    picture.recycle();
                }

                boolean fileDeletedSuccessfully = file.delete();
                Log.d(TAG, "fileDeletedSuccessfully = " + fileDeletedSuccessfully);
            } catch (final IOException e) {
                return  e;
            }



            /* old code
            try {
                URL url = new URL(params[0]);
                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                int width = getResources().getDimensionPixelSize(R.dimen.evidence_width);
                int height = getResources().getDimensionPixelSize(R.dimen.evidence_height);
                Bitmap fitBitmap = Bitmap.createScaledBitmap(bmp, width, height, false);
                ViolationActivity.this.evidenceAdapter.mediaContent.add(fitBitmap);
                bmp.recycle();
            } catch (Exception e) {
                return e;
            }*/
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
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            Globals.hideSoftKeyboard(ViolationActivity.this);
        }

        @Override
        protected String doInBackground(Integer... params) {
            String result = null;
            if (HttpHelper.internetConnected(ViolationActivity.this)) {
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
            } else return HttpHelper.ERROR_JSON;
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.GONE);
            try {
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals("success")){
                    JSONArray dataJSON = json.getJSONArray("data");
                    ObjectMapper mapper = new ObjectMapper();
                    JSONObject requestBody = dataJSON.getJSONObject(1);
                    String requestBodyString = requestBody.toString();
                    request = mapper.readValue(requestBodyString, Request.class);

                    request.type = dataJSON.getString(0);
                    }

                historyAdapter.setContent(request.updates);
            } catch (JSONException | IOException | NullPointerException e) {
                Globals.showError(ViolationActivity.this, R.string.cannot_connect_server, e);
            }
        }
    }
}
