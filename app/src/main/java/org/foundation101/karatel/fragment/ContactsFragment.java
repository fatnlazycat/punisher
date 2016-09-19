package org.foundation101.karatel.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.foundation101.karatel.Karatel;
import org.foundation101.karatel.R;

public class ContactsFragment extends Fragment {
    static final String TAG = "Contact";
    public ContactsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Google Analytics part
        ((Karatel)getActivity().getApplication()).sendScreenName(TAG);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }


}
