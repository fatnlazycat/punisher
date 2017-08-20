package org.foundation101.karatel.fragment;

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
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.foundation101.karatel.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.ComplainDraftsAdapter;
import org.foundation101.karatel.adapter.ItemTouchHelperAdapter;
import org.foundation101.karatel.entity.Request;

import java.util.ArrayList;

public class ComplainDraftsFragment extends Fragment {
    static final String TAG = "Complain Drafts";

    View mainView;
    RecyclerView recycler;
    ComplainDraftsAdapter complainDraftsAdapter = new ComplainDraftsAdapter();
    Snackbar snackbar;

    SQLiteDatabase db;

    public static boolean punishPerformed = false;

    public ComplainDraftsFragment(){
        //required empty constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DBHelper(getContext(), DBHelper.DATABASE, 1).getReadableDatabase();
        ((KaratelApplication)getActivity().getApplication()).sendScreenName(TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_complain_drafts, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainView = view;

        recycler = (RecyclerView)view.findViewById(R.id.recyclerViewDrafts);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        makeComplainsBookAdapterContent();

        recycler.setAdapter(complainDraftsAdapter);
        ItemTouchHelper.Callback callback = new MyItemTouchHelperCallback(complainDraftsAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recycler);
    }

    @Override
    public void onResume() {
        if (punishPerformed) {
            makeComplainsBookAdapterContent();
            punishPerformed = false;
        }
        super.onResume();
    }

    void makeComplainsBookAdapterContent(){
        complainDraftsAdapter.setContent(getDraftRequests());
    }

    ArrayList<Request> getDraftRequests(){
        ArrayList<Request> draftRequests = new ArrayList<>();
        String table = DBHelper.COMPLAINS_TABLE;
        String[] columns = {DBHelper._ID, DBHelper.ID_SERVER, DBHelper.TYPE, DBHelper.STATUS,
                DBHelper.TIME_STAMP};
        String where = "user_id=?";
        String[] selectionArgs = {Integer.toString(Globals.user.id)};
        Cursor cursor = db.query(table, columns, where, selectionArgs, null, null, null);
        while (cursor.moveToNext()){
            Request request = new Request();
            request.type = cursor.getString(cursor.getColumnIndex(DBHelper.TYPE));
            request.complain_status_id = cursor.getInt(cursor.getColumnIndex(DBHelper.STATUS));
            request.created_at = cursor.getString(cursor.getColumnIndex(DBHelper.TIME_STAMP));
            request.id = cursor.getInt(cursor.getColumnIndex(DBHelper._ID));
            draftRequests.add(request);
        }
        cursor.close();
        return draftRequests;
    }

    void undoDeleteRequest(Request request, int position){
        complainDraftsAdapter.getContent().add(position, request);
        complainDraftsAdapter.notifyItemInserted(position);
        recycler.scrollToPosition(position);
    }

    void deleteDraftRequest(Integer position){
        DBHelper.deleteRequest(db, position);
    }

    private class MyItemTouchHelperCallback extends ItemTouchHelper.Callback{
        private final ItemTouchHelperAdapter touchAdapter;
        private Paint p = new Paint();

        MyItemTouchHelperCallback(ItemTouchHelperAdapter adapter){
            touchAdapter = adapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags, swipeFlags;
            dragFlags = 0; // no drag at all
            swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
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

            final Request requestToDelete = complainDraftsAdapter.getContent().get(position);
            touchAdapter.onItemDismiss(position);
            snackbar = Snackbar.make(recycler, "Видалено", Snackbar.LENGTH_LONG)
                    .setAction("UNDO", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            undoDeleteRequest(requestToDelete, position);
                        }
                    }).setActionTextColor(ContextCompat.getColor(KaratelApplication.getInstance(), R.color.colorPrimary))
                    .setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            super.onDismissed(snackbar, event);
                            //if we didn't restore the position - delete it
                            if (event == DISMISS_EVENT_TIMEOUT
                                    || event == DISMISS_EVENT_SWIPE
                                    || event == DISMISS_EVENT_CONSECUTIVE) {
                                deleteDraftRequest(requestToDelete.id);
                            }
                        }
                    });
            snackbar.show();
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            Bitmap icon;
            if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE){
                View itemView = viewHolder.itemView;
                float height = (float) itemView.getBottom() - (float) itemView.getTop();
                float width = height / 3;

                p.setColor(Color.parseColor("#c4cad0"));
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