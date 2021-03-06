package org.foundation101.karatel;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import androidx.multidex.MultiDexApplication;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;

import com.evernote.android.job.JobManager;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.splunk.mint.Mint;
import com.squareup.leakcanary.LeakCanary;

import org.foundation101.karatel.dagger.DaggerComponent;
import org.foundation101.karatel.dagger.DaggerDaggerComponent;
import org.foundation101.karatel.entity.Request;
import org.foundation101.karatel.scheduler.MyJobCreator;
import org.foundation101.karatel.utils.RetrofitUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import retrofit2.Retrofit;

public class KaratelApplication extends MultiDexApplication {
    public static final String TAG = "KaratelApplication";
    private static Retrofit retrofit = null;

    //google's Status.startResolutionForResult doesn't give any control over its dialog, so check ourselves
    public boolean locationSettingsDialogShown = false;

    public CopyOnWriteArrayList<Request> requests = new CopyOnWriteArrayList<>();

    private static KaratelApplication instance;
    public static KaratelApplication getInstance() {
        return instance;
    }


    private static DaggerComponent daggerComponent;
    public static DaggerComponent dagger() {
        if (daggerComponent == null) daggerComponent = DaggerDaggerComponent.builder().build();
        return daggerComponent;
    }

    //Google Analytics part
    private Tracker mTracker;
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }
    public void sendScreenName(String screenName){
        Tracker thisTracker = getDefaultTracker();
        thisTracker.setScreenName(screenName);
        thisTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        instance = this;
        initACRA();
        JobManager.create(getApplicationContext()).addJobCreator(new MyJobCreator());
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
    }

    @Override
    public void onLowMemory(){
        super.onLowMemory();
        Runtime.getRuntime().gc();
    }

    void initACRA() {
        Log.d("KaratelApplication", "BuildConfig.DEBUG = " + BuildConfig.DEBUG);
        if (!BuildConfig.DEBUG) {
            Mint.initAndStartSession(this, "c609df56");
            //Mint.startANRMonitoring(5000, true);
        }
    }

    public void showOneButtonDialogFromService(String title, String message, DialogInterface.OnClickListener action) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppTheme);
        dialogBuilder.setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.ok, action)
            .setCancelable(false);
        final AlertDialog dialog = dialogBuilder.create();
        final Window dialogWindow = dialog.getWindow();
        final WindowManager.LayoutParams dialogWindowAttributes = dialogWindow.getAttributes();

        // Set fixed width (280dp) and WRAP_CONTENT height
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialogWindowAttributes);
        lp.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 280, getResources().getDisplayMetrics());
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialogWindow.setAttributes(lp);

        // Set to TYPE_SYSTEM_ALERT so that the Service can display it
        dialogWindow.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    public boolean locationIsMock(Location location){
        boolean isMock;// = !Settings.Secure.getString(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isMock = (location==null || location.isFromMockProvider());
        } else {
            isMock = !Settings.Secure.getString(getContentResolver(), Settings.Secure.ALLOW_MOCK_LOCATION).equals("0");
        }

        /*//debug block
        String message = isMock ? "mock" : "good!";
        Toast.makeText(this, "georesult = "+message, Toast.LENGTH_LONG).show();
        Log.e("Punisher","georesult = "+message);*/

        return isMock;
    }


    public static Retrofit getClient(int apiVersion) {
        return RetrofitUtils.build(retrofit, apiVersion);
    }

    public static Retrofit getClient() {
        return getClient(1);
    }

    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }

        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileOutputStream(destFile).getChannel();
        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null) {
            source.close();
        }
        if (destination != null) {
            destination.close();
        }


    }

    public static void longLastingOperation(long duration) {
        for (long l = 0; l < duration; l++) {
            double d = l / Math.PI;
            if (Math.round(d) % 1000 == 0) Log.d(TAG, "longLastingOperation, d=" + d);
        }
        Log.d(TAG, "longLastingOperation ended");
    }
}
