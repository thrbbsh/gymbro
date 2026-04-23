package com.example.gymbro.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ExerciseApiService {
    @GET("exercises")
    Call<List<ExerciseDto>> getAllExercises(@Query("limit") int limit);
}
