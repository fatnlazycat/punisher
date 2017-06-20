package org.foundation101.karatel.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.retrofit.RetrofitDownloader;
import org.foundation101.karatel.utils.MediaUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ShowMediaActivity extends AppCompatActivity {
    Bitmap picture;
    FrameLayout progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_media);

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);

        Intent intent = getIntent();
        String source = intent.getStringExtra(Globals.MEDIA_FILE);
        if (source.endsWith(CameraManager.JPG) || source.endsWith(CameraManager.PNG)) {
            ImageView iView = (ImageView) findViewById(R.id.imageViewJustShow);
            iView.setVisibility(View.VISIBLE);
            new BitmapWorkerTask(iView).execute(source);

        } else if (source.endsWith(CameraManager.MP4)) {
            VideoView vView = (VideoView) findViewById(R.id.videoViewJustShow);
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
        } else {
            Toast.makeText(this, R.string.media_format_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        /*
      ATTENTION: This was auto-generated to implement the App Indexing API.
      See https://g.co/AppIndexing/AndroidStudio for more information.
     */
        GoogleApiClient client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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

    public void empty(View view) {
        //empty method to handle click events
    }

         Point getDesiredSize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        return point;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    /**
     * BitmapWorkerTask
     */
    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        static final String TAG = "BitmapWorkerTask";
        final String newFilePath = getExternalFilesDir(null) + File.separator + TAG + CameraManager.JPG;
        File newFile = new File(newFilePath);

        BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
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
            progressBar.setVisibility(View.GONE);
        }

        Bitmap decodeSampledBitmapFromFile(String fileName) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            Point point = MediaUtils.getDesiredSize(ShowMediaActivity.this);
            try {
                if (fileName.matches("https?://.+")) {
                    String baseUrl = Globals.SERVER_URL.replace("/api/v1/", "");

                    RetrofitDownloader downloader = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .build().create(RetrofitDownloader.class);
                    Call<ResponseBody> call = downloader.downloadFileWithDynamicUrl(fileName.replace(baseUrl, ""));
                    Response<ResponseBody> response = call.execute();
                    if (response.isSuccessful()) {
                        Log.d(TAG, "server contacted and has file");
                        boolean writtenToDisk = writeResponseBodyToDisk(response.body(), newFile);
                        Log.d(TAG, "file download was a success? " + writtenToDisk);
                    } else {
                        Log.d(TAG, "server contact failed");
                    }

                    int orientation = MediaUtils.getOrientation(newFilePath);

                    //first run to determine image size
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(newFilePath, options);

                    //calculate sample size
                    options.inSampleSize = MediaUtils.calculateInSampleSize(options, point.x, point.y);

                    //second run to get bitmap & set to resulting picture
                    options.inJustDecodeBounds = false;
                    picture = MediaUtils.rotateBitmap(BitmapFactory.decodeFile(newFilePath, options), orientation);
                    boolean fileDeletedSuccessfully = newFile.delete();
                    Log.d(TAG, "fileDeletedSuccessfully = " + fileDeletedSuccessfully);
                } else {
                    //first run to determine image size
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(fileName, options);

                    //calculate sample size
                    options.inSampleSize = MediaUtils.calculateInSampleSize(options, point.x, point.y);

                    //second run to get bitmap
                    options.inJustDecodeBounds = false;

                    int orientation = MediaUtils.getOrientation(fileName);
                    try {
                        picture = MediaUtils.rotateBitmap(BitmapFactory.decodeFile(fileName, options), orientation);
                    } catch (OutOfMemoryError err) {
                        Log.d(TAG, "reducing in sample size");
                        options.inSampleSize *= 4;
                        picture = MediaUtils.rotateBitmap(BitmapFactory.decodeFile(fileName, options), orientation);
                    }
                }
            } catch (final IOException e) {
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

    /**
     * class of ShowMediaActivity
     * @param body
     * @param file
     * @return
     */
    public static boolean writeResponseBodyToDisk(ResponseBody body, File file) {
        try {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(file);
                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d("writeResponseBodyToDisk", "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }

}
