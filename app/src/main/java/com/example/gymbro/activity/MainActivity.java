package com.example.gymbro.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.adapter.WorkoutAdapter;
import com.example.gymbro.api.ExerciseApiService;
import com.example.gymbro.api.ExerciseDto;
import com.example.gymbro.api.RetrofitClient;
import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.entity.WorkoutTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textLoading;
    private WorkoutAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = AppDatabase.getDatabase(this);
        recyclerView = findViewById(R.id.recyclerViewTemplates);
        textLoading = findViewById(R.id.textLoadingTemplates);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button btnStatistics = findViewById(R.id.buttonStats);
        btnStatistics.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        Button btnCommunity = findViewById(R.id.buttonCommunity);
        btnCommunity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CommunityActivity.class);
            startActivity(intent);
        });

        syncExercisesWithApi();
        loadTemplates();
    }

    private void syncExercisesWithApi() {
        Executors.newSingleThreadExecutor().execute(() -> {
            int currentCount = db.exerciseDao().getCount();
            // If we have more than a few exercises, assume we already synced
            if (currentCount > 20) {
                Log.d("API_SYNC", "Exercises already synced. Count: " + currentCount);
                return;
            }

            Log.d("API_SYNC", "Starting initial sync of exercises...");
            ExerciseApiService apiService = RetrofitClient.getClient().create(ExerciseApiService.class);
            apiService.getAllExercises(1500).enqueue(new Callback<>() {
                @Override
                public void onResponse(Call<List<ExerciseDto>> call, Response<List<ExerciseDto>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        saveExercisesToDb(response.body());
                    } else {
                        Log.e("API_SYNC", "Sync failed. Code: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<List<ExerciseDto>> call, Throwable t) {
                    Log.e("API_SYNC", "Sync network error: " + t.getMessage());
                }
            });
        });
    }

    private void saveExercisesToDb(List<ExerciseDto> dtos) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Exercise> entities = new ArrayList<>();
            for (ExerciseDto dto : dtos) {
                Exercise exercise = new Exercise();
                exercise.apiId = dto.apiId;
                exercise.name = dto.name;
                exercise.bodyPart = dto.bodyPart;
                exercise.equipment = dto.equipment;
                exercise.gifUrl = dto.gifUrl;
                exercise.target = dto.target;
                exercise.secondaryMuscles = dto.secondaryMuscles;
                exercise.instructions = dto.instructions;
                entities.add(exercise);
            }
            db.exerciseDao().insertAll(entities);
            Log.d("API_SYNC", "Sync complete! Saved " + entities.size() + " exercises.");
            runOnUiThread(() -> Toast.makeText(MainActivity.this, 
                "Synced " + entities.size() + " exercises from API", Toast.LENGTH_SHORT).show());
        });
    }

    private void loadTemplates() {
        // Show loading text
        textLoading.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<WorkoutTemplate> templates;
            
            // Wait for DB prepopulation
            do {
                templates = db.workoutDao().getAllTemplates();
                if (templates.isEmpty()) {
                    try {
                        Thread.sleep(500); // Check every 300ms
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            } while (templates.isEmpty());

            List<WorkoutTemplate> finalTemplates = templates;
            runOnUiThread(() -> {
                textLoading.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                
                adapter = new WorkoutAdapter(finalTemplates, template -> {
                    Intent intent = new Intent(MainActivity.this, ExerciseActivity.class);
                    intent.putExtra("TEMPLATE_ID", template.id);
                    intent.putExtra("TEMPLATE_NAME", template.name);
                    startActivity(intent);
                });
                recyclerView.setAdapter(adapter);
            });
        });
    }
}
