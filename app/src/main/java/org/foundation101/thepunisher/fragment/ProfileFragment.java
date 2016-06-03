package org.foundation101.thepunisher.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.foundation101.thepunisher.CameraManager;
import org.foundation101.thepunisher.Globals;
import org.foundation101.thepunisher.MultipartUtility;
import org.foundation101.thepunisher.PunisherUser;
import org.foundation101.thepunisher.R;
import org.foundation101.thepunisher.RequestMaker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ProfileFragment extends Fragment {
    static final int CHANGE_AVATAR_DIALOG = 500;
    public static final int NEW_PHOTO_FOE_AVATAR = 600;

    ImageView avatarView;
    ViewGroup memberEmail, memberPassword, memberSurname, memberName, memberSecondName, memberPhone;
    SharedPreferences preferences;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        memberEmail = (ViewGroup) v.findViewById(R.id.profile_email);
        ((TextView)memberEmail.getChildAt(0)).setText(R.string.email);
        EditText emailEditText = (EditText)memberEmail.getChildAt(2);
        emailEditText.setText(Globals.user.email);
        emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        memberEmail.getChildAt(1).setVisibility(View.GONE);

        memberPassword = (ViewGroup) v.findViewById(R.id.profile_password);
        ((TextView)memberPassword.getChildAt(0)).setText(R.string.passw);
        EditText passwordEditText = (EditText)memberPassword.getChildAt(2);
        passwordEditText.setText(Globals.user.password);
        passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);

        memberSurname = (ViewGroup) v.findViewById(R.id.profile_surname);
        ((TextView)memberSurname.getChildAt(0)).setText(R.string.surname);
        EditText surnameEditText = (EditText)memberSurname.getChildAt(2);
        surnameEditText.setText(Globals.user.surname);
        surnameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberName = (ViewGroup) v.findViewById(R.id.profile_name);
        ((TextView)memberName.getChildAt(0)).setText(R.string.name);
        EditText nameEditText = (EditText)memberName.getChildAt(2);
        ((EditText)memberName.getChildAt(2)).setText(Globals.user.name);
        nameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberSecondName = (ViewGroup) v.findViewById(R.id.profile_second_name);
        ((TextView)memberSecondName.getChildAt(0)).setText(R.string.second_name);
        EditText secondNameEditText = (EditText)memberSecondName.getChildAt(2);
        secondNameEditText.setText(Globals.user.secondName);
        secondNameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberPhone = (ViewGroup) v.findViewById(R.id.profile_phone);
        ((TextView)memberPhone.getChildAt(0)).setText(R.string.phone);
        EditText phoneEditText = (EditText)memberPhone.getChildAt(2);
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
        if (Globals.user.avatarFileName == null || Globals.user.avatarFileName.isEmpty()){
            avatarView.setImageResource(R.mipmap.no_avatar);
        } else {
            avatarView.setImageURI(Uri.fromFile(new File(Globals.user.avatarFileName)));
        }

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (requestCode == ChangeAvatarFragment.PICK_IMAGE && resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    Log.e("Punisher", "data=null");
                } else {
                    InputStream inputStream = getContext().getContentResolver().openInputStream(data.getData());
                    Bitmap bigImage = BitmapFactory.decodeStream(inputStream);
                    setNewAvatar(bigImage);
                }
            }
            if (requestCode == CameraManager.IMAGE_CAPTURE_INTENT && resultCode == Activity.RESULT_OK) {
                Bitmap bigImage = BitmapFactory.decodeFile(CameraManager.lastCapturedFile);
                setNewAvatar(bigImage);
                boolean b = new File(CameraManager.lastCapturedFile).delete();
                //Toast.makeText(getContext(), "file deleted: " + b, Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.e("Punisher", e.getMessage());
        }
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

    public void setNewAvatar(Bitmap image) throws IOException {
        int dimension = getResources().getDimensionPixelOffset(R.dimen.thumbnail_size);
        BitmapDrawable avatar = new BitmapDrawable(getResources(),
                ThumbnailUtils.extractThumbnail(image, dimension, dimension));
        avatarView.setImageDrawable(avatar);
        Bitmap bitmap = avatar.getBitmap();

        if (Globals.user.avatarFileName == null || Globals.user.avatarFileName.isEmpty()) {
            Globals.user.avatarFileName = getContext().getFilesDir() + "avatar" + Globals.user.id + CameraManager.PNG;
        }
        FileOutputStream os = new FileOutputStream(Globals.user.avatarFileName);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.flush();
        os.close();
        new ProfileSaver().execute();
    }

    class ProfileSaver extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... params) {
            StringBuilder response = new StringBuilder();
            try {
                String requestUrl = Globals.SERVER_URL + "users/" + Globals.user.id;
                MultipartUtility multipart = new MultipartUtility(requestUrl, "UTF-8", "PUT");
                multipart.addFormField("user[firstname]", Globals.user.name);
                multipart.addFormField("user[surname]", Globals.user.surname);
                multipart.addFormField("user[secondname]", Globals.user.secondName);
                //multipart.addFormField("user[password]", "qwerty"); //Globals.user.password
                //multipart.addFormField("user[password_confirmation]", "qwerty");//Globals.user.password

                if (Globals.user.avatarFileName != null && !Globals.user.avatarFileName.isEmpty()) {
                    multipart.addFilePart("user[avatar]", new File(Globals.user.avatarFileName));
                } else {
                    multipart.addFormField("user[avatar]", ""); //delete avatar
                }

                List<String> responseList = multipart.finish();
                Log.e("Punisher", "SERVER REPLIED:");
                for (String line : responseList) {
                    Log.e("Punisher", "Upload Files Response:::" + line);
                    response.append(line);
                }
            } catch (IOException e) {
                Log.e("Punisher error", e.getMessage());
            }
            return response.toString();
        }
    }
}
