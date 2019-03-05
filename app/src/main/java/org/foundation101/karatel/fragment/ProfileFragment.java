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
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.MainActivity;
import org.foundation101.karatel.activity.TipsActivity;
import org.foundation101.karatel.asyncTasks.AsyncTaskAction;
import org.foundation101.karatel.asyncTasks.ProfileFetcher;
import org.foundation101.karatel.asyncTasks.ProfileSaver;
import org.foundation101.karatel.entity.PunisherUser;
import org.foundation101.karatel.manager.CameraManager;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.manager.PermissionManager;
import org.foundation101.karatel.utils.FileUtils;
import org.foundation101.karatel.utils.MediaUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static org.foundation101.karatel.fragment.ChangeAvatarFragment.PICK_IMAGE;
import static org.foundation101.karatel.manager.PermissionManager.CAMERA_PERMISSIONS_PHOTO;
import static org.foundation101.karatel.manager.PermissionManager.STORAGE_PERMISSION;

public class ProfileFragment extends Fragment {
    static final String TAG = "Profile";
    static final String PROFILE_VALUES = "PROFILE_VALUES";
    static final String NEW_AVATAR = "NEW_AVATAR";

    boolean textChanged   = false;
    boolean saveInstanceStateCalled = false;

    Toolbar toolbar;
    ImageView avatarView;
    ViewGroup memberEmail, memberPassword, memberSurname, memberName, memberSecondName, memberPhone;
    EditText surnameEditText, nameEditText, secondNameEditText, phoneEditText, emailEditText, passwordEditText;
    TextView userNameTextView;
    View progressBar;
    SwipeRefreshLayout srl;

    String tempAvatarFileName;

