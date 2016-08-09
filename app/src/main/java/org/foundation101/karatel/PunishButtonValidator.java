package org.foundation101.karatel;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Adapter;

import org.foundation101.karatel.activity.ViolationActivity;
import org.foundation101.karatel.adapter.RequisitesListAdapter;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Dima on 05.08.2016.
 */
public class PunishButtonValidator implements TextWatcher {
    RequisitesListAdapter adapter;
    int position;//, linesNumber;
    boolean filled;
    public static ArrayList<PunishButtonValidator> pool;

    PunishButtonValidator(RequisitesListAdapter adapter, int position){
        this.adapter = adapter;
        this.position = position;
    }

    public static void init(){
        pool = new ArrayList<>();
    }

    public static PunishButtonValidator getInstance(RequisitesListAdapter adapter, int position) {
        for (int i = pool.size(); i <= position; i++) {
            pool.add(position, new PunishButtonValidator(adapter, position));
        }
        return pool.get(position);
    }

    public static boolean validate(){
        boolean result = false;
        for (PunishButtonValidator one : pool){
            result = one.filled && result;
        }
        return result;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        //filled = !s.toString().isEmpty();
        adapter.content.get(position).value = s.toString();
        //adapter.notifyDataSetChanged();
        ((ViolationActivity)adapter.context).validatePunishButton();
            /*
            content.get(position).value = textView.getText().toString();
            int newLinesNumber = textView.getLineCount();
            if (linesNumber !=  newLinesNumber) {
                linesNumber = newLinesNumber;
                notifyDataSetChanged();
                ((ViolationActivity) context).setFocusOnEditTextInRequisitesAdapter(position);
            }*/

    }

    @Override
    public String toString() {
        return "Position = " + position + " ; filled = " + filled;
    }
}