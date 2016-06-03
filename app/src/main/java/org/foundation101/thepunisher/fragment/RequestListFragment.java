package org.foundation101.thepunisher.fragment;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.foundation101.thepunisher.Globals;
import org.foundation101.thepunisher.DBHelper;
import org.foundation101.thepunisher.ItemTouchHelperAdapter;
import org.foundation101.thepunisher.R;
import org.foundation101.thepunisher.adapter.RequestListAdapter;

public class RequestListFragment extends Fragment {
    View mainView;
    RecyclerView recycler;
    FrameLayout progressBar;

    public RequestListFragment(){
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list_of_requests, container, false);

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainView = view;

        SQLiteDatabase db = new DBHelper(getContext(), "violations_db", 1).getReadableDatabase();
        String table = "violations_table";
        String[] columns = {DBHelper._ID, DBHelper.ID_SERVER, DBHelper.TYPE, DBHelper.STATUS,
                DBHelper.TIME_STAMP};
        String where = "user_id=?";
        String[] selectionArgs = {Globals.user.id.toString()};
        Cursor cursor = db.query(table, columns, where, selectionArgs, null, null, null);

        recycler = (RecyclerView)view.findViewById(R.id.recyclerViewRequests);
        if (cursor.getCount() == 0) { //there are no requests
            showNoRequestsLayout();
        } else {
            progressBar = (FrameLayout) view.findViewById(R.id.frameLayoutProgress);

            recycler.setHasFixedSize(true);
            recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
            RequestListAdapter requestListAdapter = new RequestListAdapter(getContext(), cursor, progressBar);
            recycler.setAdapter(requestListAdapter);
            ItemTouchHelper.Callback callback = new MyItemTouchHelperCallback(requestListAdapter);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(recycler);
        }
    }

    void showNoRequestsLayout(){
        recycler.setVisibility(View.GONE);
        LinearLayout layoutNoRequests = (LinearLayout)mainView.findViewById(R.id.layoutNoRequests);
        layoutNoRequests.setVisibility(View.VISIBLE);
    }

    class MyItemTouchHelperCallback extends ItemTouchHelper.Callback{
        private final ItemTouchHelperAdapter touchAdapter;
        private Paint p = new Paint();

        MyItemTouchHelperCallback(ItemTouchHelperAdapter adapter){
            touchAdapter = adapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            touchAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            final int position = viewHolder.getAdapterPosition();
            touchAdapter.onItemDismiss(position);
            if (position == 0) showNoRequestsLayout();
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            Bitmap icon;
            if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){
                View itemView = viewHolder.itemView;
                float height = (float) itemView.getBottom() - (float) itemView.getTop();
                float width = height / 3;

                p.setColor(Color.parseColor("#D32F2F"));
                RectF background = new RectF((float) itemView.getRight() + dX, (float) itemView.getTop(),(float) itemView.getRight(), (float) itemView.getBottom());
                c.drawRect(background,p);
                icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete);
                RectF icon_dest = new RectF((float) itemView.getRight() - 2*width ,(float) itemView.getTop() + width,(float) itemView.getRight() - width,(float)itemView.getBottom() - width);
                c.drawBitmap(icon,null,icon_dest,p);
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}




