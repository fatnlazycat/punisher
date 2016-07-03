package org.foundation101.karatel.activity;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
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
    private static final String USER_NAME_PREFERENCE = "userName";

    public Toolbar toolbar;
    ImageView avatarImageView;
    TextView avatarTextView;
    FragmentManager fManager;
    FragmentTransaction ft;
    int currentFragment;

    //facebook part
    CallbackManager fbCallbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fManager = getSupportFragmentManager();

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
        drawerLayout.setDrawerListener(drawerToggle);
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
                    case 7 : finishAffinity();
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
        AccessToken fbToken = AccessToken.getCurrentAccessToken();
        boolean loggedIn = fbToken != null;
        if (loggedIn){
            sendBindRequest(fbToken);
        } else {
            List<String> permissionNeeds= Arrays.asList("user_photos", "email", "user_birthday", "user_friends");
            fbCallbackManager = CallbackManager.Factory.create();
            LoginManager.getInstance().logInWithReadPermissions(this,permissionNeeds);
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
    }

    void sendBindRequest(AccessToken fbToken){
        String fbUserId = fbToken.getUserId();
        String request = new HttpHelper("social").makeRequestString(new String[]{
                "provider", "facebook", "uid", fbUserId
        });
        new FacebookBinder(fbUserId).execute(request);
    }

    public void initDrawerHeader(){
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

    class FacebookBinder extends AsyncTask<String, Void, String> {
        String fbUserId;

        public FacebookBinder(String fbUserId) {
            this.fbUserId = fbUserId;
        }

        @Override
        protected String doInBackground(String... params) {
            String result;
            try {
                result = HttpHelper.proceedRequest("socials", params[0], true);
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
}
