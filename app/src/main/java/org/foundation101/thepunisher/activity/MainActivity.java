package org.foundation101.thepunisher.activity;

import android.app.Activity;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.foundation101.thepunisher.CameraManager;
import org.foundation101.thepunisher.Globals;
import org.foundation101.thepunisher.R;
import org.foundation101.thepunisher.adapter.DrawerAdapter;
import org.foundation101.thepunisher.fragment.AboutFragment;
import org.foundation101.thepunisher.fragment.ContactsFragment;
import org.foundation101.thepunisher.fragment.MainFragment;
import org.foundation101.thepunisher.fragment.ProfileFragment;
import org.foundation101.thepunisher.fragment.RequestListFragment;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String USER_NAME_PREFERENCE = "userName";

    Toolbar toolbar;
    FragmentManager fManager;
    FragmentTransaction ft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fManager = getSupportFragmentManager();

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //init navigation drawer
        ListView drawerListView = (ListView)findViewById(R.id.drawerListView);
        //first line where we need avatar icon
        View drawerHeader = LayoutInflater.from(this).inflate(R.layout.drawer_header, drawerListView, false);
        drawerListView.addHeaderView(drawerHeader, null, false);
        String userName = Globals.user.name + " " + Globals.user.surname;
        ImageView avatarImageView = (ImageView) findViewById(R.id.avatarImageView);
        avatarImageView.setImageResource(R.mipmap.no_avatar);
        TextView avatarTextView = (TextView)findViewById(R.id.avatarTextView);
        avatarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        avatarTextView.setText(userName);

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
            }
        };
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();

        drawerListView.setOnItemClickListener(new ListView.OnItemClickListener(){
            public void onItemClick(AdapterView parent, View view, int position, long id){
                switch (position){
                    case 1 : {
                        toolbar.setTitle(R.string.do_punish);
                        fManager.beginTransaction().detach(fManager.findFragmentByTag("current_fragment")).commit();
                        fManager.beginTransaction().add(R.id.frameLayoutMain, new MainFragment(), "current_fragment").commit();
                        break;
                    }
                    case 2 : {
                        //startActivity(new Intent(parent.getContext(), RequestListFragment.class));
                        toolbar.setTitle(R.string.punishment_requests);
                        fManager.beginTransaction().detach(fManager.findFragmentByTag("current_fragment")).commit();
                        fManager.beginTransaction().add(R.id.frameLayoutMain, new RequestListFragment(), "current_fragment").commit();
                        break;
                    }
                    case 3 : {
                        toolbar.setTitle(R.string.about_header);
                        fManager.beginTransaction().detach(fManager.findFragmentByTag("current_fragment")).commit();
                        fManager.beginTransaction().add(R.id.frameLayoutMain, new AboutFragment(), "current_fragment").commit();
                        break;
                    }
                    case 4 : startActivity(new Intent(parent.getContext(), NewsActivity.class)); break;
                    case 5 : {
                        toolbar.setTitle(R.string.contacts_header);
                        fManager.beginTransaction().detach(fManager.findFragmentByTag("current_fragment")).commit();
                        fManager.beginTransaction().add(R.id.frameLayoutMain, new ContactsFragment(), "current_fragment").commit();
                        break;
                    }
                    case 6 : {
                        toolbar.setTitle(R.string.profile_header);
                        fManager.beginTransaction().detach(fManager.findFragmentByTag("current_fragment")).commit();
                        fManager.beginTransaction().add(R.id.frameLayoutMain, new ProfileFragment(), "current_fragment").commit();
                        break;
                    }
                    case 7 : finishAffinity();
                }
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        //getSupportActionBar().setHomeButtonEnabled(false);

        ft = fManager.beginTransaction();
        toolbar.setTitle(R.string.do_punish);
        ft.add(R.id.frameLayoutMain, new MainFragment(), "current_fragment").commit();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CameraManager.IMAGE_CAPTURE_INTENT && resultCode == Activity.RESULT_OK){
            fManager.findFragmentByTag("current_fragment").onActivityResult(requestCode, resultCode, data);
        }
    }

    /*
         *the task is to add the first list item with the user's name
         */
    String[] makeDrawerList(){
        String[] menuItems = getResources().getStringArray(R.array.drawerMenuItems);
        ArrayList<String> tempList = new ArrayList(Arrays.asList(menuItems));
        return tempList.toArray(new String[0]);
    }

    public void startTutorial(View view) {
        Intent intent = new Intent(this, TutorialActivity.class);
        intent.putExtra(TutorialActivity.FIRST_TIME_TUTORIAL, false);
        startActivity(intent);
    }

    public void createRequest(View view) {
        toolbar.setTitle(R.string.do_punish);
        fManager.beginTransaction().detach(fManager.findFragmentByTag("current_fragment")).commit();
        fManager.beginTransaction().add(R.id.frameLayoutMain, new MainFragment(), "current_fragment").commit();
    }

    public void open101(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.url_foundation101)));
        startActivity(browserIntent);
    }

    public void openPeoplesProject(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.url_peoples_project)));
        startActivity(browserIntent);
    }

    /*PunisherUser getUser(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean b = preferences.contains(Globals.USER_SURNAME);
        PunisherUser result = new PunisherUser(
                preferences.getString(Globals.USER_EMAIL, ""),
                preferences.getString(Globals.USER_PASSWORD, ""),
                preferences.getString(Globals.USER_SURNAME, ""),
                preferences.getString(Globals.USER_NAME, ""),
                preferences.getString(Globals.USER_SECOND_NAME, ""),
                preferences.getString(Globals.USER_PHONE, "")
        );
        result.id = preferences.getInt(Globals.USER_ID, 0);
        return result;
    }*/
}
