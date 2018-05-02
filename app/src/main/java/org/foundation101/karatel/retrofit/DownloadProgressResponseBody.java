package org.foundation101.karatel.retrofit;

import android.support.annotation.NonNull;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class DownloadProgressResponseBody extends ResponseBody {
    public DownloadProgressResponseBody(String downloadIdentifier, ResponseBody responseBody, NetworkProgressListener progressListener) {
        this.responseBody = responseBody;
        this.progressListener = progressListener;
        this.downloadIdentifier = downloadIdentifier;
    }

    private String downloadIdentifier;
    private NetworkProgressListener progressListener;
    private ResponseBody responseBody;
    private BufferedSource bufferedSource;

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(source(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source source(Source source) {
        return new ForwardingSource(source) {
            long totalBytesRead = 0L;

            @Override
            public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                if(bytesRead != -1){
                    totalBytesRead += bytesRead;
                }

                if (progressListener != null) {
                    progressListener.update(
                            downloadIdentifier, totalBytesRead, responseBody.contentLength(), bytesRead == -1
                    );
                }
                return bytesRead;
            }
        };

    }
}

