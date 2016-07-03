package org.foundation101.karatel.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.Violation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

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
            options.inJustDecodeBounds = false;
            //options.inSampleSize = 8;
            try {
                if (fileName.matches("https?://.+")){
                    InputStream is = (InputStream) new URL(fileName).getContent();
                    picture = BitmapFactory.decodeStream(is);
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
