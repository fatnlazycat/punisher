package org.foundation101.karatel.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.MainActivity;
import org.foundation101.karatel.activity.TipsActivity;
import org.foundation101.karatel.entity.PunisherUser;
import org.foundation101.karatel.manager.CameraManager;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.manager.PermissionManager;
import org.foundation101.karatel.utils.MediaUtils;
import org.foundation101.karatel.utils.MultipartUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.foundation101.karatel.fragment.ChangeAvatarFragment.PICK_IMAGE;
import static org.foundation101.karatel.manager.PermissionManager.CAMERA_PERMISSIONS_PHOTO;
import static org.foundation101.karatel.manager.PermissionManager.STORAGE_PERMISSION;

public class ProfileFragment extends Fragment {
    static final String TAG = "Profile";
    static final int CHANGE_AVATAR_DIALOG = 500;

    boolean avatarChanged = false;

    ImageView avatarView;
    ViewGroup memberEmail, memberPassword, memberSurname, memberName, memberSecondName, memberPhone;
    EditText surnameEditText, nameEditText, secondNameEditText, phoneEditText, emailEditText, passwordEditText;
    View progressBar;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //Google Analytics part
        KaratelApplication.getInstance().sendScreenName(TAG);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final Activity activity = getActivity();
        if (activity != null && activity instanceof MainActivity) {
            Toolbar toolbar = ((MainActivity) activity).toolbar;
            toolbar.inflateMenu(R.menu.profile_fragment_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (progressBar.getVisibility() != View.VISIBLE) new ProfileSaver(activity).execute();
                    return false;
                }
            });
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        progressBar = v.findViewById(R.id.rlProgress);

        memberEmail = v.findViewById(R.id.profile_email);
        ((TextView)memberEmail.getChildAt(0)).setText(R.string.email);
        memberEmail.getChildAt(1).setVisibility(View.GONE);
        emailEditText = (EditText) memberEmail.getChildAt(2);
        //emailEditText.setEnabled(false);//! - disabled views don't receive neither onClick nor onTouch events!
        //emailEditText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEditText.setFocusable(false);
        emailEditText.setAlpha(0.4f);
        emailEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) ((MainActivity)getActivity()).changeEmail(v);
            }
        });

        memberPassword = v.findViewById(R.id.profile_password);
        ((TextView)memberPassword.getChildAt(0)).setText(R.string.passw);
        memberPassword.getChildAt(1).setVisibility(View.GONE);
        passwordEditText = (EditText)memberPassword.getChildAt(2);
        passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordEditText.setFocusable(false);
        passwordEditText.setAlpha(0.4f);
        passwordEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) ((MainActivity)getActivity()).changePassword(v);
            }
        });

        memberSurname = v.findViewById(R.id.profile_surname);
        ((TextView)memberSurname.getChildAt(0)).setText(R.string.surname);
        memberSurname.getChildAt(1).setVisibility(View.GONE);
        surnameEditText = (EditText)memberSurname.getChildAt(2);
        surnameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberName = v.findViewById(R.id.profile_name);
        ((TextView)memberName.getChildAt(0)).setText(R.string.name);
        memberName.getChildAt(1).setVisibility(View.GONE);
        nameEditText = (EditText)memberName.getChildAt(2);
        nameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberSecondName = v.findViewById(R.id.profile_second_name);
        ((TextView)memberSecondName.getChildAt(0)).setText(R.string.second_name);
        memberSecondName.getChildAt(1).setVisibility(View.GONE);
        secondNameEditText = (EditText)memberSecondName.getChildAt(2);
        secondNameEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        memberPhone = v.findViewById(R.id.profile_phone);
        ((TextView)memberPhone.getChildAt(0)).setText(R.string.phone);
        memberPhone.getChildAt(1).setVisibility(View.GONE);
        phoneEditText = (EditText)memberPhone.getChildAt(2);
        phoneEditText.setInputType(InputType.TYPE_CLASS_PHONE);

        TextView userNameTextView = v.findViewById(R.id.userNameTextView);
        userNameTextView.setText(Globals.user.name + " " + Globals.user.surname);

        avatarView = v.findViewById(R.id.avatarProfileImageView);
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialog = new ChangeAvatarFragment();
                //setTargetFragment(ProfileFragment.this, CHANGE_AVATAR_DIALOG);
                dialog.show(getChildFragmentManager(), "changeAvatar");
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (HttpHelper.internetConnected(/*getActivity()*/)) {
            new ProfileFetcher(getActivity()).execute(Globals.user.id);
        }
        fillTextFields();
        ((MainActivity)getActivity()).setAvatarImageView(avatarView);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean granted = PermissionManager.allGranted(grantResults);
        if (granted) switch (requestCode) {
            case CAMERA_PERMISSIONS_PHOTO : {
                startCamera();
                break;
            }
            case STORAGE_PERMISSION : {
                startGallery();
                break;
            }
        }
    }

    void startCamera() {
        CameraManager cameraManager = CameraManager.getInstance(getActivity());
        cameraManager.startCamera(CameraManager.IMAGE_CAPTURE_INTENT);
    }

    void startGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        startActivityForResult(Intent.createChooser(intent,
                getResources().getString(R.string.choose_picture)), PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    Log.e("Punisher", "data=null");
                } else {
                    InputStream inputStream = getContext().getContentResolver().openInputStream(data.getData());
                    Bitmap bigImage = BitmapFactory.decodeStream(inputStream, null, options);
                    int orientation = MediaUtils.getOrientation(getActivity(), data.getData());
                    setNewAvatar(MediaUtils.rotateBitmap(bigImage, orientation));
                }
            }
            if (requestCode == CameraManager.IMAGE_CAPTURE_INTENT && resultCode == Activity.RESULT_OK) {
                //no need to call CameraManager.setLastCapturedFile because with built in camera intent
                //we call cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mediaFileUri); (see CameraManager.startCamera())
                // and the result is in the path provided
                //the below line will be needed if we switch to CustomCamera
                //CameraManager.setLastCapturedFile(data.getStringExtra(eu.aejis.mycustomcamera.IntentExtras.MEDIA_FILE));
                Bitmap bigImage = BitmapFactory.decodeFile(CameraManager.lastCapturedFile, options);

                int orientation = MediaUtils.getOrientation(CameraManager.lastCapturedFile);

                setNewAvatar(MediaUtils.rotateBitmap(bigImage, orientation));
                boolean b = new File(CameraManager.lastCapturedFile).delete();
            }
        } catch (IOException | NullPointerException e) {
            Globals.showError(R.string.error, e);
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

    private class ProfileSaver extends AsyncTask<Void, Void, String>{
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
            if (HttpHelper.internetConnected(/*getActivity()*/)) {
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
                        Globals.showError(R.string.cannot_connect_server, e);
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
                        if (getContext() != null)
                            Toast.makeText(getContext(), R.string.profile_changes_saved, Toast.LENGTH_LONG).show();
                        Globals.user.name = name;
                        Globals.user.surname = surname;
                        Globals.user.secondName = secondName;
                        Globals.user.phone = phone;

                        KaratelPreferences.saveUserWithAvatar
                                (surname, name, secondName, phone, Globals.user.avatarFileName);
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
                Globals.showError(R.string.error, eJSON);
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
                Globals.showError(R.string.cannot_connect_server, e);
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

                    KaratelPreferences.saveUserWithEmail(
                            dataJSON.getString("email"),
                            dataJSON.getString("surname"),
                            dataJSON.getString("firstname"),
                            dataJSON.getString("secondname"),
                            dataJSON.getString("phone_number"));

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
                Globals.showError(R.string.error, e);
            }
            progressBar.setVisibility(View.GONE);
        }
    }
}
