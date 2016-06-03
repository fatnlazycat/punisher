package org.foundation101.thepunisher.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.foundation101.thepunisher.DBHelper;
import org.foundation101.thepunisher.Globals;
import org.foundation101.thepunisher.ItemTouchHelperAdapter;
import org.foundation101.thepunisher.activity.ViolationActivity;

/**
 * Created by Dima on 04.05.2016.
 */
public class RequestListAdapter extends RecyclerView.Adapter<RequestListAdapter.ViewHolder> implements ItemTouchHelperAdapter{
    //public static ArrayList<Violation> content;
    CursorAdapterForRecyclerView cursorAdapter;
    Context context;
    RecyclerView recycler;
    View progressBar;

    public void setProgressBar(View progressBar) {
        this.progressBar = progressBar;
    }

    public RequestListAdapter(Context context, Cursor c, View progressBar) {
        this.context = context;
        cursorAdapter = new CursorAdapterForRecyclerView(context, c, 0, progressBar);
        setProgressBar(progressBar);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Passing the inflater job to the cursor-adapter
        View v = cursorAdapter.newView(context, cursorAdapter.getCursor(), parent);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Passing the binding operation to cursor loader
        cursorAdapter.getCursor().moveToPosition(position);
        cursorAdapter.bindView(holder.itemView, context, cursorAdapter.getCursor());
    }

    @Override
    public int getItemCount() {
        return cursorAdapter.getCount();
    }

    //methods of org.foundation101.thepunisher.ItemTouchHelperAdapter
    @Override
    public void onItemDismiss(int position) {
        cursorAdapter.getCursor().moveToPosition(position);
        Integer idInDb = cursorAdapter.getCursor().getInt(0); //"_id" is the first column in the db
        removeLine(idInDb);
        notifyItemRemoved(position);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
        } else {
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recycler = recyclerView;
    }

    void openRequest(int requestPosition){
        Cursor cursor = cursorAdapter.getCursor();
        cursor.moveToPosition(requestPosition);
        int id = cursor.getInt(cursor.getColumnIndex(DBHelper._ID));
        int status = cursor.getInt(cursor.getColumnIndex(DBHelper.STATUS));
        Intent intent = new Intent(context, ViolationActivity.class)
                .putExtra(Globals.VIOLATION_ACTIVITY_MODE, status)
                .putExtra(Globals.ITEM_ID, id); //the list items are 1-based, not 0-based
        context.startActivity(intent);
    }


    public void removeLine(Integer idInDb) {
        //delete the row
        String idString = idInDb.toString();
        SQLiteDatabase db = new DBHelper(context, "violations_db", 1).getWritableDatabase();
        db.delete("violations_table", "_id = ?", new String[]{idString});
        db.delete("media_table", "id = ?", new String[]{idString});
        //create new cursor
        db = new DBHelper(context, "violations_db", 1).getReadableDatabase();
        String table = "violations_table";
        String[] columns = {"_id", "type", "time_stamp"};
        String where = "user_id=?";
        String[] selectionArgs = {Globals.user.id.toString()};
        Cursor newCursor = db.query(table, columns, where, selectionArgs, null, null, null);

        //assign it to the adapter
        cursorAdapter.getCursor().close();
        cursorAdapter = new CursorAdapterForRecyclerView(context, newCursor, 0, progressBar);

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        //TextView textViewRequestStatus, textViewRequestTimeStamp, textViewRequestType;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RequestListAdapter.this.openRequest(ViewHolder.this.getAdapterPosition());
                }
            });
            /*textViewRequestStatus = (TextView)itemView.findViewById(R.id.textViewRequestStatus);
            textViewRequestTimeStamp=(TextView)itemView.findViewById(R.id.textViewRequestTimeStamp);
            textViewRequestType=(TextView)itemView.findViewById(R.id.textViewRequestType);*/
        }
    }
}
