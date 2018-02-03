package org.foundation101.karatel.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.foundation101.karatel.entity.Violation;
import org.foundation101.karatel.entity.ViolationRequisite;

import java.util.ArrayList;

/**
 * Created by Dima on 08.05.2016.
 */
public class DBHelper extends SQLiteOpenHelper {

    //look at this - maybe we don't need it?
    Context context;

    public static final int DB_VERSION = 2;
    public static final String DATABASE = "violations_db";
    public static final String VIOLATIONS_TABLE = "violations_table";
    public static final String MEDIA_TABLE = "media_table";
    public static final String COMPLAINS_TABLE = "complains_table";
    public static final String COMPLAINS_MEDIA_TABLE = "complains_media_table";

    //column names
    public static final String _ID = "_id";
    public static final String ID = "id";
    public static final String ID_SERVER = "id_server";
    public static final String ID_NUMBER_SERVER = "id_number";
    public static final String USER_ID = "user_id";
    public static final String TYPE = "type";
    public static final String STATUS = "status";
    public static final String TIME_STAMP = "time_stamp";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String FILE_NAME = "file_name";

    public static final int DB_TAG_STEP = 5;

    public DBHelper(Context context, String name, int version) {
        super(context, name, null, version);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        /*Resources res = context.getResources();
        String[] violations = res.getStringArray(org.foundation101.karatel.R.array.violationTypes);
        for (String violation : violations){
            int arrayId = res.getIdentifier(violation + "Requisites", "array", context.getPackageName());
            String[] requisites = res.getStringArray(arrayId);
            for (int i = 0; i < requisites.length; i += DB_TAG_STEP){
                sb.append(requisites[i] + " TEXT,");
            }
        }*/

        String dataTableStructure = getTableStructure(Violation.CATEGORY_PUBLIC);
        db.execSQL("CREATE TABLE "
                + VIOLATIONS_TABLE
                + " ("
                + _ID               + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ID_SERVER         + " INTEGER,"
                + ID_NUMBER_SERVER  + " TEXT,"
                + USER_ID           + " INTEGER,"
                + TYPE              + " TEXT,"
                + STATUS            + " INTEGER,"
                + TIME_STAMP        + " TEXT,"
                + LONGITUDE         + " REAL,"
                + LATITUDE          + " REAL,"
                + dataTableStructure
                + ");");
        db.execSQL("CREATE TABLE "
                + MEDIA_TABLE
                + " ("
                + _ID               + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ID                + " INTEGER,"
                + FILE_NAME         + " TEXT"
                + ");");

        createTablesForComplains(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createTablesForComplains(db);
    }

    private void createTablesForComplains(SQLiteDatabase db) {
        String dataTableStructure = getTableStructure(Violation.CATEGORY_BUSINESS);
        db.execSQL("CREATE TABLE "
                + COMPLAINS_TABLE
                + " ("
                + _ID           + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + USER_ID       + " INTEGER,"
                + TYPE          + " TEXT,"
                + TIME_STAMP    + " TEXT,"
                + LONGITUDE     + " REAL,"
                + LATITUDE      + " REAL,"
                + dataTableStructure
                + ");");
        db.execSQL("CREATE TABLE "
                + COMPLAINS_MEDIA_TABLE
                + " ("
                + _ID           + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ID            + " INTEGER,"
                + FILE_NAME     + " TEXT"
                + ");");
    }

    private String getTableStructure(int category) {
        StringBuilder sb = new StringBuilder();
        ArrayList<Violation> violations = Violation.getViolationsList(category);
        for (Violation v : violations) {
            for (ViolationRequisite violationRequisite : v.getRequisites()) {
                sb.append(violationRequisite.dbTag + " TEXT,");
            }
        }

        return new String(sb).substring(0,sb.length()-1); //remove trailing ','
    }

    public static ContentValues getRowAsContentValues(Cursor c) {
        c.moveToFirst();
        ContentValues result = new ContentValues();
        String[] columnNames = c.getColumnNames();
        for (int i = 0; i < columnNames.length; i++) {
            switch (c.getType(i)) {
                case Cursor.FIELD_TYPE_STRING: {
                    result.put(columnNames[i], c.getString(i));
                    break;
                }
                case Cursor.FIELD_TYPE_INTEGER: {
                    result.put(columnNames[i], c.getInt(i));
                    break;
                }
                case Cursor.FIELD_TYPE_FLOAT: {
                    result.put(columnNames[i], c.getFloat(i));
                    break;
                }
                default: {
                    try { //assume it's double since there's no such type in getType method
                        result.put(columnNames[i], c.getDouble(i));
                        break;
                    } catch (Exception e) {
                        Log.e("Punisher", "parse double exception" + e.getMessage());
                    }
                }
            }
        }
        return result;
    }

    public static void deleteViolationRequest(SQLiteDatabase db, Integer id){
        String idString = id.toString();
        db.delete(DBHelper.VIOLATIONS_TABLE, "_id = ?", new String[]{idString});
        db.delete(DBHelper.MEDIA_TABLE, "id = ?", new String[]{idString});
    }

    public static void deleteComplainRequest(SQLiteDatabase db, Integer id){
        String idString = id.toString();
        db.delete(DBHelper.COMPLAINS_TABLE, "_id = ?", new String[]{idString});
        db.delete(DBHelper.COMPLAINS_MEDIA_TABLE, "id = ?", new String[]{idString});
    }
}
