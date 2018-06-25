package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.ViolationActivity;
import org.foundation101.karatel.entity.UpdateEntity;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.retrofit.RetrofitDownloader;
import org.foundation101.karatel.utils.DrawingUtils;
import org.foundation101.karatel.utils.PDFUtils;
import org.foundation101.karatel.utils.RetrofitUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Dima on 15.06.2016.
 */
public class HistoryAdapter extends BaseAdapter implements View.OnClickListener {
    public static final String TAG = "HistoryAdapter";
    private AlertDialog dialog;

    public HistoryAdapter(Context context){
        this.context = context;
        violationStatuses = context.getResources().getStringArray(R.array.violationStatuses);
        fourSidesGradient = new DrawingUtils().fourSidesGradient();
    }

    private Drawable fourSidesGradient;
    public UpdateEntity[] content = new UpdateEntity[0];
    Context context;
    private String[] violationStatuses;
    private Map<String, WebView> webViews= new ConcurrentHashMap<>();
    private static final String googleDocsUrl = "https://drive.google.com/viewerng/viewer?embedded=true&url=";//"http://docs.google.com/viewer?url=";
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
            holder.imageViewStatus          = convertView.findViewById(R.id.imageViewStatus);
            holder.textViewRequestStatus    = convertView.findViewById(R.id.textViewRequestStatus);
            holder.textViewRequestTimeStamp = convertView.findViewById(R.id.textViewRequestTimeStamp);
            holder.textViewDetailsAction    = convertView.findViewById(R.id.textViewDetailsAction);
            holder.collapsableLayout        = convertView.findViewById(R.id.collapsableLayout);

           //collapsed views
            holder.headerOperatorComment    = convertView.findViewById(R.id.headerOperatorComment);
            holder.operatorComment          = convertView.findViewById(R.id.operatorComment);
            holder.headerAnswer             = convertView.findViewById(R.id.headerAnswer);
            holder.answerLayout             = convertView.findViewById(R.id.answerLayout);
            //holder.imageAnswer            = (WebView)         convertView.findViewById(R.id.imageAnswer);
            holder.flWebView                = convertView.findViewById(R.id.flWebView);
            holder.ic_zoom                  = convertView.findViewById(R.id.ic_zoom);
            holder.tvPressHere              = convertView.findViewById(R.id.tvPressHere);
            holder.headerAnswerBy           = convertView.findViewById(R.id.headerAnswerBy);
            holder.textAnswerBy             = convertView.findViewById(R.id.textAnswerBy);
            holder.rateLayout               = convertView.findViewById(R.id.rateLayout);
            holder.textRate                 = convertView.findViewById(R.id.textRate);
            holder.buttonLike               = convertView.findViewById(R.id.buttonLike);
            holder.buttonDislike            = convertView.findViewById(R.id.buttonDislike);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        //the gradient (two linear gradients inside a compose shader) doesn't work with hardware acceleration
        holder.tvPressHere.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        holder.tvPressHere.setBackground(fourSidesGradient);


        String dateString = Globals.translateDate(RequestListAdapter.INPUT_DATE_FORMAT,
                RequestListAdapter.OUTPUT_DATE_FORMAT, thisUpdate.created_at);
        String formattedDateString = "<b>" + dateString.substring(0, 10) + "</b>"
                + dateString.substring(11, dateString.length());
        holder.textViewRequestTimeStamp.setText(Html.fromHtml(formattedDateString));

        String statusText = "";
        int statusIdOnServer = thisUpdate.complain_status_id;
        if (Globals.statusesMap.containsKey(statusIdOnServer)) {
            int status = Globals.statusesMap.get(statusIdOnServer);
            //statusText = context.getResources().getStringArray(R.array.violationStatuses)[status];
            statusText = violationStatuses[status];
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
                UpdateEntity.DocUrl firstDocument = thisUpdate.documents[0];
                String thisUrl = firstDocument.url;

                if (holder.answerLayout.getVisibility() == View.GONE
                        || (holder.imageAnswer == null)
                        || !(holder.imageAnswer.equals(webViews.get(thisUrl)))) {
                    holder.headerAnswer.setVisibility(View.VISIBLE);
                    holder.answerLayout.setVisibility(View.VISIBLE);

                    WebView wv = new WebView(KaratelApplication.getInstance());
                    holder.imageAnswer = wv;
                    holder.flWebView.addView(wv);

                    holder.imageAnswer.getSettings().setLoadWithOverviewMode(true);
                    holder.imageAnswer.getSettings().setUseWideViewPort(true);
                    holder.imageAnswer.getSettings().setJavaScriptEnabled(true); //for the pdf viewer
                    holder.imageAnswer.setWebViewClient(new WebViewClient() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            Log.d("", "onPageFinished");
                            Object condition = view.getTag(R.id.webViewTagTitleLoaded);
                            if (condition != null && (Boolean)condition) {
                                view.setTag(R.id.webViewTagTitleLoaded, false);
                                super.onPageFinished(view, url);
                            } else {
                                view.loadUrl(url);
                            }
                        }
                    });
                    holder.imageAnswer.setWebChromeClient(new WebChromeClient() {
                        @Override
                        public void onReceivedTitle(WebView view, String title) {
                            view.setTag(R.id.webViewTagTitleLoaded, true);
                            super.onReceivedTitle(view, title);
                        }
                    });

