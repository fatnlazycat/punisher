package org.foundation101.karatel.fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.TipsActivity;

/**
 * Created by Dima on 02.08.2016.
 */
public class TutorialFragment extends Fragment {

    public static final String STAGE = "tutorialFragmentStage";

    private CharSequence[] texts;

    ImageView tutorialImage;
    ImageView[] circles;
    TextView tutorialText;

    public TutorialFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int stage = getArguments().getInt(STAGE);
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.item_tutorial, container, false);


        tutorialImage = (ImageView)rootView.findViewById(R.id.imageViewTutorial);
        tutorialImage.setImageResource(R.drawable.level_list_tutorial);
        tutorialImage.setImageLevel(stage);

        tutorialText = (TextView)rootView.findViewById(R.id.textViewTutorial);
        texts=getResources().getTextArray(R.array.tutorialStringArray);
        tutorialText.setText(texts[stage]);

        //setting the small circles
        circles = new ImageView[]{
                (ImageView)rootView.findViewById(R.id.small_circle0),
                (ImageView)rootView.findViewById(R.id.small_circle1),
                (ImageView)rootView.findViewById(R.id.small_circle2),
                (ImageView)rootView.findViewById(R.id.small_circle3),
                (ImageView)rootView.findViewById(R.id.small_circle4),
        };
        for (int i=0; i < circles.length; i++){
            if (i==stage) circles[i].setImageResource(R.drawable.small_green_circle);
            else circles[i].setImageResource(R.drawable.small_grey_circle);
        }

        return rootView;
    }
}
