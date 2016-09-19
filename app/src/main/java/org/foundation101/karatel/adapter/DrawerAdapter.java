package org.foundation101.karatel.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.foundation101.karatel.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;

/**
 * Created by Dima on 04.05.2016.
 */
public class DrawerAdapter extends BaseAdapter {
    public static String[] content;
    Context context;

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
            context = parent.getContext();
        }

        //set menu item text
        TextView menuItemText=(TextView)convertView.findViewById(R.id.menuItemText);
        menuItemText.setText(content[position]);

        switch (position) {
            case 0:
                setDivider(menuItemText);
                break;
            case 2:
                setDivider(menuItemText);
                break;
            case 5:
                setDivider(menuItemText);
                break;
        }

        /*
         *set icon with the number of claims for the line that equals "Заявки"
         *(or else specified in appropriate String resource)
         */
        if (content[position].equals(parent.getContext().getResources().getString(R.string.menu_item_claims))) {
            TextView extraText = (TextView)convertView.findViewById(R.id.extraText);
            Integer count = getNumberOfRequests(parent.getContext());
            if (count > 0) {
                menuItemText.setTypeface(menuItemText.getTypeface(), Typeface.BOLD);
                extraText.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
                extraText.setText(count.toString());
                extraText.setVisibility(View.VISIBLE);
            } else {
                extraText.setVisibility(View.GONE);
                menuItemText.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            }
        }

        return convertView;
    }

    void setDivider(TextView view){
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.divider_drawable, 0, 0);
        view.setCompoundDrawablePadding(dpToPx(12));
        view.setPadding(0, 0, 0, dpToPx(12));
    }

    public int dpToPx(int dp){
        return dpToPx(context, dp);
    }

    public static int dpToPx(Context context, int dp){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

    public static int getNumberOfRequests(Context context){
        SQLiteDatabase db = new DBHelper(context, DBHelper.DATABASE, 1).getReadableDatabase();
        String table = DBHelper.VIOLATIONS_TABLE;
        String[] columns = {DBHelper._ID, DBHelper.TYPE};
        String where = "user_id=?";
        String[] selectionArgs = {Globals.user.id.toString()};
        Cursor cursor = db.query(table, columns, where, selectionArgs, null, null, null);
        int result = cursor.getCount();
        cursor.close();
        db.close();
        return result;
    }
}
