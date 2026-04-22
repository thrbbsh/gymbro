package com.example.gymbro.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.adapter.EditExerciseAdapter;
import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class EditTemplateActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditExerciseAdapter adapter;
    private AppDatabase db;
    private int templateId;
    private List<TemplateExerciseWithDetails> exercises = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_template);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerViewEditExercises);
        Button buttonAddExercise = findViewById(R.id.buttonAddExercise);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        db = AppDatabase.getDatabase(this);

        templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);

        if (templateId != -1) {
            loadExercises();
        }

        buttonAddExercise.setOnClickListener(v -> {
            Toast.makeText(this, "Add Exercise functionality coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadExercises() {
        Executors.newSingleThreadExecutor().execute(() -> {
            exercises = db.workoutDao().getExercisesForTemplateWithDetails(templateId);
            runOnUiThread(() -> {
                adapter = new EditExerciseAdapter(exercises);
                recyclerView.setAdapter(adapter);
            });
        });
    }
}