    AsyncTask profileFetcher;

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override
        public void afterTextChanged(Editable s) {
            if (!textChanged ) invalidateOptionsMenu();
            textChanged = true;
        }
    };

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        //Google Analytics part
        KaratelApplication.getInstance().sendScreenName(TAG);

        if (savedInstanceState != null) {
            String temp = savedInstanceState.getString(NEW_AVATAR);
            if (temp != null) tempAvatarFileName = temp;
        } else { //clear temporary file in case the previous fragment unexpectedly destroyed after saveInstanceState
            File tmpFile = new File(FileUtils.INSTANCE.avatarFileName(true));
            if (tmpFile.exists()) tmpFile.delete();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        final Activity activity = getActivity();
        if (activity != null && activity instanceof MainActivity) {
            toolbar = ((MainActivity) activity).toolbar;
            toolbar.inflateMenu(R.menu.profile_fragment_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (progressBar.getVisibility() != View.VISIBLE) {
                        PunisherUser userToSave = new PunisherUser(
                            emailEditText.getText().toString(),
                            passwordEditText.getText().toString(),
                            surnameEditText.getText().toString().replace(" ", ""),
                            nameEditText.getText().toString().replace(" ", ""),
                            secondNameEditText.getText().toString().replace(" ", ""),
                            phoneEditText.getText().toString()
                        );
                        new ProfileSaver(
                                new ProfileSaverActions(ProfileFragment.this), userToSave, tempAvatarFileName
                        ).execute();
                    }
                    return false;
                }
            });

            MenuItem item = menu.findItem(R.id.saveProfileMenuItem);
            item.setVisible(changesMade());
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        srl = v.findViewById(R.id.srlProfileFragment);
        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                profileFetcher = new ProfileFetcher(new ProfileFetcherActions(ProfileFragment.this))
                        .execute(KaratelPreferences.userId());
            }
        });

        userNameTextView = v.findViewById(R.id.userNameTextView);

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
        surnameEditText.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        memberName = v.findViewById(R.id.profile_name);
        ((TextView)memberName.getChildAt(0)).setText(R.string.name);
        memberName.getChildAt(1).setVisibility(View.GONE);
        nameEditText = (EditText)memberName.getChildAt(2);
        nameEditText.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        memberSecondName = v.findViewById(R.id.profile_second_name);
        ((TextView)memberSecondName.getChildAt(0)).setText(R.string.second_name);
        memberSecondName.getChildAt(1).setVisibility(View.GONE);
        secondNameEditText = (EditText)memberSecondName.getChildAt(2);
        secondNameEditText.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        memberPhone = v.findViewById(R.id.profile_phone);
        ((TextView)memberPhone.getChildAt(0)).setText(R.string.phone);
        memberPhone.getChildAt(1).setVisibility(View.GONE);
        phoneEditText = (EditText)memberPhone.getChildAt(2);
        phoneEditText.setInputType(InputType.TYPE_CLASS_PHONE);

        avatarView = v.findViewById(R.id.avatarProfileImageView);
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialog = new ChangeAvatarFragment();
                dialog.show(getChildFragmentManager(), "changeAvatar");
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String avatarFileName = tempAvatarFileName == null ? KaratelPreferences.userAvatar() : tempAvatarFileName;
        ((MainActivity) getActivity()).setAvatarImageView(avatarView, avatarFileName);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        ArrayList<String> savedValues = null;
        if (savedInstanceState != null) {
            savedValues = savedInstanceState.getStringArrayList(PROFILE_VALUES);
        }
        fillTextFields(savedValues);

        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        saveInstanceStateCalled = false;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        saveInstanceStateCalled = true;

        if (textChanged) {
            ArrayList<String> valuesToSave = new ArrayList<>();
            TextView[] textViews = editableViews();
            for (TextView v : textViews) {
                valuesToSave.add(v.getText().toString());
            }
            outState.putStringArrayList(PROFILE_VALUES, valuesToSave);
        }
        if (tempAvatarFileName != null) outState.putString(NEW_AVATAR, tempAvatarFileName);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        if (!saveInstanceStateCalled && tempAvatarFileName != null) {
            File tmpFile = new File(tempAvatarFileName);
            if (tmpFile.exists()) tmpFile.delete();
        }

        if (toolbar != null) toolbar.setOnMenuItemClickListener(null);

        super.onDestroy();
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
        synchronized (ProfileFetcher.TAG) {
            try {
                if (profileFetcher != null) profileFetcher.cancel(false);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
                    if (data == null) {
                        Log.e(TAG, "data=null");
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
    }

    void fillTextFields(ArrayList<String> savedValues){
        PunisherUser user = KaratelPreferences.user();
        TextView[] textViews = editableViews();

        if (savedValues == null) {
            String[] data = {user.surname, user.name, user.secondName, user.phone};
            savedValues = new ArrayList<>(Arrays.asList(data));
        } else {
            textChanged = true;
        }

        int listSize = textViews.length;
        for (int i = 0; i < listSize; i++) {
            textViews[i].removeTextChangedListener(textWatcher);
            textViews[i].setText(savedValues.get(i));
            textViews[i].addTextChangedListener(textWatcher);
        }

        userNameTextView.setText(user.name + " " + user.surname);
        emailEditText   .setText(user.email);
        passwordEditText.setText("qwerty"); //just to show 6 dots
    }

    TextView[] editableViews() {
        return new TextView[]{surnameEditText, nameEditText, secondNameEditText, phoneEditText};
    }

    boolean changesMade() { return tempAvatarFileName != null || textChanged; }

    void invalidateOptionsMenu() {
        Activity activity = getActivity();
        if (activity != null ) activity.invalidateOptionsMenu();
    }

    public void setNewAvatar(Bitmap image) throws IOException {
        if (image == null){
            if (tempAvatarFileName != null && !tempAvatarFileName.isEmpty()) {
                new File(tempAvatarFileName).delete();
            }
            tempAvatarFileName = "";
            avatarView.setBackgroundResource(R.mipmap.no_avatar);
        } else {
            int dimension = getResources().getDimensionPixelOffset(R.dimen.thumbnail_size);
            BitmapDrawable avatar = new BitmapDrawable(getResources(),
                    ThumbnailUtils.extractThumbnail(image, dimension, dimension));
            Bitmap bitmap = avatar.getBitmap();

            if (tempAvatarFileName == null || tempAvatarFileName.isEmpty()) {
                tempAvatarFileName = FileUtils.INSTANCE.avatarFileName(true);
            }
            FileOutputStream os = new FileOutputStream(tempAvatarFileName);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.flush();
            os.close();
            avatarView.setBackground(avatar);
        }
        invalidateOptionsMenu();
    }


    private static class ProfileSaverActions extends AsyncTaskAction<Void, String, ProfileFragment> {
        ProfileSaverActions(ProfileFragment component) { super(component); }

        @Override
        public void pre(Void arg) { }

        @Override
        public void post(String status) {
            ProfileFragment fragment = ref.get();
            if (fragment != null && Globals.SERVER_SUCCESS.equals(status)) {
                fragment.tempAvatarFileName = null;
                fragment.textChanged = false;
                fragment.fillTextFields(null);
                fragment.invalidateOptionsMenu();
            }
        }
    }


    private static class ProfileFetcherActions extends AsyncTaskAction<Void, Void, ProfileFragment> {
        ProfileFetcherActions(ProfileFragment component) { super(component); }

        private void progressBarVisibility(int visibility) {
            ProfileFragment fragment = ref.get();
            if (fragment != null) {
                View progress = fragment.progressBar;
                if (progress != null) progress.setVisibility(visibility);

                SwipeRefreshLayout srl = fragment.srl;
                if (srl != null) srl.setRefreshing(false);
            }
        }

        @Override
        public void pre(Void arg) {
            progressBarVisibility(View.VISIBLE);
        }

        @Override
        public void post(Void arg) {
            ProfileFragment fragment = ref.get();
            if (fragment != null) {
                fragment.tempAvatarFileName = null;
                String avatarUrl = KaratelPreferences.user().avatarFileName;
                if (avatarUrl != null && !avatarUrl.equals("null")) {
                    TipsActivity.AvatarGetter avatarGetter = new TipsActivity.AvatarGetter(fragment.getActivity());
                    avatarGetter.setViewToSet(fragment.avatarView);
                    avatarGetter.execute(avatarUrl);
                }

                fragment.textChanged = false;
                fragment.fillTextFields(null);
                fragment.invalidateOptionsMenu();

                progressBarVisibility(View.GONE);
            }
        }

        @Override
        public void onCancel() {
            progressBarVisibility(View.GONE);
        }
    }
}
