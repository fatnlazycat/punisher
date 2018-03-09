package org.foundation101.karatel.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;
import org.foundation101.karatel.adapter.NewsListAdapter;
import org.foundation101.karatel.entity.NewsItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class NewsFragment extends Fragment implements AdapterView.OnItemClickListener{
    static final String TAG = "News";

    ArrayList<NewsItem> newsListContent;
    NewsListAdapter newsListAdapter;
    FrameLayout progressBar;
    private static final String rssURL = "https://www.foundation101.org/rss.xml";

    public NewsFragment(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Google Analytics part
        ((KaratelApplication)getActivity().getApplication()).sendScreenName(TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_news, container, false);

        progressBar = (FrameLayout) v.findViewById(R.id.frameLayoutProgressNews);

        newsListContent = new ArrayList<>();
        newsListAdapter = new NewsListAdapter(newsListContent);
        ListView listViewNews = (ListView)v.findViewById(R.id.listViewNews);
        listViewNews.setAdapter(newsListAdapter);
        listViewNews.setOnItemClickListener(this);
        try {
            AsyncDocFetcher docFetcher = new AsyncDocFetcher();
            docFetcher.execute(rssURL);

        } catch (Exception e){
            Globals.showError(R.string.cannot_connect_server, e);
        }
        return v;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        //opens news in browser
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(newsListContent.get(position).link));
        if (browserIntent.resolveActivity(getActivity().getPackageManager()) != null) startActivity(browserIntent);

        //opens news in app activity
        /*Intent intent = new Intent(getActivity(), NewsActivity.class);
        intent.putExtra(Globals.NEWS_ITEM, newsListContent.get(position).link);
        intent.putExtra(Globals.NEWS_TITLE, newsListContent.get(position).title);
        startActivity(intent);*/
    }

    class AsyncDocFetcher extends AsyncTask<String, NewsItem, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(String... params) {
            if (HttpHelper.internetConnected(/*getActivity()*/)) {
                try {
                    Document doc = Jsoup.connect(params[0]).get();
                    Elements news = doc.select("item");
                    for (Element el : news) {
                        String title = el.getElementsByTag("title").first().text();
                        String description = el.getElementsByTag("description").first().text();
                        String pubDate = el.getElementsByTag("pubDate").first().text();
                        String link = el.getElementsByTag("link").first().text();
                        String imageLink = el.getElementsByTag("enclosure").first().attr("url");

                    /*this gets the image from inside the link. But the image exists in RSS, so we don't use this approach
                    //but saved it as a Jsoup tutorial
                    Document docToGetImage = Jsoup.connect(link).get();
                    String imageLink = docToGetImage.select("div.preview_img > img").first().attr("src");*/

                        publishProgress(new NewsItem(title, description, pubDate, link, imageLink));
                    }
                } catch (final IOException e){
                    Globals.showError(R.string.cannot_connect_server, e);
                }
            } else {
                Globals.showMessage(R.string.no_internet_connection);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(NewsItem... values) {
            super.onProgressUpdate(values);
            newsListContent.add(values[0]);
            newsListAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            newsListAdapter.notifyDataSetChanged();
            progressBar.setVisibility(View.GONE);
        }
    }
}