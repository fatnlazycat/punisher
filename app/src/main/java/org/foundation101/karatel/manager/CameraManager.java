package org.foundation101.karatel.manager;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

//import org.foundation101.karatel.activity.CameraActivity;

import com.splunk.mint.Mint;

import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Dima on 07.05.2016.
 */
public class CameraManager {

    public static final int IMAGE_CAPTURE_INTENT = 100;
    public static final int VIDEO_CAPTURE_INTENT = 200;
    public static final int GENERIC_CAMERA_CAPTURE_INTENT = 300;
    private static final String FILE_NAME_PREFIX = "punisher_";
    //private static final String DEFAULT_CAMERA_PACKAGE = "com.google.android.camera";
    public static final String JPG = ".jpg";
    public static final String PNG = ".png";
    public static final String MP4 = ".mp4";
    public static final String MOV = ".mov";

    public static final String FILENAME_PATTERN = "(" + FILE_NAME_PREFIX + ".+)"
            + "(\\" + JPG
            + "|\\" + PNG
            + "|\\" + MP4
            + "|\\" + MOV + ")";

    public static String lastCapturedFile;
    public static void setLastCapturedFile(String lastCapturedFile) {
        CameraManager.lastCapturedFile = lastCapturedFile;
    }

    private Activity context;

    private CameraManager(Activity context){
        this.context = context;
    }

    public static CameraManager getInstance(Activity context){
        return new CameraManager(context);
    }

    public void startCamera(int photoOrVideo){
        String actionFlag = MediaStore.ACTION_VIDEO_CAPTURE; //capture video by default
        if (photoOrVideo == IMAGE_CAPTURE_INTENT){ //capture photo
            actionFlag = MediaStore.ACTION_IMAGE_CAPTURE;
        }
        Intent cameraIntent = makeCameraIntent(actionFlag);
        Uri mediaFileUri = getMediaFileUri(actionFlag);
        if (mediaFileUri != null) {
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mediaFileUri);
            lastCapturedFile = mediaFileUri.getPath();
            context.startActivityForResult(cameraIntent, photoOrVideo);
        } else {
            Toast.makeText(KaratelApplication.getInstance(), R.string.cannot_write_file, Toast.LENGTH_LONG).show();
        }
    }

    public void startCustomCamera(int photoOrVideo, boolean startImmediately, boolean noSwitch) {
        Mint.leaveBreadcrumb("startCustomCamera from " + context + " startImmediately=" + startImmediately);
        String actionFlag = "";
        switch (photoOrVideo) {
            case IMAGE_CAPTURE_INTENT : {
                actionFlag = MediaStore.ACTION_IMAGE_CAPTURE; break;
            }
            case VIDEO_CAPTURE_INTENT : {
                actionFlag = MediaStore.ACTION_VIDEO_CAPTURE; break;
            }
        }

        Uri mediaFileUri = getMediaFileUriWithoutExtension();
        if (mediaFileUri != null) {
            Intent cameraIntent = new Intent(
                    actionFlag,
                    mediaFileUri/*,
                    context,
                    CameraActivity.class*/
            );
            cameraIntent.setClassName(context.getPackageName(), "eu.aejis.mycustomcamera.CameraActivity");

            if (startImmediately)   cameraIntent.putExtra(eu.aejis.mycustomcamera.IntentExtras.START_IMMEDIATELY, true);
            if (noSwitch)           cameraIntent.putExtra(eu.aejis.mycustomcamera.IntentExtras.NO_SWITCH, true);
            //lastCapturedFile = mediaFileUri.getPath();
            context.startActivityForResult(cameraIntent, GENERIC_CAMERA_CAPTURE_INTENT);
        } else {
            Toast.makeText(KaratelApplication.getInstance(), R.string.cannot_write_file, Toast.LENGTH_LONG).show();
        }
    }

    private Uri getMediaFileUriWithoutExtension(){
        Uri mediaFileUri = null;
        String fileName = "yet empty";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                File appPrivateDir = context.getExternalFilesDir(null);
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                fileName = appPrivateDir + File.separator + FILE_NAME_PREFIX + timeStamp;
                File mediaFile = new File(fileName);
                mediaFileUri = Uri.fromFile(mediaFile);
            } catch (Exception e){
                return null;
            }
        }
        return mediaFileUri;
    }

    private Uri getMediaFileUri(String mediaType){
        Uri mediaFileUri = null;
        String fileName = "yet empty";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            try {
                File appPrivateDir = context.getExternalFilesDir(null);
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String extension = mediaType.equals(MediaStore.ACTION_IMAGE_CAPTURE) ? JPG : MP4;
                fileName = appPrivateDir + File.separator + FILE_NAME_PREFIX + timeStamp + extension;
                File mediaFile = new File(fileName);
                mediaFileUri = Uri.fromFile(mediaFile);
            } catch (Exception e){
                return null;
            }
        }
        return mediaFileUri;
    }

    private Intent makeCameraIntent(String action) {
        Intent cameraIntent = new Intent(action);
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(cameraIntent, 0);
        cameraIntent.setPackage(listCam.get(0).activityInfo.packageName);
        //cameraIntent.setPackage(DEFAULT_CAMERA_PACKAGE);
        return cameraIntent;
    }
}
