package org.foundation101.karatel.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.fragment.TutorialFragment;

public class TutorialActivity extends FragmentActivity {
    static final String TAG = "Instruction";
    final int MAX_STAGE=4;
    public static final String FIRST_TIME_TUTORIAL = "firstTimeTutorial";

    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        // Instantiate a ViewPager and a PagerAdapter.
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        ScreenSlidePagerAdapter adapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
    }

    public void proceedWithTutorial(View view) {
        int stage = viewPager.getCurrentItem();
        if (stage < MAX_STAGE) {
            viewPager.setCurrentItem(stage + 1);
            ((KaratelApplication)getApplication()).sendScreenName(TAG + (stage + 1));
        } else {
            if (getIntent().getBooleanExtra(FIRST_TIME_TUTORIAL, true)) {
                startActivity(new Intent(this, TipsActivity.class));
            } else {
                finish();
            }
        }
    }

    /**
     * A simple pager adapter
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new TutorialFragment();
            Bundle arg = new Bundle();
            arg.putInt(TutorialFragment.STAGE, position);
            fragment.setArguments(arg);
            ((KaratelApplication)getApplication()).sendScreenName(TAG + (position + 1));
            return fragment;
        }

        @Override
        public int getCount() {
            return MAX_STAGE + 1;
        }
    }
}
