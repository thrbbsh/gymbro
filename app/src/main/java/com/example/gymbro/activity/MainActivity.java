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
import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.entity.WorkoutTemplate;
import com.example.gymbro.network.RetrofitClient;

import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private TextView textLoading;
    private View syncOverlay;
    private View syncLoadingLayout;
    private View syncErrorLayout;
    private TextView textSyncError;
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
        syncOverlay = findViewById(R.id.syncOverlay);
        syncLoadingLayout = findViewById(R.id.syncLoadingLayout);
        syncErrorLayout = findViewById(R.id.syncErrorLayout);
        textSyncError = findViewById(R.id.textSyncError);
        
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

        findViewById(R.id.btnRetrySync).setOnClickListener(v -> syncExercisesWithApi());

        syncExercisesWithApi();
        loadTemplates();
    }

    private void syncExercisesWithApi() {
        Executors.newSingleThreadExecutor().execute(() -> {
            int currentCount = db.exerciseDao().getExerciseCount();
            Log.d(TAG, "Current exercise count in DB: " + currentCount);
            
            if (currentCount > 20) {
                runOnUiThread(() -> syncOverlay.setVisibility(View.GONE));
                return;
            }

            runOnUiThread(() -> {
                syncOverlay.setVisibility(View.VISIBLE);
                syncLoadingLayout.setVisibility(View.VISIBLE);
                syncErrorLayout.setVisibility(View.GONE);
            });

            Log.d(TAG, "Starting API request to sync exercises...");
            RetrofitClient.getApiService().getExercises().enqueue(new Callback<List<Exercise>>() {
                @Override
                public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Successfully received " + response.body().size() + " exercises from server");
                        saveExercisesToDb(response.body());
                    } else {
                        Log.e(TAG, "Sync failed with code: " + response.code());
                        showSyncError("Sync failed (Code: " + response.code() + "). Make sure server is running.");
                    }
                }

                @Override
                public void onFailure(Call<List<Exercise>> call, Throwable t) {
                    Log.e(TAG, "Network error during sync: " + t.getMessage(), t);
                    showSyncError("Connection Error: " + t.getMessage() + "\n\n1. Check if server is RUNNING\n2. Check IP address in RetrofitClient\n3. Check Firewall settings");
                }
            });
        });
    }

    private void showSyncError(String message) {
        runOnUiThread(() -> {
            syncLoadingLayout.setVisibility(View.GONE);
            syncErrorLayout.setVisibility(View.VISIBLE);
            textSyncError.setText(message);
        });
    }

    private void saveExercisesToDb(List<Exercise> exercises) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.exerciseDao().insertAll(exercises);
            
            // Trigger linking exercises to templates now that we have the data
            AppDatabase.prepopulateTemplates(db);
            
            runOnUiThread(() -> {
                syncOverlay.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Exercises synced and templates updated!", Toast.LENGTH_SHORT).show();
                loadTemplates();
            });
        });
    }

    private void loadTemplates() {
        textLoading.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<WorkoutTemplate> templates;
            do {
                templates = db.workoutDao().getAllTemplates();
                if (templates.isEmpty()) {
                    try { Thread.sleep(500); } catch (InterruptedException e) { break; }
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
