package org.foundation101.karatel.fragment;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;

/**
 * Created by Dima on 14.05.2016.
 */
public class PartnersFragment extends Fragment {
    static final String TAG = "Partners";

    public PartnersFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((KaratelApplication)getActivity().getApplication()).sendScreenName(TAG);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_partners, container, false);
        return v;
    }
}
