package org.foundation101.karatel.AsyncTasks;

import android.os.AsyncTask;

import org.codehaus.jackson.map.ObjectMapper;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.Request;
import org.foundation101.karatel.manager.HttpHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class RequestListFetcher extends AsyncTask<Void, Void, String> {
    private AsyncTaskAction<Void, ArrayList<Request>, ?> actions;

    public RequestListFetcher(AsyncTaskAction<Void, ArrayList<Request>, ?> actions) {
        this.actions = actions;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        actions.pre(null);
    }

    @Override
    protected String doInBackground(Void... params) {
        return getRequests();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        actions.post(parseResponse(s));
    }


    public static String getRequests() {
        try {
            if (HttpHelper.internetConnected()) {
                return HttpHelper.proceedRequest("complains", "GET", "", true);
            } else return HttpHelper.ERROR_JSON;
        } catch (final IOException e){
            return "";
        }
    }

    public static ArrayList<Request> parseResponse(String response) {
        ArrayList<Request> requestsFromServer = new ArrayList<>();
        try {
            JSONObject json = new JSONObject(response);
            switch (json.getString("status")) {
                case Globals.SERVER_SUCCESS : {
                    JSONArray dataJSON = json.getJSONArray("data");
                    ObjectMapper mapper = new ObjectMapper();
                    for (int i = 0; i < dataJSON.length(); i++) {
                        JSONArray oneRequest = dataJSON.getJSONArray(i);
                        JSONObject requestBody = oneRequest.getJSONObject(1);
                        String requestBodyString = requestBody.toString();
                        Request request = mapper.readValue(requestBodyString, Request.class);

                        request.type = oneRequest.getString(0);

                        requestsFromServer.add(request);
                    }
                    break;
                }
                case Globals.SERVER_ERROR : {
                    Globals.showMessage(json.getString(Globals.SERVER_ERROR));
                    break;
                }
            }
        } catch (JSONException | IOException e) {
            Globals.showError(R.string.error, e);
        }
        return requestsFromServer;
    }
}
