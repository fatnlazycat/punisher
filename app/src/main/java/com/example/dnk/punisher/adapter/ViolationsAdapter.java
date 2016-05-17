package com.example.dnk.punisher.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.dnk.punisher.R;
import com.example.dnk.punisher.Violation;

import java.util.ArrayList;

/**
 * Created by Dima on 04.05.2016.
 */
public class ViolationsAdapter extends BaseAdapter {
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

        if (thisViolation.usesCamera){
            ImageView cameraIcon=(ImageView)convertView.findViewById(R.id.cameraIcon);
            cameraIcon.setImageResource(R.mipmap.ic_record);
        }

        return convertView;
    }
}
