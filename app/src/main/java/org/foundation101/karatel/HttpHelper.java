package org.foundation101.karatel;

import android.util.Log;

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
    final String fixed;

    public HttpHelper(String prefix){
        this.fixed = prefix;
    }

    public String makeRequestString(String[] args){
        String result = "";
        int i = 0;
        while (i < args.length){
            if (args[i].contains("@")) args[i] = args[i].replace("@", "%40");
            if (args[i+1].contains("@")) args[i+1] = args[i+1].replace("@", "%40");
            result = result.concat(fixed).concat("%5B"+args[i++]+"%5D=").concat(args[i++]+"&");
        }
        return result.substring(0, result.length()-1); //remove trailing "&"
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
                } case "DELETE" : {
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    urlConnection.setRequestProperty("Accept-Encoding", "UTF-8");
                    break;
                }//default is GET
            }
            if (authorizationRequired) urlConnection.setRequestProperty("Authorization", Globals.sessionToken);

            if  (!request.isEmpty()) {
                OutputStream os = urlConnection.getOutputStream();
                os.write(request.getBytes());
                os.flush();
                os.close();
            }

            int responseCode = urlConnection.getResponseCode();
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
