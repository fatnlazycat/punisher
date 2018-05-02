package org.foundation101.karatel.retrofit;

import org.foundation101.karatel.entity.ComplainCreationResponse;
import org.foundation101.karatel.entity.CreationResponse;

import okhttp3.RequestBody;
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
    Call<CreationResponse> uploadComplain(@Header("Authorization") String sessionToken,
                                          @Path("path") String api,
                                          @Body RequestBody filePart);

    @POST("{path}")
    Call<ComplainCreationResponse>  uploadGrievance(@Header("Authorization") String sessionToken,
                                          @Path(value = "path", encoded = true) String api,
                                          @Body RequestBody filePart);
}
