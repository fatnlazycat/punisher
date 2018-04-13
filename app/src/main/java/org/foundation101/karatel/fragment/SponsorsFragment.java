package org.foundation101.karatel.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;

/**
 * Created by Dima on 14.05.2016.
 */
public class SponsorsFragment extends Fragment {
    static final String TAG = "Sponsors";

    public SponsorsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        KaratelApplication.getInstance().sendScreenName(TAG);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sponsors, container, false);
        return v;
    }
}
