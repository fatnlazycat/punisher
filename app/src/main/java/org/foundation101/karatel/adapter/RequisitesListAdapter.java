package org.foundation101.karatel.adapter;

import android.content.Context;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.foundation101.karatel.PunishButtonValidator;
import org.foundation101.karatel.activity.ViolationActivity;
import org.foundation101.karatel.entity.ViolationRequisite;
import org.foundation101.karatel.utils.MapUtils;

import java.util.ArrayList;

/**
 * Created by Dima on 08.05.2016.
 */
public class RequisitesListAdapter implements OnMapReadyCallback {

    public RequisitesListAdapter(Context context){
        this.context=context;
        addressAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line);

        /*MapView mapView = new MapView(context, new GoogleMapOptions().liteMode(true));
        mapView.getMapAsync(this);*/

        //PunishButtonValidator.init();
    }

    public static final float DEFAULT_ZOOM = 17;//zoom values are floats from 2 to 21
    //tags for save instance state
    private static final String MAP_CENTER = "MAP_CENTER";
    private static final String MAP_ZOOM = "MAP_ZOOM";
    private static final String MAP_MARKER = "MAP_MARKER";

    public ArrayList<ViewHolder> holders = new ArrayList<>();

    private LatLng savedLatLng;
    private float savedZoom;
    //private boolean savedHasMarker;

    public Context context;
    public ArrayList<ViolationRequisite> content;

    public boolean editTrigger = true;
    private LatLng markerLatLng = null;

    public EditText addressEditText;
    private GoogleMap mMap;
    private PlaceLikelihoodBuffer likelyPlaces;
    public ArrayAdapter<PlaceLikelihoodHolder> addressAdapter;

    public void setEditTrigger(boolean editTrigger) {
        this.editTrigger = editTrigger;
    }

    public int getCount() {
        return content.size();
    }

    public Object getItem(int position) {
        return content.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        float zoom = DEFAULT_ZOOM;

        LatLng here = ((ViolationActivity) context).getLocationForMap();

        if (((ViolationActivity) context).getMode() > ViolationActivity.MODE_EDIT){
            markerLatLng = here;
            /*MarkerOptions markerOptions = new MarkerOptions().position(here);
            Marker marker = mMap.addMarker(markerOptions);
            hasMarker = true;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, DEFAULT_ZOOM));*/
        } else {
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    setMarker(new LatLng(latLng.latitude, latLng.longitude));
                    MapUtils.fetchAddressInto(latLng, addressEditText);
                }
            });
            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    mMap.clear();
                    markerLatLng = null;
                }
            });
            mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    if (cameraPosition.target.latitude > 0) {
                        savedLatLng = cameraPosition.target;
                        savedZoom = cameraPosition.zoom;
                    }
                }
            });

            if (savedLatLng != null) {
                here = savedLatLng;
                zoom = savedZoom;
            }
        }

        updateMap(here, markerLatLng, zoom);

        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    public void onAddressesReady(PlaceLikelihoodBuffer placesResult) {
        addressAdapter.clear();
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

    public void reclaimMap(){
        if (mMap != null) onMapReady(mMap);
    }

    public void releaseMap(){
        if (likelyPlaces != null) likelyPlaces.release();
    }

    public void nullSavedLatLng(){
        savedLatLng = null;
    }

    public void getMapDataFromBundle(Bundle savedState) {
        savedLatLng = savedState.getParcelable(MAP_CENTER);
        savedZoom = savedState.getFloat(MAP_ZOOM);
        markerLatLng = savedState.getParcelable(MAP_MARKER);
    }

    public void putMapDataToBundle(Bundle outState) {
        if (mMap != null){
            outState.putParcelable(MAP_CENTER, mMap.getCameraPosition().target);
            outState.putFloat(MAP_ZOOM, mMap.getCameraPosition().zoom);
            outState.putParcelable(MAP_MARKER, markerLatLng);
        }
    }

    public void updateMap(LatLng latLng, LatLng markerLatLng, float zoom) {
        if (mMap != null) {
            if (markerLatLng != null) setMarker(markerLatLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        }
    }

    public void setMarker(LatLng latLng) {
        if (mMap != null) {
            mMap.clear();
            MarkerOptions marker = new MarkerOptions().position(latLng);
            mMap.addMarker(marker);
            markerLatLng = latLng;
        }
    }

    public class PlaceLikelihoodHolder{
        public PlaceLikelihood field;
        PlaceLikelihoodHolder(PlaceLikelihood field){
            this.field = field;
        }

        @Override
        public String toString() {
            return (field != null) ? field.getPlace().getAddress().toString() : super.toString();
        }
    }

    public static class ViewHolder{
        public TextView textViewRequisiteHeader, textViewRequisiteDescription;
        public AutoCompleteTextView editTextRequisite;
        public FrameLayout mapContainer;
    }
}
