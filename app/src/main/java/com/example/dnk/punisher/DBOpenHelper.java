package com.example.dnk.punisher;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Dima on 08.05.2016.
 */
public class DBOpenHelper extends SQLiteOpenHelper {
    Context context;
    String dataTableStructure;

    public DBOpenHelper(Context context, String name, int version) {
        super(context, name, null, version);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder sb = new StringBuilder();
        Resources res = context.getResources();
        String[] violations = res.getStringArray(R.array.violationTypes);
        for (String violation : violations){
            int arrayId = res.getIdentifier(violation + "Requisites", "array", context.getPackageName());
            String[] requisites = res.getStringArray(arrayId);
            for (int i = 0; i < requisites.length; i += 4){
                sb.append(requisites[i] + " TEXT,");
            }
        }
        dataTableStructure = new String(sb).substring(0,sb.length()-1); //remove trailing ','

        db.execSQL("CREATE TABLE "
                + "violations_table"
                + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "type TEXT,"
                + "time_stamp TEXT,"
                + "longitude REAL,"
                + "latitude REAL,"
                + dataTableStructure
                + ");");
        db.execSQL("CREATE TABLE "
                + "media_table"
                + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "id INTEGER,"
                + "file_name TEXT"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}
