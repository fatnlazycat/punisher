package org.foundation101.karatel.utils;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.foundation101.karatel.KaratelApplication;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Dima on 07.06.2017.
 */

public class MapUtils {
    private static final String TAG = "MapUtils";
    private static final String CANT_FIND_ADDRESS = "can't find address";
    private static boolean running = false;
    public static void fetchAddressInto(final LatLng latLng, final EditText addressEditText){
        if (running) return;

        new AsyncTask<Void, Void, String>(){
            @Override
            protected void onPreExecute(){
                running = true;
            }

            @Override
            protected String doInBackground(Void... params){
                String result;
                if (Geocoder.isPresent()){
                    Geocoder geocoder = new Geocoder(KaratelApplication.getInstance(), Locale.getDefault());
                    //reverse geocoding
                    try {
                        List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        if (addresses.size() > 0) {
                            StringBuilder sb = new StringBuilder("");
                            for (int i=0; i <= addresses.get(0).getMaxAddressLineIndex(); i++){
                                sb.append(addresses.get(0).getAddressLine(i) + " ");
                            }
                            result = sb.toString();
                        } else {
                            result = fetchAddressUsingGoogleMap();
                        }
                    } catch (IOException e) {// Geocoder failed
                        Log.d(TAG, CANT_FIND_ADDRESS, e);
                        result = fetchAddressUsingGoogleMap();
                    }
                } else { // Geocoder failed
                    result = fetchAddressUsingGoogleMap();
                }
                return result;
            }

            // Geocoder failed, Plan B: Google Map
            private String fetchAddressUsingGoogleMap() {
                String result = CANT_FIND_ADDRESS;
                String googleMapUrl = "http://maps.googleapis.com/maps/api/geocode/json?latlng="
                        + latLng.latitude + "," + latLng.longitude + "&sensor=false";
                try {
                    String jsonAddresses = Jsoup.connect(googleMapUrl).ignoreContentType(true).execute().body();
                    JSONObject googleMapResponse = new JSONObject(jsonAddresses);

                    JSONArray results = (JSONArray) googleMapResponse.get("results");
                    if (results.length() > 0) {
                        JSONObject firstAddress = results.getJSONObject(0);
                            result = firstAddress.optString("formatted_address", CANT_FIND_ADDRESS);
                    }
                } catch (Exception ignored) {
                    Log.d(TAG, "fetchAddressUsingGoogleMap", ignored);
                }
                return result;
            }

            protected void onPostExecute(String result) {
                running = false;
                if (result.equals(CANT_FIND_ADDRESS)) {
                    Toast.makeText(KaratelApplication.getInstance(), result, Toast.LENGTH_LONG).show();
                } else {
                    if (addressEditText != null) addressEditText.setText(result);
                }
            }
        }.execute();
    }
}
