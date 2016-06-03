package org.foundation101.thepunisher.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import org.foundation101.thepunisher.CameraManager;
import org.foundation101.thepunisher.Globals;
import org.foundation101.thepunisher.R;

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
            VideoView vView = (VideoView)findViewById(R.id.videoViewJustShow);
            MediaController mc = new MediaController(this);
            vView.setMediaController(mc);
            vView.setVideoPath(source);
            vView.setVisibility(View.VISIBLE);
            mc.show();
        }

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
            picture = BitmapFactory.decodeFile(fileName, options);
            return picture;

        }
    }
}
