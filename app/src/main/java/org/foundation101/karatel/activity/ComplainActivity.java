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
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;

import org.codehaus.jackson.map.ObjectMapper;
import org.foundation101.karatel.entity.EvidenceEntity;
import org.foundation101.karatel.manager.CameraManager;
import org.foundation101.karatel.manager.GoogleApiManager;
import org.foundation101.karatel.manager.KaratelLocationManager;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.entity.ComplainCreationResponse;
import org.foundation101.karatel.manager.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.EvidenceAdapter;
import org.foundation101.karatel.adapter.RequestListAdapter;
import org.foundation101.karatel.adapter.RequisitesListAdapter;
import org.foundation101.karatel.entity.ComplainRequest;
import org.foundation101.karatel.entity.Violation;
import org.foundation101.karatel.entity.ViolationRequisite;
import org.foundation101.karatel.manager.PermissionManager;
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

import static org.foundation101.karatel.manager.KaratelLocationManager.*;
import static org.foundation101.karatel.manager.PermissionManager.CUSTOM_CAMERA_PERMISSIONS_START_IMMEDIATELY;
import static org.foundation101.karatel.manager.PermissionManager.CUSTOM_CAMERA_PERMISSIONS_START_NORMAL;
import static org.foundation101.karatel.manager.PermissionManager.LOCATION_PERMISSIONS;

public class ComplainActivity extends AppCompatActivity implements Formular {

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

    public boolean blockButtons = true;//used to check if the location is defined

    ArrayList<EvidenceEntity> savedInstanceStateEvidenceFileNames = new ArrayList<>();
    public ComplainRequest request = null;
    Integer id;
    String idInDbString, time_stamp;
    Violation violation = new Violation();
    public double latitude = 0, longitude = 0;
    boolean saveInstanceStateCalled = false;
    //boolean needReclaimFullLocation = false;

    boolean changesMade = false;
    @Override
    public boolean changesMade() { return changesMade; }
    @Override
    public void setChangesMade(boolean value) { changesMade = value; }

    AlertDialog changesLostDialog;

    public FrameLayout progressBar;
    Button punishButton, saveButton;

    GoogleApiManager googleApiManager;

    @Override
    public GoogleApiManager getGoogleManager() {
        return googleApiManager;
    }

    static final int REQUEST_CHECK_SETTINGS = 2000;

    //variable for customCamera
    boolean videoOnly = false;

    KaratelLocationManager lManager;

    @Override
    protected void onStart() {
        super.onStart();
        lManager.onStart();
        googleApiManager.onStart();

        validatePunishButton();
        validateSaveButton();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            //init requisite EditTexts after activity recreation otherwise they all contain the same value (value of the last one)
            ArrayList<String> savedValues = savedInstanceState.getStringArrayList(Globals.REQUISITES_VALUES);
            if (savedValues != null) {
                int listSize = requisiteViews.size();
                for (int i = 0; i < listSize; i++) {
                    requisiteViews.get(i).editTextRequisite.setText(savedValues.get(i));
                }
            }

            changesMade = savedInstanceState.getBoolean(Globals.VIOLATION_ACTIVITY_CHANGES_MADE, false);
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


        Intent intent = this.getIntent();
        mode = intent.getIntExtra(Globals.VIOLATION_ACTIVITY_MODE, MODE_EDIT);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey  (Globals.VIOLATION_ACTIVITY_MODE)) {
                mode = savedInstanceState.getInt(Globals.VIOLATION_ACTIVITY_MODE);
            }

