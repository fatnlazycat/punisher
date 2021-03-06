package org.foundation101.karatel.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.MainActivity;
import org.foundation101.karatel.activity.ViolationActivity;
import org.foundation101.karatel.adapter.ItemTouchHelperAdapter;
import org.foundation101.karatel.adapter.RequestListAdapter;
import org.foundation101.karatel.asyncTasks.AsyncTaskAction;
import org.foundation101.karatel.asyncTasks.RequestListFetcher;
import org.foundation101.karatel.entity.Request;
import org.foundation101.karatel.manager.DBHelper;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.service.MyGcmListenerService;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

public class RequestListFragment extends Fragment {
    static final String TAG = "Complaints";

    @Inject KaratelPreferences preferences;

    View mainView;
    RecyclerView recycler;
    View progressBar;
    RequestListAdapter requestListAdapter;
    RelativeLayout sortMenu;
    SwipeRefreshLayout swipeRefreshLayout;
    TextView textViewByStatus, textViewByDate;
    Snackbar snackbar;
    Toolbar toolbar;

    SQLiteDatabase db;

    final int COLOR_GREY = Color.parseColor("#86888a");
    final int COLOR_GREEN = Color.parseColor("#6c8c39");

    public static boolean punishPerformed = false;

    public RequestListFragment(){
        //required empty constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        punishPerformed = false;
        super.onCreate(savedInstanceState);
        db = new DBHelper(getContext(), DBHelper.DATABASE, DBHelper.DB_VERSION).getReadableDatabase();
        setHasOptionsMenu(true);

        KaratelApplication.getInstance().sendScreenName(TAG);
    }

