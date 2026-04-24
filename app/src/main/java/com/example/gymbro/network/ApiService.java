package com.example.gymbro.network;

import com.example.gymbro.db.entity.Exercise;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("api/exercises")
    Call<List<Exercise>> getExercises();
}
