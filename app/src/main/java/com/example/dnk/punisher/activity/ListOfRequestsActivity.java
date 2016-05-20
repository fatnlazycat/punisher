package com.example.dnk.punisher.activity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.dnk.punisher.Globals;
import com.example.dnk.punisher.DBOpenHelper;
import com.example.dnk.punisher.R;
import com.example.dnk.punisher.adapter.RequestListAdapter;

public class ListOfRequestsActivity  extends AppCompatActivity {

    RequestListAdapter requestListAdapter;
    Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_of_requests);

        SQLiteDatabase db = new DBOpenHelper(this, "violations_db", 1).getReadableDatabase();
        String table = "violations_table";
        String[] columns = {"_id", "type", "time_stamp"};
        String where = null;
        String[] selectionArgs = null;
        cursor = db.query(table, columns, where, selectionArgs, null, null, null);

        ListView listViewRequests = (ListView)findViewById(R.id.listViewRequests);
        listViewRequests.setAdapter(new RequestListAdapter(this, cursor, 0));
        listViewRequests.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openRequest(position);
            }
        });
    }

    void openRequest(int requestPosition){

        cursor.moveToPosition(requestPosition);
        int id = cursor.getInt(0); //"_id" is the first column in the db

        Intent intent = new Intent(this, ViolationActivity.class)
                .putExtra(Globals.CREATE_MODE, false)
                .putExtra(Globals.ITEM_ID, id - 1); //because the list items are 1-based, not 0-based
        startActivity(intent);
    }
}
