package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.Request;
import org.foundation101.karatel.entity.Violation;
import org.foundation101.karatel.activity.ViolationActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Dima on 04.05.2016.
 */
public class RequestListAdapter extends RecyclerView.Adapter<RequestListAdapter.ViewHolder> implements ItemTouchHelperAdapter{
    public static final String INPUT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ";
    public static final String OUTPUT_DATE_FORMAT = "dd.MM.yyyy, HH:mm";

    public ArrayList<Request> content = new ArrayList<>();
    String[] violationStatuses;
    Context context;
    RecyclerView recycler;

    public ArrayList<Request> getContent() {
        return content;
    }
    public void setContent(ArrayList<Request> content) {
        this.content = content;
    }

    public RequestListAdapter(Context context) {
        this.context = context;
        violationStatuses = context.getResources().getStringArray(R.array.violationStatuses);
        new StatusListFetcher().execute();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =  LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Request thisRequest = content.get(position);

        String dateString = Globals.translateDate(INPUT_DATE_FORMAT, OUTPUT_DATE_FORMAT, thisRequest.created_at);
        String formattedDateString = "<b>" + dateString.substring(0, 10) + "</b>"
                + dateString.substring(11, dateString.length());
        holder.textViewRequestTimeStamp.setText(Html.fromHtml(formattedDateString));

        holder.textViewRequestType.setText(Violation.getByType(thisRequest.type).getName());

        int statusIdOnServer = thisRequest.complain_status_id;
        if (Globals.statusesMap.containsKey(statusIdOnServer)) {
            int status = Globals.statusesMap.get(statusIdOnServer);
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

    public void openRequest(int requestPosition){
        Request thisRequest = content.get(requestPosition);
        int status = thisRequest.complain_status_id;
        if (status <= ViolationActivity.MODE_EDIT || HttpHelper.internetConnected(/*context*/)) {
            int id = thisRequest.id;
            Intent intent = new Intent(context, ViolationActivity.class)
                    .putExtra(Globals.VIOLATION_ACTIVITY_MODE, status)
                    .putExtra(Globals.ITEM_ID, id) //the list items are 1-based, not 0-based
                    .putExtra(Globals.REQUEST_JSON, thisRequest);
            context.startActivity(intent);
        } else {
            Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
        }
    }

    public int getRequestNumberFromTag(String tag){
        for (int i=0; i < content.size(); i++){
            String thisRequestTag = content.get(i).id_number;
            if (thisRequestTag != null && thisRequestTag.equals(tag)) return i;
        }
        return -1;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewRequestStatus, textViewRequestTimeStamp, textViewRequestType;
        ImageView imageViewStatus;

        public ViewHolder(View itemView) {
            super(itemView);
            textViewRequestStatus=(TextView)itemView.findViewById(R.id.textViewRequestStatus);
            textViewRequestTimeStamp=(TextView)itemView.findViewById(R.id.tvRequestTimeStamp);
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

    private class StatusListFetcher extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                return HttpHelper.proceedRequest("complain_statuses", "GET", "", false);
            } catch (final IOException e) {
                Globals.showError(R.string.cannot_connect_server, e);
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Globals.statusesMap.put(0, 0); //for the draft requests
            try {
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals("success")){
                    JSONArray dataJSON = json.getJSONArray("data");
                    for (int i = 0; i < dataJSON.length(); i++) {
                        JSONObject oneStatus = dataJSON.getJSONObject(i);
                        int statusIdOnServer = oneStatus.getInt("id");
                        String statusName = oneStatus.getString("title");
                        int statusIndex = Arrays.asList(violationStatuses).indexOf(statusName);
                        Globals.statusesMap.put(statusIdOnServer, statusIndex);
                    }
                notifyDataSetChanged();
                }
            } catch (JSONException e) {
                Log.e("Punisher", e.getMessage()); //no need for Globals#showError here
            }
        }
    }
}
