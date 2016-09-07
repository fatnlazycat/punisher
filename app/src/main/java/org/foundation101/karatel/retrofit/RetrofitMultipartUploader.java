package org.foundation101.karatel.retrofit;

import org.foundation101.karatel.CreationResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by Dima on 01.09.2016.
 */
public interface RetrofitMultipartUploader {
    @POST("{path}")
    Call<CreationResponse> upload(@Header("Authorization") String sessionToken,
                                  @Path("path") String api,
                                  @Body MultipartBody filePart);
}
