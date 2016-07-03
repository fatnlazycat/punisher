package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.R;
import org.foundation101.karatel.Request;
import org.foundation101.karatel.UpdateEntity;
import org.foundation101.karatel.activity.ViolationActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Created by Dima on 15.06.2016.
 */
public class HistoryAdapter extends BaseAdapter {
    public HistoryAdapter(Context context){
        this.context = context;
        request = ((ViolationActivity)context).request;
        violationStatuses = context.getResources().getStringArray(R.array.violationStatuses);
    }

    Request request;
    public UpdateEntity[] content = new UpdateEntity[0];
    Context context;
    String[] violationStatuses;
    private static final String googleDocsUrl = "http://docs.google.com/viewer?url=";

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
            holder.headerOperatorComment = (TextView) convertView.findViewById(R.id.headerOperatorComment);
            holder.operatorComment = (TextView) convertView.findViewById(R.id.operatorComment);
            holder.headerAnswer = (TextView)  convertView.findViewById(R.id.headerAnswer);
            holder.imageAnswer = (WebView) convertView.findViewById(R.id.imageAnswer);
            holder.headerAnswerBy = (TextView)  convertView.findViewById(R.id.headerAnswerBy);
            holder.textAnswerBy = (TextView) convertView.findViewById(R.id.textAnswerBy);
            holder.rateLayout = (LinearLayout) convertView.findViewById(R.id.rateLayout);
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
        holder.textViewRequestTimeStamp.setText(Html.fromHtml(formattedDateString));

        int statusIdOnServer = thisUpdate.complain_status_id;
        if (Globals.statusesMap.containsKey(statusIdOnServer)) {
            int status = Globals.statusesMap.get(statusIdOnServer);
            String statusText = context.getResources().getStringArray(R.array.violationStatuses)[status];
            holder.textViewRequestStatus.setText(statusText);
            holder.imageViewStatus.setImageResource(R.drawable.level_list_status);
            holder.imageViewStatus.setImageLevel(status);
        }

        if (thisUpdate.isCollapsed()){
            holder.textViewDetailsAction.setText(R.string.show_details);
            holder.textViewDetailsAction.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_action_expand, 0);
        } else {
            holder.textViewDetailsAction.setText(R.string.hide_details);
            holder.textViewDetailsAction.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_action_collapse, 0);
            if (!thisUpdate.reply.isEmpty()) {
                holder.headerOperatorComment.setVisibility(View.VISIBLE);
                holder.operatorComment.setVisibility(View.VISIBLE);
                holder.operatorComment.setText(thisUpdate.reply);
            }

            if (thisUpdate.documents.length > 0) {
                holder.headerAnswer.setVisibility(View.VISIBLE );
                holder.imageAnswer.setVisibility(View.VISIBLE );
                holder.imageAnswer.loadUrl(googleDocsUrl
                        + Globals.SERVER_URL.replace("/api/v1/", "") + thisUpdate.documents[0].url);
                holder.imageAnswer.setWebViewClient(new WebViewClient(){
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        view.scrollTo(0, view.getContentHeight());
                    }
                });
                int progress = holder.imageAnswer.getProgress();
                /*if (progress >= 100) {
                    //use the param "view", and call getContentHeight in scrollTo
                    float scale = holder.imageAnswer.getScale();
                    int height = holder.imageAnswer.getContentHeight();
                    int yScroll = Math.round(height * scale / 20);
                    holder.imageAnswer.scrollTo(0, 500);
                }*/

                holder.rateLayout.setVisibility(View.VISIBLE);
                switch (request.rating){
                    case -1 : {
                        holder.buttonDislike.setEnabled(false);
                        holder.buttonLike.setVisibility(View.INVISIBLE);
                    }
                    case 1 : {
                        holder.buttonLike.setEnabled(false);
                        holder.buttonDislike.setVisibility(View.INVISIBLE);
                    }
                    default: { //not rated yet
                        holder.buttonLike.setOnClickListener(new LikeDislike(thisUpdate.complain_id, true));
                        holder.buttonDislike.setOnClickListener(new LikeDislike(thisUpdate.complain_id, false));
                    }
                }
            }

            if (!thisUpdate.name_of_authority.isEmpty()) {
                holder.headerAnswerBy.setVisibility(View.VISIBLE);
                holder.textAnswerBy.setVisibility(View.VISIBLE);
                holder.textAnswerBy.setText(thisUpdate.name_of_authority);
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
        ImageView imageViewStatus;
        WebView imageAnswer;
        ImageButton buttonLike, buttonDislike;
        TextView textViewRequestStatus, textViewRequestTimeStamp, headerOperatorComment, operatorComment,
                headerAnswer, headerAnswerBy, textAnswerBy, textViewDetailsAction;
        LinearLayout rateLayout;
        RelativeLayout collapsableLayout;
    }

    class LikeDislike extends AsyncTask<Void, Void, String> implements View.OnClickListener{
        int requestId;
        Integer rate;
        LikeDislike(int requestId, boolean like){
            this.requestId = requestId;
            rate = like ? 1 : -1;
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
        protected String doInBackground(Void... params) {
            String request = new HttpHelper("complain").makeRequestString(new String[]{"rating", rate.toString()});
            try {
                return HttpHelper.proceedRequest("complains/" + requestId, "PUT", request, true);
            } catch (final IOException e) {
                ((ViolationActivity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Globals.showError(context, R.string.cannot_connect_server, e);
                    }
                });
                return "";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals(Globals.SERVER_SUCCESS)){
                    request.rating = rate;
                    notifyDataSetChanged();
                }
            } catch (JSONException e) {
                Globals.showError(context, R.string.cannot_connect_server, e);
            }
        }
    }

}
