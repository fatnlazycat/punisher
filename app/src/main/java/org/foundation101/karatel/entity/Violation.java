package org.foundation101.karatel.entity;

import android.util.Log;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.utils.AssetsUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Dima on 04.05.2016.
 */
public class Violation implements Serializable{
    public static final String TAG = "Violation.class";
    public static final int VIDEO_ONLY = 1;
    public static final int CATEGORY_BUSINESS = 1;
    public static final int CATEGORY_PUBLIC = 2;

    public static ArrayList<Violation> violationsList;

    public String name;
    public String type;
    public String textInactive, header;
    public int id; //on device
    public int idOnServer;
    public int drawableId;
    public boolean usesCamera, active;
    public boolean locationRequired = true;
    public int mediaTypes, category;
    public boolean wasSendAttempt = false;
    ArrayList<ViolationRequisite> requisites;

    public ArrayList<ViolationRequisite> getRequisites() {
        return requisites;
    }
    public void setRequisites(ArrayList<ViolationRequisite> requisites) {
        this.requisites = requisites;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getTextInactive() {return textInactive; }
    public void setTextInactive(String textInactive) { this.textInactive = textInactive; }

    public int getDrawableId() {
        return drawableId;
    }
    public void setDrawableId(int drawableId) {
        this.drawableId = drawableId;
    }

    public boolean usesCamera() {
        return usesCamera;
    }
    public void setUsesCamera(boolean usesCamera) {
        this.usesCamera = usesCamera;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public int getMediaTypes() {
        return mediaTypes;
    }
    public void setMediaTypes(int mediaTypes) {
        this.mediaTypes = mediaTypes;
    }

    public int getCategory() { return category; }
    public void setCategory(int category) { this.category = category; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public static Violation getByType(String type){
        ArrayList<Violation> list = getViolationsList();
        for (Violation v : list) {
            if (v.getType().equals(type)) return v.clearValues();
        }
        return new Violation();
    }

    public static ArrayList<Violation> getViolationsList(int categoryFilter) {
        ArrayList<Violation> filteredList = new ArrayList<>(getViolationsList());
        for (Violation v : violationsList) {
            if (v.getCategory() != categoryFilter) filteredList.remove(v);
        }
        return filteredList;
    }

    public static ArrayList<Violation> getActiveViolationsList(int categoryFilter) {
        ArrayList<Violation> filteredList = new ArrayList<>(getViolationsList());
        for (Violation v : violationsList) {
            if (v.getCategory() != categoryFilter || !v.isActive()) filteredList.remove(v);
        }
        return filteredList;
    }

    public static ArrayList<Violation> getViolationsList() {
        if (violationsList == null) {
            try {
                String jsonString = readViolationAssets();
                ObjectMapper objectMapper = new ObjectMapper();
                violationsList = objectMapper.readValue(jsonString, new TypeReference<List<Violation>>() {
                });

                String packageName = KaratelApplication.getInstance().getPackageName();
                for (Violation v : violationsList) {
                    String id = "violation_picture_" + v.getType().toLowerCase();
                    v.setDrawableId(KaratelApplication.getInstance().getResources().getIdentifier(id, "mipmap", packageName));
                }
            } catch (IOException | JSONException e) {
                Log.e(TAG, "error parsing violations list", e);
            }
        }

        return violationsList;
    }

    public String[] getRequisitesString() {
        String[] common;
        ArrayList<String> resultingList = new ArrayList<>();

        switch (getCategory()) {
            case Violation.CATEGORY_BUSINESS: {
                common = new String[]{"user_id", "company_id", "longitude", "latitude", "description", "creation_date"};
                resultingList = new ArrayList<>(Arrays.asList(common));
                break;
            }
            case Violation.CATEGORY_PUBLIC: {
                common = new String[]{"user_id", "id_number", "complain_status_id", "longitude", "latitude",
                        "create_in_the_device", "type"};

                resultingList = new ArrayList<>(Arrays.asList(common));
                ArrayList<ViolationRequisite> thisRequisites = getRequisites();
                String type = getType();
                for (ViolationRequisite violationRequisite : thisRequisites) {
                    resultingList.add(violationRequisite.dbTag.replace(type + "_", ""));
                }
                break;
            }
        }

        String[] result = resultingList.toArray(new String[0]);
        return result;
    }

    private static String readViolationAssets() throws IOException, JSONException {
        return new JSONObject(AssetsUtils.readAssets("violations.json")).getJSONArray("violations").toString();
    }

    public Violation clearValues() {
        if (requisites == null) return this;
        for (ViolationRequisite requisite : requisites) {
            requisite.value = null;
        }
        return this;
    }
}
