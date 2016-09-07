package org.foundation101.karatel.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.opengl.GLES10;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public class ShowMediaActivity extends AppCompatActivity {
    Bitmap picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_media);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);

        Intent intent = getIntent();
        String source = intent.getStringExtra(Globals.MEDIA_FILE);
        if (source.endsWith(CameraManager.JPG)) {
            ImageView iView = (ImageView) findViewById(R.id.imageViewJustShow);
            iView.setVisibility(View.VISIBLE);
            new BitmapWorkerTask(iView).execute(source);

        } else if (source.endsWith(CameraManager.MP4)) {
            VideoView vView = (VideoView)findViewById(R.id.videoViewJustShow);
            final MediaController mc = new MediaController(this);
            vView.setMediaController(mc);
            vView.setVideoPath(source);
            vView.setVisibility(View.VISIBLE);
            vView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mc.show();
                }
            });
        }

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
        if (Math.max(newWidth, newHeight) > textureSize) inSampleSize *= 2;

        return inSampleSize;
    }

    public static int getMaxTextureSize() {
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

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            return decodeSampledBitmapFromFile(params[0]);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }

        public Bitmap decodeSampledBitmapFromFile(String fileName) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            try {
                if (fileName.matches("https?://.+")){
                    //first run to determine image size
                    InputStream is = (InputStream) new URL(fileName).getContent();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(is, null, options);

                    //calculate sample size
                    Display display = getWindowManager().getDefaultDisplay();
                    Point point = new Point();
                    display.getSize(point);

                    options.inSampleSize = calculateInSampleSize(options, point.x, point.y);

                    //second run to get bitmap
                    is = (InputStream) new URL(fileName).getContent();
                    options.inJustDecodeBounds = false;
                    picture = BitmapFactory.decodeStream(is, null, options);
                    is.close();
                } else {
                    picture = BitmapFactory.decodeFile(fileName, options);
                }
            } catch (final IOException e){
                ShowMediaActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(ShowMediaActivity.this, R.string.cannot_connect_server, e);
                    }
                });
            }
            return picture;
        }
    }


}
