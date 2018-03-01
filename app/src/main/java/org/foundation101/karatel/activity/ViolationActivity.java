package org.foundation101.karatel.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
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

import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.codehaus.jackson.map.ObjectMapper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.EvidenceAdapter;
import org.foundation101.karatel.adapter.HistoryAdapter;
import org.foundation101.karatel.adapter.RequestListAdapter;
import org.foundation101.karatel.adapter.RequisitesListAdapter;
import org.foundation101.karatel.entity.CreationResponse;
import org.foundation101.karatel.entity.Request;
import org.foundation101.karatel.entity.UpdateEntity;
import org.foundation101.karatel.entity.Violation;
import org.foundation101.karatel.entity.ViolationRequisite;
import org.foundation101.karatel.fragment.RequestListFragment;
import org.foundation101.karatel.manager.CameraManager;
import org.foundation101.karatel.manager.DBHelper;
import org.foundation101.karatel.manager.GoogleApiManager;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.manager.KaratelLocationManager;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.manager.PermissionManager;
import org.foundation101.karatel.retrofit.RetrofitDownloader;
import org.foundation101.karatel.retrofit.RetrofitMultipartUploader;
import org.foundation101.karatel.utils.DBUtils;
import org.foundation101.karatel.utils.Formular;
import org.foundation101.karatel.utils.MediaUtils;
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
import java.util.Arrays;
import java.util.Comparator;
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

import static org.foundation101.karatel.manager.KaratelLocationManager.*;
import static org.foundation101.karatel.manager.PermissionManager.CUSTOM_CAMERA_PERMISSIONS_START_IMMEDIATELY;
import static org.foundation101.karatel.manager.PermissionManager.CUSTOM_CAMERA_PERMISSIONS_START_NORMAL;
import static org.foundation101.karatel.manager.PermissionManager.LOCATION_PERMISSIONS;

public class ViolationActivity extends AppCompatActivity implements Formular {

    public static final int MODE_CREATE = -1;
    public static final int MODE_EDIT = 0;
    public static final String STATUS_REFUSED = "Відмовлено в розгляді"; //do not delete - needed to treat refused requests separately
    public static final String TAG = "ViolationActivity";

    RequisitesListAdapter requisitesAdapter;
    LinearLayout requisitesList;
    RelativeLayout tabStatus;
    EvidenceAdapter evidenceAdapter = new EvidenceAdapter(this);
    HistoryAdapter historyAdapter;
    ListView historyListView;
    DBHelper dbHelper;
    Cursor cursor;
    int mode;
    Long rowID;

    //debug view
    TextView tv;

    FragmentManager fm;
    SupportMapFragment supportMapFragment;

    AlertDialog changesLostDialog;

    public boolean blockButtons = true;//used to check if the location is defined

    public int getMode() { return mode; }

    public void setMode(int mode) {
        this.mode = mode;
        if (lManager != null) lManager.initFields(getLocationServicesArray(false));
    }

    ArrayList<String> savedInstanceStateEvidenceFileNames = new ArrayList<>();
    public Request request = null;
    Integer id, status, idOnServer;
    String idInDbString, time_stamp;
    String id_number_server = "";
    Violation violation = new Violation();
    public Double latitude, longitude;
    boolean statusTabFirstShow = true;
    boolean saveInstanceStateCalled = false;
    boolean needReclaimFullLocation = false;

    boolean changesMade = false;
    @Override
    public boolean changesMade() { return changesMade; }
    @Override
    public void setChangesMade(boolean value) { changesMade = value; }

    public FrameLayout progressBar;
    TabHost tabs;
    TabHost.OnTabChangeListener tabChangeListener;
    Button punishButton, saveButton;

    GoogleApiManager googleApiManager;

    @Override
    public GoogleApiManager getGoogleManager() {
        return googleApiManager;
    }

    public static final int REQUEST_CHECK_SETTINGS = 2000;

    boolean videoOnly = false;

    KaratelLocationManager lManager;

