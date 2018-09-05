package org.foundation101.karatel.asyncTasks;

import android.os.AsyncTask;
import android.util.Log;

import com.splunk.mint.Mint;

import org.foundation101.karatel.Globals;
import org.foundation101.karatel.R;
import org.foundation101.karatel.entity.PunisherUser;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.manager.KaratelPreferences;
import org.foundation101.karatel.utils.FileUtils;
import org.foundation101.karatel.utils.MultipartUtility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ProfileSaver extends AsyncTask<Void, Void, String> {
    private static final String TAG = "ProfileSaver";

    private AsyncTaskAction<Void, String, ?> actions;
    private String avatarFileName;
    private PunisherUser userToSave;
    private String swapFileName;

    public ProfileSaver(AsyncTaskAction<Void, String, ?> actions, PunisherUser user, String avatarFileName) {
        this.actions = actions;
        this.userToSave = user;
        this.avatarFileName = avatarFileName;

        String avatarFromPreferences = KaratelPreferences.userAvatar();
        this.swapFileName = avatarFromPreferences.isEmpty() ?
                FileUtils.INSTANCE.avatarFileName(false) : avatarFromPreferences;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        actions.pre(null);
    }

    @Override
    protected String doInBackground(Void... params) {
        StringBuilder response = new StringBuilder();
        if (HttpHelper.internetConnected()) {
            int tries = 0;
            final int MAX_TRIES = 2;
            while (tries++ < MAX_TRIES) try {
                String requestUrl = Globals.SERVER_URL + "users/" + Globals.user.id;
                MultipartUtility multipart = new MultipartUtility(requestUrl, "UTF-8", "PUT");
                //multipart.addFormField("user[email]", Globals.user.email);
                multipart.addFormField("user[firstname]", userToSave.name);
                multipart.addFormField("user[surname]", userToSave.surname);
                multipart.addFormField("user[secondname]", userToSave.secondName);
                multipart.addFormField("user[phone_number]", userToSave.phone);

                if (avatarFileName != null) { //null means that the avatar wasn't changed
                    if (avatarFileName.isEmpty()) {
                        multipart.addFormField("user[avatar]", ""); //delete avatar
                    } else {
                        boolean fileOperationsSuccess =
                                FileUtils.INSTANCE.swapRename(avatarFileName, swapFileName);
                        File file = new File(swapFileName);

                        if (fileOperationsSuccess && file.length() > 0) {
                            multipart.addFilePart("user[avatar]", file);
                        }
                    }
                }

                List<String> responseList = multipart.finish();
                for (String line : responseList) {
                    Log.e("Punisher", "Upload Files Response:::" + line);
                    response.append(line);
                }
                return response.toString();
            } catch (final IOException e) {
                if (tries == MAX_TRIES) {
                    Globals.showError(R.string.cannot_connect_server, e);
                }
            }
        } else {
            response.append(HttpHelper.ERROR_JSON);
        }
        return response.toString();
    }

    @Override
    protected void onPostExecute(String s) {
        try{
            JSONObject json = new JSONObject(s);
            String status = json.getString("status");
            switch (status){
                case Globals.SERVER_SUCCESS : {
                    Globals.showMessage(R.string.profile_changes_saved);

                    Globals.user.name = userToSave.name;
                    Globals.user.surname = userToSave.surname;
                    Globals.user.secondName = userToSave.secondName;
                    Globals.user.phone = userToSave.phone;

                    if (avatarFileName != null) {
                        String fileToDelete;

                        if (avatarFileName.isEmpty()) {
                            swapFileName = avatarFileName; //i.e. ""
                            fileToDelete = FileUtils.INSTANCE.avatarFileName(false);
                        } else {
                            fileToDelete = avatarFileName;
                        }
                        if (!new File(fileToDelete).delete()) Mint.logException(TAG,
                                "delete failed, oldAvatarFile=" + fileToDelete,
                                new Exception("помилка операції з файлом"));
                    }

                    KaratelPreferences.saveUserWithAvatar
                            (userToSave.surname, userToSave.name,
                                    userToSave.secondName, userToSave.phone, swapFileName);
                    break;
                }
                case Globals.SERVER_ERROR : {
                    if (avatarFileName != null && !avatarFileName.isEmpty()) { //rename the files back
                        FileUtils.INSTANCE.swapRename(swapFileName, avatarFileName);
                    }

                    StringBuilder errorMessage = new StringBuilder();
                    JSONObject errorJSON = json.getJSONObject(Globals.SERVER_ERROR);
                    JSONArray errorNames = errorJSON.names();
                    for (int i = 0; i < errorNames.length(); i++){
                        //read only the first message in the array for each error type
                        String oneMessage = errorJSON.getJSONArray(errorNames.getString(i)).getString(0);
                        errorMessage.append(oneMessage + "\n");
                    }
                    Globals.showMessage(errorMessage.toString());
                    break;
                }
                default : { }
            }

            actions.post(status);

        } catch (JSONException eJSON){
            Globals.showError(R.string.error, eJSON);
        }
    }
}
