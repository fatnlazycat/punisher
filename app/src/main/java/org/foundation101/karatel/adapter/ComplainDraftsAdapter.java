package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.ComplainActivity;
import org.foundation101.karatel.entity.ComplainRequest;
import org.foundation101.karatel.entity.Violation;

import java.util.ArrayList;

/**
 * Created by Dima on 04.05.2016.
 */
public class ComplainDraftsAdapter extends RecyclerView.Adapter<ComplainDraftsAdapter.ViewHolder> implements ItemTouchHelperAdapter{
    public static final String INPUT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ";
    public static final String OUTPUT_DATE_FORMAT = "dd.MM.yyyy, HH:mm";

    public ArrayList<ComplainRequest> content = new ArrayList<>();
    private Context context;
    private RecyclerView recycler;

    public ComplainDraftsAdapter(Context context) {
        this.context = context;
    }

    public ArrayList<ComplainRequest> getContent() {
        return content;
    }
    public void setContent(ArrayList<ComplainRequest> content) {
        this.content = content;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =  LayoutInflater.from(parent.getContext()).inflate(R.layout.item_complains_draft, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ComplainRequest thisRequest = content.get(position);

        String dateString = Globals.translateDate(INPUT_DATE_FORMAT, OUTPUT_DATE_FORMAT, thisRequest.creation_date);
        String formattedDateString = "<b>" + dateString.substring(0, 10) + "</b>"
                + dateString.substring(11, dateString.length());
        holder.textViewComplainTimeStamp.setText(Html.fromHtml(formattedDateString));

        holder.tvComplainType.setText(Violation.getByType(thisRequest.type).getName());

        holder.tvComplainName.setText(thisRequest.brief);
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

    public void openRequest(int requestPosition){
        ComplainRequest thisRequest = content.get(requestPosition);
        int id = thisRequest.id;
        Intent intent = new Intent(context, ComplainActivity.class)
                .putExtra(Globals.VIOLATION_ACTIVITY_MODE, ComplainActivity.MODE_EDIT)
                .putExtra(Globals.ITEM_ID, id) //the list items are 1-based, not 0-based
                .putExtra(Globals.REQUEST_JSON, thisRequest);
        context.startActivity(intent);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvComplainType, tvComplainName, textViewComplainTimeStamp;

        public ViewHolder(View itemView) {
            super(itemView);
            tvComplainName              = itemView.findViewById(R.id.tvComplainName);
            textViewComplainTimeStamp   = itemView.findViewById(R.id.tvComplainTimeStamp);
            tvComplainType              = itemView.findViewById(R.id.tvComplainType);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ComplainDraftsAdapter.this.openRequest(ViewHolder.this.getAdapterPosition());
                }
            });
        }
    }
}
