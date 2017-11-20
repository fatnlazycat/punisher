package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.KaratelPreferences;
import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.DrawerAdapter;
import org.foundation101.karatel.fragment.AboutFragment;
import org.foundation101.karatel.fragment.ComplainDraftsFragment;
import org.foundation101.karatel.fragment.ComplainsBookFragment;
import org.foundation101.karatel.fragment.ContactsFragment;
import org.foundation101.karatel.fragment.MainFragment;
import org.foundation101.karatel.fragment.NewsFragment;
import org.foundation101.karatel.fragment.PartnersFragment;
import org.foundation101.karatel.fragment.ProfileFragment;
import org.foundation101.karatel.fragment.RequestListFragment;
import org.foundation101.karatel.fragment.VideoListFragment;
import org.foundation101.karatel.retrofit.RetrofitSignOutSender;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    HashMap<Integer, String> fragmentTags;

    private static final String TAG = "MainActivity";

    public Toolbar toolbar;
    ImageView avatarImageView;
    TextView avatarTextView;
    FrameLayout progressBar;
    FragmentManager fManager;
    FragmentTransaction ft;
    int currentFragment;

    //facebook part
    CallbackManager fbCallbackManager;

    private final BroadcastReceiver myBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boolean justLogout = intent.getBooleanExtra(TAG_JUST_LOGOUT, false);
            if (justLogout){
                new SignOutSender(MainActivity.this).execute();
            } else {
                final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this, R.style.AppTheme);
                dialogBuilder.setTitle("Увага! Змінився токен Google Cloud Messaging.")
                        .setMessage("Вийдіть з програми та зайдіть знову, щоб отримувати сповіщення.")
                        .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                new SignOutSender(MainActivity.this).execute();
                            }
                        })
                        .setCancelable(false);
                final AlertDialog dialog = dialogBuilder.create();
                dialog.show();
            }
        }
    };
    public static final String BROADCAST_RECEIVER_TAG = "myBroadcastReceiver_MainActivity";
    public static final String TAG_JUST_LOGOUT = "myBroadcastReceiver_MainActivity_gcm_token_changed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fManager = getSupportFragmentManager();

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);

        KaratelPreferences.restoreUser();

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //init navigation drawer
        ListView drawerListView = (ListView)findViewById(R.id.drawerListView);
        //first line where we need avatar icon
        View drawerHeader = LayoutInflater.from(this).inflate(R.layout.drawer_header, drawerListView, false);
        drawerListView.addHeaderView(drawerHeader, null, false);

        avatarImageView = (ImageView) findViewById(R.id.avatarImageView);
        avatarTextView = (TextView)findViewById(R.id.avatarTextView);
        avatarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        initDrawerHeader();

        final DrawerLayout drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayoutMainActivity);
        final DrawerAdapter drawerAdapter = new DrawerAdapter();
        DrawerAdapter.content = makeDrawerList();
        drawerListView.setAdapter(drawerAdapter);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                drawerAdapter.notifyDataSetChanged();
                initDrawerHeader();
                Globals.hideSoftKeyboard(MainActivity.this);
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        fragmentTags = getFragmentTags();

        drawerListView.setOnItemClickListener(new ListView.OnItemClickListener(){
            public void onItemClick(AdapterView parent, View view, int position, long id){
                drawerLayout.closeDrawer(Gravity.LEFT);
                if (currentFragment == position) {//do not create duplicates
                    return;
                }
                String tag = fragmentTags.get(position);
                if (!tag.isEmpty()) { //empty tags are for donate & exit - no special fragment
                    currentFragment = position;
                    toolbar.setTitle(tag);
                }
                switch (position){
                    case Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new MainFragment(), tag)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case Globals.MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new RequestListFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case Globals.MAIN_ACTIVITY_COMPLAINS_BOOK_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new ComplainsBookFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case Globals.MAIN_ACTIVITY_VIDEO_LIST_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new VideoListFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case Globals.MAIN_ACTIVITY_DONATE: {
                        openDonatePage();
                        break;
                    }
                    case Globals.MAIN_ACTIVITY_ABOUT_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new AboutFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case Globals.MAIN_ACTIVITY_PARTNERS_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new PartnersFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case Globals.MAIN_ACTIVITY_NEWS_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new NewsFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                        case Globals.MAIN_ACTIVITY_CONTACTS_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new ContactsFragment(), tag)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case Globals.MAIN_ACTIVITY_PROFILE_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new ProfileFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case Globals.MAIN_ACTIVITY_EXIT: {
                        new SignOutSender(MainActivity.this).execute();
                    }
                }
            }
        });

        //select fragment to start with (from savedInstanceState)
        ft = fManager.beginTransaction();
        String tag = "";
        if (!loggedIn()){ //this happens when we tap push notification icon after logging out - we are not signed in so close the app
            finish();
        } else {
            if (KaratelPreferences.startedFromPush()) {
                KaratelPreferences.setStartedFromPush(false);
                currentFragment = Globals.MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT;
                tag = fragmentTags.get(currentFragment);
                ft.add(R.id.frameLayoutMain, new RequestListFragment(), tag).addToBackStack(tag).commit();
            } else {
                if (savedInstanceState != null) {
                    currentFragment = savedInstanceState.getInt(Globals.MAIN_ACTIVITY_SAVED_INSTANCE_STATE, 0);
                    tag = fragmentTags.get(currentFragment);

                    Fragment fragmentInstance = fManager.findFragmentByTag(tag);
                    if (fragmentInstance == null) {
                        switch (currentFragment) {
                            case Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT: {
                                fragmentInstance = new MainFragment();
                                break;
                            }
                            case Globals.MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT: {
                                fragmentInstance = new RequestListFragment();
                                break;
                            }
                            case Globals.MAIN_ACTIVITY_COMPLAINS_BOOK_FRAGMENT: {
                                fragmentInstance = new ComplainsBookFragment();
                                break;
                            }
                            case Globals.MAIN_ACTIVITY_VIDEO_LIST_FRAGMENT: {
                                fragmentInstance = new VideoListFragment();
                                break;
                            }
                            case Globals.MAIN_ACTIVITY_ABOUT_FRAGMENT: {
                                fragmentInstance = new AboutFragment();
                                break;
                            }
                            case Globals.MAIN_ACTIVITY_PARTNERS_FRAGMENT: {
                                fragmentInstance = new PartnersFragment();
                                break;
                            }
                            case Globals.MAIN_ACTIVITY_NEWS_FRAGMENT: {
                                fragmentInstance = new NewsFragment();
                                break;
                            }
                            case Globals.MAIN_ACTIVITY_CONTACTS_FRAGMENT: {
                                fragmentInstance = new ContactsFragment();
                                break;
                            }
                            case Globals.MAIN_ACTIVITY_PROFILE_FRAGMENT: {
                                fragmentInstance = new ProfileFragment();
                                break;
                            }
                            case Globals.MAIN_ACTIVITY_COMPLAIN_DRAFTS: {
                                fragmentInstance = new ComplainDraftsFragment();
                                break;
                            }
                        }
                    }
                    ft.replace(R.id.frameLayoutMain, fragmentInstance, tag).addToBackStack(tag).commit();

                    /*switch (currentFragment) {
                        case Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT: {
                            ft.replace(R.id.frameLayoutMain, new MainFragment(), tag).addToBackStack(tag).commit();
                            break;
                        }
                        case Globals.MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT: {
                            ft.replace(R.id.frameLayoutMain, new RequestListFragment(), tag).addToBackStack(tag).commit();
                            break;
                        }
                        case Globals.MAIN_ACTIVITY_COMPLAINS_BOOK_FRAGMENT: {
                            ft.replace(R.id.frameLayoutMain, new ComplainsBookFragment(), tag).addToBackStack(tag).commit();
                            break;
                        }
                        case Globals.MAIN_ACTIVITY_VIDEO_LIST_FRAGMENT: {
                            ft.replace(R.id.frameLayoutMain, new VideoListFragment(), tag).addToBackStack(tag).commit();
                            break;
                        }
                        case Globals.MAIN_ACTIVITY_ABOUT_FRAGMENT: {
                            ft.replace(R.id.frameLayoutMain, new AboutFragment(), tag).addToBackStack(tag).commit();
                            break;
                        }
                        case Globals.MAIN_ACTIVITY_PARTNERS_FRAGMENT: {
                            ft.replace(R.id.frameLayoutMain, new PartnersFragment(), tag).addToBackStack(tag).commit();
                            break;
                        }
                        case Globals.MAIN_ACTIVITY_NEWS_FRAGMENT: {
                            ft.replace(R.id.frameLayoutMain, new NewsFragment(), tag).addToBackStack(tag).commit();
                            break;
                        }
                        case Globals.MAIN_ACTIVITY_CONTACTS_FRAGMENT: {
                            ft.replace(R.id.frameLayoutMain, new ContactsFragment(), tag).addToBackStack(tag).commit();
                            break;
                        }
                        case Globals.MAIN_ACTIVITY_PROFILE_FRAGMENT: {
                            ft.replace(R.id.frameLayoutMain, new ProfileFragment(), tag).addToBackStack(tag).commit();
                            break;
                        }
                        case Globals.MAIN_ACTIVITY_COMPLAIN_DRAFTS: {
                            ft.replace(R.id.frameLayoutMain, new ComplainDraftsFragment(), tag).addToBackStack(tag).commit();
                            break;
                        }
                    }*/
                } else {
                    currentFragment = Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT;
                    tag = fragmentTags.get(currentFragment);
                    ft.add(R.id.frameLayoutMain, new MainFragment(), tag).addToBackStack(tag).commit();
                }
            }
        }
        toolbar.setTitle(tag);

        FacebookSdk.sdkInitialize(getApplicationContext());

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(myBroadcastReceiver, new IntentFilter(BROADCAST_RECEIVER_TAG));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CameraManager.IMAGE_CAPTURE_INTENT && resultCode == Activity.RESULT_OK){
            fManager.findFragmentByTag(getResources().getString(R.string.profile_header)).onActivityResult(requestCode, resultCode, data);
        } else if (fbCallbackManager != null) {
            fbCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        int entryNumber = fManager.getBackStackEntryCount() - 2;
        if (entryNumber >= 0) {
            String tag = fManager.getBackStackEntryAt(entryNumber).getName();
            if (tag.equals(getResources().getString(R.string.profile_header))) {//skip the profile page
                tag = getResources().getString(R.string.do_punish);//& go to main page
                currentFragment = Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT;
            } else {
                for (int i : fragmentTags.keySet()){
                    if (tag.equals(fragmentTags.get(i))) {
                        currentFragment = i;
                        break;
                    }
                }
            }
            toolbar.setTitle(tag);
            fManager.popBackStackImmediate(tag, 0);
        } else {
            moveTaskToBack(true);
        }
    }

    //hides the software keyboard
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Globals.hideSoftKeyboard(this, event);
        return super.dispatchTouchEvent( event );
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(Globals.MAIN_ACTIVITY_SAVED_INSTANCE_STATE, currentFragment);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(myBroadcastReceiver);
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    //prepares final map of fragment tags
    HashMap<Integer, String> getFragmentTags(){
        HashMap<Integer, String> result = new HashMap<>();

        result.put(Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT,           getResources().getString(R.string.do_punish));
        result.put(Globals.MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT,     getResources().getString(R.string.punishment_requests));
        result.put(Globals.MAIN_ACTIVITY_COMPLAINS_BOOK_FRAGMENT,   getResources().getString(R.string.complains_book));
        result.put(Globals.MAIN_ACTIVITY_VIDEO_LIST_FRAGMENT,       getResources().getString(R.string.video_tutorial));
        result.put(Globals.MAIN_ACTIVITY_DONATE,                    "");
        result.put(Globals.MAIN_ACTIVITY_ABOUT_FRAGMENT,            getResources().getString(R.string.about_header));
        result.put(Globals.MAIN_ACTIVITY_PARTNERS_FRAGMENT,         getResources().getString(R.string.partners_header));
        result.put(Globals.MAIN_ACTIVITY_NEWS_FRAGMENT,             getResources().getString(R.string.news_header));
        result.put(Globals.MAIN_ACTIVITY_CONTACTS_FRAGMENT,         getResources().getString(R.string.contacts_header));
        result.put(Globals.MAIN_ACTIVITY_PROFILE_FRAGMENT,          getResources().getString(R.string.profile_header));
        result.put(Globals.MAIN_ACTIVITY_EXIT,                      "");//exit - no tag
        result.put(Globals.MAIN_ACTIVITY_COMPLAIN_DRAFTS,           getResources().getString(R.string.drafts));

        return result;
    }

    /*
     *the task is to add the first list item with the user's name
     */
    String[] makeDrawerList(){
        String[] menuItems = getResources().getStringArray(R.array.drawerMenuItems);
        ArrayList<String> tempList = new ArrayList<>(Arrays.asList(menuItems));
        return tempList.toArray(new String[0]);
    }

    public void empty(View view) {
        //empty method to handle click events
    }


    boolean loggedIn(){
        return KaratelPreferences.loggedIn();
    }

    public void startTutorial(View view) {
        Intent intent = new Intent(this, TutorialActivity.class);
        intent.putExtra(TutorialActivity.FIRST_TIME_TUTORIAL, false);
        startActivity(intent);
    }

    public void createRequest(View view) {
        currentFragment = Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT; //Покарати за порушення
        String tag = fragmentTags.get(currentFragment);
        toolbar.setTitle(tag);
        fManager.beginTransaction().replace(R.id.frameLayoutMain, new MainFragment(), tag).commit();
    }

    public void openComplainDrafts() {
        currentFragment = Globals.MAIN_ACTIVITY_COMPLAIN_DRAFTS;
        String tag = fragmentTags.get(currentFragment);
        toolbar.setTitle(tag);
        fManager.beginTransaction().replace(R.id.frameLayoutMain, new ComplainDraftsFragment(), tag)
                .addToBackStack(tag).commit();
    }

    public void openDonatePage() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.url_donate)));
        if (browserIntent.resolveActivity(getPackageManager()) != null)
            startActivity(browserIntent);
    }

    public void open101(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.url_foundation101)));
        startActivity(browserIntent);
    }

    public void openPeoplesProject(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.url_peoples_project)));
        startActivity(browserIntent);
    }

    public void changeEmail(View view) {
        startActivity(new Intent(this, ChangeEmailActivity.class));
    }

    public void changePassword(View view) {
        startActivity(new Intent(this, ChangePasswordActivity.class));
    }

    public void bindFacebook(View view) {
        if (HttpHelper.internetConnected(/*this*/)) {
            AccessToken fbToken = AccessToken.getCurrentAccessToken();
            boolean loggedIn = fbToken != null;
            if (loggedIn) {
                sendBindRequest(fbToken);
            } else {
                List<String> permissionNeeds = Arrays.asList("user_photos", "email", "user_birthday", "user_friends");
                fbCallbackManager = CallbackManager.Factory.create();
                LoginManager.getInstance().logInWithReadPermissions(this, permissionNeeds);
                LoginManager.getInstance().registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResults) {
                        AccessToken fbToken = AccessToken.getCurrentAccessToken();
                        boolean loggedIn = fbToken != null;
                        if (loggedIn) sendBindRequest(fbToken);
                    /*GraphRequest request = GraphRequest.newMeRequest(loginResults.getAccessToken(),
                            new GraphRequest.GraphJSONObjectCallback() {
                                @Override
                                public void onCompleted(JSONObject object, GraphResponse response) {

                                }
                            });
                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "id,name,email,gender, birthday");
                    request.setParameters(parameters);
                    request.executeAsync();*/
                    }

                    @Override
                    public void onCancel() {
                        Globals.showError(MainActivity.this, R.string.operation_cancelled, null);
                    }

                    @Override
                    public void onError(FacebookException e) {
                        Globals.showError(MainActivity.this, R.string.cannot_connect_server, e);
                    }
                });
            }
        } else Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
    }

    void sendBindRequest(AccessToken fbToken){
        String fbUserId = fbToken.getUserId();
        String request = new HttpHelper("social").makeRequestString(new String[]{
                "provider", "facebook", "uid", fbUserId
        });
        new FacebookBinder(fbUserId).execute(request);
    }

    public void initDrawerHeader(){
        if (Globals.user == null) finish();
        String userName = Globals.user.name + " " + Globals.user.surname;
        setAvatarImageView(avatarImageView);
        avatarTextView.setText(userName);
    }

    public void setAvatarImageView(ImageView avatarView){
        if (Globals.user.avatarFileName == null || Globals.user.avatarFileName.isEmpty()){
            avatarView.setBackgroundResource(R.mipmap.no_avatar);
        } else try {
            avatarView.setBackground(Drawable.createFromPath(Globals.user.avatarFileName));
        } catch (Exception e){
            Globals.showError(this, R.string.error, e);
        }
    }

    public void openSocialLink(View view) {
        Uri socialUrl = Uri.parse((String)view.getTag());
        String host = socialUrl.getHost();
        Intent appIntent = new Intent(Intent.ACTION_VIEW);
        switch (host){
            case "www.facebook.com" : {
                appIntent.setData(Uri.parse("fb://facewebmodal/f?href=" + view.getTag()));
                break;
            }
            case "www.instagram.com" : {
                appIntent.setData(Uri.parse("http://instagram.com/_u/" + socialUrl.getLastPathSegment()));
                appIntent.setPackage("com.instagram.android");
                break;
            }
        }
        if ((appIntent.getData() != null) && (appIntent.resolveActivity(getPackageManager()) != null)){
            startActivity(appIntent);
        } else {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, socialUrl);
            if (browserIntent.resolveActivity(getPackageManager()) != null)
                startActivity(browserIntent);
        }
    }

    public void sendEmail(View view) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.office_mail)});
        //intent.setType("text/plain");
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivity(intent);
    }



    class FacebookBinder extends AsyncTask<String, Void, String> {
        String fbUserId;

        public FacebookBinder(String fbUserId) {
            this.fbUserId = fbUserId;
        }

        @Override
        protected String doInBackground(String... params) {
            String result;
            try {
                if (HttpHelper.internetConnected(/*MainActivity.this*/)) {
                    result = HttpHelper.proceedRequest("socials", params[0], true);
                } else return HttpHelper.ERROR_JSON;
            } catch (final IOException e){
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(MainActivity.this, R.string.cannot_connect_server, e);
                    }
                });
                return "";
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String message;
            try {
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals("success")){
                    message = MainActivity.this.getResources().getString(R.string.facebook_profile_binded);
                } else message = json.getString("error");
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            } catch (JSONException e){
                Globals.showError(MainActivity.this, R.string.cannot_connect_server, e);
            }
        }
    }

    static class SignOutSender extends AsyncTask<Void, Void, String>{
        static final String TAG = "Logout";
        final String BANNED = "banned";

        SignOutSender(Activity activity) {
            this.activity = activity;
            if (activity != null && activity instanceof MainActivity)
                this.progressBar = ((MainActivity)activity).progressBar;
        }

        Activity activity;
        View progressBar;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if (HttpHelper.internetConnected()) {
                Toast.makeText(KaratelApplication.getInstance(), R.string.loggingOut, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            String result;
            try {
                if (HttpHelper.internetConnected()){
                    String gcmToken = KaratelPreferences.pushToken();


                    RetrofitSignOutSender api = KaratelApplication.getClient().create(RetrofitSignOutSender.class);
                    Call<String> call = api.signOut(Globals.sessionToken, gcmToken);
                    Response<String> json = call.execute();
                    if (json.isSuccessful()) {
                        result = json.body();
                    } else {
                        if (json.code() == 403) {//this code will be returned is the user is banned - agreed with Nazar
                            return BANNED; //& check this in postExecute()
                        }
                        ResponseBody errorBody = json.errorBody();
                        result = json.errorBody().string();
                        errorBody.close();
                    }

                    /* old code with HttpHelper
String request = new HttpHelper("session").makeRequestString(new String[]{"token", gcmToken});
                        result = HttpHelper.proceedRequest("signout", "DELETE", request, true);
                    */
                } else return HttpHelper.ERROR_JSON;

            } catch (final IOException e){
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(KaratelApplication.getInstance(), R.string.cannot_connect_server, e);
                    }
                });

                /*MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(MainActivity.this, R.string.cannot_connect_server, e);
                    }
                });*/
                return HttpHelper.ERROR_JSON;
            }
            return result == null ? "" : result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (!s.isEmpty() && !s.equals(BANNED)) {
                try {
                    JSONObject json = new JSONObject(s);
                    if (json.getString("status").equals(Globals.SERVER_ERROR)) {
                        Toast.makeText(KaratelApplication.getInstance(), json.getString("error"), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Globals.showError(KaratelApplication.getInstance(), R.string.error, e);
                }
                return;
            }
            boolean appClosed = KaratelPreferences.appClosed();
            KaratelPreferences.clearAll();
            if (appClosed){
                KaratelPreferences.setAppClosed();
            }

            //Google Analytics part
            KaratelApplication.getInstance().sendScreenName(TAG);
            if (activity != null) activity.finishAffinity();
        }
    }
}
