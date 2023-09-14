package com.example.highpointcomparer;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ElevationApi {
    @GET("lookup")
    Call<ElevationResponse> getElevation(
            @Query("locations") String locations,
            @Query("format") String format
    );
}
