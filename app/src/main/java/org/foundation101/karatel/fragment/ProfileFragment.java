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

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.MultipartUtility;
import org.foundation101.karatel.PunisherUser;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ProfileFragment extends Fragment {
    static final int CHANGE_AVATAR_DIALOG = 500;
    //public static final int NEW_PHOTO_FOR_AVATAR = 600;

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
        EditText surnameEditText = (EditText)memberSurname.getChildAt(2);
        surnameEditText.setText(Globals.user.surname);
        surnameEditText.setEnabled(false);
        //surnameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberName = (ViewGroup) v.findViewById(R.id.profile_name);
        ((TextView)memberName.getChildAt(0)).setText(R.string.name);
        memberName.getChildAt(1).setVisibility(View.GONE);
        EditText nameEditText = (EditText)memberName.getChildAt(2);
        nameEditText.setText(Globals.user.name);
        nameEditText.setEnabled(false);
        //nameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberSecondName = (ViewGroup) v.findViewById(R.id.profile_second_name);
        ((TextView)memberSecondName.getChildAt(0)).setText(R.string.second_name);
        memberSecondName.getChildAt(1).setVisibility(View.GONE);
        EditText secondNameEditText = (EditText)memberSecondName.getChildAt(2);
        secondNameEditText.setText(Globals.user.secondName);
        secondNameEditText.setEnabled(false);
        //secondNameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberPhone = (ViewGroup) v.findViewById(R.id.profile_phone);
        ((TextView)memberPhone.getChildAt(0)).setText(R.string.phone);
        memberPhone.getChildAt(1).setVisibility(View.GONE);
        EditText phoneEditText = (EditText)memberPhone.getChildAt(2);
        phoneEditText.setText(Globals.user.phone);
        phoneEditText.setEnabled(false);
        //phoneEditText.setInputType(InputType.TYPE_CLASS_PHONE);

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
        new ProfileSaver().execute();
    }

    class ProfileSaver extends AsyncTask<Void, Void, String>{

        @Override
        protected String doInBackground(Void... params) {
            StringBuilder response = new StringBuilder();
            try {
                String requestUrl = Globals.SERVER_URL + "users/" + Globals.user.id;
                MultipartUtility multipart = new MultipartUtility(requestUrl, "UTF-8", "PUT");
                multipart.addFormField("user[email]", Globals.user.email);
                multipart.addFormField("user[firstname]", Globals.user.name);
                multipart.addFormField("user[surname]", Globals.user.surname);
                multipart.addFormField("user[secondname]", Globals.user.secondName);
                multipart.addFormField("user[phone_number]", Globals.user.phone);
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
            } catch (IOException e) {
                Log.e("Punisher error", e.getMessage());
            }
            return response.toString();
        }
    }
}