                    //using a map rewrites old non-actual webview with the new actual one so that only one webview for each url
                    webViews.put(thisUrl, holder.imageAnswer);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                            && thisUrl.endsWith(PDF)) {
                        fetchPdf(thisUrl);
                    } else {
                        String docUrl = getDocUrl(firstDocument);
                        holder.imageAnswer.loadUrl(docUrl);
                    }

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
                }

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

                            holder.buttonLike   .setOnClickListener(this);
                            holder.buttonLike   .setTag(thisUpdate.complain_id);
                            holder.buttonDislike.setOnClickListener(this);
                            holder.buttonDislike.setTag(thisUpdate.complain_id);
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

    private void changeCollapseStatus(UpdateEntity update){
        update.setCollapsed(!update.isCollapsed());
        notifyDataSetChanged();
    }

    private String getDocUrl(UpdateEntity.DocUrl arg){
        String suffix = arg.url.endsWith(PDF) ? googleDocsUrl : "";
        final String result = suffix + Globals.SERVER_URL.replace("/api/v1/", "") + arg.url;
        return result;
    }

    private boolean docsPopup(UpdateEntity.DocUrl[] documents, int position){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getDocUrl(documents[position])));
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(KaratelApplication.getInstance(),
                    "web-browser not installed", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private void fetchPdf(final String fileUrl) {
        String[] pathSegments = fileUrl.split("/");
        final String fileName = pathSegments[pathSegments.length - 1];

        final File file = new File(KaratelApplication.getInstance().getCacheDir(), fileName);
        if (file.exists()) webViews.get(fileUrl).loadData(PDFUtils.contentForWebView(file), "text/html", "utf-8");
        else {
            RetrofitDownloader downloader = KaratelApplication.getClient().create(RetrofitDownloader.class);
            Call<ResponseBody> call = downloader.downloadSmallFileWithDynamicUrl(fileUrl);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        boolean writtenToDisk = RetrofitUtils.writeResponseBodyToDisk(response.body(), file);
                        if (writtenToDisk) {
                            webViews.get(fileUrl).loadData(PDFUtils.contentForWebView(file), "text/html", "utf-8");
                        }
                    } else {
                        Log.e(TAG, "response unsuccessfull, status= " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e(TAG, "error", t);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        final boolean like = (v.getId() == R.id.buttonLike);
        final int requestId = (Integer) v.getTag();

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(HistoryAdapter.this.context);
        dialog = dialogBuilder.setMessage(R.string.are_you_sure)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new LikeDislike(requestId, like).execute();
                    }
                }).create();
        dialog.show();
    }

    public void onDestroy() {
        if (dialog != null) dialog.dismiss();
    }

    public static class ViewHolder{
        FrameLayout flWebView;
        ImageView imageViewStatus;
        View ic_zoom;
        WebView imageAnswer;
        ImageButton buttonLike, buttonDislike;
        TextView textViewRequestStatus, textViewRequestTimeStamp, headerOperatorComment, operatorComment,
                headerAnswer, headerAnswerBy, textAnswerBy, textRate, textViewDetailsAction, tvPressHere;
        LinearLayout rateLayout;
        RelativeLayout collapsableLayout, answerLayout;
    }

    class LikeDislike extends AsyncTask<Void, Void, String> {
        int requestId;
        Integer rate;

        LikeDislike(int requestId, boolean like){
            this.requestId = requestId;
            rate = like ? 1 : -1;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ((ViolationActivity)context).progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... params) {
            if (HttpHelper.internetConnected()) {
                String request = new HttpHelper("complain").makeRequestString(new String[]{"rating", rate.toString()});
                try {
                    return HttpHelper.proceedRequest("complains/" + requestId, "PUT", request, true);
                } catch (final IOException e) {
                    Globals.showError(R.string.cannot_connect_server, e);
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
                        Toast.makeText(KaratelApplication.getInstance(),
                                json.getString("error"), Toast.LENGTH_LONG).show();
                        break;
                    }
                }
            } catch (JSONException e) {
                Globals.showError(R.string.cannot_connect_server, e);
            }
        }
    }

}
