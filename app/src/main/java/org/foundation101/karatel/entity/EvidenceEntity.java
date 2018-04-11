package org.foundation101.karatel.entity;

import android.graphics.Bitmap;

import java.io.Serializable;

/**
 * Created by Dima on 14.03.2018.
 */

public class EvidenceEntity implements Serializable {
    public String fileName;
    public double latitude, longitude;

    public EvidenceEntity(String fileName, double latitude, double longitude) {
        this.fileName = fileName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
