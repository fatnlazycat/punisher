package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.HttpHelper;
import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.UpdateEntity;
import org.foundation101.karatel.activity.ViolationActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by Dima on 15.06.2016.
 */
public class HistoryAdapter extends BaseAdapter {
    public HistoryAdapter(Context context){
        this.context = context;
        violationStatuses = context.getResources().getStringArray(R.array.violationStatuses);
    }

    public UpdateEntity[] content = new UpdateEntity[0];
    Context context;
    String[] violationStatuses;
    private static final String googleDocsUrl = "http://docs.google.com/viewer?url=";
    public static final String PDF = ".pdf";
    public static final String STATUS_CLOSED = "Закрито";

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
            holder.answerLayout = (RelativeLayout) convertView.findViewById(R.id.answerLayout);
            holder.imageAnswer = (WebView) convertView.findViewById(R.id.imageAnswer);
            holder.ic_zoom = (ImageView) convertView.findViewById(R.id.ic_zoom);
            holder.headerAnswerBy = (TextView)  convertView.findViewById(R.id.headerAnswerBy);
            holder.textAnswerBy = (TextView) convertView.findViewById(R.id.textAnswerBy);
            holder.rateLayout = (LinearLayout) convertView.findViewById(R.id.rateLayout);
            holder.textRate = (TextView) convertView.findViewById(R.id.textRate);
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

        String statusText = "";
        int statusIdOnServer = thisUpdate.complain_status_id;
        if (Globals.statusesMap.containsKey(statusIdOnServer)) {
            int status = Globals.statusesMap.get(statusIdOnServer);
            statusText = context.getResources().getStringArray(R.array.violationStatuses)[status];
            holder.textViewRequestStatus.setText(statusText);
            holder.imageViewStatus.setImageResource(R.drawable.level_list_status);
            holder.imageViewStatus.setImageLevel(status);
        }

