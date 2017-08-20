package org.foundation101.karatel.adapter;

import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.Violation;

import java.util.ArrayList;

/**
 * Created by Dima on 04.05.2016.
 */
public class ViolationsAdapter extends BaseAdapter {
    private static final float[] COLOR_MATRIX_ALL_GREY = {
            0, 0, 0, 0, 255, // red
            0, 0, 0, 0, 255, // green
            0, 0, 0, 0, 255, // blue
            1, 1, 1, 1, 0  // alpha
    };

    public static ArrayList<Violation> content;

    @Override
    public int getCount() {
        return content.size();
    }

    @Override
    public Object getItem(int position) {
        return content.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (convertView==null){
            convertView=inflater.inflate(R.layout.item_violation, parent, false);
        }

        Violation thisViolation = content.get(position);

        ImageView violationImage=(ImageView)convertView.findViewById(R.id.violationImage);
        violationImage.setImageResource(thisViolation.drawableId);

        TextView violationName=(TextView)convertView.findViewById(R.id.violationName);
        violationName.setText(thisViolation.name);

        ImageView cameraIcon=(ImageView)convertView.findViewById(R.id.cameraIcon);
        cameraIcon.setVisibility(thisViolation.usesCamera ? View.VISIBLE : View.GONE);

        if (thisViolation.isActive()) {
            convertView.setActivated(false); //views are not activated by default -> use activation for disabled violations
            violationImage.setColorFilter(null);
            violationName.setTextColor(ContextCompat.getColor(KaratelApplication.getInstance(), R.color.violationName));
        } else {
            convertView.setActivated(true);
            violationImage.setColorFilter(new ColorMatrixColorFilter(COLOR_MATRIX_ALL_GREY));
            violationName.setTextColor(Color.WHITE);
        }

        return convertView;
    }
}
