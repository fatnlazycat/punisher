package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.Request;
import org.foundation101.karatel.UpdateEntity;
import org.foundation101.karatel.Violation;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Dima on 15.06.2016.
 */
public class HistoryAdapter extends BaseAdapter {
    public HistoryAdapter(Context context){
        this.context = context;
    }

    public UpdateEntity[] content = new UpdateEntity[0];
    Context context;

    public UpdateEntity[] getContent() {
        return content;
    }

    public void setContent(UpdateEntity[] newContent) {
        this.content = newContent;
    }

    @Override
    public int getCount() {
        return content.length;
    }

    @Override
    public Object getItem(int position) {
        return content[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final UpdateEntity thisUpdate = content[position];
        if (convertView==null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.item_history, parent, false);

            holder = new ViewHolder();
            holder.imageViewStatus = (ImageView) convertView.findViewById(R.id.imageViewStatus);
            holder.textViewRequestStatus = (TextView) convertView.findViewById(R.id.textViewRequestStatus);
            holder.textViewRequestTimeStamp = (TextView) convertView.findViewById(R.id.textViewRequestTimeStamp);
            holder.textViewDetailsAction = (TextView) convertView.findViewById(R.id.textViewDetailsAction);
            holder.collapsableLayout = (RelativeLayout) convertView.findViewById(R.id.collapsableLayout);

           //collapsed views
            holder.operatorComment = (TextView) convertView.findViewById(R.id.operatorComment);
            holder.imageAnswer = (ImageView) convertView.findViewById(R.id.imageAnswer);
            holder.textAnswerBy = (TextView) convertView.findViewById(R.id.textAnswerBy);
            holder.buttonLike = (ImageButton) convertView.findViewById(R.id.buttonLike);
            holder.buttonDislike = (ImageButton) convertView.findViewById(R.id.buttonDislike);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String dateString = Globals.translateDate(RequestListAdapter.INPUT_DATE_FORMAT,
                RequestListAdapter.OUTPUT_DATE_FORMAT, thisUpdate.created_at);
        String formattedDateString = "<b>" + dateString.substring(0, 10) + "</b>"
                + dateString.substring(11, dateString.length());
        holder.textViewRequestTimeStamp.setText(formattedDateString);

        int status = thisUpdate.complain_status_id;
        String statusText = context.getResources().getStringArray(R.array.violationStatuses)[status];
        holder.textViewRequestStatus.setText(statusText);
        holder.imageViewStatus.setImageResource(R.drawable.level_list_status);
        holder.imageViewStatus.setImageLevel(status);

        if (thisUpdate.isCollapsed()){
            holder.textViewDetailsAction.setText(R.string.show_details);
            holder.textViewDetailsAction.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_action_expand, 0);
        } else {
            holder.textViewDetailsAction.setText(R.string.hide_details);
            holder.textViewDetailsAction.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_action_collapse, 0);
            holder.operatorComment.setText(thisUpdate.reply);
            if (thisUpdate.documents.length > 0)
                holder.imageAnswer.setImageURI(Uri.parse(Globals.SERVER_URL + thisUpdate.documents[0]));
            holder.textAnswerBy.setText(thisUpdate.name_of_authority);

            if (!thisUpdate.isRated()) {
                holder.buttonLike.setOnClickListener(new LikeDislike(thisUpdate.complain_id, true));
                holder.buttonDislike.setOnClickListener(new LikeDislike(thisUpdate.complain_id, false));
            } else {
                holder.buttonLike.setEnabled(false);
                holder.buttonDislike.setEnabled(false);
            }
        }
        holder.textViewDetailsAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCollapseStatus(thisUpdate);
                holder.collapsableLayout.setVisibility(thisUpdate.isCollapsed() ? View.GONE : View.VISIBLE);
            }
        });
        return convertView;
    }

    void changeCollapseStatus(UpdateEntity update){
        update.setCollapsed(!update.isCollapsed());
        notifyDataSetChanged();
    }

    public static class ViewHolder{
        ImageView imageViewStatus, imageAnswer;
        ImageButton buttonLike, buttonDislike;
        TextView textViewRequestStatus, textViewRequestTimeStamp, headerOperatorComment, operatorComment,
                headerAnswer, headerAnswerBy, textAnswerBy, textViewDetailsAction;
        RelativeLayout collapsableLayout;
    }

    class LikeDislike extends AsyncTask<Void, Void, Void> implements View.OnClickListener{
        int requestId;
        boolean like;
        LikeDislike(int requestId, boolean like){
            this.requestId = requestId;
            this.like = like;
        }

        @Override
        public void onClick(View v) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(HistoryAdapter.this.context);
            AlertDialog dialog = dialogBuilder.setMessage(R.string.are_you_sure)
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            LikeDislike.this.execute();

                        }
                    }).create();
            dialog.show();

        }

        @Override
        protected Void doInBackground(Void... params) {
            return null;//send like-dislike
        }
    }
}
