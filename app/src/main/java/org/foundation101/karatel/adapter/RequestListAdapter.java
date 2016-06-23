package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.Request;
import org.foundation101.karatel.Violation;
import org.foundation101.karatel.activity.ViolationActivity;

import java.util.ArrayList;

/**
 * Created by Dima on 04.05.2016.
 */
public class RequestListAdapter extends RecyclerView.Adapter<RequestListAdapter.ViewHolder> implements ItemTouchHelperAdapter{
    public static final String INPUT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ";
    public static final String OUTPUT_DATE_FORMAT = "dd.MM.yyyy, HH:mm";

    public ArrayList<Request> content;
    String[] violationStatuses;
    Context context;
    RecyclerView recycler;
    View progressBar;

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
        setProgressBar(progressBar);
        violationStatuses = context.getResources().getStringArray(R.array.violationStatuses);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v =  LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Request thisRequest = content.get(position);

        String dateString = Globals.translateDate(INPUT_DATE_FORMAT, OUTPUT_DATE_FORMAT, thisRequest.created_at);
        String formattedDateString = "<b>" + dateString.substring(0, 10) + "</b>"
                + dateString.substring(11, dateString.length());
        holder.textViewRequestTimeStamp.setText(Html.fromHtml(formattedDateString));

        holder.textViewRequestType.setText(Violation.getViolationNameFromType(context, thisRequest.type));

        int status = thisRequest.complain_status_id;
        if (status <= violationStatuses.length) {
            String statusText = violationStatuses[status];
            holder.textViewRequestStatus.setText(statusText);
            holder.imageViewStatus.setImageResource(R.drawable.level_list_status);
            holder.imageViewStatus.setImageLevel(status);
        }
    }

    @Override
    public int getItemCount() {
        return content.size();
    }

    //methods of org.foundation101.karatel.adapter.ItemTouchHelperAdapter
    @Override
    public void onItemDismiss(int position) {
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
