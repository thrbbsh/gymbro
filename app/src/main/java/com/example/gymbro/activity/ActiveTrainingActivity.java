package com.example.gymbro.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gymbro.R;
import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class ActiveTrainingActivity extends AppCompatActivity {

    private TextView textExerciseName, textExerciseDetails, textTimerLabel, textTimerValue;
    private Button buttonNext;
    private AppDatabase db;
    private List<TemplateExerciseWithDetails> exercises = new ArrayList<>();
    private int currentExerciseIndex = 0;
    private int currentSet = 1;
    private boolean isResting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_active_training);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textExerciseName = findViewById(R.id.textExerciseName);
        textExerciseDetails = findViewById(R.id.textExerciseDetails);
        textTimerLabel = findViewById(R.id.textTimerLabel);
        textTimerValue = findViewById(R.id.textTimerValue);
        buttonNext = findViewById(R.id.buttonNext);

        db = AppDatabase.getDatabase(this);
        int templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);

        if (templateId != -1) {
            loadExercises(templateId);
        }

        buttonNext.setOnClickListener(v -> handleNext());

        // Handle back press with confirmation
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmation();
            }
        });
    }

    private void loadExercises(int templateId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            exercises = db.workoutDao().getExercisesForTemplateWithDetails(templateId);
            runOnUiThread(this::updateUI);
        });
    }

    private void handleNext() {
        if (exercises.isEmpty()) return;

        TemplateExerciseWithDetails current = exercises.get(currentExerciseIndex);

        if (isResting) {
            // Finished resting, go to next set or next exercise
            isResting = false;
            if (currentSet < current.templateExercise.targetSets) {
                currentSet++;
            } else {
                currentExerciseIndex++;
                currentSet = 1;
            }
            
            if (currentExerciseIndex >= exercises.size()) {
                finishWorkout();
            } else {
                updateUI();
            }
        } else {
            // Finished a set
            // Check if it was the last set of the last exercise
            if (currentSet == current.templateExercise.targetSets && currentExerciseIndex == exercises.size() - 1) {
                finishWorkout();
            } else {
                isResting = true;
                updateUI();
            }
        }
    }

    private void updateUI() {
        if (currentExerciseIndex >= exercises.size()) return;

        TemplateExerciseWithDetails current = exercises.get(currentExerciseIndex);
        
        if (isResting) {
            textExerciseName.setText("Rest Period");
            
            if (currentSet < current.templateExercise.targetSets) {
                // Next set of the SAME exercise
                textExerciseDetails.setText("Next: " + current.exercise.name + " (Set " + (currentSet + 1) + ")");
            } else if (currentExerciseIndex + 1 < exercises.size()) {
                // Next is the FIRST set of the NEXT exercise
                TemplateExerciseWithDetails nextEx = exercises.get(currentExerciseIndex + 1);
                textExerciseDetails.setText("Next: " + nextEx.exercise.name + " (Set 1)");
            }

            textTimerLabel.setVisibility(View.VISIBLE);
            textTimerValue.setText(current.templateExercise.restSeconds + "s");
            buttonNext.setText("Skip Rest");
        } else {
            textExerciseName.setText(current.exercise.name);
            String details = "Set " + currentSet + "/" + current.templateExercise.targetSets;
            if (current.templateExercise.targetReps > 0) {
                details += " • " + current.templateExercise.targetReps + " reps";
            } else if (current.templateExercise.targetDuration > 0) {
                details += " • " + current.templateExercise.targetDuration + "s";
            }
            textExerciseDetails.setText(details);
            textTimerLabel.setVisibility(View.INVISIBLE);
            textTimerValue.setText("00:00");
            buttonNext.setText("Next Set");
        }
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Quit Training?")
                .setMessage("Are you sure you want to interrupt your workout? Progress will not be saved.")
                .setPositiveButton("Yes, Quit", (dialog, which) -> finish())
                .setNegativeButton("No, Continue", null)
                .show();
    }

    private void finishWorkout() {
        new AlertDialog.Builder(this)
                .setTitle("Workout Completed!")
                .setMessage("Congratulations! You have successfully finished your training session.")
                .setCancelable(false)
                .setPositiveButton("Go to Statistics", (dialog, which) -> {
                    // First, clear everything up to MainActivity to remove ExerciseActivity from stack
                    Intent mainIntent = new Intent(ActiveTrainingActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mainIntent);
                    
                    // Then start StatisticsActivity on top of MainActivity
                    Intent statsIntent = new Intent(ActiveTrainingActivity.this, StatisticsActivity.class);
                    startActivity(statsIntent);

                    finish();
                })
                .show();
    }
}
