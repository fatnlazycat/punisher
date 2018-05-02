package org.foundation101.karatel.retrofit;

public interface NetworkProgressListener {
    void update(String downloadIdentifier, long bytesRead, long contentLength, boolean complete);
}
