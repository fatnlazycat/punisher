package org.foundation101.karatel.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.MultipartUtility;
import org.foundation101.karatel.PunisherUser;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.MainActivity;
import org.foundation101.karatel.activity.ViolationActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ProfileFragment extends Fragment {
    static final int CHANGE_AVATAR_DIALOG = 500;

    ImageView avatarView;
    ViewGroup memberEmail, memberPassword, memberSurname, memberName, memberSecondName, memberPhone;
    EditText surnameEditText, nameEditText, secondNameEditText, phoneEditText;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Toolbar toolbar = ((MainActivity)getActivity()).toolbar;
        toolbar.inflateMenu(R.menu.profile_fragment_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new ProfileSaver().execute();
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        memberEmail = (ViewGroup) v.findViewById(R.id.profile_email);
        ((TextView)memberEmail.getChildAt(0)).setText(R.string.email);
        memberEmail.getChildAt(1).setVisibility(View.GONE);
        TextView emailEditText = (TextView)memberEmail.getChildAt(2);
        emailEditText.setText(Globals.user.email);
        emailEditText.setEnabled(false);
        //emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        memberPassword = (ViewGroup) v.findViewById(R.id.profile_password);
        ((TextView)memberPassword.getChildAt(0)).setText(R.string.passw);
        memberPassword.getChildAt(1).setVisibility(View.GONE);
        EditText passwordEditText = (EditText)memberPassword.getChildAt(2);
        passwordEditText.setText("qwerty"); //just to show 6 dots
        passwordEditText.setEnabled(false);
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        memberSurname = (ViewGroup) v.findViewById(R.id.profile_surname);
        ((TextView)memberSurname.getChildAt(0)).setText(R.string.surname);
        memberSurname.getChildAt(1).setVisibility(View.GONE);
        surnameEditText = (EditText)memberSurname.getChildAt(2);
        surnameEditText.setText(Globals.user.surname);
        surnameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberName = (ViewGroup) v.findViewById(R.id.profile_name);
        ((TextView)memberName.getChildAt(0)).setText(R.string.name);
        memberName.getChildAt(1).setVisibility(View.GONE);
        nameEditText = (EditText)memberName.getChildAt(2);
        nameEditText.setText(Globals.user.name);
        nameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberSecondName = (ViewGroup) v.findViewById(R.id.profile_second_name);
        ((TextView)memberSecondName.getChildAt(0)).setText(R.string.second_name);
        memberSecondName.getChildAt(1).setVisibility(View.GONE);
        secondNameEditText = (EditText)memberSecondName.getChildAt(2);
        secondNameEditText.setText(Globals.user.secondName);
        secondNameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberPhone = (ViewGroup) v.findViewById(R.id.profile_phone);
        ((TextView)memberPhone.getChildAt(0)).setText(R.string.phone);
        memberPhone.getChildAt(1).setVisibility(View.GONE);
        phoneEditText = (EditText)memberPhone.getChildAt(2);
        phoneEditText.setText(Globals.user.phone);
        phoneEditText.setInputType(InputType.TYPE_CLASS_PHONE);

        TextView userNameTextView = (TextView)v.findViewById(R.id.userNameTextView);
        userNameTextView.setText(Globals.user.name + " " + Globals.user.surname);

        avatarView = (ImageView)v.findViewById(R.id.avatarProfileImageView);
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialog = new ChangeAvatarFragment();
                setTargetFragment(ProfileFragment.this, CHANGE_AVATAR_DIALOG);
                dialog.show(getChildFragmentManager(), "changeAvatar");
            }
        });
        ((MainActivity)getActivity()).setAvatarImageView(avatarView);
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            if (requestCode == ChangeAvatarFragment.PICK_IMAGE && resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    Log.e("Punisher", "data=null");
                } else {
                    InputStream inputStream = getContext().getContentResolver().openInputStream(data.getData());
                    Bitmap bigImage = BitmapFactory.decodeStream(inputStream, null, options);
                    setNewAvatar(bigImage);
                }
            }
            if (requestCode == CameraManager.IMAGE_CAPTURE_INTENT && resultCode == Activity.RESULT_OK) {
                Bitmap bigImage = BitmapFactory.decodeFile(CameraManager.lastCapturedFile, options);
                setNewAvatar(bigImage);
                boolean b = new File(CameraManager.lastCapturedFile).delete();
            }
        } catch (IOException e) {
            Globals.showError(getActivity(), R.string.error, e);
        }
    }

    public void setNewAvatar(Bitmap image) throws IOException {
        if (image == null){
            Globals.user.avatarFileName = "";
            avatarView.setBackgroundResource(R.mipmap.no_avatar);
        } else {
            int dimension = getResources().getDimensionPixelOffset(R.dimen.thumbnail_size);
            BitmapDrawable avatar = new BitmapDrawable(getResources(),
                    ThumbnailUtils.extractThumbnail(image, dimension, dimension));
            Bitmap bitmap = avatar.getBitmap();

            if (Globals.user.avatarFileName == null || Globals.user.avatarFileName.isEmpty()) {
                Globals.user.avatarFileName = getContext().getFilesDir() + "avatar" + Globals.user.id + CameraManager.PNG;
            }
            FileOutputStream os = new FileOutputStream(Globals.user.avatarFileName);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
            os.close();
            avatarView.setBackground(avatar);
        }
    }

    class ProfileSaver extends AsyncTask<Void, Void, String>{
        String name, surname, secondName, phone;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            name = nameEditText.getText().toString();
            surname = surnameEditText.getText().toString();
            secondName = secondNameEditText.getText().toString();
            phone = phoneEditText.getText().toString();
        }

        @Override
        protected String doInBackground(Void... params) {
            StringBuilder response = new StringBuilder();
            try {
                String requestUrl = Globals.SERVER_URL + "users/" + Globals.user.id;
                MultipartUtility multipart = new MultipartUtility(requestUrl, "UTF-8", "PUT");
                //multipart.addFormField("user[email]", Globals.user.email);
                multipart.addFormField("user[firstname]", name);
                multipart.addFormField("user[surname]", surname);
                multipart.addFormField("user[secondname]", secondName);
                multipart.addFormField("user[phone_number]", phone);
                //multipart.addFormField("user[password]", "qwerty"); //Globals.user.password
                //multipart.addFormField("user[password_confirmation]", "qwerty");//Globals.user.password

                if (Globals.user.avatarFileName != null && !Globals.user.avatarFileName.isEmpty()) {
                    multipart.addFilePart("user[avatar]", new File(Globals.user.avatarFileName));
                } else {
                    multipart.addFormField("user[avatar]", ""); //delete avatar
                }

                List<String> responseList = multipart.finish();
                for (String line : responseList) {
                    Log.e("Punisher", "Upload Files Response:::" + line);
                    response.append(line);
                }
            } catch (final IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(getActivity(), R.string.cannot_connect_server, e);
                    }
                });
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            try{
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals(Globals.SERVER_SUCCESS)){
                    Toast.makeText(getContext(), R.string.profile_changes_saved, Toast.LENGTH_LONG).show();
                    Globals.user.name = name;
                    Globals.user.surname = surname;
                    Globals.user.secondName = secondName;
                }
            } catch (JSONException eJSON){
                Globals.showError(getContext(), R.string.error, eJSON);
            }
        }
    }
}
