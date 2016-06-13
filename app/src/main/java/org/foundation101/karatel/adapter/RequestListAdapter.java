package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.ItemTouchHelperAdapter;
import org.foundation101.karatel.R;
import org.foundation101.karatel.Request;
import org.foundation101.karatel.activity.ViolationActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Dima on 04.05.2016.
 */
public class RequestListAdapter extends RecyclerView.Adapter<RequestListAdapter.ViewHolder> implements ItemTouchHelperAdapter{
    public ArrayList<Request> content;
    //CursorAdapterForRecyclerView cursorAdapter;
    Context context;
    RecyclerView recycler;
    View progressBar;
    SimpleDateFormat inFormatter, outFormatter;

    public void setProgressBar(View progressBar) {
        this.progressBar = progressBar;
    }

    public ArrayList<Request> getContent() {
        return content;
    }
    public void setContent(ArrayList<Request> content) {
        this.content = content;
    }

    public RequestListAdapter(Context context, View progressBar) {
        this.context = context;
        //cursorAdapter = new CursorAdapterForRecyclerView(context, c, 0, progressBar);
        setProgressBar(progressBar);
        inFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        outFormatter = new SimpleDateFormat("dd MMMMMMMM yyyy", new Locale("uk", "UA"));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Passing the inflater job to the cursor-adapter
        //View v = cursorAdapter.newView(context, cursorAdapter.getCursor(), parent);
        View v =  LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Passing the binding operation to cursor loader
        /*cursorAdapter.getCursor().moveToPosition(position);
        cursorAdapter.bindView(holder.itemView, context, cursorAdapter.getCursor());*/

        Request thisRequest = content.get(position);
        try {
            Date date = inFormatter.parse(thisRequest.created_at);
            String dateString = outFormatter.format(date);
            holder.textViewRequestTimeStamp.setText(dateString);
        } catch (ParseException e) {
            Log.e("Punisher", e.getMessage());
        }
        holder.textViewRequestType.setText(thisRequest.type);
        int status = thisRequest.complain_status_id;
        String statusText = context.getResources().getStringArray(R.array.violationStatuses)[status];
        holder.textViewRequestStatus.setText(statusText);
        holder.imageViewStatus.setImageResource(R.drawable.level_list_status);
        holder.imageViewStatus.setImageLevel(status);
    }

    @Override
    public int getItemCount() {
        return content.size();
    }

    //methods of org.foundation101.karatel.ItemTouchHelperAdapter
    @Override
    public void onItemDismiss(int position) {
        /*cursorAdapter.getCursor().moveToPosition(position);
        Integer idInDb = cursorAdapter.getCursor().getInt(0); //"_id" is the first column in the db
        removeLine(idInDb);*/

        content.remove(position);
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
        Request thisRequest = content.get(requestPosition);
        int id = thisRequest.id;
        int status = thisRequest.complain_status_id;
        Intent intent = new Intent(context, ViolationActivity.class)
                .putExtra(Globals.VIOLATION_ACTIVITY_MODE, status)
                .putExtra(Globals.ITEM_ID, id) //the list items are 1-based, not 0-based
                .putExtra(Globals.REQUEST_JSON, thisRequest);
        context.startActivity(intent);
    }


    /*public void removeLine(Integer idInDb) {
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

    }*/

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRequestStatus, textViewRequestTimeStamp, textViewRequestType;
        ImageView imageViewStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewRequestStatus=(TextView)itemView.findViewById(R.id.textViewRequestStatus);
            textViewRequestTimeStamp=(TextView)itemView.findViewById(R.id.textViewRequestTimeStamp);
            textViewRequestType=(TextView)itemView.findViewById(R.id.textViewRequestType);
            imageViewStatus = (ImageView) itemView.findViewById(R.id.imageViewStatus);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RequestListAdapter.this.openRequest(ViewHolder.this.getAdapterPosition());
                }
            });
        }
    }
}
