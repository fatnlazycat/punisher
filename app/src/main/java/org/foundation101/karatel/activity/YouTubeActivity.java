package org.foundation101.karatel.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.VideoTutorialItem;
import org.foundation101.karatel.utils.ApplicableToView;
import org.foundation101.karatel.utils.ViewUtils;

/**
 * Created by Dima on 10.08.2017.
 */

public class YouTubeActivity extends YouTubeBaseActivity implements AppCompatCallback,
        YouTubePlayer.OnInitializedListener {
    private static final String TAG = "VideoLesson";
    private static final String YOU_TUBE_APP_BUTTON_CONTENT_DESCRIPTION = "Watch this video in YouTube";

    YouTubePlayerView youTubeView;
    String youTubeKey;

    VideoTutorialItem videoTutorialItem;

    private static final int RECOVERY_DIALOG_REQUEST = 1;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        AppCompatDelegate delegate = AppCompatDelegate.create(this, this);
        delegate.setContentView(R.layout.activity_youtube);

        Toolbar toolbar = findViewById(R.id.toolbar);
        delegate.setSupportActionBar(toolbar);
        ActionBar actionBar = delegate.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        actionBar.setTitle(R.string.video_tutorial);

        ImageView ivLogo = findViewById(R.id.ivCoachLogo);

        youTubeKey = getString(R.string.google_api_key);
        youTubeView = findViewById(R.id.youTubeView);
        initYouTubeView();

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvDescription = findViewById(R.id.tvDescription);

        videoTutorialItem = (VideoTutorialItem) getIntent().getSerializableExtra(Globals.YOUTUBE_VIDEO_NAME);

        tvTitle.setText(videoTutorialItem.title);
        tvDescription.setText(videoTutorialItem.description);

        KaratelApplication.getInstance().sendScreenName(TAG + " " + videoTutorialItem.title);
    }

    void initYouTubeView() {
        if (youTubeView != null) youTubeView.initialize(youTubeKey, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_DIALOG_REQUEST && resultCode == RESULT_OK) {
            // Retry initialization if user performed a recovery action
            initYouTubeView();
        }
    }


    //AppCompatCallback methods
    @Override
    public void onSupportActionModeStarted(ActionMode mode) { }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) { }

    @Nullable
    @Override
    public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback) { return null; }

    // YouTubePlayer.OnInitializedListener methods
    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
        ApplicableToView viewFinder = new ViewFinderByContentDescription(YOU_TUBE_APP_BUTTON_CONTENT_DESCRIPTION);
        View youTubeAppButton = ViewUtils.findFirstAmongChildren((YouTubePlayerView) provider, viewFinder);
        if (youTubeAppButton != null) youTubeAppButton.setVisibility(View.GONE);

        String localizedContentDescription = getResources().getString(R.string.youTubeShareButtonContentDescription);
        viewFinder = new ViewFinderByContentDescription(localizedContentDescription);
        View shareButton = ViewUtils.findFirstAmongChildren((YouTubePlayerView) provider, viewFinder);
        if (shareButton != null) try {
            ViewGroup.LayoutParams lp = shareButton.getLayoutParams();
            lp.height = 0;
            lp.width = 0;
            shareButton.setLayoutParams(lp);
        } catch (Exception e) { /*ignored*/ }

        player.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {
            @Override
            public void onLoading() { }
            @Override
            public void onLoaded(String s) { }
            @Override
            public void onAdStarted() { }
            @Override
            public void onVideoStarted() {}
            @Override
            public void onVideoEnded() { }
            @Override
            public void onError(YouTubePlayer.ErrorReason errorReason) {
                Log.d(TAG, errorReason.toString());
            }
        });
        player.loadVideo(videoTutorialItem.url/*"1af69S3B-OY"*/);
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult error) {
        if (error.isUserRecoverableError()) {
            error.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show();
        } else {
            Globals.showMessage("YouTube initialization failed" + error.toString());
        }
    }

    private static class ViewFinderByContentDescription implements ApplicableToView {
        String sampleContentDescription;

        ViewFinderByContentDescription(String contentDescription) {
            this.sampleContentDescription = contentDescription;
        }

        @Override
        public boolean methodToApply(View v) {
            CharSequence contentDescription = (v instanceof ImageView/*no need for null check in instanceof*/) ?
                    v.getContentDescription() : null;
            return (contentDescription != null && contentDescription.equals(sampleContentDescription));
        }
    }
}
