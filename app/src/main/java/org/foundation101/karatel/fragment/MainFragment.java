package org.foundation101.karatel.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.Violation;
import org.foundation101.karatel.activity.ViolationActivity;
import org.foundation101.karatel.adapter.ViolationsAdapter;

public class MainFragment extends Fragment {
    static final String TAG = "ChooseForm";
    AlertDialog alertDialog;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Google Analytics part
        KaratelApplication.getInstance().sendScreenName(TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        //init  gridView with violations
        GridView violationsGridView = (GridView) v.findViewById(R.id.gridViewMain);
        ViolationsAdapter violationsAdapter = new ViolationsAdapter();
        ViolationsAdapter.content = Violation.getViolationsList();
        violationsGridView.setAdapter(violationsAdapter);
        violationsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Violation violation = ViolationsAdapter.content.get(position);
                if (violation.isActive()) {
                    Intent intent = new Intent(parent.getContext(), ViolationActivity.class)
                            .putExtra(Globals.VIOLATION_ACTIVITY_MODE, ViolationActivity.MODE_CREATE)
                            .putExtra(Globals.VIOLATION, violation);
                    startActivity(intent);
                } else {
                    showDialog(violation);
                }
            }
        });
        return v;
    }

    void showDialog(Violation violation) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder
                .setMessage(violation.getTextInactive())
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onDetach() {
        if (alertDialog != null) alertDialog.dismiss();
        super.onDetach();
    }


}
