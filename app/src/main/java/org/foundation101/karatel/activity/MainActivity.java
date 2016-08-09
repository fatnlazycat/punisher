package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
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
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.Karatel;
import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.DrawerAdapter;
import org.foundation101.karatel.fragment.AboutFragment;
import org.foundation101.karatel.fragment.ContactsFragment;
import org.foundation101.karatel.fragment.MainFragment;
import org.foundation101.karatel.fragment.NewsFragment;
import org.foundation101.karatel.fragment.ProfileFragment;
import org.foundation101.karatel.fragment.RequestListFragment;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

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
            new SignOutSender().execute();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fManager = getSupportFragmentManager();

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);

        ((Karatel)getApplication()).restoreUserFromPreferences();

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
        drawerAdapter.content = makeDrawerList();
        drawerListView.setAdapter(drawerAdapter);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                drawerAdapter.notifyDataSetChanged();
                initDrawerHeader();
            }
        };
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        drawerListView.setOnItemClickListener(new ListView.OnItemClickListener(){
            public void onItemClick(AdapterView parent, View view, int position, long id){
                String tag = "";
                currentFragment = position;
                switch (position){
                    case 1 : {
                        tag = getResources().getString(R.string.do_punish);
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new MainFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case 2 : {
                        tag = getResources().getString(R.string.punishment_requests);
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new RequestListFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case 3 : {
                        tag = getResources().getString(R.string.about_header);
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new AboutFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case 4 : {
                        tag = getResources().getString(R.string.news_header);
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new NewsFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                        case 5 : {
                        tag = getResources().getString(R.string.contacts_header);
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new ContactsFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case 6 : {
                        tag = getResources().getString(R.string.profile_header);
                        //do not add profile fragment to back stack because of its strange behavior
                        fManager.beginTransaction().replace(R.id.frameLayoutMain, new ProfileFragment(), tag)
                                .addToBackStack(tag).commit();
                        break;
                    }
                    case 7 : {
                        new SignOutSender().execute();
                    }
                }
                toolbar.setTitle(tag);
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });

        //select fragment to start with (from savedInstanceState)
        ft = fManager.beginTransaction();
        String tag = "";
        if (Globals.MAIN_ACTIVITY_FROM_PUSH){
            Globals.MAIN_ACTIVITY_FROM_PUSH = false;
            tag = getResources().getString(R.string.punishment_requests);
            ft.add(R.id.frameLayoutMain, new RequestListFragment(), tag).addToBackStack(tag).commit();
        } else {
            if (savedInstanceState != null) {
                int action = savedInstanceState.getInt(Globals.MAIN_ACTIVITY_SAVED_INSTANCE_STATE, 0);
                switch (action){
                    case Globals.MAIN_ACTIVITY_REQUEST_LIST_FRAGMENT:{
                        tag = getResources().getString(R.string.punishment_requests);
                        ft.add(R.id.frameLayoutMain, new RequestListFragment(), tag).addToBackStack(tag).commit();
                    }
                    case Globals.MAIN_ACTIVITY_PROFILE_FRAGMENT:{//profile fragment not added to back stack due to its strange behavior
                        tag = getResources().getString(R.string.profile_header);
                        ft.add(R.id.frameLayoutMain, new ProfileFragment(), tag).addToBackStack(tag).commit();
                    }
                    case Globals.MAIN_ACTIVITY_NEWS_FRAGMENT:{
                        tag = getResources().getString(R.string.news_header);
                        ft.add(R.id.frameLayoutMain, new NewsFragment(), tag).addToBackStack(tag).commit();
                    }
                }
            } else {
                tag = getResources().getString(R.string.do_punish);
                ft.add(R.id.frameLayoutMain, new MainFragment(), tag).addToBackStack(tag).commit();
            }
        }
        toolbar.setTitle(tag);

        FacebookSdk.sdkInitialize(getApplicationContext());

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(myBroadcastReceiver, new IntentFilter("myBroadcastReceiver"));
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
            }
            toolbar.setTitle(tag);
            fManager.popBackStackImmediate(tag, 0);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(Globals.MAIN_ACTIVITY_SAVED_INSTANCE_STATE, currentFragment);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(myBroadcastReceiver);
        super.onDestroy();
    }

    /*
             *the task is to add the first list item with the user's name
             */
    String[] makeDrawerList(){
        String[] menuItems = getResources().getStringArray(R.array.drawerMenuItems);
        ArrayList<String> tempList = new ArrayList(Arrays.asList(menuItems));
        return tempList.toArray(new String[0]);
    }

    public void empty(View view) {
        //empty method to handle click events
    }

    public void startTutorial(View view) {
        Intent intent = new Intent(this, TutorialActivity.class);
        intent.putExtra(TutorialActivity.FIRST_TIME_TUTORIAL, false);
        startActivity(intent);
    }

    public void createRequest(View view) {
        String tag = getResources().getString(R.string.do_punish);
        toolbar.setTitle(tag);
        fManager.beginTransaction().replace(R.id.frameLayoutMain, new MainFragment(), tag).commit();
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
        if (HttpHelper.internetConnected(this)) {
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
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, socialUrl);
        Uri socialUrlInApp = null;
        switch (host){
            case "www.facebook.com" : {
                socialUrlInApp = Uri.parse("fb://facewebmodal/f?href=" + view.getTag());
                break;
            }
        }
        if (socialUrlInApp != null){
            Intent appIntent = new Intent(Intent.ACTION_VIEW, socialUrlInApp);
            if (appIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(appIntent);
                return;
            }
        }
        if (browserIntent.resolveActivity(getPackageManager()) != null)
            startActivity(browserIntent);
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
                if (HttpHelper.internetConnected(MainActivity.this)) {
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

    class SignOutSender extends AsyncTask<Void, Void, String>{

        SharedPreferences globalPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            if (HttpHelper.internetConnected(MainActivity.this)) {
                Toast.makeText(MainActivity.this, R.string.loggingOut, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            String result;
            try {
                String gcmToken = globalPreferences.contains(Globals.PUSH_TOKEN) ?
                        globalPreferences.getString(Globals.PUSH_TOKEN, "") : "";
                String request = new HttpHelper("session").makeRequestString(new String[]{"token", gcmToken});
                if (HttpHelper.internetConnected(MainActivity.this)) {
                    result = HttpHelper.proceedRequest("signout", "DELETE", request, true);
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
            progressBar.setVisibility(View.GONE);
            if (!s.isEmpty()) {
                try {
                    JSONObject json = new JSONObject(s);
                    if (json.getString("status").equals(Globals.SERVER_ERROR)) {
                        Toast.makeText(MainActivity.this, json.getString("error"), Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (JSONException e) {
                    Globals.showError(MainActivity.this, R.string.error, e);
                }
            }
            globalPreferences.edit().clear().apply();
            //Globals.user = null;
            //Globals.sessionToken = null;
            finishAffinity();
        }
    }
}
