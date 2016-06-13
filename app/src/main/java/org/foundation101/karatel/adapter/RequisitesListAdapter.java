package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.foundation101.karatel.R;
import org.foundation101.karatel.ViolationRequisite;
import org.foundation101.karatel.activity.ViolationActivity;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * Created by Dima on 08.05.2016.
 */
public class RequisitesListAdapter extends BaseAdapter implements OnMapReadyCallback {

    public RequisitesListAdapter(Context context){
        this.context=context;
        fm = ((ViolationActivity)context).getSupportFragmentManager();
        supportMapFragment =  SupportMapFragment.newInstance();
        //ArrayList<String> addressList = new ArrayList<>();
        addressAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line);
    }

    private Context context;
    public ArrayList<ViolationRequisite> content;

    boolean editTrigger = true;

    FragmentManager fm;
    SupportMapFragment supportMapFragment;
    private GoogleMap mMap;
    ArrayAdapter<String> addressAdapter;

    public void setEditTrigger(boolean editTrigger) {
        this.editTrigger = editTrigger;
    }

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
        if (convertView==null){
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            convertView=inflater.inflate(R.layout.item_violation_requisite, parent, false);

            ViolationRequisite thisRequisite = content.get(position);

            TextView textViewRequisiteHeader=(TextView)convertView.findViewById(R.id.textViewRequisiteHeader);
            textViewRequisiteHeader.setText(thisRequisite.name);

            TextView textViewRequisiteDescription=(TextView)convertView.findViewById(R.id.textViewRequisiteDescription);
            textViewRequisiteDescription.setText(thisRequisite.description);

            //work with map & autocomplete textview
            EditText editTextRequisite;
            if (thisRequisite.dbTag.endsWith("_address")){
                fm.beginTransaction().replace(R.id.mapContainer, supportMapFragment).commit();
                supportMapFragment.getMapAsync(this);
                FrameLayout mapContainer = (FrameLayout)convertView.findViewById(R.id.mapContainer);
                mapContainer.setVisibility(View.VISIBLE);

                editTextRequisite = (AutoCompleteTextView)convertView.findViewById(R.id.editTextRequisite);
                ((AutoCompleteTextView)editTextRequisite).setThreshold(0);
                ((AutoCompleteTextView)editTextRequisite).setAdapter(addressAdapter);
            } else { //it's not autocomplete - ordinary EditText
                editTextRequisite = (EditText) convertView.findViewById(R.id.editTextRequisite);
            }
            String requisiteValue = thisRequisite.value;
            if ((requisiteValue==null) || (requisiteValue.isEmpty())) editTextRequisite.setHint(thisRequisite.hint);
                else editTextRequisite.setText(thisRequisite.value);
            editTextRequisite.setEnabled(editTrigger);
            editTextRequisite.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    ((ViolationActivity)context).validatePunishButton();
                }
            });
        }
        return convertView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        /*DecimalFormat formatter = new DecimalFormat("#0.0000");
        DecimalFormatSymbols dfs = formatter.getDecimalFormatSymbols();
        dfs.setDecimalSeparator('.');
        formatter.setDecimalFormatSymbols(dfs);

        Double latitude = Double.valueOf(formatter.format(((ViolationActivity)context).latitude));
        Double longitude = Double.valueOf(formatter.format(((ViolationActivity)context).longitude));*/
        Double latitude = ((ViolationActivity)context).latitude;
        Double longitude = ((ViolationActivity)context).longitude;
        LatLng here = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(here));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 15)); //zoom values are floats from 2 to 21

        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                .getCurrentPlace(((ViolationActivity)context).googleApiClient, null);
        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    String address = placeLikelihood.getPlace().getAddress().toString();
                    addressAdapter.add(address);
                }
                addressAdapter.notifyDataSetChanged();
                likelyPlaces.release();
            }
        });
    }
}
