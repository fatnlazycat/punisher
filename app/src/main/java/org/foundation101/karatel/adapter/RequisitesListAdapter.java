package org.foundation101.karatel.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.ViolationRequisite;
import org.foundation101.karatel.activity.ViolationActivity;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Dima on 08.05.2016.
 */
public class RequisitesListAdapter extends BaseAdapter implements OnMapReadyCallback {

    public RequisitesListAdapter(Context context){
        this.context=context;
        fm = ((ViolationActivity)context).getSupportFragmentManager();
        supportMapFragment =  SupportMapFragment.newInstance();
        addressAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line);
    }

    static final float DEFAULT_ZOOM = 17;//zoom values are floats from 2 to 21
    //tags for save instance state
    static final String MAP_CENTER = "MAP_CENTER";
    static final String MAP_ZOOM = "MAP_ZOOM";
    static final String MAP_HAS_MARKER = "MAP_HAS_MARKER";

    LatLng savedLatLng;
    float savedZoom;
    boolean savedHasMarker;

    private Context context;
    public ArrayList<ViolationRequisite> content;

    boolean editTrigger = true;
    boolean hasMarker = false;

    FragmentManager fm;
    SupportMapFragment supportMapFragment;
    private GoogleMap mMap;
    PlaceLikelihoodBuffer likelyPlaces;
    PendingResult<PlaceLikelihoodBuffer> placeLikelihoodResult;
    ArrayAdapter<PlaceLikelihoodHolder> addressAdapter;
    String addressForEditText = "";

    public void setEditTrigger(boolean editTrigger) {
        this.editTrigger = editTrigger;
    }

    public ArrayList<ViolationRequisite> makeContent(String violationType){
        ArrayList<ViolationRequisite> result = new ArrayList<>();
        String packageName = context.getPackageName();
        Resources res = context.getResources();
        int arrayId = res.getIdentifier(violationType + "Requisites", "array", packageName);
        String[] array = res.getStringArray(arrayId);
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
        if (convertView==null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.item_violation_requisite, parent, false);
        }

        ViolationRequisite thisRequisite = content.get(position);

        TextView textViewRequisiteHeader=(TextView)convertView.findViewById(R.id.textViewRequisiteHeader);
        textViewRequisiteHeader.setText(thisRequisite.name);

        TextView textViewRequisiteDescription=(TextView)convertView.findViewById(R.id.textViewRequisiteDescription);
        if (thisRequisite.description.isEmpty()) {
            textViewRequisiteDescription.setVisibility(View.GONE);
        } else {
            textViewRequisiteDescription.setVisibility(View.VISIBLE);
            textViewRequisiteDescription.setText(thisRequisite.description);
        }

        //work with map & autocomplete textview
        EditText editTextRequisite;
        String requisiteValue;
        if (thisRequisite.dbTag.endsWith("_address")){
            FrameLayout mapContainer = (FrameLayout)convertView.findViewById(R.id.mapContainer);
            fm.beginTransaction().replace(R.id.mapContainer, supportMapFragment).commitAllowingStateLoss();// allowingStateLoss - Fighting with java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
            supportMapFragment.getMapAsync(this);
            mapContainer.setVisibility(View.VISIBLE);

            editTextRequisite = (AutoCompleteTextView)convertView.findViewById(R.id.editTextRequisite);
            ((AutoCompleteTextView)editTextRequisite).setThreshold(0);
            ((AutoCompleteTextView)editTextRequisite).setAdapter(addressAdapter);
            ((AutoCompleteTextView)editTextRequisite).setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (mMap != null) {
                        mMap.clear();
                        PlaceLikelihoodHolder placeHolder= (PlaceLikelihoodHolder) parent.getAdapter().getItem(position);
                        LatLng place = placeHolder.field.getPlace().getLatLng();
                        MarkerOptions marker = new MarkerOptions().position(place);
                        mMap.addMarker(marker);
                        hasMarker = true;
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place, 17));
                    }
                }
            });
            requisiteValue = addressForEditText.isEmpty() ? thisRequisite.value : addressForEditText;
        } else { //it's not autocomplete - ordinary EditText
            editTextRequisite = (EditText) convertView.findViewById(R.id.editTextRequisite);
            requisiteValue = thisRequisite.value;
        }
        if ((requisiteValue == null) || (requisiteValue.isEmpty())){
            editTextRequisite.setHint(thisRequisite.hint);
        } else {
            editTextRequisite.setText(requisiteValue);
        }
        editTextRequisite.setEnabled(editTrigger);
        editTextRequisite.setFocusable(editTrigger);
        if (editTrigger) {
            editTextRequisite.addTextChangedListener(new MyTextWatcher(editTextRequisite, position));
            editTextRequisite.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        ((TextView) v).setText(((TextView) v).getText().toString());
                        ((EditText) v).setSelection(((EditText) v).getText().length());
                    }
                }
            });
        }
        return convertView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        float zoom = DEFAULT_ZOOM;
        double latitude = 0;
        double longitude = 0;
        if (((ViolationActivity) context).latitude == null){
            Location l = ((ViolationActivity) context).getOldAndroidLocation();
            if (l == null) {
                ((ViolationActivity) context).blockButtons(true);
            } else {
                latitude = l.getLatitude();
                longitude = l.getLongitude();
                ((ViolationActivity) context).setLatitudeAndLongitude(l);
                ((ViolationActivity) context).blockButtons(false);
            }
        } else {
            latitude = ((ViolationActivity) context).latitude;
            longitude = ((ViolationActivity) context).longitude;
            if (((ViolationActivity) context).getMode() == ViolationActivity.MODE_CREATE)
                ((ViolationActivity) context).blockButtons(false);
        }
        LatLng here = new LatLng(latitude, longitude);
        /*if (hasMarker){
            mMap.clear();
        }*/

        if (((ViolationActivity) context).getMode() > ViolationActivity.MODE_EDIT){
            MarkerOptions markerOptions = new MarkerOptions().position(here);
            Marker marker = mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, DEFAULT_ZOOM));
            hasMarker = true;
        } else {
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    mMap.clear();
                    MarkerOptions marker = new MarkerOptions().position(new LatLng(latLng.latitude, latLng.longitude));
                    mMap.addMarker(marker);
                    hasMarker = true;

                    //reverse geocoding
                    Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
                    try {
                        List<Address> addresses = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        if (addresses.size() > 0) {
                            StringBuilder sb = new StringBuilder("");
                            for (int i=0; i < addresses.get(0).getMaxAddressLineIndex(); i++){
                                sb.append(addresses.get(0).getAddressLine(i) + " ");
                            }
                            addressForEditText = sb.toString();
                            notifyDataSetChanged();
                        }
                    } catch (IOException e) {
                        Globals.showError(context, "can't find address", e);
                    }
                }
            });
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    mMap.clear();
                    hasMarker = false;
                }
            });
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    if (mMap.getCameraPosition().target.latitude > 0) {
                        savedLatLng = mMap.getCameraPosition().target;
                        savedZoom = mMap.getCameraPosition().zoom;
                        savedHasMarker = hasMarker;
                    }
                }
            });
        }
        if (savedLatLng != null) {
            here = savedLatLng;
            zoom = savedZoom;
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, zoom));

        if (placeLikelihoodResult == null
                && ((ViolationActivity) context).googleApiClient != null) {
            placeLikelihoodResult = Places.PlaceDetectionApi.getCurrentPlace(((ViolationActivity) context).googleApiClient, null);
            placeLikelihoodResult.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(PlaceLikelihoodBuffer placesResult) {
                    likelyPlaces = placesResult;
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        boolean uniqueValue = true;
                        PlaceLikelihoodHolder holder = new PlaceLikelihoodHolder(placeLikelihood);
                        for (int i = 0; i < addressAdapter.getCount(); i++){
                            if (holder.toString().equals(addressAdapter.getItem(i).toString())){
                                uniqueValue = false;
                                break;
                            }
                        }
                        if (uniqueValue) addressAdapter.add(holder);
                    }
                    addressAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void releaseMap(){
        if (likelyPlaces != null) likelyPlaces.release();
    }

    public void nullSavedLatLng(){
        savedLatLng = null;
    }

    /*public void getMapDataFromBundle(Bundle savedState) {
        if (mMap != null){
            LatLng latLng = savedState.getParcelable(MAP_CENTER);
            float zoom = savedState.getFloat(MAP_ZOOM);
            boolean marker = savedState.getBoolean(MAP_HAS_MARKER);
            if (latLng != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
                if (marker) mMap.addMarker(new MarkerOptions().position(latLng));
            }
        }
    }

    public void putMapDataToBundle(Bundle outState) {
        if (mMap != null){
            outState.putParcelable(MAP_CENTER, mMap.getCameraPosition().target);
            outState.putFloat(MAP_ZOOM, mMap.getCameraPosition().zoom);
            outState.putBoolean(MAP_HAS_MARKER, hasMarker);
        }
    }*/

    class MyTextWatcher implements TextWatcher{
        TextView textView;
        int position, linesNumber;

        MyTextWatcher(TextView view, int position){
            textView = view;
            this.position = position;
            linesNumber = textView.getLineCount();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            content.get(position).value = textView.getText().toString();
            addressForEditText = "";
            int newLinesNumber = textView.getLineCount();
            if (linesNumber !=  newLinesNumber) {
                linesNumber = newLinesNumber;
                notifyDataSetChanged();
                ((ViolationActivity) context).setFocusOnEditTextInRequisitesAdapter(position);
            }
            ((ViolationActivity) context).validatePunishButton();
        }
    }

    class PlaceLikelihoodHolder{
        PlaceLikelihood field;
        PlaceLikelihoodHolder(PlaceLikelihood field){
            this.field = field;
        }

        @Override
        public String toString() {
            return (field != null) ? field.getPlace().getAddress().toString() : super.toString();
        }
    }
}
