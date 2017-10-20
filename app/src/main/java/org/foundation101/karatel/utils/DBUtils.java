package org.foundation101.karatel.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.foundation101.karatel.CameraManager;
import org.foundation101.karatel.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.R;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Dima on 20.10.2017.
 */

public class DBUtils {
    public static void clearEvidences(final DBHelper dbHelper) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> filesInDB   = new ArrayList<>();

                //TODO - check if it wouldn't be garbage collected
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                try {
                    //query 1 - violations media
                    String table = DBHelper.MEDIA_TABLE;
                    String[] columns = new String[]{DBHelper.FILE_NAME};
                    Cursor cursor = db.query(table, columns, null, null, null, null, null);
                    while (cursor.moveToNext()) {
                        String evidenceFileName = cursor.getString(cursor.getColumnIndex(DBHelper.FILE_NAME));
                        filesInDB.add(evidenceFileName);
                    }
                    cursor.close();

                    //query 2 - complains media
                    table = DBHelper.COMPLAINS_MEDIA_TABLE;
                    columns = new String[]{DBHelper.FILE_NAME};
                    Cursor cursor2 = db.query(table, columns, null, null, null, null, null);
                    while (cursor2.moveToNext()) {
                        String evidenceFileName = cursor2.getString(cursor.getColumnIndex(DBHelper.FILE_NAME));
                        filesInDB.add(evidenceFileName);
                    }
                    cursor2.close();


                    File[] filesOnDisk = KaratelApplication.getInstance().getExternalFilesDir(null)
                            .listFiles(new FilenameFilter() {
                                final Pattern pattern = Pattern.compile(CameraManager.FILENAME_PATTERN);
                                @Override
                                public boolean accept(File dir, String filename) {
                                    Matcher matcher = pattern.matcher(filename);
                                    return matcher.matches();
                                }
                            });

                    boolean deletedSuccessfully = true;

                    for (File f : filesOnDisk) {
                        if (filesInDB.indexOf(f.getPath()) == -1)
                            deletedSuccessfully = f.delete() && deletedSuccessfully;
                    }

                    Log.e("Punisher", "files deleted successfully " + deletedSuccessfully);

                } catch (Exception e) {
                    Globals.showError(KaratelApplication.getInstance(), R.string.error, e);
                }

                db.close();
            }
        }).start();
    }
}
