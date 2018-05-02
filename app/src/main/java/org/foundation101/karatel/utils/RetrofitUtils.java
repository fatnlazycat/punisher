package org.foundation101.karatel.utils;

import android.util.Log;

import org.foundation101.karatel.BuildConfig;
import org.foundation101.karatel.Globals;
import org.foundation101.karatel.KaratelApplication;
import org.foundation101.karatel.retrofit.DownloadProgressInterceptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by Dima on 11.10.2017.
 */

public class RetrofitUtils {
    public static final String TAG = "RetrofitUtils";

    public static Retrofit build(Retrofit retrofit, int apiVersion) {
        if (needsRebuilding(retrofit, apiVersion)) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            HttpLoggingInterceptor.Level logLevel = BuildConfig.DEBUG ?
                    HttpLoggingInterceptor.Level.BASIC :
                    HttpLoggingInterceptor.Level.NONE;
            logging.setLevel(logLevel);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            // add your other interceptors …
            //httpClient.addInterceptor(new DownloadProgressInterceptor());
            // add logging as last interceptor
            httpClient.addInterceptor(logging);

            String newBaseUrl = Globals.SERVER_URL.replace("/v1/", "/v" + apiVersion + "/");

            retrofit = new Retrofit.Builder()
                    .baseUrl(newBaseUrl)
                    .addConverterFactory(JacksonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return retrofit;
    }

    private static boolean needsRebuilding(Retrofit retrofit, int apiVersion) {
        if (retrofit == null) return true;

        HttpUrl currentUrl = retrofit.baseUrl();
        String lastPathSegment = currentUrl.pathSegments().get(currentUrl.pathSize() - 1);

        return !lastPathSegment.equals("v" + apiVersion);
    }



    public static boolean writeResponseBodyToDisk(ResponseBody body, File file) {
        try {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] bytes = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(file);

                while (true) {
                    int read = inputStream.read(bytes);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(bytes, 0, read);

                    fileSizeDownloaded += read;
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}
