package org.foundation101.karatel;

import android.content.Context;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Dima on 08.05.2016.
 */
public class ViolationRequisite {
    public String dbTag, name, description, hint, value;
    public boolean necessary;

    public static String[] getRequisites(Context context, String type){
        String[] common = new String[] {"user_id", "id_number", "complain_status_id", "longitude", "latitude",
                "create_in_the_device", "type"};
        ArrayList<String> resultingList = new ArrayList<>(Arrays.asList(common));

        String packageName = context.getPackageName();
        Resources res = context.getResources();
        int arrayId = res.getIdentifier(type + "Requisites", "array", packageName);
        String[] array = res.getStringArray(arrayId);

        for (int i=0; i < array.length; i += DBHelper.DB_TAG_STEP){
            String dbTag = array[i].replace(type + "_", "");
            resultingList.add(dbTag);
        }
        String[] result = resultingList.toArray(new String[0]);
        return result;
    }
}
