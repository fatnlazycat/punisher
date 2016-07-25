package org.foundation101.karatel;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Dima on 17.05.2016.
 */
public class HttpHelper {
    public static final String ERROR_JSON = "{\"status\": \"error\", \"error\": \"no Internet connection\"}";

    final String fixed;

    public HttpHelper(String prefix){
        this.fixed = prefix;
    }

    public String makeRequestString(String[] args){
        String result = "";
        int i = 0;
        while (i < args.length){
            if (args[i] == null) args[i] = "";
            if (args[i+1] == null) args[i+1] = "";
            if (args[i].contains("@")) args[i] = args[i].replace("@", "%40");
            if (args[i+1].contains("@")) args[i+1] = args[i+1].replace("@", "%40");
            result = result.concat(fixed).concat("%5B"+args[i++]+"%5D=").concat(args[i++]+"&");
        }
        return result.substring(0, result.length()-1); //remove trailing "&"
    }

    public static boolean internetConnected(Context context){
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    public static String proceedRequest (String api, String method, String request, boolean authorizationRequired)
        throws IOException {
            StringBuilder response = new StringBuilder();
            HttpURLConnection urlConnection = (HttpURLConnection) new URL(Globals.SERVER_URL + api).openConnection();
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
                    urlConnection = (HttpURLConnection) new URL(Globals.SERVER_URL + api + "?" + request).openConnection();
                    urlConnection.setRequestMethod(method);
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("Accept-Encoding", "UTF-8");
                    break;
                }//default is GET
            }
            if (authorizationRequired) urlConnection.setRequestProperty("Authorization", Globals.sessionToken);

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
