package org.foundation101.thepunisher.activity;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.foundation101.thepunisher.R;
import org.foundation101.thepunisher.adapter.NewsListAdapter;
import org.foundation101.thepunisher.NewsItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class NewsActivity extends Activity {

    ArrayList<NewsItem> newsListContent;
    NewsListAdapter newsListAdapter;
    FrameLayout progressBar;
    Document docToGetImage;
    private static final String rssURL = "https://www.foundation101.org/rss.xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        progressBar = (FrameLayout) findViewById(R.id.frameLayoutProgress);

        newsListContent = new ArrayList<>();
        newsListAdapter = new NewsListAdapter(newsListContent);
        ListView listViewNews = (ListView)findViewById(R.id.listViewNews);
        listViewNews.setAdapter(newsListAdapter);

        try {
            AsyncDocFetcher docFetcher = new AsyncDocFetcher();
            docFetcher.execute(rssURL);

        } catch (Exception e){
            Log.e("punisher.NewsActivity", e.toString());
        }

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
            //progressBar.setVisibility(View.VISIBLE);
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
        }
    }
}