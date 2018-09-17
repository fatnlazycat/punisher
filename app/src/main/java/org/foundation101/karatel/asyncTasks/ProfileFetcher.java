package org.foundation101.karatel.asyncTasks;

import android.os.AsyncTask;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.PunisherUser;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ProfileFetcher extends AsyncTask<Integer, Void, String> {
    public static final String TAG = "ProfileFetcher"; //also used in ProfileFragment for synchronization when cancelling task

    private AsyncTaskAction<Void, PunisherUser, ?> actions;

    public ProfileFetcher(AsyncTaskAction<Void, PunisherUser, ?> a){
        this.actions = a;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        actions.pre(null);
    }

    @Override
    protected String doInBackground(Integer... params) {
        if (HttpHelper.internetConnected()) try {
            return HttpHelper.proceedRequest("users/" + params[0], "GET", "", true);
        } catch (final IOException e){
            Globals.showError(R.string.cannot_connect_server, e);
            return "";
        }
        return "";
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        synchronized (TAG) {
            try {
                JSONObject json = new JSONObject(s);
                if (json.getString("status").equals("success")) {
                    JSONObject dataJSON = json.getJSONObject("data");
                    Globals.user = new PunisherUser(
                            dataJSON.getString("email"),
                            "", //for password
                            dataJSON.getString("surname"),
                            dataJSON.getString("firstname"),
                            dataJSON.getString("secondname"),
                            dataJSON.getString("phone_number"));
                    Globals.user.id = dataJSON.getInt("id");
                    Globals.user.avatarFileName = dataJSON.getJSONObject("avatar").getString("url");

                    KaratelPreferences.saveUserWithEmail(
                            dataJSON.getString("email"),
                            dataJSON.getString("surname"),
                            dataJSON.getString("firstname"),
                            dataJSON.getString("secondname"),
                            dataJSON.getString("phone_number"));




                } else {
                    String errorMessage;
                    if (json.getString("status").equals("error")) {
                        errorMessage = json.getString("error");
                    } else {
                        errorMessage = s;
                    }
                    Globals.showMessage(errorMessage);
                }
            } catch (JSONException e) {
                Globals.showError(R.string.error, e);
            }
        }
        actions.post(Globals.user);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        actions.onCancel();
    }
}
