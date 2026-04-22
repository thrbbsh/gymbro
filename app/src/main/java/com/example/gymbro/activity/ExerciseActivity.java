package com.example.gymbro.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.adapter.ExerciseAdapter;
import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;

import java.util.List;
import java.util.concurrent.Executors;

public class ExerciseActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textViewTitle;
    private ImageButton buttonEditTemplate;
    private Button buttonStartWorkout;
    private AppDatabase db;
    private ExerciseAdapter adapter;
    private int templateId;
    private String templateName;

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
        buttonEditTemplate = findViewById(R.id.buttonEditTemplate);
        buttonStartWorkout = findViewById(R.id.buttonStartWorkout);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = AppDatabase.getDatabase(this);

        templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);
        templateName = getIntent().getStringExtra("TEMPLATE_NAME");

        if (templateName != null) {
            textViewTitle.setText(templateName);
        }

        buttonEditTemplate.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseActivity.this, EditTemplateActivity.class);
            intent.putExtra("TEMPLATE_ID", templateId);
            intent.putExtra("TEMPLATE_NAME", templateName);
            startActivity(intent);
        });

        buttonStartWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseActivity.this, ActiveTrainingActivity.class);
            intent.putExtra("TEMPLATE_ID", templateId);
            startActivity(intent);
        });

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
