package org.foundation101.karatel;

import java.io.Serializable;

/**
 * Created by Dima on 07.06.2016.
 */
public class Request implements Serializable{
    public String type;
    public int id, user_id, complain_status_id;
    public String id_number, address, description, vehicle_number, the_closest_landmark, all_name, position,
            name_of_authority, name_of_institution, name_of_entity, product_name, name_producer, created_at, updated_at;
    public double latitude, longitude;
    public String[] media_files;
}