package com.example.dnk.punisher.adapter;

import android.util.TypedValue;
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
public class DrawerAdapter extends BaseAdapter {
    public static String[] content;

    @Override
    public int getCount() {
        return content.length;
    }

    @Override
    public Object getItem(int position) {
        return content[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (convertView==null){
            convertView=inflater.inflate(R.layout.item_drawer, parent, false);
        }

        //set menu item text
        TextView menuItemText=(TextView)convertView.findViewById(R.id.menuItemText);
        menuItemText.setText(content[position]);

        switch (position) {
            case 0: {//first line where we need avatar icon
                ImageView avatarImageView = (ImageView) convertView.findViewById(R.id.avatarImageView);
                avatarImageView.setImageResource(R.mipmap.no_avatar);
                menuItemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
                break;
            }
            case 1:
                menuItemText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.divider_drawable, 0, 0);
                break;
            case 3:
                menuItemText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.divider_drawable, 0, 0);
                break;
            case 6:
                menuItemText.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.divider_drawable, 0, 0);
                break;
        }

        /*
         *set icon with the number of claims for the line that equals "Заявки"
         *(or else specified in appropriate String resource)
         */
        if (content[position].equals(parent.getContext().getResources().getString(R.string.menu_item_claims))) {
            TextView extraText = (TextView)convertView.findViewById(R.id.extraText);
            extraText.setText("0");
        }

        return convertView;
    }
}
