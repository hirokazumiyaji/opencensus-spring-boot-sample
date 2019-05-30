package com.github.hirokazumiyaji.opencensus;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface EchoRepository {
    @POST("echo")
    Call<Message> echo(@Body Message message);
}
