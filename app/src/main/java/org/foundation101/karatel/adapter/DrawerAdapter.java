package org.foundation101.karatel.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.foundation101.karatel.manager.DBHelper;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.manager.KaratelPreferences;

import javax.inject.Inject;

import static org.foundation101.karatel.utils.LayoutUtils.dpToPx;

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
        TextView menuItemText = convertView.findViewById(R.id.menuItemText);
        menuItemText.setText(content[position]);

        switch (position) {
            case 0:
                setDivider(menuItemText);
                break;
            case 1:
                boolean hasRequests = !KaratelApplication.getInstance().requests.isEmpty();
                setItemEnabled(menuItemText, hasRequests);
                if (hasRequests) setDivider(menuItemText);
                break;
            case 3:
                setDonateItem(menuItemText);
                break;
        }

        /*
         *set icon with the number of claims for the line that equals "Заявки"
         *(or else specified in appropriate String resource)
         */
        if (content[position].equals(parent.getContext().getResources().getString(R.string.menu_item_claims))) {
            TextView extraText = convertView.findViewById(R.id.extraText);
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

    private void setDivider(TextView view){
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.divider_drawable, 0, 0);
        view.setCompoundDrawablePadding(dpToPx(12));
        view.setPadding(0, 0, 0, dpToPx(12));
    }

    private void setItemEnabled(TextView view, boolean enabled) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.height = enabled ? ViewGroup.LayoutParams.WRAP_CONTENT : 0;
        view.setLayoutParams(params);
        if (!enabled) view.setText("");
    }

    private void setDonateItem(TextView view) {
        Context context = KaratelApplication.getInstance();

        //background part
        View llItemDrawer = (View) view.getParent();
        llItemDrawer.setBackgroundColor(ContextCompat.getColor(context, R.color.darkGreen));

        //text part
        view.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        //view.setTypeface(view.getTypeface(), Typeface.BOLD);

        //drawable part
        Drawable image = ContextCompat.getDrawable(context, R.mipmap.ic_donate);
        int h = dpToPx(58);
        int w = dpToPx(50);
        image.setBounds( 0, 0, w, h );
        view.setCompoundDrawables( image, null, null, null );
        view.setCompoundDrawablePadding(dpToPx(12));
    }

    public static int getNumberOfRequests(Context context){
        SQLiteDatabase db = new DBHelper(context, DBHelper.DATABASE, DBHelper.DB_VERSION).getReadableDatabase();
        String table = DBHelper.VIOLATIONS_TABLE;
        String[] columns = {DBHelper._ID, DBHelper.TYPE};
        String where = "user_id=?";
        String[] selectionArgs = {"" + new KaratelPreferences().userId()};
        Cursor cursor = db.query(table, columns, where, selectionArgs, null, null, null);
        int result = cursor.getCount();
        cursor.close();
        db.close();
        return result;
    }
}
