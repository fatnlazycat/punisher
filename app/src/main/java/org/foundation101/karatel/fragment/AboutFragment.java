package org.foundation101.karatel.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.evernote.android.job.JobManager;

import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.manager.KaratelPreferences;

/**
 * Created by Dima on 14.05.2016.
 */
public class AboutFragment extends Fragment {
    static final String TAG = "About";

    public AboutFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        KaratelApplication.getInstance().sendScreenName(TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about, container, false);

        //debug block
        TextView tv = v.findViewById(R.id.textView5);
        if (JobManager.instance().getAllJobRequests().size() > 0) tv.setText(tv.getText() + "?");
        if (!KaratelPreferences.pendingJob().isEmpty()) tv.setAllCaps(true  );
        int debugColour = 0;
        if (KaratelPreferences.pushToken().isEmpty() && !KaratelPreferences.newPushToken().isEmpty()) {
            debugColour = R.color.a_la_red;
        } else {
            if ( KaratelPreferences.   pushToken().isEmpty()) debugColour = R.color.colorPrimary;
            if (!KaratelPreferences.newPushToken().isEmpty()) debugColour = R.color.a_la_blue;
        }
        if (debugColour != 0) tv.setTextColor(ContextCompat.getColor(KaratelApplication.getInstance(),debugColour));

        return v;
    }
}
