package org.foundation101.karatel.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.splunk.mint.Mint;

import org.foundation101.karatel.entity.Violation;
import org.foundation101.karatel.entity.ViolationRequisite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Dima on 08.05.2016.
 */
public class DBHelper extends SQLiteOpenHelper {
    public static final int DB_VERSION = 6;
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
    public static final String SEND_ATTEMPT = "send_attempt";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String FILE_NAME = "file_name";

    public DBHelper(Context context, String name, int version) {
        super(context, name, null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTablesForViolations(db);
        createTablesForComplains(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1 : {
                createTablesForComplains(db);
                upgrade_locationFromMainTableToMediaTable(db, Violation.CATEGORY_PUBLIC);
                break;
            }
            case 2 : {
                upgrade_locationFromMainTableToMediaTable(db, Violation.CATEGORY_BUSINESS);
                upgrade_locationFromMainTableToMediaTable(db, Violation.CATEGORY_PUBLIC);
                break;
            }
            case 3 : //v.4 was changes in column names ("Kyivblagoustriy") - no special method here, everything's done by checkViolationsAndAddColumns()
            case 4 : break; //v.5 added sendAttempt which should be common irrespective of old version
            case 5 : {  //v.6 is a fix of the error in v.5 when upgrade_addSendAttemptColumn()
                        // wasn't called when upgrading from v.1 or v.2
                clearBadRecords(db);
                break;
            }
        }

        //these two should stay here forever
        checkViolationsAndAddColumns(db, Violation.CATEGORY_BUSINESS);
        checkViolationsAndAddColumns(db, Violation.CATEGORY_PUBLIC);

        //this one is relevant for upgrading from 1 to 4, on 5 it will throw caught (it's ok) exception "duplicate column name"
        upgrade_addSendAttemptColumn(db);
    }

    private void createTablesForViolations(SQLiteDatabase db) {
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
                + SEND_ATTEMPT      + " INTEGER DEFAULT 0," //in fact we use it as boolean (sqlite has no boolean)
                /*+ LONGITUDE         + " REAL,"
                + LATITUDE          + " REAL,"*/
                + dataTableStructure
                + ");");
        db.execSQL("CREATE TABLE "
                + MEDIA_TABLE
                + " ("
                + _ID               + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ID                + " INTEGER,"
                + FILE_NAME         + " TEXT,"
                + LONGITUDE         + " REAL,"
                + LATITUDE          + " REAL"
                + ");");
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
                /*+ LONGITUDE     + " REAL,"
                + LATITUDE      + " REAL,"*/
                + dataTableStructure
                + ");");
        db.execSQL("CREATE TABLE "
                + COMPLAINS_MEDIA_TABLE
                + " ("
                + _ID           + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ID            + " INTEGER,"
                + FILE_NAME     + " TEXT,"
                + LONGITUDE     + " REAL,"
                + LATITUDE      + " REAL"
                + ");");
    }

    private void upgrade_locationFromMainTableToMediaTable(SQLiteDatabase db, int category) {
        String[] tableNames = tableNamesFromCategory(category);

        db.beginTransaction();
        try {
            db.execSQL("CREATE TEMPORARY TABLE tmp("
                    + _ID + ", " + ID + ", " + FILE_NAME + ", " + LATITUDE + ", " + LONGITUDE + ");");

            db.execSQL("INSERT INTO tmp SELECT m." + _ID + ", m." + ID + ", m." + FILE_NAME + ", v." + LATITUDE + ", v." + LONGITUDE +
                    " FROM " + tableNames[1] + " AS m INNER JOIN " + tableNames[0] + " AS v ON m." + ID + " = v." + _ID + ";");

            db.execSQL("DROP TABLE " + tableNames[1] + ";");

            db.execSQL("CREATE TABLE " + tableNames[1] + " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + ID + " INTEGER, "
                    + FILE_NAME + " TEXT, " +
                    LATITUDE + " REAL DEFAULT 0, " + LONGITUDE + " REAL DEFAULT 0);");

            db.execSQL("INSERT INTO " + tableNames[1] + " SELECT " + _ID + ", " + ID + ", "
                    + FILE_NAME + ", " + LATITUDE + ", " + LONGITUDE + " FROM tmp;");

            db.execSQL("DROP TABLE tmp;");

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DBHelper", e.toString());
            Mint.logException(e);
        } finally {
            db.endTransaction();
        }
        //we don't drop lat/lng column in violation table - let it stay there empty
    }

    /**When upgrading from 1 or 2 to 5 there was a situation where SEND_ATTEMPT column wasn't created
     * (due to break; in switch statement). The users created records that were stored in db the following way:
     * - records in media table had column "id" = -1 (because of error while saving)
     * - this led to broken join of violations_table & media_table
     * The solution - delete all these records from both media table & violations table
     * */

    private void clearBadRecords(SQLiteDatabase db) {
        db.delete(MEDIA_TABLE, ID + " = ?", new String[] {"-1"});

        db.beginTransaction();
        try {
            /*db.rawQuery("SELECT * FROM " + VIOLATIONS_TABLE + " LEFT JOIN " + MEDIA_TABLE + " ON " +
            VIOLATIONS_TABLE + "." + _ID + " = " + MEDIA_TABLE + "." + ID + " WHERE " + MEDIA_TABLE + "." + ID +
            " IS NULL", new String[]{});*/
            db.delete(VIOLATIONS_TABLE, VIOLATIONS_TABLE + "." + _ID + " NOT IN (SELECT " + ID +
                    " FROM " + MEDIA_TABLE + ")",
                    new String[]{});

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DBHelper", e.toString());
            Mint.logException(e);
        } finally {
            db.endTransaction();
        }
    }

    private void upgrade_addSendAttemptColumn(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL("ALTER TABLE " + VIOLATIONS_TABLE + " ADD " + SEND_ATTEMPT + " INTEGER DEFAULT 0;");
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DBHelper", e.toString());
            Mint.logException(e);
        } finally {
            db.endTransaction();
        }
    }

    private String[] tableNamesFromCategory(int category) {
        switch (category) {
            case (Violation.CATEGORY_BUSINESS) : {
                return new String[]{COMPLAINS_TABLE, COMPLAINS_MEDIA_TABLE};
            }
            case (Violation.CATEGORY_PUBLIC) : {
                return new String[]{VIOLATIONS_TABLE, MEDIA_TABLE};
            }
            default : return null;
        }
    }

    private String getTableStructure(int category) {
        StringBuilder sb = new StringBuilder();
        List<String> requisites = getTableStructureList(category);
        for (String requisiteName : requisites) {
            sb.append(requisiteName).append(" TEXT,");
        }

        return new String(sb).substring(0,sb.length()-1); //remove trailing ','
    }

    private List<String> getTableStructureList(int category) {
        List<String> result = new ArrayList<>();
        ArrayList<Violation> violations = Violation.getViolationsList(category);
        for (Violation v : violations) {
            for (ViolationRequisite violationRequisite : v.getRequisites()) {
                result.add(violationRequisite.dbTag);
            }
        }
        return result;
    }

    private void checkViolationsAndAddColumns(SQLiteDatabase db, int category) {
        String table = tableNamesFromCategory(category)[0];
        Cursor dbCursor = db.query(table, null, null, null, null, null, null);
        String[] currentStructure = dbCursor.getColumnNames();
        dbCursor.close();

        List<String> requiredStructure = getTableStructureList(category);
        requiredStructure.removeAll(Arrays.asList(currentStructure));
        if (requiredStructure.isEmpty()) return;

        db.beginTransaction();
        try {
            for (String columnToAdd : requiredStructure) {
                db.execSQL("ALTER TABLE " + table + " ADD " + columnToAdd + " TEXT;");
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e("DBHelper", e.toString());
            Mint.logException(e);
        } finally {
            db.endTransaction();
        }
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
        if (db.isOpen()) {
            String idString = id.toString();
            db.delete(DBHelper.VIOLATIONS_TABLE, "_id = ?", new String[]{idString});
            db.delete(DBHelper.MEDIA_TABLE, "id = ?", new String[]{idString});
        }
    }

    public static void deleteComplainRequest(SQLiteDatabase db, Integer id){
        if (db.isOpen()) {
            String idString = id.toString();
            db.delete(DBHelper.COMPLAINS_TABLE, "_id = ?", new String[]{idString});
            db.delete(DBHelper.COMPLAINS_MEDIA_TABLE, "id = ?", new String[]{idString});
        }
    }

    public ArrayList<String> getMediaFiles(String tableTag) {
        ArrayList<String> result = new ArrayList<>();

        SQLiteDatabase db = getReadableDatabase();
        String[] columns =  {FILE_NAME};
        Cursor cursor = db.query(tableTag, columns, null, null, null, null, null);
        while (cursor.moveToNext()) {
            result.add(cursor.getString(0));
        }
        cursor.close();
        db.close();
        return result;
    }
}

