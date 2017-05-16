package org.foundation101.karatel.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.Karatel;
import org.foundation101.karatel.MultipartUtility;
import org.foundation101.karatel.PunisherUser;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.MainActivity;
import org.foundation101.karatel.activity.TipsActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ProfileFragment extends Fragment {
    static final String TAG = "Profile";
    static final int CHANGE_AVATAR_DIALOG = 500;

    boolean avatarChanged = false;

    ImageView avatarView;
    ViewGroup memberEmail, memberPassword, memberSurname, memberName, memberSecondName, memberPhone;
    EditText surnameEditText, nameEditText, secondNameEditText, phoneEditText, emailEditText, passwordEditText;
    FrameLayout progressBar;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //Google Analytics part
        ((Karatel)getActivity().getApplication()).sendScreenName(TAG);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Toolbar toolbar = ((MainActivity)getActivity()).toolbar;
        toolbar.inflateMenu(R.menu.profile_fragment_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new ProfileSaver(getActivity()).execute();
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        progressBar = (FrameLayout) v.findViewById(R.id.frameLayoutProgress);

        memberEmail = (ViewGroup) v.findViewById(R.id.profile_email);
        ((TextView)memberEmail.getChildAt(0)).setText(R.string.email);
        memberEmail.getChildAt(1).setVisibility(View.GONE);
        emailEditText = (EditText) memberEmail.getChildAt(2);
        emailEditText.setEnabled(false);
        //emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        memberPassword = (ViewGroup) v.findViewById(R.id.profile_password);
        ((TextView)memberPassword.getChildAt(0)).setText(R.string.passw);
        memberPassword.getChildAt(1).setVisibility(View.GONE);
        passwordEditText = (EditText)memberPassword.getChildAt(2);
        passwordEditText.setEnabled(false);
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        memberSurname = (ViewGroup) v.findViewById(R.id.profile_surname);
        ((TextView)memberSurname.getChildAt(0)).setText(R.string.surname);
        memberSurname.getChildAt(1).setVisibility(View.GONE);
        surnameEditText = (EditText)memberSurname.getChildAt(2);
        surnameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberName = (ViewGroup) v.findViewById(R.id.profile_name);
        ((TextView)memberName.getChildAt(0)).setText(R.string.name);
        memberName.getChildAt(1).setVisibility(View.GONE);
        nameEditText = (EditText)memberName.getChildAt(2);
        nameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberSecondName = (ViewGroup) v.findViewById(R.id.profile_second_name);
        ((TextView)memberSecondName.getChildAt(0)).setText(R.string.second_name);
        memberSecondName.getChildAt(1).setVisibility(View.GONE);
        secondNameEditText = (EditText)memberSecondName.getChildAt(2);
        secondNameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberPhone = (ViewGroup) v.findViewById(R.id.profile_phone);
        ((TextView)memberPhone.getChildAt(0)).setText(R.string.phone);
        memberPhone.getChildAt(1).setVisibility(View.GONE);
        phoneEditText = (EditText)memberPhone.getChildAt(2);
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

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (HttpHelper.internetConnected(getActivity())) {
            new ProfileFetcher(getActivity()).execute(Globals.user.id);
        }
        fillTextFields();
        ((MainActivity)getActivity()).setAvatarImageView(avatarView);
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
                    int orientation = Karatel.getOrientation(getActivity(), data.getData());
                    setNewAvatar(Karatel.rotateBitmap(bigImage, orientation));
                }
            }
            if (requestCode == CameraManager.IMAGE_CAPTURE_INTENT && resultCode == Activity.RESULT_OK) {
                Bitmap bigImage = BitmapFactory.decodeFile(CameraManager.lastCapturedFile, options);

                int orientation = Karatel.getOrientation(CameraManager.lastCapturedFile);

                setNewAvatar(Karatel.rotateBitmap(bigImage, orientation));
                boolean b = new File(CameraManager.lastCapturedFile).delete();
            }
        } catch (IOException e) {
            Globals.showError(getActivity(), R.string.error, e);
        }
    }

    void fillTextFields(){
        surnameEditText.setText(Globals.user.surname);
        nameEditText.setText(Globals.user.name);
        secondNameEditText.setText(Globals.user.secondName);
        phoneEditText.setText(Globals.user.phone);
        emailEditText.setText(Globals.user.email);
        passwordEditText.setText("qwerty"); //just to show 6 dots
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
        avatarChanged = true;
    }

    class ProfileSaver extends AsyncTask<Void, Void, String>{
        String name, surname, secondName, phone;
        Activity activity;

        ProfileSaver(Activity a){
            this.activity = a;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            name = nameEditText.getText().toString().replace(" ", "");
            surname = surnameEditText.getText().toString().replace(" ", "");
            secondName = secondNameEditText.getText().toString().replace(" ", "");
            phone = phoneEditText.getText().toString();
        }

        @Override
        protected String doInBackground(Void... params) {
            StringBuilder response = new StringBuilder();
            if (HttpHelper.internetConnected(getActivity())) {
                int tries = 0;
                final int MAX_TRIES = 2;
                while (tries++ < MAX_TRIES) try {
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
                        if (avatarChanged) {
                            multipart.addFilePart("user[avatar]", new File(Globals.user.avatarFileName));
                            avatarChanged = false;
                        }
                    } else {
                        multipart.addFormField("user[avatar]", ""); //delete avatar
                    }

                    List<String> responseList = multipart.finish();
                    for (String line : responseList) {
                        Log.e("Punisher", "Upload Files Response:::" + line);
                        response.append(line);
                    }
                    return response.toString();
                } catch (final IOException e) {
                    if (tries == MAX_TRIES) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Globals.showError(getActivity(), R.string.cannot_connect_server, e);
                            }
                        });
                    }
                }
            } else {
                response.append(HttpHelper.ERROR_JSON);
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            try{
                JSONObject json = new JSONObject(s);
                switch (json.getString("status")){
                    case Globals.SERVER_SUCCESS : {
                        Toast.makeText(getContext(), R.string.profile_changes_saved, Toast.LENGTH_LONG).show();
                        Globals.user.name = name;
                        Globals.user.surname = surname;
                        Globals.user.secondName = secondName;
                        Globals.user.phone = phone;

                        PreferenceManager.getDefaultSharedPreferences(activity).edit()
                                .putString(Globals.USER_SURNAME, surname)
                                .putString(Globals.USER_NAME, name)
                                .putString(Globals.USER_SECOND_NAME, secondName)
                                .putString(Globals.USER_PHONE, phone)
                                .putString(Globals.USER_AVATAR, Globals.user.avatarFileName).apply();
                        break;
                    }
                    case Globals.SERVER_ERROR : {
                        StringBuilder errorMessage = new StringBuilder();
                        JSONObject errorJSON = json.getJSONObject(Globals.SERVER_ERROR);
                        JSONArray errorNames = errorJSON.names();
                        for (int i = 0; i < errorNames.length(); i++){
                            //read only the first message in the array for each error type
                            String oneMessage = errorJSON.getJSONArray(errorNames.getString(i)).getString(0);
                            errorMessage.append(oneMessage + "\n");
                        }
                        Toast.makeText(activity, errorMessage.toString(), Toast.LENGTH_LONG).show();
                        break;
                    }
                    default:{

                    }
                }
            } catch (JSONException eJSON){
                Globals.showError(activity, R.string.error, eJSON);
            }
        }
    }

    class ProfileFetcher extends AsyncTask<Integer, Void, String>{
        Activity activity;

        ProfileFetcher(Activity a){
            this.activity = a;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Integer... params) {
            try {
                return HttpHelper.proceedRequest("users/" + params[0], "GET", "", true);
            } catch (final IOException e){
                if (activity != null) activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(getActivity(), R.string.cannot_connect_server, e);
                    }
                });
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals("success")) {
                    JSONObject dataJSON = json.getJSONObject("data");
                    Globals.user = new PunisherUser(
                            dataJSON.getString("email"),
                            "", //for password
                            dataJSON.getString("surname"),
                            dataJSON.getString("firstname"),
                            dataJSON.getString("secondname"),
                            dataJSON.getString("phone_number"));
                    Globals.user.id = dataJSON.getInt("id");

                    PreferenceManager.getDefaultSharedPreferences(activity).edit()
                            .putString(Globals.USER_EMAIL, dataJSON.getString("email"))
                            .putString(Globals.USER_SURNAME, dataJSON.getString("surname"))
                            .putString(Globals.USER_NAME, dataJSON.getString("firstname"))
                            .putString(Globals.USER_SECOND_NAME, dataJSON.getString("secondname"))
                            .putString(Globals.USER_PHONE, dataJSON.getString("phone_number")).apply();

                    String avatarUrl = dataJSON.getJSONObject("avatar").getString("url");
                    if (avatarUrl != null && !avatarUrl.equals("null")) {
                        TipsActivity.AvatarGetter avatarGetter = new TipsActivity.AvatarGetter(activity);
                        avatarGetter.setViewToSet(avatarView);
                        avatarGetter.execute(avatarUrl);
                    }

                    fillTextFields();

                } else {
                    String errorMessage;
                    if (json.getString("status").equals("error")){
                        errorMessage = json.getString("error");
                    } else {
                        errorMessage = s;
                    }
                    Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Globals.showError(activity, R.string.error, e);
            }
            progressBar.setVisibility(View.GONE);
        }
    }
}
