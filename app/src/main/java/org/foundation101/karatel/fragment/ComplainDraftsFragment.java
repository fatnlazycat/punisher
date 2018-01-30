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
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.foundation101.karatel.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.ComplainDraftsAdapter;
import org.foundation101.karatel.adapter.ItemTouchHelperAdapter;
import org.foundation101.karatel.entity.ComplainRequest;
import org.foundation101.karatel.entity.Violation;
import org.foundation101.karatel.utils.DBUtils;

import java.util.ArrayList;

public class ComplainDraftsFragment extends Fragment {
    static final String TAG = "Drafts";

    View mainView;
    RecyclerView recycler;
    ComplainDraftsAdapter complainDraftsAdapter;
    Snackbar snackbar;

    SQLiteDatabase db;

    public static boolean punishPerformed = false;

    public ComplainDraftsFragment(){
        //required empty constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DBHelper(getContext(), DBHelper.DATABASE, DBHelper.DB_VERSION).getReadableDatabase();

        //Google Analytics part
        KaratelApplication.getInstance().sendScreenName(TAG);
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

        complainDraftsAdapter = new ComplainDraftsAdapter(getContext());

        makeComplainsBookAdapterContent();

        recycler.setAdapter(complainDraftsAdapter);
        ItemTouchHelper.Callback callback = new MyItemTouchHelperCallback(complainDraftsAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recycler);
    }

    @Override
    public void onResume() {
        makeComplainsBookAdapterContent();
        super.onResume();
    }

    void makeComplainsBookAdapterContent(){
        ArrayList<ComplainRequest> drafts = getDraftRequests();
        if (drafts.isEmpty()) {
            returnToComplainsBook();
        } else {
            complainDraftsAdapter.setContent(drafts);
        }
    }

    ArrayList<ComplainRequest> getDraftRequests(){
        ArrayList<ComplainRequest> draftRequests = new ArrayList<>();
        String table = DBHelper.COMPLAINS_TABLE;
        String[] columns = null /*{DBHelper._ID, DBHelper.ID_SERVER, DBHelper.TYPE, DBHelper.STATUS,
                DBHelper.TIME_STAMP}*/;
        String where = "user_id=?";
        String[] selectionArgs = {Integer.toString(Globals.user.id)};
        Cursor cursor = db.query(table, columns, where, selectionArgs, null, null, null);
        while (cursor.moveToNext()){
            ComplainRequest request = new ComplainRequest();
            request.type = cursor.getString(cursor.getColumnIndex(DBHelper.TYPE));

            String briefColumn = Violation.getByType(request.type).getRequisites().get(0).dbTag;
            request.brief = cursor.getString(cursor.getColumnIndex(briefColumn));

            request.creation_date = cursor.getString(cursor.getColumnIndex(DBHelper.TIME_STAMP));
            request.id = cursor.getInt(cursor.getColumnIndex(DBHelper._ID));
            draftRequests.add(request);
        }
        cursor.close();
        return draftRequests;
    }

    void undoDeleteRequest(ComplainRequest request, int position){
        complainDraftsAdapter.getContent().add(position, request);
        complainDraftsAdapter.notifyItemInserted(position);
        recycler.scrollToPosition(position);
    }

    void deleteDraftRequest(Integer position){
        DBHelper.deleteComplainRequest(db, position);
    }

    void returnToComplainsBook() {
        Log.d(TAG, "isVisible = " + isVisible());
        Log.d(TAG, "isResumed = " + isResumed());

        //check if isResumed to avoid fragmentTransaction after activity's saveInstanceState
        //and we need to delay because calling this before onViewCreated finishes causes IllegalStateException: FragmentManager is already executing transactions
        if (isResumed()) new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
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

            final ComplainRequest requestToDelete = complainDraftsAdapter.getContent().get(position);
            touchAdapter.onItemDismiss(position);
            snackbar = Snackbar.make(recycler, "Видалено", Snackbar.LENGTH_LONG)
                    .setAction("UNDO", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            undoDeleteRequest(requestToDelete, position);
                        }
                    })
                    .setActionTextColor(ContextCompat.getColor(KaratelApplication.getInstance(), R.color.colorPrimary))
                    .setCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar snackbar, int event) {
                            super.onDismissed(snackbar, event);
                            //if we didn't restore the position - delete it
                            if (event == DISMISS_EVENT_TIMEOUT
                                    || event == DISMISS_EVENT_SWIPE
                                    || event == DISMISS_EVENT_CONSECUTIVE) {
                                deleteDraftRequest(requestToDelete.id);
                                if (DBUtils.getNumberOfComplains() == 0) returnToComplainsBook();
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