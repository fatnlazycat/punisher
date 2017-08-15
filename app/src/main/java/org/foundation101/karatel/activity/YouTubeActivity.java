package org.foundation101.karatel.activity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubePlayerView;

import org.foundation101.karatel.R;
import org.foundation101.karatel.utils.ApplicableToView;
import org.foundation101.karatel.utils.ViewUtils;

/**
 * Created by Dima on 10.08.2017.
 */

public class YouTubeActivity extends YouTubeBaseActivity implements AppCompatCallback,
        YouTubePlayer.OnInitializedListener {
    private static final String TAG = "YouTubeActivity";
    private static final String YOU_TUBE_APP_BUTTON_CONTENT_DESCRIPTION = "Watch this video in YouTube";

    private AppCompatDelegate delegate;
    YouTubePlayerView youTubeView;
    String youTubeKey;

    private static final int RECOVERY_DIALOG_REQUEST = 1;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        delegate = AppCompatDelegate.create(this, this);
        delegate.setContentView(R.layout.activity_youtube);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        delegate.setSupportActionBar(toolbar);
        ActionBar actionBar = delegate.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_back_green);
        actionBar.setTitle(R.string.video_tutorial);

        ImageView ivLogo = (ImageView) findViewById(R.id.ivCoachLogo);

        youTubeKey = getString(R.string.google_api_key);
        youTubeView = (YouTubePlayerView) findViewById(R.id.youTubeView);
        initYouTubeView();

        TextView tvTitle = (TextView) findViewById(R.id.tvTitle);
        TextView tvDescription = (TextView) findViewById(R.id.tvDescription);
    }

    void initYouTubeView() {
        if (youTubeView != null) youTubeView.initialize(youTubeKey, this);
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
        /*View youTubeAppButton = ViewUtils.findFirstAmongChildren((YouTubePlayerView) provider, new ApplicableToView() {
            @Override
            public boolean methodToApply(View v) {
                CharSequence contentDescription = (v != null && v instanceof ImageView) ?
                        v.getContentDescription() : null;
                return (contentDescription != null && contentDescription.equals(YOU_TUBE_APP_BUTTON_CONTENT_DESCRIPTION));
            }
        });
        if (youTubeAppButton != null) youTubeAppButton.setVisibility(View.GONE);*/
        player.setPlayerStateChangeListener(new YouTubePlayer.PlayerStateChangeListener() {

            @Override
            public void onLoading() {

            }

            @Override
            public void onLoaded(String s) {

            }

            @Override
            public void onAdStarted() {

            }

            @Override
            public void onVideoStarted() {

            }

            @Override
            public void onVideoEnded() {

            }

            @Override
            public void onError(YouTubePlayer.ErrorReason errorReason) {
                Log.d(TAG, errorReason.toString());
            }
        });
        player.loadVideo(/*getIntent().getStringExtra()*/"1af69S3B-OY");
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult error) {
        if (error.isUserRecoverableError()) {
            error.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show();
        } else {
            Toast.makeText(YouTubeActivity.this,
                    "YouTube initialization failed" + error.toString(), Toast.LENGTH_LONG).show();
        }
    }
}
