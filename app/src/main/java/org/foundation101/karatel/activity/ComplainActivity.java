package org.foundation101.karatel.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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

import org.codehaus.jackson.map.ObjectMapper;
import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.KaratelPreferences;
import org.foundation101.karatel.entity.ComplainCreationResponse;
import org.foundation101.karatel.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.EvidenceAdapter;
import org.foundation101.karatel.adapter.RequestListAdapter;
import org.foundation101.karatel.adapter.RequisitesListAdapter;
import org.foundation101.karatel.entity.ComplainRequest;
import org.foundation101.karatel.entity.Violation;
import org.foundation101.karatel.entity.ViolationRequisite;
import org.foundation101.karatel.fragment.ComplainDraftsFragment;
import org.foundation101.karatel.retrofit.RetrofitMultipartUploader;
import org.foundation101.karatel.utils.DBUtils;
import org.foundation101.karatel.utils.DescriptionFormatter;
import org.foundation101.karatel.utils.Formular;
import org.foundation101.karatel.utils.MediaUtils;
import org.foundation101.karatel.view.ExpandedGridView;

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

public class ComplainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, Formular {

    final int RQS_GooglePlayServices = 1000;
    final int REQUEST_CODE_SELECT_POSSIBLE_VALUE = 1001;
    final double EMPTY_LOCATION_STUB = 0;

    public static final int MODE_CREATE = -1;
    public static final int MODE_EDIT = 0;
    public static final String TAG = "ComplainActivity";

    ArrayList<ViolationRequisite> requisites;
    ArrayList<RequisitesListAdapter.ViewHolder> requisiteViews = new ArrayList<>();
    LinearLayout requisitesList, llAddEvidence;
    EvidenceAdapter evidenceAdapter = new EvidenceAdapter(this);
    DBHelper dbHelper;
    Cursor cursor;
    int mode;
    Long rowID;

    final Double REFRESH_ACCURACY = 0.001;
    boolean mockLocationDialogShown = false;
    public boolean blockButtons = true;//used to check if the location is defined

    public int getMode() {
        return mode;
    }

    ArrayList<String> savedInstanceStateEvidenceFileNames = new ArrayList<>();
    public ComplainRequest request = null;
    Integer id, status, companyIdOnServer;
    String idInDbString, time_stamp;
    String id_number_server = "";
    Violation violation = new Violation();
    public Double latitude, longitude;
    boolean saveInstanceStateCalled = false;

    public FrameLayout progressBar;
    Button punishButton, saveButton;

    public GoogleApiClient googleApiClient;
    LocationManager locationManager;
    LocationRequest locationRequest;
    android.location.LocationListener locationListener;
    static final int REQUEST_CHECK_SETTINGS = 2000;

    //variable for customCamera
    boolean videoOnly = false;

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

        //init requisite EditTexts after activity recreation otherwise they all contain the same value (value of the last one)
        if (savedInstanceState != null) {
            ArrayList<String> savedValues = savedInstanceState.getStringArrayList(Globals.REQUISITES_VALUES);
            if (savedValues != null) {
                int listSize = requisiteViews.size();
                for (int i = 0; i < listSize; i++) {
                    requisiteViews.get(i).editTextRequisite.setText(savedValues.get(i));
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        saveInstanceStateCalled = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complain);

        requisitesList = (LinearLayout) findViewById(R.id.requisitesList);
        llAddEvidence = (LinearLayout) findViewById(R.id.llAddEvidence);

        TextView addedPhotoVideoTextView = (TextView)findViewById(R.id.addedPhotoVideoTextView);
        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);
        punishButton = (Button) findViewById(R.id.punishButton);
        saveButton = (Button) findViewById(R.id.saveButton);

        if (checkGooglePlayServices()) { buildGoogleApiClient(); }
        initOldAndroidLocation();

        KaratelPreferences.restoreUser();

        dbHelper = new DBHelper(this, DBHelper.DATABASE, DBHelper.DB_VERSION);

        Intent intent = this.getIntent();
        mode = intent.getIntExtra(Globals.VIOLATION_ACTIVITY_MODE, MODE_EDIT);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey  (Globals.VIOLATION_ACTIVITY_MODE)) {
                mode = savedInstanceState.getInt(Globals.VIOLATION_ACTIVITY_MODE);
            }

