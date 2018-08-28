package org.foundation101.karatel.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.foundation101.karatel.manager.DBHelper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.Violation;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class is not used here - I keep it just for reference of how to handle cursorAdapter inside the recyclerView
 */
class CursorAdapterForRecyclerView extends CursorAdapter{
    private View progressBar;
    private RecyclerView recyclerView;
    private Integer status,  _id, idOnServer;
    private Cursor cursor;
    private Context context;

    public CursorAdapterForRecyclerView(Context context, Cursor c, int flags, View progressBar) {
        super(context, c, flags);
        setProgressBar(progressBar);
        this.context = context;
        this.cursor = c;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        recyclerView = (RecyclerView)parent;
        return LayoutInflater.from(context).inflate(R.layout.item_request, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        status = cursor.getInt(cursor.getColumnIndex(DBHelper.STATUS));
        _id = cursor.getInt(cursor.getColumnIndex(DBHelper._ID));
        idOnServer = cursor.getInt(cursor.getColumnIndex(DBHelper.ID_SERVER));
        new ViolationFetcher(context, view, idOnServer, status, _id).execute();

        TextView textViewRequestTimeStamp=(TextView)view.findViewById(R.id.tvRequestTimeStamp);
        textViewRequestTimeStamp.setText(cursor.getString(cursor.getColumnIndex("time_stamp")));

        TextView textViewRequestType=(TextView)view.findViewById(R.id.textViewRequestType);
        String violationType = cursor.getString(cursor.getColumnIndex("type"));
        textViewRequestType.setText(Violation.getByType(violationType).getName());
    }

    public void setProgressBar(View progressBar){
        this.progressBar = progressBar;
    }

    private void updateCursor(int status, Integer rowId){
        DBHelper dbHelper = new DBHelper(context, DBHelper.DATABASE, DBHelper.DB_VERSION);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        //query to data table
        String table = DBHelper.VIOLATIONS_TABLE;
        String[] columns = null;
        String where = DBHelper._ID + "=?";
        String idString = rowId.toString();
        String[] selectionArgs = {idString};
        cursor = db.query(table, columns, where, selectionArgs, null, null, null);

        ContentValues cv = DBHelper.getRowAsContentValues(cursor);
        cursor.close();
        cv.put(DBHelper.STATUS, status);

        db = dbHelper.getWritableDatabase();
        db.replace(DBHelper.VIOLATIONS_TABLE, null, cv);

    }

    private class ViolationFetcher extends AsyncTask<Void, Void, String> {
        Context context;
        View itemView;
        Integer idOnServer, rowId, status;

        ViolationFetcher(Context context, View itemView, int idOnServer, int oldStatus, int rowId){
            this.context = context;
            this.itemView = itemView;
            this.idOnServer = idOnServer;
            this.rowId = rowId;
            this.status = oldStatus;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... params) {
            StringBuilder response = new StringBuilder();
            try {
                String stringParam = Globals.SERVER_URL + "complains/" + idOnServer.toString();
                HttpURLConnection urlConnection = (HttpURLConnection) new URL(stringParam).openConnection();

                int responseCode = urlConnection.getResponseCode();
                InputStream is;
                is = (responseCode == HttpURLConnection.HTTP_OK) ?
                        urlConnection.getInputStream(): urlConnection.getErrorStream();
                BufferedReader reader = new BufferedReader((new InputStreamReader(is)));
                String inputLine;
                while ((inputLine = reader.readLine()) != null) {
                    response.append(inputLine);
                }
                reader.close();
            } catch (IOException e) {
                response.append(e.getMessage());
            }
            return response.toString();
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            int newStatus = this.status;
            try {
                JSONObject json = new JSONObject(response).getJSONArray("data").getJSONObject(1);
                String newStatusString = json.getString("complain_status_id");
                try {
                    newStatus = Integer.parseInt(newStatusString);
                    if (this.status != newStatus){
                        CursorAdapterForRecyclerView.this.updateCursor(newStatus, this.rowId);
                    }
                } catch (NumberFormatException e) {
                    Log.e("Punisher", e.getMessage());
                }
            } catch (JSONException e) {
                Log.e("Punisher", e.getMessage());
            }
            ImageView imageViewStatus = (ImageView) itemView.findViewById(R.id.imageViewStatus);
            imageViewStatus.setImageResource(R.drawable.level_list_status);
            imageViewStatus.setImageLevel(newStatus);


            TextView textViewRequestStatus=(TextView)itemView.findViewById(R.id.textViewRequestStatus);
            String statusText = context.getResources().getStringArray(R.array.violationStatuses)[newStatus];
            textViewRequestStatus.setText(statusText);

            progressBar.setVisibility(View.GONE);
        }
    }
}

