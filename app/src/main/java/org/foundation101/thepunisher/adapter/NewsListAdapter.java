package org.foundation101.thepunisher.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.foundation101.thepunisher.NewsItem;
import org.foundation101.thepunisher.R;

import java.util.ArrayList;

/**
 * Created by Dima on 10.05.2016.
 */
public class NewsListAdapter extends BaseAdapter {

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
        textViewNewsDate.setText(thisNews.pubDate);
        ImageView imageViewNews = (ImageView)convertView.findViewById(R.id.imageViewNews);
        imageViewNews.setImageDrawable(thisNews.image);

        return convertView;
    }


}

