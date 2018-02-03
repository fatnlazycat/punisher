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
import android.widget.Toast;

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
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.ViolationActivity;
import org.foundation101.karatel.utils.Formular;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import dagger.DaggerComponent;
import dagger.DaggerDaggerComponent;

import static org.foundation101.karatel.activity.ViolationActivity.MODE_CREATE;
import static org.foundation101.karatel.activity.ViolationActivity.MODE_EDIT;
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
        googleApiClient = formular.getGoogleManager().client;

        DaggerComponent component = DaggerDaggerComponent.builder().build();
        component.inject(this);
    }

    private Activity activity;
    private Formular formular;

    @Inject
    PermissionManager permissionManager;

    boolean locationPermitted = false;
    public Double latitude, longitude;
    final Double REFRESH_ACCURACY = 0.001;
    boolean mockLocationDialogShown = false;
    LocationRequest locationRequest;
    android.location.LocationListener locationListener;
    LocationManager locationManager;
    GoogleApiClient googleApiClient;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (locationPermitted) {
            if (formular.getMode() == MODE_CREATE) {
                locationRequest = LocationRequest.create()
                        .setNumUpdates(3)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(1);
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
                                if (activity != null) try {
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    status.startResolutionForResult(activity, ViolationActivity.REQUEST_CHECK_SETTINGS);
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
            }
            if (formular.getMode() >= MODE_CREATE) {
                getCurrentPlace();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e("Punisher", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onLocationChanged(final Location location) {
        if (formular.getMode() == MODE_CREATE
                && !(KaratelApplication.getInstance().locationIsMock(location))
                && googleClientOk()){

            Double latToCheck = latitude!=null ? latitude : 0;
            Double lonToCheck = longitude!=null ? longitude : 0;
            Double absLat = Math.abs(latToCheck - location.getLatitude());
            Double absLon = Math.abs(lonToCheck - location.getLongitude());
            if (absLat > REFRESH_ACCURACY || absLon > REFRESH_ACCURACY) {
                PendingResult<LocationSettingsResult> result = getPendingLocationSettings();

                result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                    @Override
                    public void onResult(LocationSettingsResult result) {
                        if (result.getStatus().getStatusCode() == LocationSettingsStatusCodes.SUCCESS) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                            if (formular != null) {
                                formular.onLocationChanged(latitude, longitude);
                            }
                        } else {
                            latitude = null; //this will block the buttons
                        }
                    }
                });
            }
        }
    }

    PendingResult<LocationSettingsResult> getPendingLocationSettings(){
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        return result;
    }

    void obtainLocation() {
        initOldAndroidLocation();
        locationPermitted = true;
    }

    @SuppressWarnings({"MissingPermission"})
    public void requestLocationFromFusedApi() {
        if (googleClientOk() && locationPermitted)
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    /*
    old Android package location code
    */
    @SuppressWarnings({"MissingPermission"})
    void initOldAndroidLocation(){
        if (activity != null) {
            locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationListener = new MyOldAndroidLocationListener();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        }
    }

    private class MyOldAndroidLocationListener implements android.location.LocationListener{
        @Override
        public void onLocationChanged(Location location) {
            if (latitude == null || longitude == 0) {//use this only if no result from FusedLocationAPI

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
                } else if (formular.getMode() == MODE_CREATE) {
                    Double latToCheck = latitude != null ? latitude : 0;
                    Double lonToCheck = longitude != null ? longitude : 0;
                    Double absLat = Math.abs(latToCheck - location.getLatitude());
                    Double absLon = Math.abs(lonToCheck - location.getLongitude());
                    if (absLat > REFRESH_ACCURACY || absLon > REFRESH_ACCURACY) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        if (formular != null) {
                            formular.onLocationChanged(latitude, longitude);
                        }
                    }
                }
            }
            locationManager.removeUpdates(locationListener);
            //locationListener = null;
        }
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        @Override
        public void onProviderEnabled(String provider) {}
        @Override
        public void onProviderDisabled(String provider) {}
    }


    boolean checkLocation(){
        if (latitude == null){
            /*AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setTitle(R.string.cannot_define_location).setNegativeButton(R.string.ok, null).create();
            dialog.show();*/
            return false;
        } else return true;
    }

    public void onPermissionResult(boolean granted) {
        if (granted) {
            obtainLocation();
            requestLocationFromFusedApi();
        } else locationPermitted = false;
    }

    public void onSettingsResult() {
        requestLocationFromFusedApi();
    }

    boolean checkLocationPermissions(boolean withDialog) {
        if (withDialog) return permissionManager.checkWithDialog (LOCATION_PERMISSIONS, activity);
        else            return permissionManager.checkPermissions(LOCATION_PERMISSIONS);
    }

    public boolean googleClientOk() {
        return (googleApiClient != null) && (googleApiClient.isConnected());
    }

    @SuppressWarnings({"MissingPermission"})
    private void getCurrentPlace() {
            PendingResult<PlaceLikelihoodBuffer> placeLikelihoodResult =
                    Places.PlaceDetectionApi.getCurrentPlace(googleApiClient, null);
            placeLikelihoodResult.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(@NonNull PlaceLikelihoodBuffer placesResult) {
                    formular.onAddressesReady(placesResult);
                }
            });
    }

    /**
     * lyfecycle methods
     */

    public void onCreate() {
        if (checkLocationPermissions(true)) obtainLocation();
    }

    public void onStart(){
        locationPermitted = checkLocationPermissions(false);
    }

    public void onStop() {
        if (googleClientOk())
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    public void onDestroy() {
        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
            //locationListener = null;
        }

        activity = null;
        formular = null;
    }
}