            //init evidences after activity recreation otherwise they will be lost
            if (savedInstanceState.containsKey(Globals.EVIDENCES)) { //this will be true only in MODE_CREATE | MODE_EDIT
                savedInstanceStateEvidenceFileNames = (ArrayList<EvidenceEntity>) savedInstanceState.getSerializable(Globals.EVIDENCES);
            }
        }

        requisitesList = (LinearLayout) findViewById(R.id.requisitesList);
        llAddEvidence = (LinearLayout) findViewById(R.id.llAddEvidence);

        TextView addedPhotoVideoTextView = (TextView)findViewById(R.id.addedPhotoVideoTextView);
        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);
        punishButton = (Button) findViewById(R.id.punishButton);
        saveButton = (Button) findViewById(R.id.saveButton);

        if (mode <= MODE_EDIT) {
            lManager = new KaratelLocationManager(this);
            googleApiManager = new GoogleApiManager(this);
            googleApiManager.init(lManager, lManager);
            lManager.onCreate();
        }

        KaratelPreferences.restoreUser();

        dbHelper = new DBHelper(this, DBHelper.DATABASE, DBHelper.DB_VERSION);

        if (mode == MODE_CREATE) {
            violation = (Violation) intent.getExtras().getSerializable(Globals.VIOLATION);
            requisites = violation.getRequisites();
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
                String table = DBHelper.COMPLAINS_TABLE;
                String[] columns = null;
                String where = "_id=?";
                String[] selectionArgs = {idInDbString};
                cursor = db.query(table, columns, where, selectionArgs, null, null, null);
                if (cursor.moveToFirst()) {
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

            requisiteViews.add(holder);
            requisitesList.addView(v);
        }
    }

    void makeEvidenceAdapterContent(ArrayList<EvidenceEntity> savedEvidences){
        if (!savedEvidences.isEmpty()) { //this can be true only in MODE_CREATE | MODE_EDIT
            for (EvidenceEntity evidence : savedEvidences) try {
                Bitmap thumbnail = MediaUtils.getThumbnail(evidence.fileName);
                evidenceAdapter.content.add(evidence);
                evidenceAdapter.mediaContent.add(thumbnail);
            } catch (IOException e) {
                Globals.showError(R.string.error, e);
            }
        } else if (mode == MODE_EDIT) {
            SQLiteDatabase _db = dbHelper.getReadableDatabase();
            //query to media table
            String table = DBHelper.COMPLAINS_TABLE + " INNER JOIN " + DBHelper.COMPLAINS_MEDIA_TABLE + " ON "
                    + DBHelper.COMPLAINS_TABLE + "._id = " + DBHelper.COMPLAINS_MEDIA_TABLE + ".id";
            String[] columns = null;//new String[]{DBHelper.ID, DBHelper.FILE_NAME};
            String where = "id=?";
            String[] selectionArgs = {idInDbString};
            Cursor _cursor = _db.query(table, columns, where, selectionArgs, null, null, null);
            if (_cursor.moveToFirst()) {
                do try {
                    String evidenceFileName = _cursor.getString(_cursor.getColumnIndex(DBHelper.FILE_NAME));
                    double evidenceLatitude  = _cursor.getDouble(_cursor.getColumnIndex(DBHelper.LATITUDE));
                    double evidenceLongitude = _cursor.getDouble(_cursor.getColumnIndex(DBHelper.LONGITUDE));
                    EvidenceEntity evidence  = new EvidenceEntity(evidenceFileName, evidenceLatitude, evidenceLongitude);

                    Bitmap thumbnail = MediaUtils.getThumbnail(evidenceFileName);

                    evidenceAdapter.content.add(evidence);
                    evidenceAdapter.mediaContent.add(thumbnail);
                } catch (Exception e) {//we read files so need to catch exceptions
                    Globals.showError(R.string.error, e);
                } while (_cursor.moveToNext());
            }
            _cursor.close();
            _db.close();
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

                setChangesMade(true);

                evidenceAdapter.content.add(new EvidenceEntity(CameraManager.lastCapturedFile, 0, 0));
                evidenceAdapter.mediaContent.add(bmp);
                evidenceAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Globals.showError(R.string.error, e);
            }
        }

        if (requestCode == REQUEST_CHECK_SETTINGS){
            lManager.onSettingsResult();
        }

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_POSSIBLE_VALUE && intent != null) {
            setChangesMade(true);
            String newValue = intent.getStringExtra(Globals.POSSIBLE_VALUES);
            int targetViewNumber = intent.getIntExtra(Globals.REQUISITE_NUMBER_FOR_POSSIBLE_VALUES, 0);
            if (requisitesList.getChildCount() >= targetViewNumber) {
                TextView targetView = requisitesList.getChildAt(targetViewNumber).findViewById(R.id.editTextRequisite);
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

    public void showChangesLostDialog() {
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
            /*cv.put(DBHelper.LONGITUDE,  longitude == null ? EMPTY_LOCATION_STUB : longitude);
            cv.put(DBHelper.LATITUDE,   latitude  == null ? EMPTY_LOCATION_STUB : latitude);*/

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
                EvidenceEntity evidence = evidenceAdapter.content.get(i);
                cv.put(DBHelper.ID, rowID);
                cv.put(DBHelper.FILE_NAME, evidence.fileName);

                if (evidence.latitude  == 0) evidence.latitude  = latitude;
                if (evidence.longitude == 0) evidence.longitude = longitude;

                cv.put(DBHelper.LONGITUDE, evidence.longitude);
                cv.put(DBHelper.LATITUDE,  evidence.latitude);

                db.insert(DBHelper.COMPLAINS_MEDIA_TABLE, null, cv);
                cv.clear();
            }
            db.close();

            setChangesMade(false);

            //show toast only if the method is called by Save button
            if (view != null ) {
                Toast.makeText(this, R.string.complainSaved, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            String message = getString(R.string.cannot_define_location);
            Globals.showMessage(message);
        }
    }

    boolean checkLocationIfRequired() {
        return evidenceAdapter.isEmpty() || (checkLocation() && !blockButtons);
    }

    boolean checkLocation(){
        if (latitude == 0){
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
                Globals.showError(R.string.error, e);
            }
        }
    }

    public void empty(View view) {
        //empty method to handle click events
    }

    //@Override
    public void validateSaveButton(){
        //saveButton.setEnabled(!evidenceAdapter.isEmpty());
    }

    //@Override
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

    @Override
    public void onLocationChanged(double lat, double lon) {
        latitude = lat;
        longitude = lon;
        blockButtons = false;
    }

    @Override
    public void onAddressesReady(PlaceLikelihoodBuffer places) { }

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
            ArrayList<EvidenceEntity> filenamesToSave = new ArrayList<>(evidenceAdapter.content);
            outState.putSerializable(Globals.EVIDENCES, filenamesToSave);
        }

        //saving mode & intent related data
        //otherwise started with MODE_CREATE and saved to base (mode=MODE_EDIT) will be recreated as MODE_CREATE again
        outState.putInt(Globals.VIOLATION_ACTIVITY_MODE, mode);
        if (mode == MODE_EDIT) outState.putInt(Globals.ITEM_ID, id);
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

        if (violation != null) violation.clearValues();

        super.onDestroy();
    }

    void clearEvidences() {
        DBUtils.clearEvidences(dbHelper);
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
            if (!HttpHelper.internetConnected(/*context*/)) return null;

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
            Double latitude  = evidenceAdapter.isEmpty() ? EMPTY_LOCATION_STUB : evidenceAdapter.content.get(0).latitude;
            Double longitude = evidenceAdapter.isEmpty() ? EMPTY_LOCATION_STUB : evidenceAdapter.content.get(0).longitude;
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
                            requestParameters.add(longitude.toString());
                            /*if (longitude == EMPTY_LOCATION_STUB) {
                                //remove the word "longitude"
                                requestParameters.remove(requestParameters.size() - 1);
                            } else {
                                requestParameters.add(longitude.toString());
                            }*/
                            break;
                        }
                        case "latitude" : {
                            requestParameters.add(latitude.toString());
                            /*if (latitude == EMPTY_LOCATION_STUB) {
                                //remove the word "latitude"
                                requestParameters.remove(requestParameters.size() - 1);
                            } else {
                                requestParameters.add(latitude.toString());
                            }*/
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
                for (EvidenceEntity evidenceEntity : evidenceAdapter.content) {
                    String requestTag, mimeType;
                    if (evidenceEntity.fileName.endsWith(CameraManager.JPG)){
                        requestTag = "images";
                        mimeType = "image/jpeg";
                    } else {
                        requestTag = "videos";
                        mimeType = "video/mp4";
                    }
                    requestBodyBuilder.addFormDataPart("grievance[" + requestTag + "][]", evidenceEntity.fileName,
                            RequestBody.create(MediaType.parse(mimeType), new File(evidenceEntity.fileName)));
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
                Globals.showError(R.string.cannot_connect_server, e);
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
                    Toast.makeText(context, answer.error.toString(), Toast.LENGTH_LONG).show();
                    break;
                }
            }

            if (progressBar  != null) progressBar.setVisibility(View.GONE);
            if (punishButton != null) punishButton.setEnabled(true);
        }
    }
}