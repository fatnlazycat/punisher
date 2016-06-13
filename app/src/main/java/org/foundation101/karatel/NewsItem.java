package org.foundation101.karatel;

import android.graphics.drawable.Drawable;

public class NewsItem{
    public NewsItem(String title, String description, String pubDate, String link, Drawable image){
        this.title = title;
        this.description = description;
        this.pubDate = pubDate;
        this.link = link;
        this.image = image;
    }

    public String title, description, pubDate, link;
    public Drawable image;
}
