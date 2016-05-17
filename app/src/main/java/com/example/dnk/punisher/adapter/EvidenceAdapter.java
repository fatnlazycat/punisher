package com.example.dnk.punisher.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.dnk.punisher.CameraManager;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.Violation;

import java.util.ArrayList;

/**
 * Created by Dima on 04.05.2016.
 */
public class EvidenceAdapter extends BaseAdapter {
    public ArrayList<String> content = new ArrayList<>();

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
            convertView=inflater.inflate(R.layout.item_evidence, parent, false);
        }

        String fileName = content.get(position);
        Bitmap thumbnail;
        if (fileName.endsWith(CameraManager.JPG)){
            //the thumbnail should have the same size as MediaStore.Video.Thumbnails.MICRO_KIND
            int d = parent.getResources().getDimensionPixelOffset(R.dimen.thumbnail_size);
            thumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(fileName), d, d);
        } else { //it's video
            thumbnail = ThumbnailUtils.createVideoThumbnail(fileName, MediaStore.Video.Thumbnails.MICRO_KIND);
        }

        ImageView imageViewEvidence = (ImageView)convertView.findViewById(R.id.imageViewEvidence);
        imageViewEvidence.setImageBitmap(thumbnail);

        return convertView;
    }
}
