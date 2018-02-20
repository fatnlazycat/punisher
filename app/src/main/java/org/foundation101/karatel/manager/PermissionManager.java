package org.foundation101.karatel.manager;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;

import org.foundation101.karatel.KaratelApplication;

import javax.inject.Inject;

import dagger.Module;

/**
 * Created by Dima on 30.01.2018.
 */
@Module
public class PermissionManager {
    public static final int LOCATION_PERMISSIONS                        = 1;
    public static final int CUSTOM_CAMERA_PERMISSIONS_START_NORMAL      = 2;
    public static final int CUSTOM_CAMERA_PERMISSIONS_START_IMMEDIATELY = 3;
    public static final int CAMERA_PERMISSIONS_PHOTO                    = 4;
    public static final int CAMERA_PERMISSIONS_VIDEO                    = 5;
    public static final int STORAGE_PERMISSION                          = 6;
    private static final String[] LOCATION_PERMISSIONS_ARRAY = {Manifest.permission.ACCESS_FINE_LOCATION};
    private static final String[] CAMERA_PERMISSIONS_ARRAY   = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private static final String[] PHOTO_PERMISSIONS_ARRAY    = {Manifest.permission.CAMERA};
    private static final String[] STORAGE_PERMISSIONS_ARRAY  = {Manifest.permission.READ_EXTERNAL_STORAGE};

    private static final SparseArray<String[]> permissionsMap = initPermissionsMap();
    private static SparseArray<String[]> initPermissionsMap() {
        SparseArray<String[]> sparseArray = new SparseArray<>(6);
        sparseArray.put(LOCATION_PERMISSIONS,                        LOCATION_PERMISSIONS_ARRAY);
        sparseArray.put(CUSTOM_CAMERA_PERMISSIONS_START_NORMAL,      CAMERA_PERMISSIONS_ARRAY);
        sparseArray.put(CUSTOM_CAMERA_PERMISSIONS_START_IMMEDIATELY, CAMERA_PERMISSIONS_ARRAY);
        sparseArray.put(CAMERA_PERMISSIONS_PHOTO,                    PHOTO_PERMISSIONS_ARRAY);
        sparseArray.put(CAMERA_PERMISSIONS_VIDEO,                    CAMERA_PERMISSIONS_ARRAY);
        sparseArray.put(STORAGE_PERMISSION,                          STORAGE_PERMISSIONS_ARRAY);
        return sparseArray;
    }

    @Inject
    public PermissionManager() { }

    public boolean checkWithDialog(int permissionsKey, @NonNull Activity activity) {
        boolean result = checkPermissions(permissionsKey);
        if (!result) showPermissionsRequestDialog(permissionsKey, activity);
        return result;
    }

    public boolean checkWithDialog(int permissionsKey, @NonNull Fragment fragment) {
        boolean result = checkPermissions(permissionsKey);
        if (!result) showPermissionsRequestDialog(permissionsKey, fragment);
        return result;
    }

    public boolean checkPermissions(int permissionsKey) {
        String[] permissions = permissionsMap.get(permissionsKey);
        boolean permissionCheck = true;
        for (String p : permissions) permissionCheck = permissionCheck &&
                ContextCompat.checkSelfPermission(KaratelApplication.getInstance(),p)
                        == PackageManager.PERMISSION_GRANTED;

        return permissionCheck;
    }

    private void showPermissionsRequestDialog(int permissionsKey, @NonNull Activity activity) {
        String[] permissions = permissionsMap.get(permissionsKey);
        ActivityCompat.requestPermissions(activity, permissions, permissionsKey);
    }

    private void showPermissionsRequestDialog(int permissionsKey, @NonNull Fragment fragment) {
        String[] permissions = permissionsMap.get(permissionsKey);
        fragment.requestPermissions(permissions, permissionsKey);
    }

    public static boolean allGranted(@NonNull int[] grantResults){
        boolean granted = grantResults.length > 0;
        if (granted) {
            for (int i : grantResults) granted = granted && (i == PackageManager.PERMISSION_GRANTED);
        }
        return granted;
    }
}
