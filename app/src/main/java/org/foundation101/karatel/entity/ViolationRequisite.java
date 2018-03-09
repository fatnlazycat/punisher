package org.foundation101.karatel.entity;

import java.io.Serializable;

/**
 * Created by Dima on 08.05.2016.
 */
public class ViolationRequisite implements Serializable {
    public String dbTag, name, description, hint, value;
    public boolean necessary;
    public String[] possibleValues;

    public String[] getPossibleValues() {
        return possibleValues;
    }
    public void setPossibleValues(String[] possibleValues) {
        this.possibleValues = possibleValues;
    }

    public String getDbTag() {
        return dbTag;
    }
    public void setDbTag(String dbTag) {
        this.dbTag = dbTag;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getHint() {
        return hint;
    }
    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    public boolean isNecessary() {
        return necessary;
    }
    public void setNecessary(boolean necessary) {
        this.necessary = necessary;
    }

    /*public static String[] getRequisites(Context context, String type){
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
    }*/
}
