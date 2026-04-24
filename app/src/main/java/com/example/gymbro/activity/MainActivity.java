package com.example.gymbro.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
        FloatingActionButton fabAddTemplate = findViewById(R.id.fabAddTemplate);
        
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

        fabAddTemplate.setOnClickListener(v -> showAddTemplateDialog());

        syncExercisesWithApi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTemplates();
    }

    private void showAddTemplateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Workout Template");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("Template Name");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        builder.setView(input);
        input.setPadding(padding, padding, padding, padding);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                createNewTemplate(name);
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createNewTemplate(String name) {
        Executors.newSingleThreadExecutor().execute(() -> {
            long id = db.workoutDao().insertTemplate(new WorkoutTemplate(name));
            runOnUiThread(() -> {
                loadTemplates();
                // Optionally open the edit screen for the new template immediately
                Intent intent = new Intent(MainActivity.this, ExerciseActivity.class);
                intent.putExtra("TEMPLATE_ID", (int) id);
                intent.putExtra("TEMPLATE_NAME", name);
                startActivity(intent);
            });
        });
    }

    private void syncExercisesWithApi() {
        Executors.newSingleThreadExecutor().execute(() -> {
            int currentCount = db.exerciseDao().getExerciseCount();
            if (currentCount > 20) {
                runOnUiThread(() -> syncOverlay.setVisibility(View.GONE));
                return;
            }

            runOnUiThread(() -> {
                syncOverlay.setVisibility(View.VISIBLE);
                syncLoadingLayout.setVisibility(View.VISIBLE);
                syncErrorLayout.setVisibility(View.GONE);
            });

            RetrofitClient.getApiService().getExercises().enqueue(new Callback<List<Exercise>>() {
                @Override
                public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        saveExercisesToDb(response.body());
                    } else {
                        showSyncError("Sync failed (Code: " + response.code() + ")");
                    }
                }

                @Override
                public void onFailure(Call<List<Exercise>> call, Throwable t) {
                    showSyncError("Connection Error: " + t.getMessage());
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
            AppDatabase.prepopulateTemplates(db);
            runOnUiThread(() -> {
                syncOverlay.setVisibility(View.GONE);
                loadTemplates();
            });
        });
    }

    private void loadTemplates() {
        if (adapter == null) {
            textLoading.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            List<WorkoutTemplate> templates = db.workoutDao().getAllTemplates();
            runOnUiThread(() -> {
                textLoading.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                
                adapter = new WorkoutAdapter(templates, new WorkoutAdapter.OnTemplateActionListener() {
                    @Override
                    public void onTemplateClick(WorkoutTemplate template) {
                        Intent intent = new Intent(MainActivity.this, ExerciseActivity.class);
                        intent.putExtra("TEMPLATE_ID", template.id);
                        intent.putExtra("TEMPLATE_NAME", template.name);
                        startActivity(intent);
                    }

                    @Override
                    public void onTemplateDelete(WorkoutTemplate template) {
                        deleteTemplate(template);
                    }
                });
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void deleteTemplate(WorkoutTemplate template) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Template")
                .setMessage("Are you sure you want to delete '" + template.name + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.workoutDao().deleteTemplate(template);
                        runOnUiThread(this::loadTemplates);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
