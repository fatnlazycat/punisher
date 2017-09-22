package org.foundation101.karatel.entity;

import java.io.Serializable;

/**
 * Created by Dima on 17.06.2016.
 */
public class MediaEntity implements Serializable{
    public String url;
    public MediaEntity.Thumb thumb;

    public static class Thumb implements Serializable{
        public String url;
    }
}
