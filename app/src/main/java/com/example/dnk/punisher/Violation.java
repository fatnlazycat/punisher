package com.example.dnk.punisher;

import java.io.Serializable;

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

    public String name, type;
    public int drawableId;
    public boolean usesCamera;
}
