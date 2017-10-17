package org.foundation101.karatel.utils;

import org.foundation101.karatel.BuildConfig;
import org.foundation101.karatel.Globals;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by Dima on 11.10.2017.
 */

public class RetrofitUtils {
    public static Retrofit build(Retrofit retrofit, int apiVersion) {
        if (needsRebuilding(retrofit, apiVersion)) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            HttpLoggingInterceptor.Level logLevel = BuildConfig.DEBUG ?
                    HttpLoggingInterceptor.Level.BODY :
                    HttpLoggingInterceptor.Level.NONE;
            logging.setLevel(logLevel);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            // add your other interceptors …
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
}
