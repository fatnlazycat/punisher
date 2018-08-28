package org.foundation101.karatel.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.manager.CameraManager;
import org.foundation101.karatel.retrofit.ProgressEvent;
import org.foundation101.karatel.retrofit.RetrofitDownloader;
import org.foundation101.karatel.utils.MediaUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class ShowMediaActivity extends AppCompatActivity {
    static final String TAG = "ShowMediaActivity";

    Bitmap picture;
    View progressBar;
    TextView tvProgress;
    VideoView vView;
    AsyncTask loadingTask;
    String downloadIdentifier;

    Thread progressThread;

    String source;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_media);

        progressBar = findViewById(R.id.rlProgress);
        tvProgress  = findViewById(R.id.tvProgress);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);

        Intent intent = getIntent();
        source = intent.getStringExtra(Globals.MEDIA_FILE);
        if (source.endsWith(CameraManager.JPG) || source.endsWith(CameraManager.PNG)) {
            ImageView iView = findViewById(R.id.imageViewJustShow);
            iView.setVisibility(View.VISIBLE);
            loadingTask = new BitmapWorkerTask(iView).execute(source);
            downloadIdentifier = loadingTask.toString();

        } else if (source.endsWith(CameraManager.MP4)) {
            vView = findViewById(R.id.videoViewJustShow);

            //source = "http://104.154.131.217:9000/files/pdf/CMI10599.pdf" - to test VideoView error dialog with app context
            /*vView = new VideoView(KaratelApplication.getInstance());
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.BELOW, R.id.toolbar);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            vView.setLayoutParams(layoutParams);
            ((RelativeLayout) findViewById(R.id.rlShowMediaActivity)).addView(vView);*/


            final MediaController mc = new MediaController(this);
            vView.setMediaController(mc);
            vView.setVideoPath(source);
            vView.setVisibility(View.VISIBLE);

            progressBar.setVisibility(View.VISIBLE);

            vView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    progressBar.setVisibility(View.GONE);
                    mc.show();
                }
            });

            //show buffering progress only starting from API 16
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                vView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mp, int what, int extra) {
                        Log.d(TAG, "onInfo, what=" + what + ", extra=" + extra);
                        switch (what) {
                            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                                progressBar.setVisibility(View.VISIBLE);
                                runProgressThread();
                                break;
                            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                                progressBar.setVisibility(View.GONE);
                                break;
                        }
                        return false;
                    }
                });
            }

            downloadIdentifier = vView.toString();
            runProgressThread();
        } else {
            Toast.makeText(this, R.string.media_format_not_supported, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    void runProgressThread() {
        Log.d(TAG, "runProgressThread, thread:" + progressThread + ", alive=" + (progressThread != null && progressThread.isAlive()));
        if (progressThread != null && progressThread.isAlive()) {
            Log.d(TAG, "Interrupting thread:" + progressThread);
            progressThread.interrupt();
        }
        progressThread = new Thread(new Runnable() {
            int percent = 0;
            @Override
            public void run() {
                try {
                    while (percent < 100 && progressBar != null && progressBar.getVisibility() == View.VISIBLE) {
                        percent = vView.getBufferPercentage();
                        EventBus.getDefault().post(new ProgressEvent(downloadIdentifier, percent));
                        Log.d(TAG, "Thread:" + progressThread.toString() + " post progress:" + percent);
                        Thread.sleep(700);
                    }
                } catch (InterruptedException e) {
                    Log.d(TAG, e.toString());
                }
                Log.d(TAG, "exiting run in thread:" + progressThread);
            }
        });
        progressThread.start();
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


    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    public void postProgress(ProgressEvent progressEvent) {
        boolean isLoading = (thisDownload(progressEvent.getIdentifier())
                && progressEvent.getProgress() > 0 && progressEvent.getProgress() <= 100);

        tvProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) tvProgress.setText(progressEvent.getProgress() + "%");
    }

    private boolean thisDownload(String id) {
        return id != null && id.equals(downloadIdentifier);

        /*HttpUrl httpUrl = HttpUrl.parse(source);

        if (httpUrl == null) return false;

        List<String> path = httpUrl.pathSegments();
        String fileName = (path.size() > 1) ? path.get(path.size() - 1) : "";

        return fileName.equalsIgnoreCase(downloadIdentifier);*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (loadingTask != null) loadingTask.cancel(true);

        Log.d(TAG, "onDestroy, thread:" + progressThread + ", alive=" + (progressThread != null && progressThread.isAlive()));
        if (progressThread != null && progressThread.isAlive()) {
            Log.d(TAG, "Interrupting thread:" + progressThread);
            progressThread.interrupt();
        }
        progressThread = null;

        vView = null; //otherwise the activity isn't garbage collected

        super.onDestroy();
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
            EventBus.getDefault().post(new ProgressEvent(BitmapWorkerTask.this.toString(), 1));
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            return decodeSampledBitmapFromFile(params[0]);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        }

        Bitmap decodeSampledBitmapFromFile(String fileUrl) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            Point point = MediaUtils.getDesiredSize(ShowMediaActivity.this);
            try {
                if (fileUrl.matches("https?://.+")) {
                    String baseUrl = Globals.SERVER_URL.replace("/api/v1/", "");

                    /*RetrofitDownloader downloader = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .build().create(RetrofitDownloader.class);*/

                    RetrofitDownloader downloader = KaratelApplication.getClient().create(RetrofitDownloader.class);
                    Call<ResponseBody> call = downloader.downloadFileWithDynamicUrl(fileUrl.replace(baseUrl, ""));
                    Response<ResponseBody> response = call.execute();

                    //check if this AsyncTask is cancelled & quit if yes
                    if (isCancelled()) return null;

                    if (response.isSuccessful()) {
                        Log.d(TAG, "server contacted and has file");
                        boolean writtenToDisk = writeResponseBodyToDisk(BitmapWorkerTask.this.toString(), response.body(), newFile);
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
                    BitmapFactory.decodeFile(fileUrl, options);

                    //calculate sample size
                    options.inSampleSize = MediaUtils.calculateInSampleSize(options, point.x, point.y);

                    //second run to get bitmap
                    options.inJustDecodeBounds = false;

                    int orientation = MediaUtils.getOrientation(fileUrl);
                    try {
                        picture = MediaUtils.rotateBitmap(BitmapFactory.decodeFile(fileUrl, options), orientation);
                    } catch (OutOfMemoryError err) {
                        Log.d(TAG, "reducing in sample size");
                        options.inSampleSize *= 4;
                        picture = MediaUtils.rotateBitmap(BitmapFactory.decodeFile(fileUrl, options), orientation);
                    }
                }
            } catch (final IOException e) {
                Globals.showError(R.string.cannot_connect_server, e);
            }
            return picture;
        }
    }

    public static boolean writeResponseBodyToDisk(String downloadId, ResponseBody body, File file) {
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
                    int progress = (int) ((float)fileSizeDownloaded / fileSize * 100);
                    EventBus.getDefault().post(new ProgressEvent(downloadId, progress));
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
