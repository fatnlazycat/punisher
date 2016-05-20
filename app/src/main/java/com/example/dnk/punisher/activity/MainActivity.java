package com.example.dnk.punisher.activity;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.dnk.punisher.R;
import com.example.dnk.punisher.adapter.DrawerAdapter;
import com.example.dnk.punisher.fragment.AboutFragment;
import com.example.dnk.punisher.fragment.MainFragment;

import java.util.ArrayList;
import java.util.Arrays;

import layout.ProfileFragment;

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
        toolbar.setTitle(R.string.do_punish);

        //init navigation drawer
        ListView drawerListView = (ListView)findViewById(R.id.drawerListView);
        DrawerAdapter drawerAdapter = new DrawerAdapter();
        drawerAdapter.content = makeDrawerList();
        drawerListView.setAdapter(drawerAdapter);
        drawerListView.setOnItemClickListener(new ListView.OnItemClickListener(){
            public void onItemClick(AdapterView parent, View view, int position, long id){
                switch (position){
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
        ft.add(R.id.frameLayoutMain, new MainFragment(), "current_fragment").commit();
    }



    /*
     *the task is to add the first list item with the user's name
     */
    String[] makeDrawerList(){
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String[] menuItems = getResources().getStringArray(R.array.drawerMenuItems);
        ArrayList<String> tempList = new ArrayList(Arrays.asList(menuItems));
        tempList.add(0,preferences.getString(USER_NAME_PREFERENCE, "no user"));
        return tempList.toArray(new String[0]);
    }

}
