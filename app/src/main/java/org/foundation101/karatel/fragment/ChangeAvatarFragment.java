package org.foundation101.karatel.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;

import java.io.File;
import java.io.IOException;

/**
 * Created by Dima on 02.06.2016.
 */
public class ChangeAvatarFragment extends DialogFragment {

    static final int PICK_IMAGE = 400;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.change_profile_photo).
                setItems(R.array.change_avatar_dialog_items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0 : {//pick from gallery
                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
                                getParentFragment().startActivityForResult(Intent.createChooser(intent,
                                        getResources().getString(R.string.choose_picture)), PICK_IMAGE);
                                break;
                            }
                            case 1 : {//make a photo
                                CameraManager cameraManager = CameraManager.getInstance(ChangeAvatarFragment.this.getActivity());
                                cameraManager.startCamera(CameraManager.IMAGE_CAPTURE_INTENT);
                                break;
                            }
                            case 2 : {//delete image
                                try {
                                    new File(Globals.user.avatarFileName).delete();
                                } catch (Exception e){
                                    Log.e("Punisher", e.getMessage());
                                }
                                try {
                                    /*excluded this into a separate try/catch block to avoid situation when
                                     * problem with file deletion breaks & we do not make a server request
                                     * File deletion should go first because it uses Globals.user.avatarFileName
                                     * & setNewAvatar(null) in a separate thread uses it too
                                     */
                                    ((ProfileFragment) getParentFragment()).setNewAvatar(null);
                                } catch (IOException e) {
                                    Log.e("Punisher", e.getMessage());
                                }
                            }
                        }
                    }
                }).
                setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //launch icon picker
                    }
                });
        return builder.create();
    }
}
