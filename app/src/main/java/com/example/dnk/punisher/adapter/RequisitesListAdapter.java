package com.example.dnk.punisher.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.example.dnk.punisher.R;
import com.example.dnk.punisher.ViolationRequisite;

import java.util.ArrayList;

/**
 * Created by Dima on 08.05.2016.
 */
public class RequisitesListAdapter extends BaseAdapter {

    public RequisitesListAdapter(Context context){
        this.context=context;
    }

    private Context context;
    public ArrayList<ViolationRequisite> content;

    public ArrayList<ViolationRequisite> makeContent(String violationType){
        ArrayList<ViolationRequisite> result = new ArrayList<>();
        String packageName = context.getPackageName();
        Resources res = context.getResources();
        int arrayId = res.getIdentifier(violationType + "Requisites", "array", packageName);
        String[] array = context.getResources().getStringArray(arrayId);
        int i = 0;
        while (i < array.length){
            ViolationRequisite requisite = new ViolationRequisite();
            requisite.dbTag = array[i++];
            requisite.name = array[i++];
            requisite.description = array[i++];
            requisite.hint = array[i++];
            result.add(requisite);
        }
        return result;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (convertView==null){
            convertView=inflater.inflate(R.layout.item_violation_requisite, parent, false);
        }

        ViolationRequisite thisRequisite = content.get(position);

        TextView textViewRequisiteHeader=(TextView)convertView.findViewById(R.id.textViewRequisiteHeader);
        textViewRequisiteHeader.setText(thisRequisite.name);

        TextView textViewRequisiteDescription=(TextView)convertView.findViewById(R.id.textViewRequisiteDescription);
        textViewRequisiteDescription.setText(thisRequisite.description);

        EditText editTextRequisite=(EditText)convertView.findViewById(R.id.editTextRequisite);
        String requisiteValue = thisRequisite.value;
        if ((requisiteValue==null) || (requisiteValue.isEmpty())) editTextRequisite.setHint(thisRequisite.hint);
            else editTextRequisite.setText(thisRequisite.value);
        return convertView;
    }
}