    @Override
    protected void onStart() {
        super.onStart();
        lManager.onStart(getLocationServicesArray(needReclaimFullLocation));
        googleApiManager.onStart();

        validatePunishButton();
        validateSaveButton();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (requisitesAdapter != null) requisitesAdapter.getMapDataFromBundle(savedInstanceState);

        if (savedInstanceState != null) {
            //init requisite EditTexts after activity recreation otherwise they all contain the same value (value of the last one)
            ArrayList<String> savedValues = savedInstanceState.getStringArrayList(Globals.REQUISITES_VALUES);
            if (savedValues != null) {
                int listSize = requisitesAdapter.holders.size();
                for (int i = 0; i < listSize; i++) {
                    requisitesAdapter.holders.get(i).editTextRequisite.setText(savedValues.get(i));
                }
            }

            changesMade = savedInstanceState.getBoolean(Globals.VIOLATION_ACTIVITY_CHANGES_MADE, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        saveInstanceStateCalled = false;

        String t = tv.getText().toString();
        String t2 = t.length() > 2 ? t.substring(0, t.length() / 2 - 1) : t;
        tv.setText(String.format(Locale.US, "lat: %1$10f\nlon: %2$10f\n%3$s", latitude, longitude, t2));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_violation);

        Intent intent = this.getIntent();
        mode = intent.getIntExtra(Globals.VIOLATION_ACTIVITY_MODE, MODE_EDIT);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey  (Globals.VIOLATION_ACTIVITY_MODE)) {
                mode = savedInstanceState.getInt(Globals.VIOLATION_ACTIVITY_MODE);
            }

            needReclaimFullLocation = savedInstanceState.getBoolean(Globals.VIOLATION_ACTIVITY_NEED_RECLAIM_LOCATION, false);

            //init evidences after activity recreation otherwise they will be lost
            if (savedInstanceState.containsKey(Globals.EVIDENCES)) { //this will be true only in MODE_CREATE | MODE_EDIT
                savedInstanceStateEvidenceFileNames       = savedInstanceState.getStringArrayList(Globals.EVIDENCES);
                //evidenceAdapter.filesDeletedDuringSession = savedInstanceState.getStringArrayList(Globals.DELETED_EVIDENCES);
            }
        }
        tv = findViewById(R.id.textViewViolationHeader);
        requisitesList                      = findViewById(R.id.requisitesList);
        TextView addedPhotoVideoTextView    = (TextView)    findViewById(R.id.addedPhotoVideoTextView);
        progressBar                         = (FrameLayout) findViewById(R.id.frameLayoutProgress);
        punishButton                        = (Button)      findViewById(R.id.punishButton);
        saveButton                          = (Button)      findViewById(R.id.saveButton);

        lManager = new KaratelLocationManager(this, getLocationServicesArray(false));
        googleApiManager = new GoogleApiManager(this);
        googleApiManager.init(lManager, lManager);
        lManager.onCreate();

        KaratelPreferences.restoreUser();

        //initializing tab view with only one tab - the second will be initialized later
        tabs=(TabHost)findViewById(android.R.id.tabhost);
        tabs.setup();
        setupTab(R.id.tabInfo, getString(R.string.information));
        tabs.setCurrentTab(0);
        tabChangeListener = new TabHost.OnTabChangeListener() {
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
        };
        tabs.setOnTabChangedListener(tabChangeListener);

        dbHelper = new DBHelper(this, DBHelper.DATABASE, DBHelper.DB_VERSION);
        requisitesAdapter = new RequisitesListAdapter(this);

