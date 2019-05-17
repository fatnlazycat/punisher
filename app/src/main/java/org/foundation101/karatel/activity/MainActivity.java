package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.evernote.android.job.JobManager;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.splunk.mint.Mint;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.DrawerAdapter;
import org.foundation101.karatel.entity.PunisherUser;
import org.foundation101.karatel.fragment.AboutFragment;
import org.foundation101.karatel.fragment.ComplainDraftsFragment;
import org.foundation101.karatel.fragment.ComplainsBookFragment;
import org.foundation101.karatel.fragment.ContactsFragment;
import org.foundation101.karatel.fragment.MainFragment;
import org.foundation101.karatel.fragment.NewsFragment;
import org.foundation101.karatel.fragment.PartnersFragment;
import org.foundation101.karatel.fragment.ProfileFragment;
import org.foundation101.karatel.fragment.RequestListFragment;
import org.foundation101.karatel.fragment.SponsorsFragment;
import org.foundation101.karatel.fragment.VideoListFragment;
import org.foundation101.karatel.manager.CameraManager;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.retrofit.RetrofitSignOutSender;
import org.foundation101.karatel.scheduler.TokenExchangeJob;
import org.foundation101.karatel.service.MyGcmListenerService;
import org.foundation101.karatel.utils.JobUtils;
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
    RelativeLayout progressBar; //do not rename - used in reflection!
    DrawerLayout drawerLayout;
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

        JobUtils.INSTANCE.scheduleRequestFetch();

        fManager = getSupportFragmentManager();

        progressBar = findViewById(R.id.rlProgress);

        //KaratelPreferences.restoreUser();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //init navigation drawer
        ListView drawerListView = findViewById(R.id.drawerListView);
        //first line where we need avatar icon
        View drawerHeader = LayoutInflater.from(this).inflate(R.layout.drawer_header, drawerListView, false);
        drawerListView.addHeaderView(drawerHeader, null, false);

        avatarImageView = findViewById(R.id.avatarImageView);
        avatarTextView = findViewById(R.id.avatarTextView);
        avatarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        initDrawerHeader();

        drawerLayout = findViewById(R.id.drawerLayoutMainActivity);
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
                drawerLayout.closeDrawer(Gravity.START);
                if (currentFragment == position) {//do not create duplicates
                    return;
                }
                String tag = fragmentTags.get(position);
                if (!tag.isEmpty()) { //empty tags are for donate & exit - no special fragment
                    currentFragment = position;
                    toolbar.setTitle(tag);
                }
                switch (position){
                    case Globals.MAIN_ACTIVITY_COMPLAINS_BOOK_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new ComplainsBookFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    /*case Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new MainFragment(), tag)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                                .addToBackStack(tag).commit();
                        break;
                    }*/
                    case Globals.MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new RequestListFragment(), tag)
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
                    case Globals.MAIN_ACTIVITY_SPONSORS_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new SponsorsFragment(), tag)
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
                    /*case Globals.MAIN_ACTIVITY_PROFILE_FRAGMENT: {
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new ProfileFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case Globals.MAIN_ACTIVITY_EXIT: {
                        new SignOutSender(MainActivity.this).execute();
                    }*/
                }
            }
        });

        //select fragment to start with (from savedInstanceState)
        ft = fManager.beginTransaction();
        String tag = "";
        if (!loggedIn()){ //this happens when we tap push notification icon after logging out - we are not signed in so close the app
            finish();
        } else {
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra(MyGcmListenerService.REQUEST_NUMBER)) {
                //KaratelPreferences.setStartedFromPush(false);
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
                            case Globals.MAIN_ACTIVITY_COMPLAINS_BOOK_FRAGMENT: {
                                fragmentInstance = new ComplainsBookFragment();
                                break;
                            }
                            /*case Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT: {
                                fragmentInstance = new MainFragment();
                                break;
                            }*/
                            case Globals.MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT: {
                                fragmentInstance = new RequestListFragment();
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
                            case Globals.MAIN_ACTIVITY_SPONSORS_FRAGMENT: {
                                fragmentInstance = new SponsorsFragment();
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
                            case Globals.MAIN_ACTIVITY_COMPLAIN_DRAFTS: {
                                fragmentInstance = new ComplainDraftsFragment();
                                break;
                            }
                            case Globals.MAIN_ACTIVITY_PROFILE_FRAGMENT: {
                                fragmentInstance = new ProfileFragment();
                                break;
                            }
                        }
                    }
                    ft.replace(R.id.frameLayoutMain, fragmentInstance, tag).commit();
                } else {
                    currentFragment = Globals.MAIN_ACTIVITY_COMPLAINS_BOOK_FRAGMENT;
                    tag = fragmentTags.get(currentFragment);
                    ft.add(R.id.frameLayoutMain, new ComplainsBookFragment(), tag).addToBackStack(tag).commit();
                }
            }
        }
        toolbar.setTitle(tag);

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(myBroadcastReceiver, new IntentFilter(BROADCAST_RECEIVER_TAG));

        String fbUid = KaratelPreferences.fbLoginUid();
        if (!fbUid.isEmpty()) sendBindRequest(fbUid);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        if (drawerLayout.isDrawerOpen(Gravity.START)) drawerLayout.closeDrawer(Gravity.START);
        else {
            int entryNumber = fManager.getBackStackEntryCount() - 2;
            if (entryNumber >= 0) {
                String tag = fManager.getBackStackEntryAt(entryNumber).getName();
                /*if (tag.equals(getResources().getString(R.string.profile_header))) {//skip the profile page
                    tag = getResources().getString(R.string.do_punish);//& go to main page
                    currentFragment = Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT;
                } else {*/
                    for (int i : fragmentTags.keySet()) {
                        if (tag.equals(fragmentTags.get(i))) {
                            currentFragment = i;
                            break;
                        }
                    }
                //}
                toolbar.setTitle(tag);
                fManager.popBackStackImmediate(tag, 0);
            } else {
                moveTaskToBack(true);
            }
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
    protected void onPause() {
        super.onPause();
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

        result.put(Globals.MAIN_ACTIVITY_COMPLAINS_BOOK_FRAGMENT,   getResources().getString(R.string.complains_book));
        //result.put(Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT,           getResources().getString(R.string.do_punish));
        result.put(Globals.MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT,     getResources().getString(R.string.punishment_requests));
        result.put(Globals.MAIN_ACTIVITY_VIDEO_LIST_FRAGMENT,       getResources().getString(R.string.video_tutorial));
        result.put(Globals.MAIN_ACTIVITY_DONATE,                    "");
        result.put(Globals.MAIN_ACTIVITY_ABOUT_FRAGMENT,            getResources().getString(R.string.about_header));
        result.put(Globals.MAIN_ACTIVITY_SPONSORS_FRAGMENT,         getResources().getString(R.string.sponsors_header));
        result.put(Globals.MAIN_ACTIVITY_PARTNERS_FRAGMENT,         getResources().getString(R.string.partners_header));
        result.put(Globals.MAIN_ACTIVITY_NEWS_FRAGMENT,             getResources().getString(R.string.news_header));
        result.put(Globals.MAIN_ACTIVITY_CONTACTS_FRAGMENT,         getResources().getString(R.string.contacts_header));
        //result.put(Globals.MAIN_ACTIVITY_EXIT,                      "");//exit - no tag
        result.put(Globals.MAIN_ACTIVITY_COMPLAIN_DRAFTS,           getResources().getString(R.string.drafts));
        result.put(Globals.MAIN_ACTIVITY_PROFILE_FRAGMENT,          getResources().getString(R.string.profile_header));

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

    //commented out because we don't create requests any more
    /*public void createRequest(View view) {
        currentFragment = Globals.MAIN_ACTIVITY_PUNISH_FRAGMENT; //Покарати за порушення
        String tag = fragmentTags.get(currentFragment);
        toolbar.setTitle(tag);
        fManager.beginTransaction().replace(R.id.frameLayoutMain, new MainFragment(), tag).commit();
    }*/

    public void openComplainDrafts() {
        currentFragment = Globals.MAIN_ACTIVITY_COMPLAIN_DRAFTS;
        String tag = fragmentTags.get(currentFragment);
        toolbar.setTitle(tag);
        fManager.beginTransaction().replace(R.id.frameLayoutMain, new ComplainDraftsFragment(), tag)
                .addToBackStack(tag).commit();
    }

    public void openProfile(View view) {
        drawerLayout.closeDrawer(Gravity.START);

        if (currentFragment == Globals.MAIN_ACTIVITY_PROFILE_FRAGMENT) {//do not create duplicates
            return;
        }

        currentFragment = Globals.MAIN_ACTIVITY_PROFILE_FRAGMENT;
        String tag = fragmentTags.get(currentFragment);
        toolbar.setTitle(tag);
        fManager.beginTransaction().replace(R.id.frameLayoutMain, new ProfileFragment(), tag)
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

    /*public void openPeoplesProject(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.url_peoples_project)));
        startActivity(browserIntent);
    }*/

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
                sendBindRequest(fbToken.getUserId());
            } else {
                List<String> permissionNeeds = Arrays.asList(/*"user_photos", */"email"/*, "user_birthday", "user_friends"*/);
                fbCallbackManager = CallbackManager.Factory.create();
                LoginManager.getInstance().logInWithReadPermissions(this, permissionNeeds);
                LoginManager.getInstance().registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResults) {
                        AccessToken fbToken = AccessToken.getCurrentAccessToken();
                        boolean loggedIn = fbToken != null;
                        if (loggedIn) sendBindRequest(fbToken.getUserId());
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
                        Globals.showError(R.string.operation_cancelled, null);
                    }

                    @Override
                    public void onError(FacebookException e) {
                        Globals.showError(R.string.cannot_connect_server, e);
                    }
                });
            }
        } else Toast.makeText(this, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
    }

    void sendBindRequest(String fbUserId){
        String request = new HttpHelper("social").makeRequestString(new String[]{
                "provider", "facebook", "uid", fbUserId
        });
        new FacebookBinder(fbUserId).execute(request);
    }

    public void initDrawerHeader(){
        PunisherUser user = KaratelPreferences.user();
        if (user.id == 0) finish(); //0 is the default value, means we've got empty preferences
        String userName = user.name + " " + user.surname;
        avatarTextView.setText(userName);
        setAvatarImageView(avatarImageView, user.avatarFileName);
    }

    public void setAvatarImageView(ImageView avatarView, @NonNull String avatarFileName){
        if (avatarFileName.isEmpty()){
            avatarView.setBackgroundResource(R.mipmap.no_avatar);
        } else try {
            avatarView.setBackground(Drawable.createFromPath(avatarFileName));
        } catch (Exception e){
            Globals.showError(R.string.error, e);
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

    public void exitApp(View view) {
        drawerLayout.closeDrawer(Gravity.START);
        new SignOutSender(this).execute();
    }


    static class FacebookBinder extends AsyncTask<String, Void, String> {
        String fbUserId;

        FacebookBinder(String fbUserId) {
            this.fbUserId = fbUserId;
        }

        @Override
        protected String doInBackground(String... params) {
            String result;
            try {
                if (HttpHelper.internetConnected()) {
                    result = HttpHelper.proceedRequest("socials", params[0], true);
                } else return HttpHelper.ERROR_JSON;
            } catch (final IOException e){
                Globals.showError(R.string.cannot_connect_server, e);
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
                    KaratelPreferences.remove(Globals.BACKGROUND_FB_LOGIN_UID);
                    KaratelPreferences.remove(Globals.BACKGROUND_FB_LOGIN_PASSW);
                    KaratelPreferences.remove(Globals.BACKGROUND_FB_LOGIN_EMAIL);

                    message = KaratelApplication.getInstance().getResources().getString(R.string.facebook_profile_binded);
                } else message = json.getString("error");
                Toast.makeText(KaratelApplication.getInstance(), message, Toast.LENGTH_LONG).show();
            } catch (JSONException e){
                Globals.showError(R.string.cannot_connect_server, e);
            }
        }
    }

    public static class SignOutSender extends AsyncTask<Void, Void, String>{
        static final String TAG = "Logout";
        static final String BANNED = "banned";

        SignOutSender(Activity activity) {
            this.activity = activity;
            try {
                this.progressBar = (View) activity.getClass().getDeclaredField("progressBar").get(activity);
            } catch (Exception ignored) { /*progressBar not instantiated, it's null*/ }
        }

        Activity activity;
        View progressBar = null;
        String resultData = null;

        public static Response<String> performSignOutRequest(String sessionToken, String gcmToken)
                throws IOException {
            RetrofitSignOutSender api = KaratelApplication.getClient().create(RetrofitSignOutSender.class);
            Call<String> call = api.signOut(sessionToken, gcmToken);
            return call.execute();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            if (HttpHelper.internetConnected()) {
                Globals.showMessage(R.string.loggingOut);
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            JobManager.instance().cancelAll();
            synchronized (KaratelPreferences.TAG) {
                String result;
                try {
                    if (HttpHelper.internetConnected()) {
                        Response<String> json = performSignOutRequest(
                                KaratelPreferences.sessionToken(), KaratelPreferences.pushToken());
                        result = buildResponseString(json);
                    } else return HttpHelper.ERROR_JSON;

                } catch (final IOException e) {
                    Globals.showError(R.string.cannot_connect_server, e);
                    return HttpHelper.ERROR_JSON;
                }
                return result;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            resultData = s;

            if (progressBar != null) progressBar.setVisibility(View.GONE);

            try {
                if (!logoutPossible(s)) {
                    logAndMint(s);
                    JobUtils.INSTANCE.schedule(JobUtils.INSTANCE.pendingJobTag());
                    return;
                }
            } catch (JSONException e) {
                Globals.showError(R.string.error, e);
            }

            //all the jobs were cancelled in the beginning of signout - reschedule if necessary
            String jobTag = JobUtils.INSTANCE.pendingJobTag();
            if (doNotCancel(jobTag)) JobUtils.INSTANCE.schedule(jobTag);

            //saving required sharedPreferences for later use
            synchronized (KaratelPreferences.TAG) {
                String oldSessionToken = KaratelPreferences.oldSessionToken();
                String oldPushToken = KaratelPreferences.oldPushToken();
                //boolean appClosed = KaratelPreferences.appClosed();
                KaratelPreferences.clearAll();
                KaratelApplication.getInstance().requests.clear();
                /*if (appClosed) {
                    KaratelPreferences.setAppClosed();
                }*/
                if (!oldSessionToken.isEmpty())
                    KaratelPreferences.setOldSessionToken(oldSessionToken);
                if (!oldPushToken.isEmpty()) KaratelPreferences.setOldPushToken(oldPushToken);
            }

            //Google Analytics part
            KaratelApplication.getInstance().sendScreenName(TAG);

            try { activity.finishAffinity(); } catch (NullPointerException ignored) {
                /*two reasons for NPE:
                * 1 - activity can be null
                * 2 - activity.finishAffinity() can throw */
            }
        }

        public static String buildResponseString(Response<String> response) throws IOException {
            String result;
            if (response.isSuccessful()) {
                result = response.body();
            } else {
                if (response.code() == 403) {//this code will be returned is the user is banned - agreed with Nazar
                    return BANNED; //& check this in postExecute()
                }
                ResponseBody errorBody = response.errorBody();
                result = response.errorBody().string();
                errorBody.close();
            }
            return result == null ? "" : result;
        }

        public static boolean logoutPossible(String serverResponse) throws JSONException {
            if (serverResponse.isEmpty() || serverResponse.equals(BANNED)) return true;
            JSONObject json = new JSONObject(serverResponse);
            String status = json.optString("status");
            if (Globals.SERVER_ERROR.equals(status)) {
                String error =  json.getString("error");
                if (!"Bad Request".equalsIgnoreCase(error)) {
                    return false;
                }
                //I faced these error codes when we don't need the user to stay signed in
            } else if (!"500".equals(status) && !"404".equals(status)) {
                return false;
            }
            return true;
        }

        private static boolean doNotCancel(String jobTag) {
            return TokenExchangeJob.TAG.equals(jobTag) && KaratelPreferences.newPushToken().isEmpty();
        }

        private void logAndMint(String message) {
            Toast.makeText(KaratelApplication.getInstance(), message, Toast.LENGTH_LONG).show();

            HashMap<String, Object> logData = new HashMap<>();
            logData.put("result", resultData);
            logData.put("sessionToken",  KaratelPreferences.sessionToken());
            logData.put("gcmToken", KaratelPreferences.pushToken());
            Mint.logException(logData, new Exception("signoffFailed"));
        }
    }
}
