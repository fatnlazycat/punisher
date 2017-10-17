package org.foundation101.karatel.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.foundation101.karatel.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.ComplainActivity;
import org.foundation101.karatel.activity.MainActivity;
import org.foundation101.karatel.adapter.ComplainsBookAdapter;
import org.foundation101.karatel.entity.Violation;

/**
 * Created by Dima on 10.08.2017.
 */

public class ComplainsBookFragment extends Fragment {
    public ComplainsBookFragment() { /*Required empty public constructor*/ }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_complains_book, container, false);

        ListView lvComplain = (ListView) v.findViewById(R.id.lvComplainTypes);

        ComplainsBookAdapter adapter = new ComplainsBookAdapter(Violation.getViolationsList(Violation.CATEGORY_BUSINESS));
        lvComplain.setAdapter(adapter);
        lvComplain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Violation violation = ComplainsBookAdapter.getContent().get(position);
                Intent intent = new Intent(parent.getContext(), ComplainActivity.class)
                        .putExtra(Globals.VIOLATION_ACTIVITY_MODE, ComplainActivity.MODE_CREATE)
                        .putExtra(Globals.VIOLATION, violation);
                startActivity(intent);
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            showNumberOfComplains(((MainActivity) getActivity()).toolbar.getMenu());
        } catch (NullPointerException ignored) { /*something's null - do nothing then*/ }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        ((MainActivity)getActivity()).toolbar.inflateMenu(R.menu.complains_book_menu);
        showNumberOfComplains(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    void showNumberOfComplains(Menu menu) {
        int count = getNumberOfComplains();

        TextView tvNumberOfDrafts = (TextView) menu.findItem(R.id.complainsListItem).getActionView();
        tvNumberOfDrafts.setText(String.valueOf(count));
        if (count > 0) {
            tvNumberOfDrafts.setAlpha(1f);
            tvNumberOfDrafts.setTextColor(ContextCompat.getColor(KaratelApplication.getInstance(), android.R.color.white));
            tvNumberOfDrafts.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((MainActivity) getActivity()).openComplainDrafts();
                }
            });
        } else { //no drafts
            tvNumberOfDrafts.setAlpha(0.5f);
            tvNumberOfDrafts.setTextColor(ContextCompat.getColor(KaratelApplication.getInstance(), R.color.veryDarkGreen));
        }
    }

    public static int getNumberOfComplains(){
        SQLiteDatabase db = new DBHelper(KaratelApplication.getInstance(), DBHelper.DATABASE, DBHelper.DB_VERSION).getReadableDatabase();
        String table = DBHelper.COMPLAINS_TABLE;
        String[] columns = {DBHelper._ID};
        String where = "user_id=?";
        String[] selectionArgs = {Globals.user.id.toString()};
        Cursor cursor = db.query(table, columns, where, selectionArgs, null, null, null);
        int result = cursor.getCount();
        cursor.close();
        db.close();
        return result;
    }
}
