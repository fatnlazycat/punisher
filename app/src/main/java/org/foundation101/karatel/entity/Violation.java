package org.foundation101.karatel.entity;

import android.content.Context;
import android.util.Log;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.foundation101.karatel.KaratelApplication;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
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
    public String textInactive;
    public int drawableId;
    public boolean usesCamera, active;
    public int mediaTypes, category;

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

    public static Violation getByType(Context context, String type){
        ArrayList<Violation> list = getViolationsList();
        for (Violation v : list) {
            if (v.getType().equals(type)) return v;
        }
        return new Violation();
    }

    public static ArrayList<Violation> getViolationsList() {
        if (violationsList == null) {
            try {
                InputStream inputStream = KaratelApplication.getInstance().getAssets().open("violations.json");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                String line;
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();

                String jsonString = new JSONObject(stringBuilder.toString()).getJSONArray("violations").toString();
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
}
