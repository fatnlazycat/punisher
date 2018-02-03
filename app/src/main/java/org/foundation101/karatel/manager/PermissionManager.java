package org.foundation101.karatel.manager;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;

import org.foundation101.karatel.KaratelApplication;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import javax.inject.Inject;

import dagger.Module;

/**
 * Created by Dima on 30.01.2018.
 */
@Module
public class PermissionManager {
    public static final int LOCATION_PERMISSIONS = 1;
    public static final int CAMERA_PERMISSIONS   = 2;
    private static final String[] LOCATION_PERMISSIONS_ARRAY = {Manifest.permission.ACCESS_FINE_LOCATION};
    private static final String[] CAMERA_PERMISSIONS_ARRAY   = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    private static final SparseArray<String[]> permissionsMap = initPermissionsMap();
    private static SparseArray<String[]> initPermissionsMap() {
        SparseArray<String[]> sparseArray = new SparseArray<>(2);
        sparseArray.put(LOCATION_PERMISSIONS,   LOCATION_PERMISSIONS_ARRAY);
        sparseArray.put(CAMERA_PERMISSIONS,     CAMERA_PERMISSIONS_ARRAY);
        return sparseArray;
    }

    @Inject
    public PermissionManager() { }

    public boolean checkWithDialog(int permissionsKey, @NonNull Activity activity) {
        boolean result = checkPermissions(permissionsKey);
        if (!result) showPermissionsRequestDialog(permissionsKey, activity);
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
}
