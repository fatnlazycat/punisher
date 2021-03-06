package org.foundation101.karatel.manager;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.foundation101.karatel.Const;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;

import javax.inject.Inject;

/**
 * Created by Dima on 17.05.2016.
 */
public class HttpHelper {
    @Inject KaratelPreferences preferences;

    public static final String ERROR_JSON = "{\"status\": \"error\", \"error\": \"no Internet connection\"}";

    final String fixed;

    public HttpHelper(String prefix){
        this.fixed = prefix;
        KaratelApplication.dagger().inject(this);
    }

    public String makeRequestString(String[] args){
        final LinkedHashMap<String, String> SPECIAL_CHARACTERS = new LinkedHashMap<String, String>(){
            {
                put("%", "%25"); //this should go first to avoid double replacing '%'
                put("'", "%27");
                put("@", "%40");
            }
        };
        String result = "";
        int i = 0;
        while (i < args.length){
            if (args[i] == null) args[i] = "";
            if (args[i+1] == null) args[i+1] = "";

            //replacing special characters
            for (String key : SPECIAL_CHARACTERS.keySet()) {
                if (args[i].contains(key)) args[i] = args[i].replace(key, SPECIAL_CHARACTERS.get(key));
                if (args[i + 1].contains(key)) args[i + 1] = args[i + 1].replace(key, SPECIAL_CHARACTERS.get(key));
            }

            result = result.concat(fixed).concat("%5B"+args[i++]+"%5D=").concat(args[i++]+"&");
        }
        return result.substring(0, result.length()-1); //remove trailing "&"
    }

    public static boolean internetConnected(){
        //if (context == null) return true; //this line is needed to avoid crash if the user started AsyncTask operation & aborted it during execution
        ConnectivityManager connManager = (ConnectivityManager) KaratelApplication.getInstance()
                .getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static String proceedRequest (String api, String method, String request, boolean authorizationRequired)
        throws IOException {
            StringBuilder response = new StringBuilder();
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(Const.SERVER_URL + api).openConnection();
            switch (method){
                case "POST" : {
                    urlConnection.setRequestMethod(method);
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("Accept-Encoding", "UTF-8");
                    break;
                } case "PUT" : {
                    urlConnection.setRequestMethod(method);
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("Accept-Encoding", "UTF-8");
                    break;
                } case "DELETE" : {//DELETE with body not supported on old Android versions -> body is urlencoded
                    urlConnection = (HttpURLConnection) new URL(Const.SERVER_URL + api + "?" + request).openConnection();
                    urlConnection.setRequestMethod(method);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("Accept-Encoding", "UTF-8");
                    break;
                }//default is GET
            }
            if (authorizationRequired) urlConnection.setRequestProperty("Authorization", new KaratelPreferences().sessionToken());

            if  (!request.isEmpty() && !method.equals("DELETE")) {
                OutputStream os = urlConnection.getOutputStream();
                os.write(request.getBytes());
                os.flush();
                os.close();
            }

            try {
                int responseCode = urlConnection.getResponseCode();
            } catch (Exception e){
                Log.e("Punisher", e == null? "Exception e == null" : e.toString());
            }
            InputStream is = (urlConnection.getErrorStream() != null) ?
                    urlConnection.getErrorStream(): urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader((new InputStreamReader(is)));
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            is.close();

            return response.toString();
    }

    //short convenience for "POST"
    public static String proceedRequest(String api, String request, boolean authorizationRequired)
        throws IOException {
            return HttpHelper.proceedRequest(api, "POST", request, authorizationRequired);
    }
}
