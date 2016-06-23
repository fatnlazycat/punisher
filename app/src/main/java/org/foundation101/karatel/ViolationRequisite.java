package org.foundation101.karatel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Dima on 08.05.2016.
 */
public class ViolationRequisite {
    public String dbTag, name, description, hint, value;

    public static String[] getRequisites(String type){
        String[] result = new String[0];
        switch (type){
            case "WrongParking" : {
                result = new String[] {"user_id", "id_number", "complain_status_id",
                        "address", "longitude", "latitude", "vehicle_number"}; break;
            }
            case "PitRoad" : {
                result = new String[] {"user_id", "id_number", "complain_status_id",
                        "address", "longitude", "latitude", "the_closest_landmark", "type"}; break;
            }
            case "NoSmoking" : {
                result = new String[] {"user_id", "id_number", "complain_status_id",
                        "address", "longitude", "latitude", "name_of_institution", "name_of_entity", "type"}; break;
            }
            case "Damage" : {
                result = new String[] {"user_id", "id_number", "complain_status_id",
                        "address", "longitude", "latitude", "the_closest_landmark", "type"}; break;
            }
            case "SaleOfGood" : {
                result = new String[] {"user_id", "id_number", "complain_status_id",
                        "address", "longitude", "latitude", "name_of_authority", "name_of_institution",
                        "name_of_entity", "type"}; break;
            }
            case "Insult" : {
                result = new String[] {"user_id", "id_number", "complain_status_id",
                        "address", "longitude", "latitude", "description", "all_name", "position",
                        "name_of_authority", "name_of_institution", "name_of_entity", "type"}; break;
            }
            case "Bribe" : {
                result = new String[] {"user_id", "id_number", "complain_status_id",
                        "address", "longitude", "latitude", "description", "all_name", "position",
                        "name_of_authority", "type"}; break;
            }
        }
        return result;
    }
}
