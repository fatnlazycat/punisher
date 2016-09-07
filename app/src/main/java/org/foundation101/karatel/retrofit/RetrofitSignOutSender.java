package org.foundation101.karatel.retrofit;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.HTTP;
import retrofit2.http.Header;

/**
 * Created by Dima on 06.09.2016.
 */
public interface RetrofitSignOutSender {
    @FormUrlEncoded
    @HTTP(method = "DELETE", path = "signout", hasBody = true)
    Call<String> signOut(@Header("Authorization") String sessionToken, @Field("session[token]") String token);
}
