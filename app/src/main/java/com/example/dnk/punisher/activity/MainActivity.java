package com.example.dnk.punisher.activity;

import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.dnk.punisher.Globals;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.adapter.DrawerAdapter;
import com.example.dnk.punisher.fragment.AboutFragment;
import com.example.dnk.punisher.fragment.MainFragment;

import java.util.ArrayList;
import java.util.Arrays;

import com.example.dnk.punisher.fragment.ProfileFragment;

public class MainActivity extends AppCompatActivity {
    private static final String USER_NAME_PREFERENCE = "userName";

    FragmentManager fManager;
    FragmentTransaction ft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fManager = getSupportFragmentManager();

        final Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //init navigation drawer
        ListView drawerListView = (ListView)findViewById(R.id.drawerListView);
        //first line where we need avatar icon
        View drawerHeader = LayoutInflater.from(this).inflate(R.layout.drawer_header, drawerListView, false);
        drawerListView.addHeaderView(drawerHeader, null, false);
        SharedPreferences globalPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String userName = globalPreferences.getString(Globals.USER_NAME, "no user") + " "
                + globalPreferences.getString(Globals.USER_SURNAME, "no user");
        ImageView avatarImageView = (ImageView) findViewById(R.id.avatarImageView);
        avatarImageView.setImageResource(R.mipmap.no_avatar);
        TextView avatarTextView = (TextView)findViewById(R.id.avatarTextView);
        avatarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        avatarTextView.setText(userName);

        DrawerAdapter drawerAdapter = new DrawerAdapter();
        drawerAdapter.content = makeDrawerList();
        drawerListView.setAdapter(drawerAdapter);
        drawerListView.setOnItemClickListener(new ListView.OnItemClickListener(){
            public void onItemClick(AdapterView parent, View view, int position, long id){
                switch (position){
                    case 1 : {
                        toolbar.setTitle(R.string.do_punish);
                        fManager.beginTransaction().detach(fManager.findFragmentByTag("current_fragment")).commit();
                        fManager.beginTransaction().add(R.id.frameLayoutMain, new MainFragment(), "current_fragment").commit();
                        break;
                    }
                    case 2 : startActivity(new Intent(parent.getContext(), ListOfRequestsActivity.class)); break;
                    case 3 : {
                        toolbar.setTitle(R.string.about_header);
                        fManager.beginTransaction().detach(fManager.findFragmentByTag("current_fragment")).commit();
                        fManager.beginTransaction().add(R.id.frameLayoutMain, new AboutFragment(), "current_fragment").commit();
                        break;
                    }
                    case 4 : startActivity(new Intent(parent.getContext(), NewsActivity.class)); break;
                    case 6 : {
                        toolbar.setTitle(R.string.profile_header);
                        fManager.beginTransaction().detach(fManager.findFragmentByTag("current_fragment")).commit();
                        fManager.beginTransaction().add(R.id.frameLayoutMain, new ProfileFragment(), "current_fragment").commit();
                        break;
                    }
                    case 7 : finishAffinity();
                }
            }
        });
        DrawerLayout drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayoutMainActivity);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        //getSupportActionBar().setHomeButtonEnabled(false);

        ft = fManager.beginTransaction();
        toolbar.setTitle(R.string.do_punish);
        ft.add(R.id.frameLayoutMain, new MainFragment(), "current_fragment").commit();
    }



    /*
     *the task is to add the first list item with the user's name
     */
    String[] makeDrawerList(){
        String[] menuItems = getResources().getStringArray(R.array.drawerMenuItems);
        ArrayList<String> tempList = new ArrayList(Arrays.asList(menuItems));
        return tempList.toArray(new String[0]);
    }

}
