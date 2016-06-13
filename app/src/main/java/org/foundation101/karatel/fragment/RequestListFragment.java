package org.foundation101.karatel.fragment;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.codehaus.jackson.map.ObjectMapper;
import org.foundation101.karatel.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.ItemTouchHelperAdapter;
import org.foundation101.karatel.R;
import org.foundation101.karatel.Request;
import org.foundation101.karatel.activity.MainActivity;
import org.foundation101.karatel.activity.ViolationActivity;
import org.foundation101.karatel.adapter.RequestListAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class RequestListFragment extends Fragment {
    View mainView;
    RecyclerView recycler;
    FrameLayout progressBar;
    RequestListAdapter requestListAdapter;
    RelativeLayout sortMenu;

    SQLiteDatabase db;

    public RequestListFragment(){
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new DBHelper(getContext(), "violations_db", 1).getReadableDatabase();
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Toolbar toolbar = ((MainActivity)getActivity()).toolbar;
        toolbar.inflateMenu(R.menu.sort_items_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                changeSortMenuVisibility();
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list_of_requests, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = (FrameLayout) view.findViewById(R.id.frameLayoutProgress);
        sortMenu = (RelativeLayout) view.findViewById(R.id.sortingLayout);
        mainView = view;

        ArrayList<Request> draftRequests = new ArrayList<>();
        String table = DBHelper.VIOLATIONS_TABLE;
        String[] columns = {DBHelper._ID, DBHelper.ID_SERVER, DBHelper.TYPE, DBHelper.STATUS,
                DBHelper.TIME_STAMP};
        String where = "user_id=? AND status=?"; //select only with status "draft"
        String[] selectionArgs = {Integer.toString(Globals.user.id), Integer.toString(ViolationActivity.MODE_EDIT)};
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

        recycler = (RecyclerView)view.findViewById(R.id.recyclerViewRequests);

        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        requestListAdapter = new RequestListAdapter(getContext(), progressBar);
        requestListAdapter.setContent(draftRequests);
        recycler.setAdapter(requestListAdapter);
        ItemTouchHelper.Callback callback = new MyItemTouchHelperCallback(requestListAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recycler);

        new RequestListFetcher().execute();
    }

    void showNoRequestsLayout(){
        recycler.setVisibility(View.GONE);
        LinearLayout layoutNoRequests = (LinearLayout)mainView.findViewById(R.id.layoutNoRequests);
        layoutNoRequests.setVisibility(View.VISIBLE);
    }

    void deleteRequestPermanently(int position){
        new RequestEraser().execute(position);
    }

    void undoDeleteRequest(Request request, int position){
        requestListAdapter.getContent().add(position, request);
        requestListAdapter.notifyItemInserted(position);
        recycler.scrollToPosition(position);
    }

    void deleteDraftRequest(Integer position){
        String idString = position.toString();
        db.delete(DBHelper.VIOLATIONS_TABLE, "id = ?", new String[]{idString});
        db.delete(DBHelper.MEDIA_TABLE, "id = ?", new String[]{idString});
    }

    void changeSortMenuVisibility(){
        int visibility = (sortMenu.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;
        sortMenu.setVisibility(visibility);
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

            final Request requestToDelete = requestListAdapter.getContent().get(position);
            touchAdapter.onItemDismiss(position);
            Snackbar snackbar = Snackbar
                .make(recycler, "Видалено", Snackbar.LENGTH_LONG)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        undoDeleteRequest(requestToDelete, position);
                    }
                }).setActionTextColor(getResources().getColor(R.color.colorPrimary))
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        //if we didn't restore the position - delete it
                        if (event == DISMISS_EVENT_TIMEOUT
                                || event == DISMISS_EVENT_SWIPE
                                || event == DISMISS_EVENT_CONSECUTIVE ){
                            if (requestToDelete.complain_status_id == ViolationActivity.MODE_EDIT)
                                deleteDraftRequest(requestToDelete.id);
                            else deleteRequestPermanently(requestToDelete.id);
                            if (position == 0) showNoRequestsLayout();
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

    class RequestListFetcher extends AsyncTask<Void, Void, String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... params) {
            return HttpHelper.proceedRequest("complains", "GET", "", true);
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.GONE);
            ArrayList<Request> requestsFromServer = new ArrayList<>();
            try {
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals("success")){
                    JSONArray dataJSON = json.getJSONArray("data");
                    for (int i = 0; i < dataJSON.length(); i++) {
                        JSONArray oneRequest = dataJSON.getJSONArray(i);
                        JSONObject requestBody = oneRequest.getJSONObject(1);

                        ObjectMapper mapper = new ObjectMapper();
                        String requestBodyString = requestBody.toString();
                        Request request = mapper.readValue(requestBodyString, Request.class);
                        request.type = oneRequest.getString(0);

                        requestsFromServer.add(request);

                    }
                    RequestListFragment.this.requestListAdapter.getContent().addAll(requestsFromServer);
                    RequestListFragment.this.requestListAdapter.notifyDataSetChanged();
                }
                if (RequestListFragment.this.requestListAdapter.getItemCount() == 0) { //there are no requests
                    showNoRequestsLayout();
                }
            } catch (JSONException | IOException e) {
                Log.e("Punisher", e.getMessage());
            }
        }
    }

    class RequestEraser extends AsyncTask<Integer, Void, String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Integer... params) {
            return HttpHelper.proceedRequest("complains/" + params[0], "DELETE", "", true);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.GONE);
        }
    }
}