            //init evidences after activity recreation otherwise they will be lost
            if (savedInstanceState.containsKey(Globals.EVIDENCES)) { //this will be true only in MODE_CREATE | MODE_EDIT
                savedInstanceStateEvidenceFileNames       = savedInstanceState.getStringArrayList(Globals.EVIDENCES);
                //evidenceAdapter.filesDeletedDuringSession = savedInstanceState.getStringArrayList(Globals.DELETED_EVIDENCES);
            }
        }

        if (mode == MODE_CREATE) {
            violation = (Violation) intent.getExtras().getSerializable(Globals.VIOLATION);
            //companyIdOnServer = violation.getId();
            //status = 0; //status = draft
            //requisites = RequisitesListAdapter.makeContent(violation.type);
            requisites = violation.getRequisites();
            if (violation.usesCamera) {//create mode means we have to capture video at start
                CameraManager cameraManager = CameraManager.getInstance(this);
                videoOnly = violation.getMediaTypes() == Violation.VIDEO_ONLY;                        //
                cameraManager.startCustomCamera(CameraManager.VIDEO_CAPTURE_INTENT, true, videoOnly); //cameraManager.startCamera(CameraManager.VIDEO_CAPTURE_INTENT);
            }
        } else {//edit or view mode means we have to fill requisites & evidenceGridView
            if (savedInstanceState != null && savedInstanceState.containsKey(Globals.ITEM_ID)) {
                id = savedInstanceState.getInt(Globals.ITEM_ID);
            } else {
                id = intent.getIntExtra(Globals.ITEM_ID, 0);
            }
            idInDbString = id.toString();
            blockButtons = false;

            if (mode == MODE_EDIT) {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                //query to data table
                String table = DBHelper.COMPLAINS_TABLE;
                String[] columns = null;
                String where = "_id=?";
                String[] selectionArgs = {idInDbString};
                cursor = db.query(table, columns, where, selectionArgs, null, null, null);
                if (cursor.moveToFirst()) {
                    //companyIdOnServer = cursor.getInt(cursor.getColumnIndex(DBHelper.ID_SERVER));
                    //id_number_server = cursor.getString(cursor.getColumnIndex(DBHelper.ID_NUMBER_SERVER));
                    latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
                    longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
                    time_stamp = cursor.getString(cursor.getColumnIndex(DBHelper.TIME_STAMP));
                    //status = cursor.getInt(cursor.getColumnIndex("status"));
                    String type = cursor.getString(cursor.getColumnIndex("type"));
                    violation = Violation.getByType(type);
                    //requisites = RequisitesListAdapter.makeContent(violation.type);
                    requisites = violation.getRequisites();
                    for (ViolationRequisite oneRequisite : requisites) {
                        oneRequisite.value = cursor.getString(cursor.getColumnIndex(oneRequisite.dbTag));
                    }
                    cursor.close();
                    db.close();
                } else {
                    cursor.close();
                    db.close();
                    finish();
                    return;
                }
            } else {/*if neither create nor edit mode then it's loaded from server*/}
        }

        ExpandedGridView evidenceGridView = (ExpandedGridView) findViewById(R.id.evidenceGridView);
        makeEvidenceAdapterContent(savedInstanceStateEvidenceFileNames);
        evidenceGridView.setAdapter(evidenceAdapter);
        evidenceGridView.setEmptyView(findViewById(R.id.emptyView));
        evidenceGridView.setFocusable(false);

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);

        ImageView violationImage = (ImageView) findViewById(R.id.ivComplainLogo);
        violationImage.setImageResource(violation.drawableId);

        if (videoOnly) addedPhotoVideoTextView.setText(getString(R.string.takeVideo));

        /*llAddEvidence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                photoVideoPopupMenu(v);
            }
        });*/

        makeRequisitesViews();

        KaratelApplication.getInstance().sendScreenName(violation.type);
    }

    void makeRequisitesViews(){
        for (final ViolationRequisite thisRequisite : requisites){
            RequisitesListAdapter.ViewHolder holder = new RequisitesListAdapter.ViewHolder();

            View v = LayoutInflater.from(this).inflate(R.layout.item_violation_requisite, requisitesList, false);

            holder.textViewRequisiteHeader      = (TextView)            v.findViewById(R.id.textViewRequisiteHeader);
            holder.textViewRequisiteDescription = (TextView)            v.findViewById(R.id.textViewRequisiteDescription);
            holder.editTextRequisite            = (AutoCompleteTextView)v.findViewById(R.id.editTextRequisite);

            holder.textViewRequisiteHeader.setText(thisRequisite.name);

            if (thisRequisite.description == null || thisRequisite.description.isEmpty()) {
                holder.textViewRequisiteDescription.setVisibility(View.GONE);
            } else {
                holder.textViewRequisiteDescription.setVisibility(View.VISIBLE);
                holder.textViewRequisiteDescription.setText(thisRequisite.description);
            }

            if ((thisRequisite.value == null) || (thisRequisite.value.isEmpty())){
                holder.editTextRequisite.setHint(thisRequisite.hint);
            } else {
                holder.editTextRequisite.setText(thisRequisite.value);
            }

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

            if (thisRequisite.getPossibleValues() != null) {
                holder.editTextRequisite.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow, 0);
                holder.editTextRequisite.setInputType(0); //disables editing
                holder.editTextRequisite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ComplainActivity.this, PossibleValuesActivity.class);
                        intent.putExtra(Globals.POSSIBLE_VALUES, thisRequisite.getPossibleValues());
                        intent.putExtra(Globals.POSSIBLE_VALUES_HEADER, thisRequisite.getName());
                        intent.putExtra(Globals.VIOLATION_TYPE, violation.getType());
                        intent.putExtra(Globals.REQUISITE_NUMBER_FOR_POSSIBLE_VALUES, requisites.indexOf(thisRequisite));
                        startActivityForResult(intent, REQUEST_CODE_SELECT_POSSIBLE_VALUE);
                    }
                });
            }

            requisiteViews.add(holder);
            requisitesList.addView(v);
        }
    }

    void makeEvidenceAdapterContent(ArrayList<String> fileNames){
        if (!fileNames.isEmpty()) { //this can be true only in MODE_CREATE | MODE_EDIT
            for (String evidenceFileName : fileNames) try {
                Bitmap thumbnail = MediaUtils.getThumbnail(evidenceFileName);
                evidenceAdapter.content.add(evidenceFileName);
                evidenceAdapter.mediaContent.add(thumbnail);
            } catch (IOException e) {
                Globals.showError(this, R.string.error, e);
            }
        } else if (mode == MODE_EDIT) {
            SQLiteDatabase _db = dbHelper.getReadableDatabase();
            //query to media table
            String table = DBHelper.COMPLAINS_TABLE + " INNER JOIN " + DBHelper.COMPLAINS_MEDIA_TABLE + " ON "
                    + DBHelper.COMPLAINS_TABLE + "._id = " + DBHelper.COMPLAINS_MEDIA_TABLE + ".id";
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
                } catch (Exception e) {//we read files so need to catch exceptions
                    Globals.showError(this, R.string.error, e);
                } while (_cursor.moveToNext());
            }
            _cursor.close();
            _db.close();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK &&
                (requestCode == CameraManager.GENERIC_CAMERA_CAPTURE_INTENT)) { //(requestCode == CameraManager.IMAGE_CAPTURE_INTENT || requestCode == CameraManager.VIDEO_CAPTURE_INTENT)) {
            try {
                Bitmap bmp;
                CameraManager.setLastCapturedFile(intent.getStringExtra(eu.aejis.mycustomcamera.IntentExtras.MEDIA_FILE)); //new line
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
                evidenceAdapter.content.add(CameraManager.lastCapturedFile);
                evidenceAdapter.mediaContent.add(bmp);
                evidenceAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Globals.showError(this, R.string.error, e);
            }
        }

        if (requestCode == REQUEST_CHECK_SETTINGS){
            if (googleApiClient != null && googleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            }
        }

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_POSSIBLE_VALUE && intent != null) {
            String newValue = intent.getStringExtra(Globals.POSSIBLE_VALUES);
            int targetViewNumber = intent.getIntExtra(Globals.REQUISITE_NUMBER_FOR_POSSIBLE_VALUES, 0);
            if (requisitesList.getChildCount() >= targetViewNumber) {
                TextView targetView = (TextView) requisitesList.getChildAt(targetViewNumber).findViewById(R.id.editTextRequisite);
                if (targetView != null) targetView.setText(newValue);
            }
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

    public void saveToBase(View view) throws Exception {
                /*first check if location is available - show error dialog if not
        this is made to prevent saving request with zero LatLng -> leads to crash.
         */
        if (checkLocationIfRequired()) {
            //db actions
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();

            cv.put("type", violation.type);
            //cv.put("status", status);
            //cv.put(DBHelper.ID_SERVER,  violation.getId());
            cv.put(DBHelper.USER_ID,    Globals.user.id);
            cv.put(DBHelper.LONGITUDE,  longitude == null ? EMPTY_LOCATION_STUB : longitude);
            cv.put(DBHelper.LATITUDE,   latitude  == null ? EMPTY_LOCATION_STUB : latitude);

            for (int i = 0; i < requisites.size(); i++) {
                ViolationRequisite thisRequisite = requisites.get(i);
                cv.put(thisRequisite.dbTag, (requisiteViews.get(i).editTextRequisite.getText().toString()));
            }
            switch (mode) {
                case MODE_CREATE: {
                    time_stamp = new SimpleDateFormat(RequestListAdapter.INPUT_DATE_FORMAT, Locale.US)
                            .format(new Date());
                    cv.put(DBHelper.TIME_STAMP, time_stamp);
                    rowID = db.insertOrThrow(DBHelper.COMPLAINS_TABLE, null, cv);
                    id = new BigDecimal(rowID).intValue();
                    idInDbString = rowID.toString();
                    mode = MODE_EDIT; //once created the record we switch to edit mode
                    break;
                }
                case MODE_EDIT :{
                    cv.put(DBHelper._ID, id);
                    cv.put(DBHelper.TIME_STAMP, time_stamp);
                    rowID = db.replace(DBHelper.COMPLAINS_TABLE, null, cv);
                    break;
                }
            }
            cv.clear();

            /*actions with the media table: if in edit mode - we delete all previously written records
             *& fill the table again, because the user could add or delete something
             */
            if (mode == MODE_EDIT) {//this condition works only in edit mode - delete all records previously added to db
                db.delete(DBHelper.COMPLAINS_MEDIA_TABLE, "id = ?", new String[]{idInDbString});
            }
            for (int i = 0; i < evidenceAdapter.content.size(); i++) {
                cv.put(DBHelper.ID, rowID);
                cv.put(DBHelper.FILE_NAME, evidenceAdapter.content.get(i));
                db.insert(DBHelper.COMPLAINS_MEDIA_TABLE, null, cv);
                cv.clear();
            }
            db.close();

            //show toast only if the method is called by Save button
            if (view != null ) Toast.makeText(this, R.string.requestSaved, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.cannot_define_location, Toast.LENGTH_LONG).show();
        }
    }

    boolean checkLocationIfRequired() {
        return evidenceAdapter.isEmpty() || (checkLocation() && !blockButtons);
    }

    boolean checkLocation(){
        if (latitude == null){
            /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setTitle(R.string.cannot_define_location).setNegativeButton(R.string.ok, null).create();
            dialog.show();*/
            return false;
        } else return true;
    }

    //new method in customCamera - don't forget to set onClick to it in activity_complain.xml
    public void launchCamera(View view) {
        int actionFlag = (violation.getMediaTypes() == Violation.VIDEO_ONLY) ?
                CameraManager.VIDEO_CAPTURE_INTENT :
                0; //0 means photoOrVideo not defined - user switches this in the camera
        CameraManager.getInstance(this).startCustomCamera(actionFlag, false, videoOnly);
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
        if (!evidenceAdapter.isEmpty() && blockButtons) {
            Toast.makeText(this, R.string.cannot_define_location, Toast.LENGTH_LONG).show();
        } else {
            try {
                if (!evidenceAdapter.sizeCheck()) {
                    Toast.makeText(this, R.string.request_too_big, Toast.LENGTH_LONG).show();
                    return;
                }
                //ComplainDraftsFragment.punishPerformed = true;
                saveToBase(null); //we pass null to point that it's called from punish()
                new ComplainSender(this).execute(violation);
            } catch (Exception e) {
                Globals.showError(this, R.string.error, e);
            }
        }
    }

    public void empty(View view) {
        //empty method to handle click events
    }

    @Override
    public void validateSaveButton(){
        //saveButton.setEnabled(!evidenceAdapter.isEmpty());
    }

    @Override
    public void validatePunishButton(){
        punishButton.setEnabled(allDataEntered());
    }

    public boolean allDataEntered(){
        //boolean result = !evidenceAdapter.isEmpty();
        boolean result = true;

        int i=0;
        while (i < requisiteViews.size() && result){
            Editable text = requisiteViews.get(i).editTextRequisite.getText();
            if (requisites.get(i).necessary) result = (text != null) && (!text.toString().isEmpty());
            i++;
        }

        return result;
    }

    void initOldAndroidLocation(){
        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationListener = new MyOldAndroidLocationListener();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    private class MyOldAndroidLocationListener implements android.location.LocationListener{
        @Override
        public void onLocationChanged(Location location) {
            if (latitude == null || latitude == 0) {//use this only if no result from FusedLocationAPI

                if (((KaratelApplication)getApplicationContext()).locationIsMock(location)){
                    if (!mockLocationDialogShown) {
                        mockLocationDialogShown = true;
                        AlertDialog.Builder builder = new AlertDialog.Builder(ComplainActivity.this);
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
                        blockButtons = false;
                    }
                }
            }
            locationManager.removeUpdates(locationListener);
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
                //.addApi(Places.GEO_DATA_API)
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
            PendingResult<LocationSettingsResult> result = getPendingLocationSettings();

            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can initialize location requests here.
                            if (googleApiClient != null && googleApiClient.isConnected()) {
                                LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,
                                        locationRequest, ComplainActivity.this);
                            }
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(ComplainActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            Toast.makeText(ComplainActivity.this,  "location settings not available", Toast.LENGTH_LONG).show();
                            ComplainActivity.this.finish();
                            break;
                    }
                }
            });
        }
    }

    @Override
    public void onConnectionSuspended(int i) {    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e("Punisher", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onLocationChanged(final Location location) {
        if (mode == MODE_CREATE && !((KaratelApplication)getApplicationContext()).locationIsMock(location)){
            Double latToCheck = latitude!=null ? latitude : 0;
            Double lonToCheck = longitude!=null ? longitude : 0;
            Double absLat = Math.abs(latToCheck - location.getLatitude());
            Double absLon = Math.abs(lonToCheck - location.getLongitude());
            if (absLat > REFRESH_ACCURACY || absLon > REFRESH_ACCURACY) {
                PendingResult<LocationSettingsResult> result = getPendingLocationSettings();

                result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                    @Override
                    public void onResult(LocationSettingsResult result) {
                        if (result.getStatus().getStatusCode() == LocationSettingsStatusCodes.SUCCESS) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            blockButtons = false;
                        } else {
                            latitude = null; //this will block the buttons
                            blockButtons = true;
                        }
                    }
                });
            }
        }
    }

    PendingResult<LocationSettingsResult> getPendingLocationSettings(){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        return result;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //saving values entered into EditText requisites
        ArrayList<String> valuesToSave = new ArrayList<>();
        for (RequisitesListAdapter.ViewHolder holder : requisiteViews) {
            valuesToSave.add(holder.editTextRequisite.getText().toString());
        }
        outState.putStringArrayList(Globals.REQUISITES_VALUES, valuesToSave);

        //saving evidence filenames & data about deleted files
        if (mode <= MODE_EDIT) {
            ArrayList<String> filenamesToSave = new ArrayList<>(evidenceAdapter.content);
            outState.putStringArrayList(Globals.EVIDENCES, filenamesToSave);

            /*ArrayList<String> deletedFilenamesToSave = new ArrayList<>(evidenceAdapter.filesDeletedDuringSession);
            outState.putStringArrayList(Globals.DELETED_EVIDENCES, deletedFilenamesToSave);*/
        }

        //saving mode & intent related data
        //otherwise started with MODE_CREATE and saved to base (mode=MODE_EDIT) will be recreated as MODE_CREATE again
        outState.putInt(Globals.VIOLATION_ACTIVITY_MODE, mode);
        if (mode == MODE_EDIT) outState.putInt(Globals.ITEM_ID, id);

        //this is needed in onDestroy to distinguish whether it was initiated by user or by system
        saveInstanceStateCalled = true;

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
        if (!saveInstanceStateCalled) clearEvidences(); //just destroy without saving state, e.g. on back pressed

        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
            //locationListener = null;
        }

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

    private class ComplainSender extends AsyncTask<Violation, Void, ComplainCreationResponse> {
        Context context;

        ComplainSender(Context context){
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            punishButton.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            Globals.hideSoftKeyboard(ComplainActivity.this);
        }

        @Override
        protected ComplainCreationResponse doInBackground(Violation... params) {
            if (!HttpHelper.internetConnected(context)) return null;

            Violation violation = params[0];
            ComplainCreationResponse result;

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //query to data table
            String table = DBHelper.COMPLAINS_TABLE;
            String[] columns = null;
            String where = "_id=?";
            idInDbString = new BigDecimal(rowID).toString();
            String[] selectionArgs = {idInDbString};
            cursor = db.query(table, columns, where, selectionArgs, null, null, null);
            cursor.moveToFirst();
            Double latitude  = cursor.getDouble(cursor.getColumnIndex(DBHelper.LATITUDE));
            Double longitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.LONGITUDE));
            Map<String, String> dbRowData = new HashMap<>();
            for (ViolationRequisite requisite : requisites) {
                String columnName = requisite.name;
                String columnValue = cursor.getString(cursor.getColumnIndex(requisite.dbTag));
                dbRowData.put(columnName, columnValue == null ? "" : columnValue);
            }
            cursor.close();
            db.close();

            try {
                String typeServerSuffix = "companies/" + violation.getId() + "/grievances";

                //prepare the request parameters
                ArrayList<String> requestParameters = new ArrayList<>();
                String[] keysForRequestParameters = violation.getRequisitesString();
                for (String str : keysForRequestParameters){
                    requestParameters.add(str);
                    switch (str) {
                        case "user_id" : {
                            requestParameters.add(Globals.user.id.toString()); break;
                        }
                        case "company_id" : {
                            requestParameters.add("" + violation.getId()); break;
                        }
                        case "longitude" : {
                            if (longitude == EMPTY_LOCATION_STUB) {
                                //remove the word "longitude"
                                requestParameters.remove(requestParameters.size() - 1);
                            } else {
                                requestParameters.add(longitude.toString());
                            }
                            break;
                        }
                        case "latitude" : {
                            if (latitude == EMPTY_LOCATION_STUB) {
                                //remove the word "latitude"
                                requestParameters.remove(requestParameters.size() - 1);
                            } else {
                                requestParameters.add(latitude.toString());
                            }
                            break;
                        }
                        case "description" : {
                            requestParameters.add(DescriptionFormatter.format(dbRowData));
                            break;
                        }
                        case "creation_date" : {
                            requestParameters.add(time_stamp);
                            break;
                        }
                    }
                }
                String[] requestParametersArray = requestParameters.toArray(new String[0]);

                //Retrofit request
                MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
                int i = 0;
                while (i < requestParametersArray.length) {
                    requestBodyBuilder.addFormDataPart("grievance[" + requestParametersArray[i++] + "]", requestParametersArray[i++]);
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
                    requestBodyBuilder.addFormDataPart("grievance[" + requestTag + "][]", mediaFileName,
                            RequestBody.create(MediaType.parse(mimeType), new File(mediaFileName)));
                }

                RetrofitMultipartUploader api = KaratelApplication.getClient(2).create(RetrofitMultipartUploader.class);
                Call<ComplainCreationResponse> call = api.uploadGrievance(Globals.sessionToken,
                        typeServerSuffix, requestBodyBuilder.build());
                Response<ComplainCreationResponse> json = call.execute();
                if (json.isSuccessful()) {
                    result = json.body();
                } else {
                    ResponseBody errorBody = json.errorBody();
                    result = new ObjectMapper().readValue(errorBody.string(), ComplainCreationResponse.class);
                    errorBody.close();
                }
            } catch (final IOException e){
                ComplainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(ComplainActivity.this, R.string.cannot_connect_server, e);
                    }
                });
                return null;
            }
            return result;
        }

        @Override
        protected void onPostExecute(ComplainCreationResponse answer) {
            super.onPostExecute(answer);

            if (answer == null) {
                try {
                    answer = new ObjectMapper().readValue(HttpHelper.ERROR_JSON, ComplainCreationResponse.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            switch (answer.status) {
                case Globals.SERVER_SUCCESS: {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    DBHelper.deleteComplainRequest(db, id);
                    db.close();
                    finish();
                    break;
                }
                case Globals.SERVER_ERROR: {
                    Toast.makeText(context, answer.error, Toast.LENGTH_LONG).show();
                    break;
                }
            }

            if (progressBar  != null) progressBar.setVisibility(View.GONE);
            if (punishButton != null) punishButton.setEnabled(true);
        }
    }
}