        if (thisUpdate.isCollapsed()){
            holder.collapsableLayout.setVisibility(View.GONE);
            holder.textViewDetailsAction.setText(R.string.show_details);
            holder.textViewDetailsAction.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_action_expand, 0);
        } else {
            holder.collapsableLayout.setVisibility(View.VISIBLE);
            holder.textViewDetailsAction.setText(R.string.hide_details);
            holder.textViewDetailsAction.setCompoundDrawablesWithIntrinsicBounds(0,0, R.drawable.ic_action_collapse, 0);
            if (thisUpdate.reply != null && !thisUpdate.reply.isEmpty()) {
                holder.headerOperatorComment.setVisibility(View.VISIBLE);
                holder.operatorComment.setVisibility(View.VISIBLE);
                holder.operatorComment.setText(thisUpdate.reply);
            }

            if (thisUpdate.documents != null && thisUpdate.documents.length > 0) {
                holder.headerAnswer.setVisibility(View.VISIBLE );
                holder.answerLayout.setVisibility(View.VISIBLE );
                holder.imageAnswer.getSettings().setLoadWithOverviewMode(true);
                holder.imageAnswer.getSettings().setUseWideViewPort(true);
                holder.imageAnswer.getSettings().setJavaScriptEnabled(true); //for the pdf viewer
                final String docUrl = getDocUrl(thisUpdate.documents[0]);
                holder.imageAnswer.loadUrl(docUrl);
                holder.imageAnswer.setWebViewClient(new WebViewClient(){
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        //view.pageDown(true);
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
                holder.ic_zoom.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(context, v);
                        int counter = 0;
                        for (UpdateEntity.DocUrl d : thisUpdate.documents) {
                            String label = Uri.parse(d.url).getLastPathSegment();
                            popup.getMenu().add(Menu.NONE, counter++, Menu.NONE, label);
                        }
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                return docsPopup(thisUpdate.documents, item.getItemId());
                            }
                        });
                        popup.show();
                    }
                });

                if (statusText.equals(STATUS_CLOSED)) {
                    holder.rateLayout.setVisibility(View.VISIBLE);
                    switch (((ViolationActivity)context).request.rating) {
                        case -1: {
                            holder.textRate.setText(R.string.your_estimation);
                            holder.buttonDislike.setClickable(false);
                            holder.buttonLike.setVisibility(View.INVISIBLE);
                            break;
                        }
                        case 1: {
                            holder.textRate.setText(R.string.your_estimation);
                            holder.buttonLike.setClickable(false);
                            holder.buttonDislike.setVisibility(View.INVISIBLE);
                            break;
                        }
                        default: { //not rated yet
                            holder.textRate.setText(R.string.estimateGivenAnswer);
                            holder.buttonLike.setOnClickListener(new LikeDislike(thisUpdate.complain_id, true));
                            holder.buttonDislike.setOnClickListener(new LikeDislike(thisUpdate.complain_id, false));
                        }
                    }
                } else {
                    holder.rateLayout.setVisibility(View.GONE);
                }
            } else {
                holder.headerAnswer.setVisibility(View.GONE );
                holder.answerLayout.setVisibility(View.GONE );
                holder.rateLayout.setVisibility(View.GONE);
            }

            if (thisUpdate.name_of_authority != null && !thisUpdate.name_of_authority.isEmpty()) {
                holder.headerAnswerBy.setVisibility(View.VISIBLE);
                holder.textAnswerBy.setVisibility(View.VISIBLE);
                holder.textAnswerBy.setText(thisUpdate.name_of_authority);
            }
        }
        holder.textViewDetailsAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCollapseStatus(thisUpdate);
            }
        });
        return convertView;
    }

    void changeCollapseStatus(UpdateEntity update){
        update.setCollapsed(!update.isCollapsed());
        notifyDataSetChanged();
    }

    String getDocUrl(UpdateEntity.DocUrl arg){
        String suffix = arg.url.endsWith(PDF) ? googleDocsUrl : "";
        final String result = suffix + Globals.SERVER_URL.replace("/api/v1/", "") + arg.url;
        return result;
    }

    public boolean docsPopup(UpdateEntity.DocUrl[] documents, int position){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getDocUrl(documents[position])));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "web-browser not installed", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    public static class ViewHolder{
        ImageView imageViewStatus, ic_zoom;
        WebView imageAnswer;
        ImageButton buttonLike, buttonDislike;
        TextView textViewRequestStatus, textViewRequestTimeStamp, headerOperatorComment, operatorComment,
                headerAnswer, headerAnswerBy, textAnswerBy, textRate, textViewDetailsAction;
        LinearLayout rateLayout;
        RelativeLayout collapsableLayout, answerLayout;
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
                            new LikeDislike(requestId, (rate==1)) //reverse from int to boolean
                                    .execute();
                        }
                    }).create();
            dialog.show();

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ((ViolationActivity)context).progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... params) {
            if (HttpHelper.internetConnected(context)) {
                String request = new HttpHelper("complain").makeRequestString(new String[]{"rating", rate.toString()});
                try {
                    return HttpHelper.proceedRequest("complains/" + requestId, "PUT", request, true);
                } catch (final IOException e) {
                    ((ViolationActivity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Globals.showError(context, R.string.cannot_connect_server, e);
                        }
                    });
                    return "";
                }
            } else return HttpHelper.ERROR_JSON;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            ((ViolationActivity)context).progressBar.setVisibility(View.GONE);
            try {
                JSONObject json = new JSONObject(s);
                switch (json.getString("status")) {
                    case Globals.SERVER_SUCCESS: {
                        ((ViolationActivity)context).request.rating = rate;
                        notifyDataSetChanged();
                        break;
                    }
                    case Globals.SERVER_ERROR: {
                        Toast.makeText(context, json.getString("error"), Toast.LENGTH_LONG).show();
                        break;
                    }
                }
            } catch (JSONException e) {
                Globals.showError(context, R.string.cannot_connect_server, e);
            }
        }
    }

}
