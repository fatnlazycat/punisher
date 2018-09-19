package org.foundation101.karatel.manager;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.ViolationActivity;
import org.foundation101.karatel.utils.Formular;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static org.foundation101.karatel.manager.PermissionManager.LOCATION_PERMISSIONS;

/**
 * Created by Dima on 01.02.2018.
 */

public class KaratelLocationManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public KaratelLocationManager(Formular formularActivity){
        if (!(formularActivity instanceof Activity))
            throw new IllegalArgumentException("formularActivity should be an instance of Activity");
        activity = (Activity) formularActivity;
        formular =            formularActivity;

        KaratelApplication.dagger().inject(this);
    }

    private Activity activity;
    private Formular formular;

    @Inject PermissionManager permissionManager;

    private boolean resumedState = false;
    private boolean locationPermitted = false;
    public double latitude = 0, longitude = 0;
    private final Double REFRESH_ACCURACY = 0.001;
    private boolean mockLocationDialogShown = false;
    private LocationRequest locationRequest;
    private android.location.LocationListener locationListener;
    private LocationManager locationManager;
    private GoogleApiClient googleApiClient;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        obtainGoogleLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e("Punisher", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onLocationChanged(final Location location) {
        if (!(KaratelApplication.getInstance().locationIsMock(location))
                && googleClientOk()){

            Double absLat = Math.abs(latitude  - location.getLatitude());
            Double absLon = Math.abs(longitude - location.getLongitude());
            if (absLat > REFRESH_ACCURACY || absLon > REFRESH_ACCURACY) {
                PendingResult<LocationSettingsResult> result = getPendingLocationSettings();

                result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                    @Override
                    public void onResult(@NonNull LocationSettingsResult result) {
                        if (result.getStatus().getStatusCode() == LocationSettingsStatusCodes.SUCCESS) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                            if (formular != null) {
                                formular.onLocationChanged(latitude, longitude);
                            }
                        }
                    }
                });
            }
        }
    }

    private PendingResult<LocationSettingsResult> getPendingLocationSettings(){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(getLocationRequest());
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        return result;
    }

    private LocationRequest getLocationRequest() {
        if (locationRequest == null) {
            locationRequest = LocationRequest.create()
                    .setNumUpdates(3)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(1);
        }
        return locationRequest;
    }

    @SuppressWarnings({"MissingPermission"})
    private void requestLocationFromFusedApi() {
        if (googleClientOk() && locationPermitted)
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(googleApiClient, getLocationRequest(), this);
    }

    /*
    old Android package location code
    */
    @SuppressWarnings({"MissingPermission"})
    private void obtainAndroidLocation(){
        if (locationPermitted && activity != null) {
            locationManager = (LocationManager) KaratelApplication.getInstance()
                    .getSystemService(Context.LOCATION_SERVICE);

            assert locationManager != null;

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationListener = new MyOldAndroidLocationListener();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        TimeUnit.MINUTES.toMillis(1),
                        100, //100 meters
                        locationListener);
            }
        }
    }

    private class MyOldAndroidLocationListener implements android.location.LocationListener{
        @Override
        public void onLocationChanged(Location location) {
            if ((latitude == 0 || longitude == 0)) {//use this only if no result from FusedLocationAPI

                if (KaratelApplication.getInstance().locationIsMock(location)){
                    if (!mockLocationDialogShown) {
                        mockLocationDialogShown = true;
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        AlertDialog dialog = builder
                                .setTitle(R.string.turn_off_mock_locations)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (activity != null) {
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
                                            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                                                activity.startActivity(intent);
                                            }
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.cancel, null).create();
                        dialog.show();
                    }
                } else {
                    Double absLat = Math.abs(latitude  - location.getLatitude());
                    Double absLon = Math.abs(longitude - location.getLongitude());
                    if (absLat > REFRESH_ACCURACY || absLon > REFRESH_ACCURACY) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        if (formular != null) {
                            formular.onLocationChanged(latitude, longitude);
                        }
                    }
                }
            }
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {
            onSettingsResult();
        }
        @Override
        public void onProviderDisabled(String provider) {}
    }

    private void obtainGoogleLocation() {
        if (formular != null) googleApiClient = formular.getGoogleManager().client;
        if (locationPermitted && googleClientOk()) {
            PendingResult<LocationSettingsResult> result = getPendingLocationSettings();

            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(@NonNull LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    //final LocationSettingsStates states = result.getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can initialize location requests here.
                            requestLocationFromFusedApi();
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                            if (activity != null
                                    && resumedState
                                    && !KaratelApplication.getInstance().locationSettingsDialogShown) try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(activity, ViolationActivity.REQUEST_CHECK_SETTINGS);
                                KaratelApplication.getInstance().locationSettingsDialogShown = true;
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.
                            Globals.showMessage("location settings not available");
                            if (activity != null) activity.finish();
                            break;
                    }
                }
            });
            getCurrentPlace();
        }
    }

    public void onPermissionResult(boolean granted) {
        locationPermitted = granted;
        obtainAndroidLocation();
        obtainGoogleLocation();
    }

    //this method doesn't check for the result (ok or cancel).
    //Because - user sees the location settings dialog, opens top-screen sliding menu & turns location on there.
    // It just tries once more, knowing that something has changed
    public void onSettingsResult() {
        KaratelApplication.getInstance().locationSettingsDialogShown = false;
        requestLocationFromFusedApi();
    }

    private boolean checkLocationPermissions(boolean withDialog) {
        if (withDialog) return permissionManager.checkWithDialog (LOCATION_PERMISSIONS, activity);
        else            return permissionManager.checkPermissions(LOCATION_PERMISSIONS);
    }

    private boolean googleClientOk() {
        return (googleApiClient != null) && (googleApiClient.isConnected());
    }

    @SuppressWarnings({"MissingPermission"})
    private void getCurrentPlace() {
            PendingResult<PlaceLikelihoodBuffer> placeLikelihoodResult =
                    Places.PlaceDetectionApi.getCurrentPlace(googleApiClient, null);
            placeLikelihoodResult.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(@NonNull PlaceLikelihoodBuffer placesResult) {
                    if (formular != null) formular.onAddressesReady(placesResult);
                }
            });
    }

    /**
     * lyfecycle methods
     */

    public void onCreate() {
        if (locationPermitted = checkLocationPermissions(true)) obtainAndroidLocation();
    }

    public void onStart(){
        locationPermitted = checkLocationPermissions(false);
    }

    public void onResume() {
        resumedState = true;
    }

    public void onPause() {
        resumedState = false;
    }

    public void onStop() {
        if (googleClientOk())
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    public void onDestroy() {
        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
            locationManager = null;
            locationListener = null; //don't do this! - since onLocationChanged is being fired several times which causes NPE
        }

        PermissionManager.clearPendingRequests();

        activity = null;
        formular = null;
    }

    /** end of lifecycle methods**/

}
