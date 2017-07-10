package org.foundation101.karatel;

import android.content.Context;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Dima on 04.05.2016.
 */
public class Violation implements Serializable{
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public static String getViolationNameFromType(Context context, String type){
        String[] violationNames = context.getResources().getStringArray(R.array.violations);
        String[] violationTypes = context.getResources().getStringArray(R.array.violationTypes);
        return violationNames[Arrays.asList(violationTypes).indexOf(type)];
    }

    public static int getMediaTypesFromType(Context context, String type){
        int[] violationMediaTypes = context.getResources().getIntArray(R.array.violationMediaTypes);
        String[] violationTypes = context.getResources().getStringArray(R.array.violationTypes);
        return violationMediaTypes[Arrays.asList(violationTypes).indexOf(type)];
    }

    public String name, type;
    public int drawableId;
    public boolean usesCamera;
    public int mediaTypes;

    public static final int VIDEO_ONLY = 1;
}
