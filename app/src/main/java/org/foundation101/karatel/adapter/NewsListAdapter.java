package org.foundation101.karatel.adapter;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.NewsItem;
import org.foundation101.karatel.R;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Dima on 10.05.2016.
 */
public class NewsListAdapter extends BaseAdapter {
    public static final String INPUT_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss Z";
    public static final String OUTPUT_DATE_FORMAT = "dd MMMM yyyy";

    public NewsListAdapter(ArrayList<NewsItem> content){
        this.content = content;
    }

    ArrayList<NewsItem> content = new ArrayList<>();

    @Override
    public int getCount() {
        return content.size();
    }

    @Override
    public Object getItem(int position) {
        return content.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (convertView==null){
            convertView=inflater.inflate(R.layout.item_news, parent, false);
        }

        NewsItem thisNews = content.get(position);
        TextView textViewNewsHeader = (TextView)convertView.findViewById(R.id.textViewNewsHeader);
        textViewNewsHeader.setText(thisNews.title);
        TextView textViewNewsDate = (TextView)convertView.findViewById(R.id.textViewNewsDate);

        String dateString = Globals.translateDate(INPUT_DATE_FORMAT, OUTPUT_DATE_FORMAT, thisNews.pubDate);
        textViewNewsDate.setText(dateString);

        final ImageView imageViewNews = (ImageView)convertView.findViewById(R.id.imageViewNews);
        final FrameLayout progressBar = (FrameLayout) convertView.findViewById(R.id.frameLayoutProgress);
        imageViewNews.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        Picasso.with(parent.getContext())
                .load(thisNews.imageLink)
                .fit().into(imageViewNews, new Callback() {
                    @Override
                    public void onSuccess() {
                        imageViewNews.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
                    @Override
                    public void onError() {
                        imageViewNews.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    }
        });

        /*URLConnection urlConnection = new URL(imageLink).openConnection();
        InputStream is = urlConnection.getInputStream();
        Drawable image = Drawable.createFromStream(is, "newsImage");
        is.close();
        imageViewNews.setBackground(thisNews.image);*/

        return convertView;
    }


}

