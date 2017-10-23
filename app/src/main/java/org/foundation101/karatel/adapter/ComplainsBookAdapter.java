package org.foundation101.karatel.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.Violation;

import java.util.ArrayList;

/**
 * Created by Dima on 20.08.2017.
 */

public class ComplainsBookAdapter extends BaseAdapter {
    static ArrayList<Violation> content;

    public static ArrayList<Violation> getContent() {
        return content;
    }
    public void setContent(ArrayList<Violation> newContent) {
        content = newContent;
    }

    public ComplainsBookAdapter(ArrayList<Violation> newContent) {
        setContent(newContent);
    }

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
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (convertView==null){
            convertView=inflater.inflate(R.layout.item_complains_book, parent, false);
        }

        Violation thisViolation = content.get(position);

        ImageView violationImage=(ImageView)convertView.findViewById(R.id.violationImage);
        violationImage.setImageResource(thisViolation.drawableId);

        return convertView;
    }
}
