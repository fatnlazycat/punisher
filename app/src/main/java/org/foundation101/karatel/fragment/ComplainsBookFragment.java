package org.foundation101.karatel.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.ComplainsBookAdapter;

/**
 * Created by Dima on 10.08.2017.
 */

public class ComplainsBookFragment extends Fragment {
    public ComplainsBookFragment() { /*Required empty public constructor*/ }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_complains_book, container, false);

        ListView lvComplain = (ListView) v.findViewById(R.id.lvComplainTypes);
        lvComplain.setAdapter(new ComplainsBookAdapter());
        lvComplain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.complains_book_menu, menu);
        final MenuItem signItem = menu.findItem(R.id.complainsListItem);
        signItem.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onOptionsItemSelected(signItem);
            }
        });
    }
}
