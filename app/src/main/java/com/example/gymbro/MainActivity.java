package com.example.gymbro;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadTemplates();
    }

    private void loadTemplates() {
        // Load templates from database in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            List<WorkoutTemplate> templates = db.workoutDao().getAllTemplates();
            runOnUiThread(() -> {
                adapter = new WorkoutAdapter(templates, template -> {
                    // Navigate to ExerciseActivity with template details
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