package org.foundation101.karatel.fragment;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.evernote.android.job.JobManager;

import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.manager.KaratelPreferences;

import javax.inject.Inject;

/**
 * Created by Dima on 14.05.2016.
 */
public class AboutFragment extends Fragment {
    static final String TAG = "About";

    @Inject KaratelPreferences preferences;

    public AboutFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        KaratelApplication.dagger().inject(this);

        KaratelApplication.getInstance().sendScreenName(TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about, container, false);

        //debug block
        TextView tv = v.findViewById(R.id.textView5);
        if (JobManager.instance().getAllJobRequests().size() > 0) tv.setText(tv.getText() + "?");
        if (!preferences.pendingJob().isEmpty()) tv.setAllCaps(true  );
        int debugColour = 0;
        if (preferences.pushToken().isEmpty() && !preferences.newPushToken().isEmpty()) {
            debugColour = R.color.a_la_red;
        } else {
            if ( preferences.   pushToken().isEmpty()) debugColour = R.color.colorPrimary;
            if (!preferences.newPushToken().isEmpty()) debugColour = R.color.a_la_blue;
        }
        if (debugColour != 0) tv.setTextColor(ContextCompat.getColor(KaratelApplication.getInstance(),debugColour));

        return v;
    }
}
