package com.example.dnk.punisher.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.example.dnk.punisher.Globals;
import com.example.dnk.punisher.PunisherUser;
import com.example.dnk.punisher.R;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class ProfileFragment extends Fragment {

    ViewGroup memberEmail, memberPassword, memberSurname, memberName, memberSecondName, memberPhone;
    SharedPreferences preferences;
    PunisherUser user;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = getUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        memberEmail = (ViewGroup) v.findViewById(R.id.profile_email);
        ((TextView)memberEmail.getChildAt(0)).setText(R.string.email);
        EditText emailEditText = (EditText)memberEmail.getChildAt(2);
        emailEditText.setText(user.email);
        emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        memberPassword = (ViewGroup) v.findViewById(R.id.profile_password);
        ((TextView)memberPassword.getChildAt(0)).setText(R.string.passw);
        EditText passwordEditText = (EditText)memberPassword.getChildAt(2);
        passwordEditText.setText(user.password);
        passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);

        memberSurname = (ViewGroup) v.findViewById(R.id.profile_surname);
        ((TextView)memberSurname.getChildAt(0)).setText(R.string.surname);
        EditText surnameEditText = (EditText)memberSurname.getChildAt(2);
        surnameEditText.setText(user.surname);
        surnameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);


        memberName = (ViewGroup) v.findViewById(R.id.profile_name);
        ((TextView)memberName.getChildAt(0)).setText(R.string.name);
        EditText nameEditText = (EditText)memberName.getChildAt(2);
        ((EditText)memberName.getChildAt(2)).setText(user.name);
        nameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberSecondName = (ViewGroup) v.findViewById(R.id.profile_second_name);
        ((TextView)memberSecondName.getChildAt(0)).setText(R.string.second_name);
        EditText secondNameEditText = (EditText)memberSecondName.getChildAt(2);
        secondNameEditText.setText(user.secondName);
        secondNameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberPhone = (ViewGroup) v.findViewById(R.id.profile_phone);
        ((TextView)memberPhone.getChildAt(0)).setText(R.string.phone);
        EditText phoneEditText = (EditText)memberPhone.getChildAt(2);
        phoneEditText.setText(user.phone);
        phoneEditText.setInputType(InputType.TYPE_CLASS_PHONE);

        TextView userNameTextView = (TextView)v.findViewById(R.id.userNameTextView);
        userNameTextView.setText(user.name + " " + user.surname);

        return v;
    }

    public PunisherUser getUser(){
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        boolean b = preferences.contains(Globals.USER_SURNAME);
        PunisherUser result = new PunisherUser(
                preferences.getString(Globals.USER_EMAIL, ""),
                preferences.getString(Globals.USER_PASSWORD, ""),
                preferences.getString(Globals.USER_SURNAME, ""),
                preferences.getString(Globals.USER_NAME, ""),
                preferences.getString(Globals.USER_SECOND_NAME, ""),
                preferences.getString(Globals.USER_PHONE, "")
        );
        return result;
    }

    public void saveUser(PunisherUser user){
        preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        preferences.edit()
                .putString(Globals.USER_EMAIL, user.email)
                .putString(Globals.USER_PASSWORD, user.password)
                .putString(Globals.USER_SURNAME, user.surname)
                .putString(Globals.USER_NAME, user.name)
                .putString(Globals.USER_SECOND_NAME, user.secondName)
                .putString(Globals.USER_PHONE, user.phone).apply();

    }
}
