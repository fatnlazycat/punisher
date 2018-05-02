package org.foundation101.karatel.retrofit;
import android.support.annotation.NonNull;

import org.foundation101.karatel.manager.CameraManager;
import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadProgressInterceptor implements Interceptor {

    @Override
    public Response intercept(@NonNull Interceptor.Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        Request originalRequest = originalResponse.request();
        Response.Builder builder = originalResponse.newBuilder();

        List<String> path = originalRequest.url().pathSegments();
        String downloadIdentifier = (path.size() > 1) ? path.get(path.size() - 1) : "";

        if("GET".equalsIgnoreCase(originalRequest.method())
                && oneOfSupportedExtensions(downloadIdentifier)) { // someone needs download progress information
            builder.body(new DownloadProgressResponseBody(
                    downloadIdentifier,
                    originalResponse.body(),
                    new NetworkProgressListener() {
                        @Override
                        public void update(String identifier, long bytesRead, long contentLength, boolean complete) {
                            int progress;
                            if (complete) progress = 100;
                            else progress = (int) ((float)bytesRead / contentLength * 100);

                            EventBus.getDefault().post(new ProgressEvent(identifier, progress));
                        }
                    })
            );
        } else if("POST".equalsIgnoreCase(originalRequest.method())) {// someone needs upload progress information!

        } else { // do nothing if it's not a file with an identifier :)
            builder.body(originalResponse.body());
        }

        return builder.build();
    }

    private boolean oneOfSupportedExtensions(String pathSegment) {
        if (pathSegment == null) return false;
        for (String s : CameraManager.SUPPORTED_EXTENSIONS) {
            if (pathSegment.toLowerCase().endsWith(s.toLowerCase())) return true;
        }
        return false;
    }
}
