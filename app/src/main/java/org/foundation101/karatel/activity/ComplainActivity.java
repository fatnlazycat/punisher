package org.foundation101.karatel.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
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
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.entity.CreationResponse;
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

public class ComplainActivity extends AppCompatActivity implements Formular {

    final int RQS_GooglePlayServices = 1000;

    public static final int MODE_CREATE = -1;
    public static final int MODE_EDIT = 0;
    public static final String TAG = "ComplainActivity";

    ArrayList<ViolationRequisite> requisites;
    ArrayList<RequisitesListAdapter.ViewHolder> requisiteViews = new ArrayList<>();
    LinearLayout requisitesList, llAddEvidence;
    EvidenceAdapter evidenceAdapter = new EvidenceAdapter(this);
    DBHelper dbHelper;
    Cursor cursor;
    int mode, thumbDimension;
    Long rowID;

    final Double REFRESH_ACCURACY = 0.001;
    boolean mockLocationDialogShown = false;
    public boolean blockButtons = true;//used to check if the location is defined

    public int getMode() {
        return mode;
    }

    public ComplainRequest request = null;
    Integer id, status, idOnServer;
    String idInDbString, time_stamp;
    String id_number_server = "";
    Violation violation = new Violation();
    public Double latitude, longitude;

    public FrameLayout progressBar;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complain);

        requisitesList = (LinearLayout) findViewById(R.id.requisitesList);
        llAddEvidence = (LinearLayout) findViewById(R.id.llAddEvidence);

        TextView addedPhotoVideoTextView = (TextView)findViewById(R.id.addedPhotoVideoTextView);
        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);
        punishButton = (Button) findViewById(R.id.punishButton);
        saveButton = (Button) findViewById(R.id.saveButton);

        ((KaratelApplication)getApplication()).restoreUserFromPreferences();

        //dimension of the thumbnail - the thumbnail should have the same size as MediaStore.Video.Thumbnails.MICRO_KIND
        thumbDimension = getResources().getDimensionPixelOffset(R.dimen.thumbnail_size);

        dbHelper = new DBHelper(this, DBHelper.DATABASE, 1);

        Intent intent = this.getIntent();
        mode = intent.getIntExtra(Globals.VIOLATION_ACTIVITY_MODE, MODE_EDIT);
        if (mode == MODE_CREATE) {
            idOnServer = 0;
            violation = (Violation) intent.getExtras().getSerializable(Globals.VIOLATION);
            status = 0; //status = draft
            requisites = RequisitesListAdapter.makeContent(violation.type);
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
                String table = DBHelper.COMPLAINS_TABLE;
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
                String type = cursor.getString(cursor.getColumnIndex("type"));
                violation = Violation.getByType(this, type);
                requisites = RequisitesListAdapter.makeContent(violation.type);
                for (ViolationRequisite oneRequisite : requisites) {
                    oneRequisite.value = cursor.getString(cursor.getColumnIndex(oneRequisite.dbTag));
                }
                cursor.close();
                db.close();
            } else {/*if neither create nor edit mode then it's loaded from server*/}
        }

        ExpandedGridView evidenceGridView = (ExpandedGridView) findViewById(R.id.evidenceGridView);
        makeEvidenceAdapterContent();
        evidenceGridView.setAdapter(evidenceAdapter);
        evidenceGridView.setEmptyView(findViewById(R.id.emptyView));
        evidenceGridView.setFocusable(false);

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);

        if (violation.getMediaTypes() == Violation.VIDEO_ONLY)
            addedPhotoVideoTextView.setText(getString(R.string.takeVideo));

        llAddEvidence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                photoVideoPopupMenu(v);
            }
        });

        makeRequisitesViews();

        //KaratelApplication.getInstance().sendScreenName(violation.type);
    }

    void makeRequisitesViews(){
        for (ViolationRequisite thisRequisite : requisites){
            RequisitesListAdapter.ViewHolder holder = new RequisitesListAdapter.ViewHolder();

            View v = LayoutInflater.from(this).inflate(R.layout.item_violation_requisite, requisitesList, false);

            holder.textViewRequisiteHeader      = (TextView)            v.findViewById(R.id.textViewRequisiteHeader);
            holder.textViewRequisiteDescription = (TextView)            v.findViewById(R.id.textViewRequisiteDescription);
            holder.editTextRequisite            = (AutoCompleteTextView)v.findViewById(R.id.editTextRequisite);

            holder.textViewRequisiteHeader.setText(thisRequisite.name);

            if (thisRequisite.description.isEmpty()) {
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

            requisiteViews.add(holder);
            requisitesList.addView(v);
        }
    }

    void makeEvidenceAdapterContent(){
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
                    int orientation = MediaUtils.getOrientation(evidenceFileName);
                    thumbnail = MediaUtils.rotateBitmap(
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK &&
                (requestCode == CameraManager.IMAGE_CAPTURE_INTENT || requestCode == CameraManager.VIDEO_CAPTURE_INTENT)) {
            try {
                Bitmap bmp;
                if (CameraManager.lastCapturedFile.endsWith(CameraManager.JPG)) {
                    int orientation = MediaUtils.getOrientation(CameraManager.lastCapturedFile);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    bmp = MediaUtils.rotateBitmap(
                            ThumbnailUtils.extractThumbnail(
                                    BitmapFactory.decodeFile(CameraManager.lastCapturedFile, options)
                                    , thumbDimension, thumbDimension
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
        try {
            if (!evidenceAdapter.sizeCheck()) {
                Toast.makeText(this, R.string.request_too_big, Toast.LENGTH_LONG).show();
                return;
            }
            ComplainDraftsFragment.punishPerformed = true;
            saveToBase(null); //we pass null to point that it's called from punish()
            new ComplainActivity.ViolationSender(this).execute(violation);
        } catch (Exception e) {
            Globals.showError(this, R.string.error, e);
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
        while (i < requisites.size() && result){
            Editable text = requisiteViews.get(i).editTextRequisite.getText();
            if (requisites.get(i).necessary) result = (text != null) && (!text.toString().isEmpty());
            i++;
        }

        return result;
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
            Globals.hideSoftKeyboard(ComplainActivity.this);
        }

        @Override
        protected CreationResponse doInBackground(Violation... params) {
            if (!HttpHelper.internetConnected(context)) return null;

            Violation violation = params[0];
            CreationResponse result;

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            //query to data table
            String table = DBHelper.COMPLAINS_TABLE;
            String[] columns = null;
            String where = "_id=?";
            idInDbString = new BigDecimal(rowID).toString();
            String[] selectionArgs = {idInDbString};
            cursor = db.query(table, columns, where, selectionArgs, null, null, null);
            cursor.moveToFirst();
            Double latitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.LATITUDE));
            Double longitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.LONGITUDE));
            Map<String, String> dbRowData = new HashMap<>();
            for (ViolationRequisite requisite : requisites) {
                String columnName = requisite.dbTag;
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
                String[] keysForRequestParameters = ViolationRequisite.getRequisites(ComplainActivity.this,
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
                Call<CreationResponse> call = api.upload(Globals.sessionToken, typeServerSuffix, requestBodyBuilder.build());
                Response<CreationResponse> json = call.execute();
                if (json.isSuccessful()) {
                    result = json.body();
                } else {
                    ResponseBody errorBody = json.errorBody();
                    result = new ObjectMapper().readValue(errorBody.string(), CreationResponse.class);
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
        protected void onPostExecute(CreationResponse answer) {
            super.onPostExecute(answer);

            if (answer == null) {
                try {
                    answer = new ObjectMapper().readValue(HttpHelper.ERROR_JSON, CreationResponse.class);
                    switch (answer.status) {
                        case Globals.SERVER_SUCCESS: {
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            DBHelper.deleteRequest(db, id);
                            db.close();
                            finish();
                            break;
                        }
                        case Globals.SERVER_ERROR: {
                            Toast.makeText(context, answer.error, Toast.LENGTH_LONG).show();
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (punishButton != null) punishButton.setEnabled(true);
        }
    }
}