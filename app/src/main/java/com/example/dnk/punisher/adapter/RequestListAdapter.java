package com.example.dnk.punisher.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.dnk.punisher.R;
import com.example.dnk.punisher.Violation;

import java.util.ArrayList;

/**
 * Created by Dima on 04.05.2016.
 */
public class RequestListAdapter extends CursorAdapter {
    public static ArrayList<Violation> content;

    public RequestListAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        TextView textViewRequestStatus=(TextView)view.findViewById(R.id.textViewRequestStatus);
        //textViewRequestStatus.setText(thisViolation.name);

        TextView textViewRequestTimeStamp=(TextView)view.findViewById(R.id.textViewRequestTimeStamp);
        textViewRequestTimeStamp.setText(cursor.getString(cursor.getColumnIndex("time_stamp")));

        TextView textViewRequestType=(TextView)view.findViewById(R.id.textViewRequestType);
        textViewRequestType.setText(cursor.getString(cursor.getColumnIndex("type")));

    }
}
