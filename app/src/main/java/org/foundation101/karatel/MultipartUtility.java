package org.foundation101.karatel;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MultipartUtility {
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;

    private OutputStream outputStream;
    BufferedOutputStream bos;
    private PrintWriter writer;

    public MultipartUtility(String requestURL, String charset) throws IOException {
        this(requestURL, charset, "POST");
    }

    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL
     * @param charset
     * @throws IOException
     */
    public MultipartUtility(String requestURL, String charset, String method) throws IOException {

        // creates a unique boundary based on time stamp
        boundary = "===" + System.currentTimeMillis() + "===";
        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setChunkedStreamingMode(4096);
        httpConn.setReadTimeout(5000);
        httpConn.setRequestMethod(method);
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true);    // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        httpConn.setRequestProperty( "Accept-Encoding", "" );
        httpConn.setRequestProperty("Authorization", Globals.sessionToken);
        outputStream = httpConn.getOutputStream();
        bos = new BufferedOutputStream(outputStream, 4096);
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);
    }

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    public void addFormField(String name, String value) {
        StringBuilder sb = new StringBuilder();
        sb.append("--" + boundary).append(LINE_FEED);
        sb.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_FEED);
        //sb.append("Content-Type: application/x-www-form-urlencoded; charset=" + charset).append(LINE_FEED);
        sb.append(LINE_FEED);
        sb.append(value);
        writer.append(sb.toString());
        writer.flush();
        writer.append(LINE_FEED);
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName  name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile) throws IOException {
        String fileName = uploadFile.getName();

        StringBuilder sb = new StringBuilder();
        sb.append("--" + boundary).append(LINE_FEED);
        sb.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName
                + "\"").append(LINE_FEED);
        sb.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName)).append(LINE_FEED);
        sb.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        sb.append(LINE_FEED);
        writer.append(sb.toString());
        writer.flush();

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        int i = 0;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            try {
                bos.write(buffer, 0, bytesRead);
                bos.flush();
                Log.e("Punisher", "#" + bytesRead + " # " + i++);
            } catch (OutOfMemoryError e){
                Runtime.getRuntime().gc();
            }
        }
        outputStream.flush();
        inputStream.close();
        writer.append(LINE_FEED);
        writer.append(LINE_FEED).flush();
    }

    /**
     * Adds a header field to the request.
     *
     * @param name  - name of the header field
     * @param value - value of the header field
     */
    public void addHeaderField(String name, String value) {
        writer.append(name + ": " + value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public List<String> finish() throws IOException {
        List<String> response = new ArrayList<String>();

        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.close();
        outputStream.close();

        // checks server's status code first
        int status = 0;
        try {
            status = httpConn.getResponseCode();
        } catch (IOException e){
            Log.e("Punisher", e.toString());
        }
        if ((status == HttpURLConnection.HTTP_OK) || (status == HttpURLConnection.HTTP_CREATED)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            if (httpConn.getErrorStream() == null) {
                throw new IOException("Server returned no ErrorStream. Status: " + status);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getErrorStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        }
        return response;
    }
}