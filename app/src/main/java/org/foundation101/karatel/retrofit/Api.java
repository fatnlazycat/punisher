package org.foundation101.karatel.retrofit;

import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface Api {
    @POST("/api/v1/signin")
    Call<String> advancedFbLogin();

    @POST("/api/v1/socials")
    Call<String> bindFb(@Header("Authorization") String temporarySessionToken);
}
