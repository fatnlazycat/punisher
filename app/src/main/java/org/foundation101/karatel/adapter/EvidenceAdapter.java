package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.activity.ShowMediaActivity;
import org.foundation101.karatel.entity.EvidenceEntity;
import org.foundation101.karatel.utils.Formular;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Dima on 04.05.2016.
 */
public class EvidenceAdapter extends BaseAdapter {
    public static String TAG = "EvidenceAdapter";
    private Context context;
    public ArrayList<EvidenceEntity> content = new ArrayList<>();
    public ArrayList<Bitmap> mediaContent = new ArrayList<>();
    private boolean editTrigger = true;

    public void setEditTrigger(boolean editTrigger) {
        this.editTrigger = editTrigger;
    }

    public EvidenceAdapter(Context context){
        if (context instanceof Formular) this.context = context;
        else throw new ClassCastException("context should implement the Formular interface");
    }

    @Override
    public void notifyDataSetChanged() {
        ((Formular)context).validatePunishButton();
        ((Formular)context).validateSaveButton();
        super.notifyDataSetChanged();
    }

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
        if (mediaContent.size() > position) {//means we have initialized this position in media content
            Bitmap thumbnail = mediaContent.get(position);
            ImageView imageViewEvidence = (ImageView) convertView.findViewById(R.id.imageViewEvidence);
            imageViewEvidence.setBackground(new BitmapDrawable(parent.getResources(), thumbnail));

            ImageButton imageButtonDeleteEvidence = (ImageButton) convertView.findViewById(R.id.imageButtonDeleteEvidence);
            if (editTrigger) {
                imageButtonDeleteEvidence.setBackgroundResource(R.mipmap.ic_delete);
                imageButtonDeleteEvidence.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        content.remove(position);
                        mediaContent.remove(position);
                        EvidenceAdapter.this.notifyDataSetChanged();

                        ((Formular) context).setChangesMade(true);
                    }
                });
            } else {
                imageButtonDeleteEvidence.setVisibility(View.GONE);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //show in Gallery (or whatever else media app)
                    /*Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(content.get(position)), "image/*");
                    if (intent.resolveActivity(context.getPackageManager()) != null) context.startActivity(intent);*/

                    //show in app activity
                    Intent newIntent = new Intent(parent.getContext(), ShowMediaActivity.class);
                    newIntent.putExtra(Globals.MEDIA_FILE, content.get(position).fileName);
                    parent.getContext().startActivity(newIntent);
                }
            });
        }
        return convertView;
    }

    public boolean sizeCheck() throws IOException {
        long count = 0;
        for (EvidenceEntity evidence : content) {
            long fileLength = new File(evidence.fileName).length();
            count += fileLength;
            Log.d(TAG, "fileLength = " + fileLength + ", count = " + count);
        }
        return (count < Globals.MAX_SERVER_REQUEST_SIZE * 0.99); //99% to leave space for other data
    }
}
