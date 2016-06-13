package org.foundation101.karatel.fragment;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.NewsActivity;
import org.foundation101.karatel.adapter.NewsListAdapter;
import org.foundation101.karatel.NewsItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class NewsFragment extends Fragment implements AdapterView.OnItemClickListener{

    ArrayList<NewsItem> newsListContent;
    NewsListAdapter newsListAdapter;
    FrameLayout progressBar;
    Document docToGetImage;
    private static final String rssURL = "https://www.foundation101.org/rss.xml";

    public NewsFragment(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_news, container, false);

        progressBar = (FrameLayout) v.findViewById(R.id.frameLayoutProgress);

        newsListContent = new ArrayList<>();
        newsListAdapter = new NewsListAdapter(newsListContent);
        ListView listViewNews = (ListView)v.findViewById(R.id.listViewNews);
        listViewNews.setAdapter(newsListAdapter);
        listViewNews.setOnItemClickListener(this);
        try {
            AsyncDocFetcher docFetcher = new AsyncDocFetcher();
            docFetcher.execute(rssURL);

        } catch (Exception e){
            Log.e("punisher.NewsFragment", e.toString());
        }
        return v;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), NewsActivity.class);
        intent.putExtra(Globals.NEWS_ITEM, newsListContent.get(position).link);
        intent.putExtra(Globals.NEWS_TITLE, newsListContent.get(position).title);
        startActivity(intent);
    }


    class AsyncDrawableFetcher extends AsyncTask<Integer, Void, Void>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Integer... params) {
            Drawable image;
            try {
                NewsItem newsItem = newsListContent.get(params[0]);
                Document docToGetImage = Jsoup.connect(newsItem.link).get();
                String imageLink = docToGetImage.select("div.preview_img > img").first().attr("src");
                URLConnection urlConnection = new URL(imageLink).openConnection();
                InputStream is = urlConnection.getInputStream();
                image = Drawable.createFromStream(is, "newsImage");
                is.close();
                newsListContent.get(params[0]).image = image;
            } catch (IOException e){
                Log.e("punisher.AsyncDrawable", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            newsListAdapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
        }
    }

    class AsyncDocFetcher extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                Document doc = Jsoup.connect(params[0]).get();
                Elements news = doc.select("item");
                for (Element el : news) {
                    String title = el.getElementsByTag("title").first().text();
                    String description = el.getElementsByTag("description").first().text();
                    String pubDate = el.getElementsByTag("pubDate").first().text();
                    String link = el.getElementsByTag("link").first().text();

                    Document docToGetImage = Jsoup.connect(link).get();
                    String imageLink = docToGetImage.select("div.preview_img > img").first().attr("src");
                    URLConnection urlConnection = new URL(imageLink).openConnection();
                    InputStream is = urlConnection.getInputStream();
                    Drawable image = Drawable.createFromStream(is, "newsImage");
                    is.close();

                    newsListContent.add(new NewsItem(title, description, pubDate, link, image));
                }
            } catch (IOException e){
                Log.e("punisher.AsyncDocFetche", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            newsListAdapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
        }
    }
}