package org.foundation101.karatel.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.YouTubeActivity;
import org.foundation101.karatel.adapter.ExpandableListAdapterWithCustomDividers;
import org.foundation101.karatel.entity.VideoTutorialGroup;
import org.foundation101.karatel.entity.VideoTutorialItem;
import org.foundation101.karatel.utils.AssetsUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Dima on 10.08.2017.
 */

public class VideoListFragment extends Fragment {
    static final String TAG = "Videolessons";

    public VideoListFragment() { /*Required empty public constructor*/ }

    List<Map<String, String>> groupData = new ArrayList<>();
    List<List<Map<String, String>>> childrenData = new ArrayList<>();
    List<VideoTutorialGroup> groupsListFromJSON = new ArrayList<>();

    private Transformer<VideoTutorialGroup, Map<String, String>> groupTransformer =
        new Transformer<VideoTutorialGroup, Map<String, String>>() {
            @Override
            public Map<String, String> transform(VideoTutorialGroup input) {
                Map<String, String> result = new HashMap<>();
                result.put("header", input.groupName);
                return result;
            }
    };

    private Transformer<VideoTutorialGroup, List<Map<String, String>>> childTransformer =
        new Transformer<VideoTutorialGroup, List<Map<String, String>>>() {
            @Override
            public List<Map<String, String>> transform(VideoTutorialGroup input) {
                return new ArrayList<>(CollectionUtils.collect(Arrays.asList(input.items), videoItemsTransformer));
            }
        };

    private Transformer<VideoTutorialItem, Map<String, String>> videoItemsTransformer
        = new Transformer<VideoTutorialItem, Map<String, String>>() {
            @Override
            public Map<String, String> transform(VideoTutorialItem input) {
                Map<String, String> result = new HashMap<>();
                result.put("header", input.title);
                return result;
            }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        KaratelApplication.getInstance().sendScreenName(TAG);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_video_list, container, false);
        ExpandableListView lvVideos = v.findViewById(R.id.lvVideos);
        ExpandableListAdapterWithCustomDividers adapter = makeAdapter();
        lvVideos.setAdapter(adapter);

        int groupCount = lvVideos.getCount();
        for (int i = 0; i < groupCount; i++) lvVideos.expandGroup(i);

        lvVideos.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                VideoTutorialItem videoItem = groupsListFromJSON.get(groupPosition).items[childPosition];
                Intent intent = new Intent(KaratelApplication.getInstance(), YouTubeActivity.class);
                intent.putExtra(Globals.YOUTUBE_VIDEO_NAME, videoItem);
                startActivity(intent);
                return true;
            }
        });

        lvVideos.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true;
            }
        });

        lvVideos.setGroupIndicator(null);
        lvVideos.setDivider(null);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private ExpandableListAdapterWithCustomDividers makeAdapter() {
        String[] groupFrom = new String[] {"header"};
        String[] childFrom = groupFrom;
        int[] groupTo = new int[] {R.id.tvVideoGroup};
        int[] childTo = new int[] {R.id.tvVideoHeader};

        try {
            String videoTutorialsString = new JSONObject(AssetsUtils.readAssets("videos.json")).getJSONArray("videos").toString();
            ObjectMapper objectMapper = new ObjectMapper();
            groupsListFromJSON = objectMapper.readValue(videoTutorialsString, new TypeReference<List<VideoTutorialGroup>>() {});
            groupData = new ArrayList<>(CollectionUtils.collect(groupsListFromJSON, groupTransformer));
            childrenData = new ArrayList<>(CollectionUtils.collect(groupsListFromJSON, childTransformer));
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        return new ExpandableListAdapterWithCustomDividers(KaratelApplication.getInstance(),
                groupData,      R.layout.item_video_group,  groupFrom, groupTo,
                childrenData,   R.layout.item_video,        childFrom, childTo);
    }
}
