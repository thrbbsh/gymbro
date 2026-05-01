package com.example.gymbro.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.adapter.EditExerciseAdapter;
import com.example.gymbro.adapter.SearchExerciseAdapter;
import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.entity.TemplateExercise;
import com.example.gymbro.db.entity.WorkoutTemplate;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class EditTemplateActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditExerciseAdapter adapter;
    private EditText editTextTemplateName;
    private AppDatabase db;
    private int templateId;
    private WorkoutTemplate currentTemplate;
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
        editTextTemplateName = findViewById(R.id.editTextTemplateName);
        Button buttonAddExercise = findViewById(R.id.buttonAddExercise);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        db = AppDatabase.getDatabase(this);

        templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);

        if (templateId != -1) {
            loadTemplateData();
            loadExercises();
        }

        setupNameChangeListener();
        buttonAddExercise.setOnClickListener(v -> showAddExerciseDialog());
    }

    private void loadTemplateData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            currentTemplate = db.workoutDao().getTemplateById(templateId);
            if (currentTemplate != null) {
                runOnUiThread(() -> {
                    editTextTemplateName.setText(currentTemplate.name);
                });
            }
        });
    }

    private void setupNameChangeListener() {
        editTextTemplateName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newName = s.toString().trim();
                if (newName.isEmpty()) {
                    editTextTemplateName.setError("Name cannot be empty");
                    return;
                }
                updateTemplateName(newName);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateTemplateName(String newName) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (currentTemplate != null) {
                currentTemplate.name = newName;
                db.workoutDao().updateTemplate(currentTemplate);
            }
        });
    }

    private void loadExercises() {
        Executors.newSingleThreadExecutor().execute(() -> {
            exercises = db.workoutDao().getExercisesForTemplateWithDetails(templateId);
            runOnUiThread(() -> {
                adapter = new EditExerciseAdapter(exercises, new EditExerciseAdapter.OnExerciseActionListener() {
                    @Override
                    public void onDelete(TemplateExerciseWithDetails item) {
                        deleteExercise(item);
                    }

                    @Override
                    public void onUpdate(TemplateExerciseWithDetails item) {
                        updateExerciseInDb(item.templateExercise);
                    }
                });
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void deleteExercise(TemplateExerciseWithDetails item) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.workoutDao().deleteTemplateExercise(item.templateExercise);
            loadExercises();
        });
    }

    private void updateExerciseInDb(TemplateExercise templateExercise) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.workoutDao().updateTemplateExercise(templateExercise);
        });
    }

    private void showAddExerciseDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_exercise, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        EditText searchInput = view.findViewById(R.id.editTextSearchExercise);
        RecyclerView allExercisesRecycler = view.findViewById(R.id.recyclerViewAllExercises);
        Button btnClose = view.findViewById(R.id.buttonCloseDialog);

        allExercisesRecycler.setLayoutManager(new LinearLayoutManager(this));
        allExercisesRecycler.setHasFixedSize(true);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Exercise> allExercises = db.exerciseDao().getAllExercises();
            runOnUiThread(() -> {
                SearchExerciseAdapter searchAdapter = new SearchExerciseAdapter(allExercises, exercise -> {
                    addExerciseToTemplate(exercise);
                    dialog.dismiss();
                });
                allExercisesRecycler.setAdapter(searchAdapter);

                searchInput.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        searchAdapter.filter(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            });
        });

        dialog.show();
    }

    private void addExerciseToTemplate(Exercise exercise) {
        Executors.newSingleThreadExecutor().execute(() -> {
            TemplateExercise newEx = new TemplateExercise(templateId, exercise.apiId, 3, 10, 0, 60);
            db.workoutDao().insertTemplateExercise(newEx);
            loadExercises();
        });
    }
}
