package org.foundation101.karatel.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.foundation101.karatel.NewsItem;
import org.foundation101.karatel.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Dima on 10.05.2016.
 */
public class NewsListAdapter extends BaseAdapter {
    SimpleDateFormat inFormatter, outFormatter;

    public NewsListAdapter(ArrayList<NewsItem> content){
        this.content = content;
        inFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        outFormatter = new SimpleDateFormat("dd MMMMMMMM yyyy", new Locale("uk", "UA"));
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

        try {
            /*Date d = new Date();
            String s = d.toString();
            String s2  = inFormatter.format(d);*/
            Date date = inFormatter.parse(thisNews.pubDate);
            String dateString = outFormatter.format(date);
            textViewNewsDate.setText(dateString);
        } catch (Exception e) {
            Log.e("Punisher", e.getMessage());
        }
        ImageView imageViewNews = (ImageView)convertView.findViewById(R.id.imageViewNews);
        imageViewNews.setImageDrawable(thisNews.image);

        return convertView;
    }


}

