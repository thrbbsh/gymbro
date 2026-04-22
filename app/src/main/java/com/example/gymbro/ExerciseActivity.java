package com.example.gymbro;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;

import java.util.List;
import java.util.concurrent.Executors;

public class ExerciseActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textViewTitle;
    private AppDatabase db;
    private ExerciseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exercise);

        // Handle system bars insets for Edge-to-Edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerViewExercises);
        textViewTitle = findViewById(R.id.textViewWorkoutTitle);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = AppDatabase.getDatabase(this);

        int templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);
        String templateName = getIntent().getStringExtra("TEMPLATE_NAME");

        if (templateName != null) {
            textViewTitle.setText(templateName);
        }

        if (templateId != -1) {
            loadExercises(templateId);
        }
    }

    private void loadExercises(int templateId) {
        // Load data in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            // Fetch exercises with their details (names, muscle groups) using a Relation/Join
            List<TemplateExerciseWithDetails> exercises = db.workoutDao().getExercisesForTemplateWithDetails(templateId);
            
            runOnUiThread(() -> {
                adapter = new ExerciseAdapter(exercises);
                recyclerView.setAdapter(adapter);
            });
        });
    }
}