package org.foundation101.karatel.utils;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;

import org.foundation101.karatel.manager.CameraManager;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Created by Dima on 02.06.2017.
 */

public class MediaUtils {
    //dimension of the thumbnail - the thumbnail should have the same size as MediaStore.Video.Thumbnails.MICRO_KIND
    public static final int THUMB_DIMENSION =
            KaratelApplication.getInstance().getResources().getDimensionPixelOffset(R.dimen.thumbnail_size);
    /*
a method from Android reference docs
*/
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        //now define if the resulting image is still bigger than the texture size
        int newWidth = width / inSampleSize;
        int newHeight = height / inSampleSize;
        int textureSize = getMaxTextureSize();
        if (Math.max(newWidth, newHeight) > textureSize) inSampleSize *= 2; //? - maybe we should use Math.min() here?

        return inSampleSize;
    }

    private static int getMaxTextureSize() {
        // Safe minimum default size
        final int IMAGE_MAX_BITMAP_DIMENSION = 128;

        // Get EGL Display
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        // Initialise
        int[] version = new int[2];
        egl.eglInitialize(display, version);

        // Query total number of configurations
        int[] totalConfigurations = new int[1];
        egl.eglGetConfigs(display, null, 0, totalConfigurations);

        // Query actual list configurations
        EGLConfig[] configurationsList = new EGLConfig[totalConfigurations[0]];
        egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations);

        int[] textureSize = new int[1];
        int maximumTextureSize = 0;

        // Iterate through all the configurations to located the maximum texture size
        for (int i = 0; i < totalConfigurations[0]; i++) {
            // Only need to check for width since opengl textures are always squared
            egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize);

            // Keep track of the maximum texture size
            if (maximumTextureSize < textureSize[0])
                maximumTextureSize = textureSize[0];
        }

        // Release
        egl.eglTerminate(display);

        // Return largest texture size found, or default
        return Math.max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION);
    }

    public static Point getDesiredSize(Activity activity) {
        Point point = new Point(100, 100); //there are no screens that are less than 100x100
        if (activity != null) {
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getSize(point);
        }
        return point;
    }

    public static File reduceFileDimensions(File f, String newFileName, Context ctx) {
        //Max image size from Bogdanovich
        final int BIGGER_SIDE_OF_IMAGE_MAX_SIZE = 1280;

        try {
            Point desiredSize = getImageSizeFromFile(f);

            int biggerSide = Math.max(desiredSize.x, desiredSize.y);
            float reduceRatio = Math.min(1, BIGGER_SIDE_OF_IMAGE_MAX_SIZE / (float) biggerSide);

            if (reduceRatio == 1) return f; //short quit to speed up

            int newX = (int)(desiredSize.x * reduceRatio);
            int newY = (int)(desiredSize.y * reduceRatio);

            desiredSize = new Point(newX, newY);
            Bitmap bitmap = getBitmapFromFile(f, desiredSize);
            Bitmap exactSizeBitmap = Bitmap.createScaledBitmap(bitmap, newX, newY, false);
            if (!bitmap.equals(exactSizeBitmap)) bitmap.recycle();
            int quality = 100;
            File reducedFile = getFileFromBitmap(exactSizeBitmap, quality, newFileName, ctx);
            exactSizeBitmap.recycle();

            //save orientation metadata
            int orientationTag = getOrientation(f.getPath());
            Log.d("MediaUtils", "from getReducedFile, orientation Tag = " + orientationTag);

            ExifInterface newExif = new ExifInterface(reducedFile.getPath());
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(orientationTag));
            newExif.saveAttributes();

            return reducedFile;
        } catch (OutOfMemoryError | IOException e) {return f;}
    }

    public static int getOrientation(String imageFileName) throws IOException {
        if (imageFileName == null) return ExifInterface.ORIENTATION_NORMAL;
        ExifInterface ei = new ExifInterface(imageFileName);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        return orientation;
    }

    public static int getOrientation(Context context, Uri photoUri) {
        /* it's on the external media. */
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor == null || cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        int result = cursor.getInt(0);
        cursor.close();
        return result;
    }

    static Point getImageSizeFromFile(File f) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), options);
        int width = options.outWidth;
        int height = options.outHeight;
        return new Point(width, height);
    }

    public static Bitmap getBitmapFromFile(File file, Point desiredSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        if (desiredSize != null) {
            //first run to determine image size
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), options);

            //calculate sample size
            options.inSampleSize = calculateInSampleSize(options, desiredSize.x, desiredSize.y);

            //second run to get bitmap
            options.inJustDecodeBounds = false;
        }
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        Bitmap result;
        try {
            result = BitmapFactory.decodeFile(file.getPath(), options);
        }
        catch (OutOfMemoryError e) {
            Log.d("OOME", "getBitmapFromFile, desiredSize=" + desiredSize);
            Point newSizes = desiredSize == null ?
                    new Point(0, 0) :
                    new Point(desiredSize.x / 2, desiredSize.y / 2);

            result = getBitmapFromFile(file, newSizes);
        }
        return result;
    }

    public static File getFileFromBitmap(Bitmap bitmap, int quality, String newFileName, Context ctx) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        byte[] byteArray = bos.toByteArray();

        return fileFromByteArray(byteArray, newFileName, ctx);
    }


    public static File fileFromByteArray(byte[] bytes, String newFileName, Context ctx) throws IOException {
        String filename = String.valueOf(Calendar.getInstance().getTimeInMillis());
        File file = new File(newFileName);
        file.createNewFile();

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bytes);
        fos.flush();
        fos.close();
        return file;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case 180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case 90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            case 270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (!bmRotated.equals(bitmap)) bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap getThumbnail(String fileName) throws IOException {
        Bitmap thumbnail;
        if (fileName.endsWith(CameraManager.JPG)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            int orientation = MediaUtils.getOrientation(fileName);
            thumbnail = MediaUtils.rotateBitmap(
                    ThumbnailUtils.extractThumbnail(
                            BitmapFactory.decodeFile(fileName, options)
                            , THUMB_DIMENSION, THUMB_DIMENSION
                    )
                    , orientation
            );
        } else { //it's video
            thumbnail = ThumbnailUtils.createVideoThumbnail(fileName, MediaStore.Video.Thumbnails.MICRO_KIND);
        }
        return thumbnail;
    }

}
