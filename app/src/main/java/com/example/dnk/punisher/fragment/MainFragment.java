package com.example.dnk.punisher.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.example.dnk.punisher.Constants;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.Violation;
import com.example.dnk.punisher.activity.violation.ViolationActivity;
import com.example.dnk.punisher.adapter.ViolationsAdapter;

import java.util.ArrayList;

public class MainFragment extends Fragment {

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                //Activity toStart = ViolationsAdapter.content.get(position).getHandlingActivity();
                Intent intent = new Intent(parent.getContext(), ViolationActivity.class)
                        .putExtra(Constants.CREATE_MODE, true)
                        .putExtra(Constants.VIOLATION, ViolationsAdapter.content.get(position));
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
        int len = violationNames.length;
        for (int i = 0; i < len; i++) {
            Violation v =  new Violation();
            v.setDrawableId(this.getResources().getIdentifier("violation_picture_" + i, "mipmap", getActivity().getPackageName()));
            v.setName(violationNames[i]);
            v.setType(violationTypes[i]);
            v.setUsesCamera(Boolean.parseBoolean(violationUsesCamera[i]));
            result.add(v);
        }
        return result;
    }
}
