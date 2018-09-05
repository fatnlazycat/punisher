package org.foundation101.karatel.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.manager.PermissionManager;

import java.io.IOException;

import javax.inject.Inject;

import static org.foundation101.karatel.manager.PermissionManager.CAMERA_PERMISSIONS_PHOTO;
import static org.foundation101.karatel.manager.PermissionManager.STORAGE_PERMISSION;

/**
 * Created by Dima on 02.06.2016.
 */
public class ChangeAvatarFragment extends DialogFragment {

    static final int PICK_IMAGE = 400;

    @Inject PermissionManager permissionManager;

    ProfileFragment parentFragment;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        KaratelApplication.dagger().inject(this);

        parentFragment = (ProfileFragment) getParentFragment();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.change_profile_photo).
            setItems(R.array.change_avatar_dialog_items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which){
                        case 0 : {//pick from gallery
                            if (permissionManager.checkWithDialog(STORAGE_PERMISSION, parentFragment)) {
                                parentFragment.startGallery();
                            }
                            break;
                        }
                        case 1 : {//make a photo
                            //although cameraManager checks permissions itself
                            // we need to get callback to ProfileFragment, not MainActivity as it would be
                            // if CameraManager requested permissions based on its activity context
                            if (permissionManager.checkWithDialog(CAMERA_PERMISSIONS_PHOTO, parentFragment))
                            parentFragment.startCamera();
                            break;
                        }
                        case 2 : {//delete image
                            try {
                                parentFragment.setNewAvatar(null);
                            } catch (IOException e) {
                                Log.e("Punisher", e.toString());
                            }
                            break;
                        }
                    }
                }
            }).
            setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { /*just dismiss*/ }
            });
        return builder.create();
    }
}
