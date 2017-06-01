package org.foundation101.karatel.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.Karatel;
import org.foundation101.karatel.R;
import org.foundation101.karatel.Violation;
import org.foundation101.karatel.activity.ViolationActivity;
import org.foundation101.karatel.adapter.ViolationsAdapter;

import java.util.ArrayList;

public class MainFragment extends Fragment {
    static final String TAG = "ChooseForm";

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Google Analytics part
        ((Karatel)getActivity().getApplication()).sendScreenName(TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        //init  gridView with violations
        GridView violationsGridView = (GridView) v.findViewById(R.id.gridViewMain);
        ViolationsAdapter violationsAdapter = new ViolationsAdapter();
        ViolationsAdapter.content = makeViolationsList();
        violationsGridView.setAdapter(violationsAdapter);
        violationsGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(parent.getContext(), ViolationActivity.class)
                        .putExtra(Globals.VIOLATION_ACTIVITY_MODE, ViolationActivity.MODE_CREATE)
                        .putExtra(Globals.VIOLATION, ViolationsAdapter.content.get(position));
                startActivity(intent);
            }
        });
        return v;
    }

    ArrayList<Violation> makeViolationsList() {
        ArrayList<Violation> result = new ArrayList<>();
        String[] violationNames = getResources().getStringArray(R.array.violations);
        String[] violationTypes = getResources().getStringArray(R.array.violationTypes);
        String[] violationUsesCamera = getResources().getStringArray(R.array.violationUsesCamera);
        int[] violationMediaTypes = getResources().getIntArray(R.array.violationMediaTypes);
        int len = violationNames.length;
        for (int i = 0; i < len; i++) {
            Violation v =  new Violation();
            v.setDrawableId(this.getResources().getIdentifier("violation_picture_" + i, "mipmap", getActivity().getPackageName()));
            v.setName(violationNames[i]);
            v.setType(violationTypes[i]);
            v.setUsesCamera(Boolean.parseBoolean(violationUsesCamera[i]));
            v.setMediaTypes(violationMediaTypes[i]);
            result.add(v);
        }
        return result;
    }
}
