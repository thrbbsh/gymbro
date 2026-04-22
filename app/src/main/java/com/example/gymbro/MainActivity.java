package com.example.gymbro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.entity.WorkoutTemplate;

import java.util.List;
import java.util.concurrent.Executors;

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

        loadTemplates();
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