        if (mode == MODE_CREATE) {
            idOnServer = 0;
            violation = (Violation) intent.getExtras().getSerializable(Globals.VIOLATION);
            status = 0; //status = draft
            requisitesAdapter.content = violation.getRequisites();
            videoOnly = violation.getMediaTypes() == Violation.VIDEO_ONLY;

            //create mode means we have to capture video at start (if it's not a recreation of course)
            if (violation.usesCamera && savedInstanceState == null) {
                launchCamera(null);
            }

        } else {//edit or view mode means we have to fill requisites & evidenceGridView
            if (savedInstanceState != null && savedInstanceState.containsKey(Globals.ITEM_ID)) {
                id = savedInstanceState.getInt(Globals.ITEM_ID);
            } else {
                id = intent.getIntExtra(Globals.ITEM_ID, 0);
            }
            idInDbString = id.toString();

            if (mode == MODE_EDIT) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                //query to data table
                String table = DBHelper.VIOLATIONS_TABLE;
                String[] columns = null;
                String where = "_id=?";
                String[] selectionArgs = {idInDbString};
                cursor = db.query(table, columns, where, selectionArgs, null, null, null);
                if (cursor.moveToFirst()) {
                    idOnServer = cursor.getInt(cursor.getColumnIndex(DBHelper.ID_SERVER));
                    id_number_server = cursor.getString(cursor.getColumnIndex(DBHelper.ID_NUMBER_SERVER));
                    latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                    longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                    time_stamp = cursor.getString(cursor.getColumnIndex(DBHelper.TIME_STAMP));
                    status = cursor.getInt(cursor.getColumnIndex("status"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    violation = Violation.getByType(type);
                    requisitesAdapter.content = violation.getRequisites();
                    for (int i = 0; i < requisitesAdapter.getCount(); i++) {
                        requisitesAdapter.content.get(i).value =
                                cursor.getString(cursor.getColumnIndex(requisitesAdapter.content.get(i).dbTag));
                    }
                    cursor.close();
                    db.close();
                } else {
                    cursor.close();
                    db.close();
                    finish();
                    return;
                }
            } else {//if neither create nor edit mode then it's loaded from server
                request = (Request)intent.getExtras().getSerializable(Globals.REQUEST_JSON);

                idOnServer = request.id;
                id_number_server = request.id_number;
                latitude = request.address_lat;
                longitude = request.address_lon;
                time_stamp = request.created_at;
                status = request.complain_status_id;
                violation = Violation.getByType(request.type);
                requisitesAdapter.content = violation.getRequisites();
                for (int i = 0; i < requisitesAdapter.getCount(); i++) {
                    try {
                        String fieldName = requisitesAdapter.content.get(i).dbTag.replace(violation.getType() + "_", "");
                        requisitesAdapter.content.get(i).value = (String)Request.class.getField(fieldName).get(request);
                    } catch (Exception e) {
                        Globals.showError(R.string.error, e);
                    }
                }

                historyAdapter = new HistoryAdapter(this);
                new RequestFetcher().execute(idOnServer);
            }
        }

        ExpandedGridView evidenceGridView = (ExpandedGridView) findViewById(R.id.evidenceGridView);
        //evidenceGridView.setHorizontalSpacing(DrawerAdapter.dpToPx(getApplicationContext(), 10));
        makeEvidenceAdapterContent(mode, savedInstanceStateEvidenceFileNames, request);
        evidenceGridView.setAdapter(evidenceAdapter);
        evidenceGridView.setEmptyView(findViewById(R.id.emptyView));
        evidenceGridView.setFocusable(false);

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        actionBar.setTitle(violation.getName());

        //setting views for appropriate mode
        if (mode > MODE_EDIT){
            //first check if the status is "refused" which means we can edit the data & re-send the request
            if (mode == calculateRefusedStatus()){
                saveButton.setVisibility(View.GONE);
                punishButton.setText(R.string.create_new_bases_on_this);
            } else { //the request is already filed & not refused - > no edit anymore
                hideButtons(true);
                requisitesAdapter.setEditTrigger(false);
            }

            evidenceAdapter.setEditTrigger(false);
            ImageButton imageButtonAddEvidence = (ImageButton)findViewById(R.id.imageButtonAddEvidence);
            imageButtonAddEvidence.setVisibility(View.GONE);
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

            if (videoOnly) addedPhotoVideoTextView.setText(getString(R.string.takeVideo));
        }

        makeRequisitesViews();

        KaratelApplication.getInstance().sendScreenName(violation.type);
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

            if (thisRequisite.description == null || thisRequisite.description.isEmpty()) {
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
                        setChangesMade(true);
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

    int[] getLocationServicesArray(boolean forceFull) {
        if (forceFull) return new int[] { LOCATION_SERVICE_MAIN, LOCATION_SERVICE_ADDRESS };

        int[] locationServicesArray = {LOCATION_SERVICE_NONE, LOCATION_SERVICE_NONE};
        switch (mode) {
            case MODE_CREATE: locationServicesArray[0] = LOCATION_SERVICE_MAIN;
            case MODE_EDIT  : locationServicesArray[1] = LOCATION_SERVICE_ADDRESS;
        }
        return locationServicesArray;
    }

    void makeEvidenceAdapterContent(int mode, ArrayList<String> fileNames, Request request){
        if (!fileNames.isEmpty()) { //this can be true only in MODE_CREATE | MODE_EDIT
            for (String evidenceFileName : fileNames) try {
                Bitmap thumbnail = MediaUtils.getThumbnail(evidenceFileName);
                evidenceAdapter.content.add(evidenceFileName);
                evidenceAdapter.mediaContent.add(thumbnail);
            } catch (IOException e) {
                Globals.showError(R.string.error, e);
            }
        } else if (mode == MODE_EDIT) {
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
                    Bitmap thumbnail = MediaUtils.getThumbnail(evidenceFileName);
                    evidenceAdapter.content.add(evidenceFileName);
                    evidenceAdapter.mediaContent.add(thumbnail);
                } catch (Exception e){//we read files so need to catch exceptions
                    Globals.showError(R.string.error, e);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean granted = PermissionManager.allGranted(grantResults);
        switch (requestCode) {
            case LOCATION_PERMISSIONS : {
                lManager.onPermissionResult(granted);
                break;
            }
            case CUSTOM_CAMERA_PERMISSIONS_START_NORMAL : {
                if (granted) launchCamera(punishButton); //just a hack to signal that it's not an immediate start
                break;
            }
            case CUSTOM_CAMERA_PERMISSIONS_START_IMMEDIATELY : {
                if (granted) launchCamera(null);
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK &&
                (requestCode == CameraManager.GENERIC_CAMERA_CAPTURE_INTENT)) {
            try {
                Bitmap bmp;
                CameraManager.setLastCapturedFile(intent.getStringExtra(eu.aejis.mycustomcamera.IntentExtras.MEDIA_FILE));
                if (CameraManager.lastCapturedFile.endsWith(CameraManager.JPG)) {
                    int orientation = MediaUtils.getOrientation(CameraManager.lastCapturedFile);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    bmp = MediaUtils.rotateBitmap(
                            ThumbnailUtils.extractThumbnail(
                                    BitmapFactory.decodeFile(CameraManager.lastCapturedFile, options)
                                    , MediaUtils.THUMB_DIMENSION, MediaUtils.THUMB_DIMENSION
                            )
                            , orientation
                    );

                    int index = CameraManager.lastCapturedFile.lastIndexOf(CameraManager.JPG);
                    index = (index < 0) ? 0 : index;
                    String nameForReducedFile = new StringBuilder(CameraManager.lastCapturedFile)
                            .insert(index, "(2)").toString();
                    File reducedFile = MediaUtils.reduceFileDimensions(
                            new File(CameraManager.lastCapturedFile),
                            nameForReducedFile,
                            getApplicationContext());
                    if (!CameraManager.lastCapturedFile.equals(reducedFile.getPath())) {
                        File lastCameraFile = new File(CameraManager.lastCapturedFile);

                        boolean fileOperationsResult =
                                lastCameraFile.delete() &&
                                reducedFile.renameTo(lastCameraFile);

                        if (!fileOperationsResult) throw new IOException("Can't procced files");
                    }
                } else {
                    bmp = ThumbnailUtils.createVideoThumbnail(CameraManager.lastCapturedFile, MediaStore.Video.Thumbnails.MICRO_KIND);
                }

                setChangesMade(true);

                evidenceAdapter.content.add(CameraManager.lastCapturedFile);
                evidenceAdapter.mediaContent.add(bmp);
                evidenceAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Globals.showError(R.string.error, e);
            }
        }

        if (requestCode == REQUEST_CHECK_SETTINGS){
            /*switch (resultCode){
                case RESULT_OK : {*/
                lManager.onSettingsResult();
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
                onBackPressed(); //- previous version - sometimes strangely caused IllegalStateException: Can not perform this action after onSaveInstanceState
                /*if (changesMade()) showChangesLostDialog(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });*/
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (changesMade()) showChangesLostDialog();
        else finish();
    }

    //hides the software keyboard
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Globals.hideSoftKeyboard(this, event);
        return super.dispatchTouchEvent( event );
    }

    public void showChangesLostDialog(/*final DialogInterface.OnClickListener exitAction*/) {
        changesLostDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.attention))
                .setMessage(getString(R.string.allChangesWillBeLost))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {/*just dismiss*/ }
                })
                .create();
        changesLostDialog.show();
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
        needReclaimFullLocation shows that old evidences were deleted and new ones created - we need their coordinates
         */
        if (checkLocation() && !blockButtons && !needReclaimFullLocation) {

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
                    setMode(MODE_EDIT); //once created the record we switch to edit mode
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
                db.delete(DBHelper.MEDIA_TABLE, "id = ?", new String[]{idInDbString});
            }
            for (int i = 0; i < evidenceAdapter.content.size(); i++) {
                cv.put(DBHelper.ID, rowID);
                cv.put(DBHelper.FILE_NAME, evidenceAdapter.content.get(i));
                db.insert(DBHelper.MEDIA_TABLE, null, cv);
                cv.clear();
            }
            db.close();

            setChangesMade(false);

            //show toast only if the method is called by Save button
            if (view != null ) Toast.makeText(this, R.string.requestSaved, Toast.LENGTH_SHORT).show();
        } else {
            String message = needReclaimFullLocation ?
                    "Зачекайте поки пристрій визначить Ваше місцезнаходження та спробуйте ще раз" :
                    getString(R.string.cannot_define_location);
            Globals.showMessage(message);
        }
    }

    boolean checkLocation(){
        if (latitude == null){
            /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setTitle(R.string.cannot_define_location).setNegativeButton(R.string.ok, null).create();
            dialog.show();*/
            return false;
        } else return true;
    }

    public void launchCamera(View view) {
        boolean startImmediately = (view == null); //view is null if we start camera from onCreate
        int actionFlag = (startImmediately || videoOnly) ?
                CameraManager.VIDEO_CAPTURE_INTENT : 0; //0 means photoOrVideo not defined - user switches this in the camera
        CameraManager.getInstance(this).startCustomCamera(actionFlag, startImmediately, videoOnly);
    }

    boolean initialEvidencesDeleted() {
        ArrayList<String> temp = new ArrayList<>(evidenceAdapter.content);
        temp.retainAll(dbHelper.getMediaFiles(DBHelper.MEDIA_TABLE));
        return temp.isEmpty();
    }

    public void photoVideoPopupMenu(View view) {
        if (violation.getMediaTypes() == Violation.VIDEO_ONLY){
            CameraManager.getInstance(this).startCamera(CameraManager.VIDEO_CAPTURE_INTENT);
        } else {
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
        if (blockButtons) {
            Toast.makeText(this, R.string.cannot_define_location, Toast.LENGTH_LONG).show();
        } else {
            try {
                if (!evidenceAdapter.sizeCheck()) {
                    Toast.makeText(this, R.string.request_too_big, Toast.LENGTH_LONG).show();
                    return;
                }
                RequestListFragment.punishPerformed = true;
                saveToBase(null); //we pass null to point that it's called from punish()
                new ViolationSender(this).execute(violation);
            } catch (Exception e) {
                Globals.showError(R.string.error, e);
            }
        }
    }

    public void empty(View view) {
        //empty method to handle click events
    }

    @Override
    public void validateSaveButton(){
        saveButton.setEnabled(!evidenceAdapter.isEmpty());
    }

    @Override
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

    public void hideButtons(boolean block){
        int visibility = block ? View.GONE : View.VISIBLE;
        punishButton.setVisibility(visibility);
        saveButton.setVisibility((visibility));
    }

    @Override
    public void onEvidenceRemoved() {
        if (mode == MODE_EDIT && initialEvidencesDeleted()) {
            lManager.restart(getLocationServicesArray(true));
            needReclaimFullLocation = true;
        }
    }

    @Override
    public void onLocationChanged(double lat, double lon) {
        latitude = lat;
        longitude = lon;
        requisitesAdapter.nullSavedLatLng();
        requisitesAdapter.reclaimMap();
        needReclaimFullLocation = false;
    }

    @Override
    public void onAddressesReady(PlaceLikelihoodBuffer places) {
        requisitesAdapter.onAddressesReady(places);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //saving map
        requisitesAdapter.putMapDataToBundle(outState);

        //saving values entered into EditText requisites
        ArrayList<String> valuesToSave = new ArrayList<>();
        for (RequisitesListAdapter.ViewHolder holder : requisitesAdapter.holders) {
            valuesToSave.add(holder.editTextRequisite.getText().toString());
        }
        outState.putStringArrayList(Globals.REQUISITES_VALUES, valuesToSave);

        //saving evidence filenames & data about deleted files
        if (mode <= MODE_EDIT) {
            ArrayList<String> filenamesToSave = new ArrayList<>(evidenceAdapter.content);
            outState.putStringArrayList(Globals.EVIDENCES, filenamesToSave);
        }

        //saving mode & intent related data
        //otherwise started with MODE_CREATE and saved to base (mode=MODE_EDIT) will be recreated as MODE_CREATE again
        outState.putInt(Globals.VIOLATION_ACTIVITY_MODE, mode);
        if (mode == MODE_EDIT) outState.putInt(Globals.ITEM_ID, id);
        outState.putBoolean(Globals.VIOLATION_ACTIVITY_NEED_RECLAIM_LOCATION, needReclaimFullLocation);
        outState.putBoolean(Globals.VIOLATION_ACTIVITY_CHANGES_MADE, changesMade);

        //this is needed in onDestroy to distinguish whether it was initiated by user or by system
        saveInstanceStateCalled = true;

        super.onSaveInstanceState(outState);
    }

    @Override //Activity method
    protected void onStop() {
        lManager.onStop();
        googleApiManager.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (!saveInstanceStateCalled) clearEvidences(); //just destroy without saving state, e.g. on back pressed

        if (changesLostDialog != null) changesLostDialog.dismiss();

        lManager.onDestroy();
        lManager = null;

        googleApiManager = null;

        requisitesAdapter.releaseMap();

        super.onDestroy();
    }

    void clearEvidences() {
        DBUtils.clearEvidences(dbHelper);

        /*boolean deletedSuccessfully = true;
        //delete erased evidence files
        for (String fileToDelete : evidenceAdapter.filesDeletedDuringSession) {
            deletedSuccessfully = new File(fileToDelete).delete() && deletedSuccessfully;
        }

        if (mode == MODE_CREATE){
            //delete the evidence files if the request was not saved
            for (String fileToDelete : evidenceAdapter.content) {
                deletedSuccessfully = new File(fileToDelete).delete() && deletedSuccessfully;
            }
        }

        Log.e("Punisher", "files deleted successfully " + deletedSuccessfully);*/
    }

    void switchTabToHistory(){
        tabs.setCurrentTab(1);
        tabChangeListener.onTabChanged(getString(R.string.request_status));
    }

    private class ViolationSender extends AsyncTask<Violation, Void, CreationResponse> {

        Context context;

        ViolationSender(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            punishButton.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            Globals.hideSoftKeyboard(ViolationActivity.this);
        }

        @Override
        protected CreationResponse doInBackground(Violation... params) {
            if (!HttpHelper.internetConnected(/*context*/)) return null;

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
                String[] keysForRequestParameters = violation.getRequisitesString();
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

                    RetrofitMultipartUploader api = KaratelApplication.getClient().create(RetrofitMultipartUploader.class);
                    Call<CreationResponse> call = api.uploadComplain(Globals.sessionToken, typeServerSuffix, requestBodyBuilder.build());
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
                Globals.showError(R.string.cannot_connect_server, e);
                return null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(CreationResponse answer) {
            super.onPostExecute(answer);

            if (answer == null) {
                try {
                    answer = new ObjectMapper().readValue(HttpHelper.ERROR_JSON, CreationResponse.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            SQLiteDatabase db = dbHelper.getWritableDatabase();

            if (mode == calculateRefusedStatus()){
                DBHelper.deleteViolationRequest(db, new BigDecimal(rowID).intValue());
            }
            switch (answer.status) {
                case Globals.SERVER_SUCCESS: {
                    if (mode != calculateRefusedStatus()) {
                        DBHelper.deleteViolationRequest(db, id);
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

            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (punishButton != null) punishButton.setEnabled(true);
        }
    }

    private class ThumbnailFetcher extends AsyncTask<String, Void, Exception>{

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

                int orientation = MediaUtils.getOrientation(filePath);

                Bitmap picture = MediaUtils.rotateBitmap(BitmapFactory.decodeFile(filePath, null), orientation);
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
                Globals.showError(R.string.cannot_connect_server, e);
        }
    }

    private class RequestFetcher extends AsyncTask<Integer, Void, String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            Globals.hideSoftKeyboard(ViolationActivity.this);
        }

        @Override
        protected String doInBackground(Integer... params) {
            String result;
            if (HttpHelper.internetConnected(/*ViolationActivity.this*/)) {
                try {
                    result = HttpHelper.proceedRequest("complains/" + params[0], "GET", "", true);
                } catch (final IOException e) {
                    Globals.showError(R.string.cannot_connect_server, e);
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
                Arrays.sort(request.updates, new Comparator<UpdateEntity>() {
                    @Override
                    public int compare(UpdateEntity first, UpdateEntity second) {
                        return -first.created_at.compareTo(second.created_at);
                    }
                });
                historyAdapter.setContent(request.updates);
                if (request.updates.length > 0) {
                    request.updates[0].setCollapsed(false); //the first item will be opened by default
                    switchTabToHistory();
                }
            } catch (JSONException | IOException | NullPointerException e) {
                Globals.showError(R.string.cannot_connect_server, e);
            }
        }
    }
}
