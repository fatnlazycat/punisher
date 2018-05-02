package org.foundation101.karatel.retrofit;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class UploadProgressRequestBody /*extends RequestBody*/ {
/*    MultipartBody body;
    NetworkProgressListener listener;

    public UploadProgressRequestBody(MultipartBody originalBody ,NetworkProgressListener listener) {
        body = originalBody;
        this. listener = listener;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse("multipart");
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(file);
            long total = 0;
            long read;

            while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                total += read;
                sink.flush();
                this.listener.transferred(total);

            }
        } finally {
            Util.closeQuietly(source);
        }
    }*/
}
