package org.foundation101.karatel.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.foundation101.karatel.R;

public class TutorialActivity extends Activity {
    private int stage;
    final int MAX_STAGE=4;
    public static final String FIRST_TIME_TUTORIAL = "firstTimeTutorial";
    private String[] texts;

    ImageView tutorialImage;
    ImageView[] circles;
    TextView tutorialText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        tutorialImage = (ImageView)findViewById(R.id.imageViewTutorial);
        tutorialImage.setImageResource(R.drawable.level_list_tutorial);

        tutorialText = (TextView)findViewById(R.id.textViewTutorial);
        texts=getResources().getStringArray(R.array.tutorialStringArray);
        tutorialText.setText(texts[0]);

        //progressBarTutorial = (ImageView)findViewById(R.id.progressBarTutorial);
        //progressBarTutorial.setImageResource(R.drawable.level_list_tutorial_progress);

        //setting the small circles
        circles = new ImageView[]{
            (ImageView)findViewById(R.id.small_circle0),
            (ImageView)findViewById(R.id.small_circle1),
            (ImageView)findViewById(R.id.small_circle2),
            (ImageView)findViewById(R.id.small_circle3),
            (ImageView)findViewById(R.id.small_circle4),
        };
        for (int i=0; i < circles.length; i++){
            if (i==stage) circles[i].setImageResource(R.drawable.small_green_circle);
                else circles[i].setImageResource(R.drawable.small_grey_circle);
        }
    }

    public void proceedWithTutorial(View view) {
        if (stage++ < MAX_STAGE) {
            tutorialText.setText(texts[stage]);
            tutorialImage.setImageLevel(stage);
            for (int i=0; i < circles.length; i++){
                if (i==stage) circles[i].setImageResource(R.drawable.small_green_circle);
                else circles[i].setImageResource(R.drawable.small_grey_circle);
            }
        } else {
            if (getIntent().getBooleanExtra(FIRST_TIME_TUTORIAL, true)) {
                startActivity(new Intent(this, TipsActivity.class));
            } else {
                finish();
            }
        }
    }
}
