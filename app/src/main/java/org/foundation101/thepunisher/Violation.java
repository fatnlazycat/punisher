package org.foundation101.thepunisher;

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


    public static String getViolationNameFromType(Context context, String type){
        String[] violationNames = context.getResources().getStringArray(R.array.violations);
        String[] violationTypes = context.getResources().getStringArray(R.array.violationTypes);
        return violationNames[Arrays.asList(violationTypes).indexOf(type)];
    }

    public String name, type;
    public int drawableId;
    public boolean usesCamera;
}
