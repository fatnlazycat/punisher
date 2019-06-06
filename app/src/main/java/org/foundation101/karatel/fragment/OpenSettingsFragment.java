package org.foundation101.karatel.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import androidx.fragment.app.DialogFragment;

import org.foundation101.karatel.R;

/**
 * Created by Dima on 09.08.2016.
 */
public class OpenSettingsFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.cannot_define_location).
                setMessage(R.string.open_settings).
                setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), null);
                    }
                }).
                setNegativeButton(R.string.cancel, null);
        return builder.create();
    }
}