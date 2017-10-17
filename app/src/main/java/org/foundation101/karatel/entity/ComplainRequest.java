package org.foundation101.karatel.entity;

import java.io.Serializable;

/**
 * Created by Dima on 19.08.2017.
 */

public class ComplainRequest implements Serializable {
    public String type, brief;
    public int id, company_id, user_id;
    public String address, description, position, created_at, updated_at;
    public double latitude, longitude, address_lat, address_lon;
    public MediaEntity[] images, videos;
}