    @Override
    public void onDestroy() {
        db.close();
        if (toolbar != null) toolbar.setOnMenuItemClickListener(null);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        toolbar = ((MainActivity)getActivity()).toolbar;
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list_of_requests, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        progressBar = view.findViewById(R.id.rlProgress);
        sortMenu = view.findViewById(R.id.sortingLayout);

        textViewByStatus = view.findViewById(R.id.textViewByStatus);
        textViewByStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textViewByStatus.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_sort_by_status_selected, 0, 0, 0);
                textViewByDate.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_sort_by_date, 0, 0, 0);
                textViewByStatus.setTextColor(COLOR_GREEN);
                textViewByDate.setTextColor(COLOR_GREY);

                Collections.sort(requestListAdapter.content, new RequestComparator(RequestComparator.SORT_FLAG_STATUS));
                requestListAdapter.notifyDataSetChanged();
            }
        });

        textViewByDate = view.findViewById(R.id.textViewByDate);
        textViewByDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textViewByStatus.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_sort_by_status, 0, 0, 0);
                textViewByDate.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_sort_by_date_selected, 0, 0, 0);
                textViewByStatus.setTextColor(COLOR_GREY);
                textViewByDate.setTextColor(COLOR_GREEN);

                Collections.sort(requestListAdapter.content, new RequestComparator(RequestComparator.SORT_FLAG_DATE));
                requestListAdapter.notifyDataSetChanged();
            }
        });

        mainView = view;

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (snackbar!=null && snackbar.isShownOrQueued()) {
                    swipeRefreshLayout.setRefreshing(false);
                } else {
                    //dialog to prompt refresh or decline - Bogdanovich asked to remove
                    //new RefreshListOfRequestsDialog(getActivity()).get().show();
                    swipeRefreshLayout.setRefreshing(false);
                    resetSortMenu();
                    makeRequestListAdapterContent();
                }
            }
        });

        recycler = view.findViewById(R.id.recyclerViewRequests);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        requestListAdapter = new RequestListAdapter(getContext());

        makeRequestListAdapterContentFirstTime();

        recycler.setAdapter(requestListAdapter);
        ItemTouchHelper.Callback callback = new MyItemTouchHelperCallback(requestListAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recycler);
    }

    @Override
    public void onResume() {
        if (punishPerformed) {
            makeRequestListAdapterContent();
            punishPerformed = false;
        }
        super.onResume();
    }

    void makeRequestListAdapterContent(){
        //commented out drafts
        //requestListAdapter.setContent(getDraftRequests());

        new RequestListFetcher(new RequestListFetcherActions(this)).execute();
    }

    void makeRequestListAdapterContentFirstTime(){
        //commented out drafts
        //requestListAdapter.setContent(getDraftRequests());

        new RequestListFetcherActions(this)
                .post(new ArrayList<>(KaratelApplication.getInstance().requests));
    }

    ArrayList<Request> getDraftRequests(){
        ArrayList<Request> draftRequests = new ArrayList<>();
        String table = DBHelper.VIOLATIONS_TABLE;
        String[] columns = {DBHelper._ID, DBHelper.ID_SERVER, DBHelper.TYPE, DBHelper.STATUS,
                DBHelper.TIME_STAMP};
        String where = "user_id=? AND status=?"; //select only with status "draft"
        String[] selectionArgs = {Integer.toString(preferences.userId()), Integer.toString(ViolationActivity.MODE_EDIT)};
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

    void showNoRequestsLayout(){
        swipeRefreshLayout.setVisibility(View.GONE);
        LinearLayout layoutNoRequests = mainView.findViewById(R.id.layoutNoRequests);
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
        DBHelper.deleteViolationRequest(db, position);
    }

    void changeSortMenuVisibility(){
        int visibility = (sortMenu.getVisibility() == View.VISIBLE) ? View.GONE : View.VISIBLE;
        sortMenu.setVisibility(visibility);
    }

    void resetSortMenu(){
        textViewByStatus.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_sort_by_status, 0, 0, 0);
        textViewByDate.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_sort_by_date_selected, 0, 0, 0);
        textViewByStatus.setTextColor(COLOR_GREY);
        textViewByDate.setTextColor(COLOR_GREEN);
    }

    private class MyItemTouchHelperCallback extends ItemTouchHelper.Callback{
        private final ItemTouchHelperAdapter touchAdapter;
        private Paint p = new Paint();

        MyItemTouchHelperCallback(ItemTouchHelperAdapter adapter){
            touchAdapter = adapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            final int position = viewHolder.getAdapterPosition();
            final Request requestToSwipe = requestListAdapter.getContent().get(position);
            int dragFlags, swipeFlags;
            dragFlags = 0; // no drag att all
            if (requestToSwipe.complain_status_id == 0) {//draft
                swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
            } else swipeFlags = 0; //we can delete only drafts
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
                                if (requestToDelete.complain_status_id == ViolationActivity.MODE_EDIT)
                                    deleteDraftRequest(requestToDelete.id);
                                else deleteRequestPermanently(requestToDelete.id);
                                if (requestListAdapter.getContent().size() == 0) showNoRequestsLayout();
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

    private class RequestEraser extends AsyncTask<Integer, Void, String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Integer... params) {
            try {
                return HttpHelper.proceedRequest("complains/" + params[0], "DELETE", "", true);
            } catch (final IOException e){
                Globals.showError(R.string.cannot_connect_server, e);
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressBar.setVisibility(View.GONE);
        }
    }

    private static class RequestComparator implements Comparator<Request>{
        static final int SORT_FLAG_STATUS = 1;
        static final int SORT_FLAG_DATE = 2;
        int sortFlag;

        RequestComparator(int sortFlag){
            this.sortFlag = sortFlag;
        }
        @Override
        public int compare(Request first, Request second) {
            switch (sortFlag){
                case SORT_FLAG_STATUS : {
                    Integer status1 = first.complain_status_id;
                    Integer status2 = second.complain_status_id;
                    return status1.compareTo(status2);
                }case SORT_FLAG_DATE : {
                    try {
                        SimpleDateFormat dateFormatter = new SimpleDateFormat(RequestListAdapter.INPUT_DATE_FORMAT, Locale.US);
                        Date date1 = dateFormatter.parse(first.created_at);
                        Date date2 = dateFormatter.parse(second.created_at);
                        return -(date1.compareTo(date2)); // "-" is for reverse sorting
                    } catch (ParseException e){
                        Globals.showError(R.string.error, e);
                        break;
                    }
                }
            }
            return 0;
        }
    }

    private class RefreshListOfRequestsDialog  extends AlertDialog {

        protected RefreshListOfRequestsDialog(Context context) {
            super(context);
        }

        public Dialog get() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.refresh).
                    setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            swipeRefreshLayout.setRefreshing(false);
                            resetSortMenu();
                            makeRequestListAdapterContent();
                        }
                    }).
                    setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }).
                    setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }).
                    setCancelable(true);
            return builder.create();
        }
    }

    private static class RequestListFetcherActions extends AsyncTaskAction<Void, ArrayList<Request>, RequestListFragment> {
        RequestListFetcherActions(RequestListFragment fragment) {
            super(fragment);
        }

        @Override public void pre(Void arg) {
            RequestListFragment requestListFragment = ref.get();
            if (requestListFragment != null) {
                View progressBar = requestListFragment.progressBar;
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override public void post(ArrayList<Request> requests) {
            RequestListFragment requestListFragment = ref.get();
            if (requestListFragment != null) {
                View progressBar = requestListFragment.progressBar;
                progressBar.setVisibility(View.GONE);

                RequestListAdapter requestListAdapter = requestListFragment.requestListAdapter;
                if (requests != null && requests.size() > 0) {
                    requestListAdapter.getContent().addAll(requests);
                }
                if (requestListAdapter.getItemCount() == 0) { //there are no requests
                    requestListFragment.showNoRequestsLayout();
                } else {
                    Collections.sort(requestListAdapter.content, new RequestComparator(RequestComparator.SORT_FLAG_DATE));
                    requestListAdapter.notifyDataSetChanged();
                }

                Activity activity = requestListFragment.getActivity();
                String requestFromPush = activity == null ?
                        null : activity.getIntent().getStringExtra(MyGcmListenerService.REQUEST_NUMBER);
                if (requestFromPush != null) {
                    activity.getIntent().removeExtra(MyGcmListenerService.REQUEST_NUMBER);
                    int index = requestListAdapter.getRequestNumberFromTag(requestFromPush);
                    if (index > -1) requestListAdapter.openRequest(index);
                }
            }
        }
    }
}




