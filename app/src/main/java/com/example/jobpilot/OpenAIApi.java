package com.example.jobpilot;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface OpenAIApi {
    @POST("v1/chat/completions")
    Call<ChatResponse> createCompletion(
            @Header("Authorization") String apiKey,
            @Body ChatRequest request
    );
}
