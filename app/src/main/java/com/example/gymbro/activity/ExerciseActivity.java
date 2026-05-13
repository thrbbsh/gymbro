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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.adapter.ExerciseAdapter;
import com.example.gymbro.viewmodel.ExerciseViewModel;

public class ExerciseActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textViewTitle;
    private ExerciseViewModel viewModel;
    private int templateId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exercise);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);

        recyclerView = findViewById(R.id.recyclerViewExercises);
        textViewTitle = findViewById(R.id.textViewWorkoutTitle);
        ImageButton buttonEditTemplate = findViewById(R.id.buttonEditTemplate);
        Button buttonStartWorkout = findViewById(R.id.buttonStartWorkout);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);
        String initialName = getIntent().getStringExtra("TEMPLATE_NAME");
        if (initialName != null) {
            textViewTitle.setText(initialName);
        }

        setupObservers();

        buttonEditTemplate.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseActivity.this, EditTemplateActivity.class);
            intent.putExtra("TEMPLATE_ID", templateId);
            startActivity(intent);
        });

        buttonStartWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(ExerciseActivity.this, ActiveTrainingActivity.class);
            intent.putExtra("TEMPLATE_ID", templateId);
            startActivity(intent);
        });
    }

    private void setupObservers() {
        viewModel.getTemplate().observe(this, template -> {
            if (template != null) {
                textViewTitle.setText(template.name);
            }
        });

        viewModel.getExercises().observe(this, exercises -> {
            recyclerView.setAdapter(new ExerciseAdapter(exercises));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (templateId != -1) {
            viewModel.loadData(templateId);
        }
    }
}
