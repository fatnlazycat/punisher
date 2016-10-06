package org.foundation101.karatel;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Dima on 07.05.2016.
 */
public class CameraManager {

    public static final int IMAGE_CAPTURE_INTENT = 100;
    public static final int VIDEO_CAPTURE_INTENT = 200;
    private static final String FILE_NAME_PREFIX = "punisher_";
    public static final String JPG = ".jpg";
    public static final String PNG = ".png";
    public static final String MP4 = ".mp4";
    public static final String MOV = ".mov";
    public static String lastCapturedFile;

    private Activity context;

    private CameraManager(Activity context){
        this.context = context;
    }

    public static CameraManager getInstance(Activity context){
        return new CameraManager(context);
    }

    public void startCamera(int photoOrVideo){
        String actionFlag=MediaStore.ACTION_VIDEO_CAPTURE; //capture video by default
        if (photoOrVideo==IMAGE_CAPTURE_INTENT){ //capture photo
            actionFlag=MediaStore.ACTION_IMAGE_CAPTURE;
        }
        Intent cameraIntent = new Intent(actionFlag);
        Uri mediaFileUri = getMediaFileUri(actionFlag);
        if (mediaFileUri != null) {
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mediaFileUri);
            lastCapturedFile = mediaFileUri.getPath();
            context.startActivityForResult(cameraIntent, photoOrVideo);
        } else {
            Toast.makeText(context, R.string.cannot_write_file, Toast.LENGTH_LONG).show();
        }
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
}
