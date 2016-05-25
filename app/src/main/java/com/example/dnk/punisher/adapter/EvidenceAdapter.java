package com.example.dnk.punisher.adapter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.dnk.punisher.CameraManager;
import com.example.dnk.punisher.Globals;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.activity.ShowMediaActivity;

import java.util.ArrayList;

/**
 * Created by Dima on 04.05.2016.
 */
public class EvidenceAdapter extends BaseAdapter {
    public ArrayList<String> content = new ArrayList<>();
    public ArrayList<String> filesDeletedDuringSession = new ArrayList<>();

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
    public View getView(final int position, View convertView, final ViewGroup parent) {
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
        imageViewEvidence.setBackground(new BitmapDrawable(parent.getResources(), thumbnail));
        //imageViewEvidence.setImageBitmap(thumbnail);

        ImageButton imageButtonDeleteEvidence = (ImageButton)convertView.findViewById(R.id.imageButtonDeleteEvidence);
        imageButtonDeleteEvidence.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                filesDeletedDuringSession.add(content.get(position));
                content.remove(position);
                EvidenceAdapter.this.notifyDataSetChanged();
            }
        });

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent newIntent = new Intent(parent.getContext(), ShowMediaActivity.class);
                newIntent.putExtra(Globals.MEDIA_FILE, content.get(position));
                parent.getContext().startActivity(newIntent);
            }
        });

        return convertView;
    }
}
