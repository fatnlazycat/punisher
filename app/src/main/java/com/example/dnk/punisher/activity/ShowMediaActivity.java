package com.example.dnk.punisher.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.example.dnk.punisher.CameraManager;
import com.example.dnk.punisher.Globals;
import com.example.dnk.punisher.R;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class ShowMediaActivity extends Activity {
    Bitmap picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_media);
        Intent intent = getIntent();
        String source = intent.getStringExtra(Globals.MEDIA_FILE);
        if (source.endsWith(CameraManager.JPG)) {
            ImageView iView = (ImageView) findViewById(R.id.imageViewJustShow);
            iView.setVisibility(View.VISIBLE);
            new BitmapWorkerTask(iView).execute(source);

        } else {
            /*MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(source);
            mp.prepare();
            mp.start();*/
            VideoView vView = (VideoView)findViewById(R.id.videoViewJustShow);
            vView.setMediaController(new MediaController(this));
            vView.setVideoPath(source);
            vView.setVisibility(View.VISIBLE);
        }

    }

    /*@Override
    protected void onStop() {
        super.onStop();
        picture.recycle();
        picture = null;
    }*/

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
            picture = BitmapFactory.decodeFile(fileName, options);
            return picture;

            /* First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(res, resId, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return BitmapFactory.decodeResource(res, resId, options);*/
        }
    }
}
