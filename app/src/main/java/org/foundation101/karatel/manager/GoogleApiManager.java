package org.foundation101.karatel.manager;

import android.app.Activity;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;

/**
 * Created by Dima on 02.02.2018.
 */

public class GoogleApiManager {
    final int RQS_GooglePlayServices = 1000;

    public GoogleApiClient client;

    private Activity activity;

    public GoogleApiManager(Activity activity) {
        this.activity = activity;
    }

    private synchronized void buildClient(
            GoogleApiClient.ConnectionCallbacks connectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener failedListener) {
        client = new GoogleApiClient.Builder(KaratelApplication.getInstance())
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(failedListener)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API).build();
        client.connect();
    }

    public void init(GoogleApiClient.ConnectionCallbacks connectionCallbacks,
                     GoogleApiClient.OnConnectionFailedListener failedListener) {
        if (checkGooglePlayServices()) { buildClient(connectionCallbacks, failedListener); }
    }

    public void onStart() {
        if (client != null) {
            client.connect();
        }
    }

    public void onStop() {
        if (clientOk()) {
            client.disconnect();
        }
    }

    public boolean clientOk() {
        return (client != null) && (client.isConnected());
    }

    private boolean checkGooglePlayServices(){
        GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
        int resultCode = gaa.isGooglePlayServicesAvailable(KaratelApplication.getInstance());
        if (resultCode == ConnectionResult.SUCCESS){
            return true;
        }else{
            if (gaa.isUserResolvableError(resultCode)) {
                gaa.getErrorDialog(activity, resultCode, RQS_GooglePlayServices).show();
            } else {
                Globals.showMessage("This device is not supported.");
            }
            return false;
        }
    }
}
