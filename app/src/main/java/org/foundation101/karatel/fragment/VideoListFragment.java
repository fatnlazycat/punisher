package org.foundation101.karatel.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.YouTubeActivity;

/**
 * Created by Dima on 10.08.2017.
 */

public class VideoListFragment extends Fragment {
    public VideoListFragment() { /*Required empty public constructor*/ }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_video_list, container, false);
        ExpandableListView lvVideos = (ExpandableListView) v.findViewById(R.id.lvVideos);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Intent intent = new Intent(KaratelApplication.getInstance(), YouTubeActivity.class);
        startActivity(intent);
    }
}
