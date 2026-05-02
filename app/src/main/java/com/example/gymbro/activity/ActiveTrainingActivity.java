package com.example.gymbro.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gymbro.BuildConfig;
import com.example.gymbro.R;
import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.entity.MeasureType;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;
import com.example.gymbro.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import coil.ComponentRegistry;
import coil.ImageLoader;
import coil.decode.GifDecoder;
import coil.decode.ImageDecoderDecoder;
import coil.request.ImageRequest;

public class ActiveTrainingActivity extends AppCompatActivity {

    private static final String TAG = "ActiveTraining";
    private TextView textExerciseName, textMuscleGroup, textExerciseDetails, textTimerLabel, textTimerValue;
    private ImageView imageExerciseGif;
    private Button buttonNext;
    private AppDatabase db;
    private List<TemplateExerciseWithDetails> exercises = new ArrayList<>();
    private int currentExerciseIndex = 0;
    private int currentSet = 1;
    private boolean isResting = false;
    private ImageLoader gifLoader;
    private static final String PROXY_GIF_URL = "http://" + BuildConfig.PROXY_HOST_LAN + ":" + BuildConfig.PROXY_PORT + "/api/image?exerciseId=";

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

        initViews();
        initGifLoader();
        
        db = AppDatabase.getDatabase(this);
        int templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);

        if (templateId != -1) {
            loadExercises(templateId);
        }

        buttonNext.setOnClickListener(v -> handleNext());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmation();
            }
        });
    }

    private void initViews() {
        textExerciseName = findViewById(R.id.textExerciseName);
        textMuscleGroup = findViewById(R.id.textMuscleGroup);
        textExerciseDetails = findViewById(R.id.textExerciseDetails);
        textTimerLabel = findViewById(R.id.textTimerLabel);
        textTimerValue = findViewById(R.id.textTimerValue);
        imageExerciseGif = findViewById(R.id.imageExerciseGif);
        buttonNext = findViewById(R.id.buttonNext);
    }

    private void initGifLoader() {
        ComponentRegistry.Builder componentsBuilder = new ComponentRegistry.Builder();
        if (Build.VERSION.SDK_INT >= 28) {
            componentsBuilder.add(new ImageDecoderDecoder.Factory());
        } else {
            componentsBuilder.add(new GifDecoder.Factory());
        }

        gifLoader = new ImageLoader.Builder(this)
                .okHttpClient(RetrofitClient.getOkHttpClient(this)) // Use cached client
                .components(componentsBuilder.build())
                .build();
    }

    private void loadExercises(int templateId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            exercises = db.workoutDao().getExercisesForTemplateWithDetails(templateId);
            runOnUiThread(this::updateUI);
        });
    }

    private void loadGif(String apiId) {
        if (apiId == null || apiId.isEmpty()) {
            Log.w(TAG, "Cannot load GIF: apiId is empty");
            return;
        }

        String url = PROXY_GIF_URL + apiId;
        Log.d(TAG, "Loading GIF via proxy: " + url);

        ImageRequest request = new ImageRequest.Builder(this)
                .data(url)
                .target(imageExerciseGif)
                .crossfade(true)
                .listener(new ImageRequest.Listener() {
                    @Override
                    public void onError(ImageRequest request, coil.request.ErrorResult result) {
                        Log.e(TAG, "Error loading GIF from proxy: " + result.getThrowable().getMessage());
                    }
                })
                .build();

        gifLoader.enqueue(request);
    }

    private void handleNext() {
        if (exercises.isEmpty()) return;

        TemplateExerciseWithDetails current = exercises.get(currentExerciseIndex);

        if (isResting) {
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
            textMuscleGroup.setVisibility(View.GONE);
            imageExerciseGif.setVisibility(View.GONE);
            
            if (currentSet < current.templateExercise.targetSets) {
                textExerciseDetails.setText("Next: " + current.exercise.name + " (Set " + (currentSet + 1) + ")");
            } else if (currentExerciseIndex + 1 < exercises.size()) {
                TemplateExerciseWithDetails nextEx = exercises.get(currentExerciseIndex + 1);
                textExerciseDetails.setText("Next: " + nextEx.exercise.name + " (Set 1)");
            }

            textTimerLabel.setVisibility(View.VISIBLE);
            textTimerValue.setText(current.templateExercise.restSeconds + "s");
            buttonNext.setText("Skip Rest");
        } else {
            textExerciseName.setText(current.exercise.name);
            textMuscleGroup.setVisibility(View.VISIBLE);
            
            StringBuilder muscles = new StringBuilder(current.exercise.target);
            if (current.exercise.secondaryMuscles != null && !current.exercise.secondaryMuscles.isEmpty()) {
                muscles.append(" | ").append(TextUtils.join(", ", current.exercise.secondaryMuscles));
            }
            textMuscleGroup.setText(muscles.toString());
            
            imageExerciseGif.setVisibility(View.VISIBLE);

            loadGif(current.exercise.apiId);

            // Format target info based on MeasureType
            MeasureType type = current.exercise.measureType;
            if (type == null) type = MeasureType.WEIGHT_REPS;
            
            StringBuilder detailsBuilder = new StringBuilder();
            detailsBuilder.append("Set ").append(currentSet).append("/").append(current.templateExercise.targetSets);
            
            switch (type) {
                case WEIGHT_REPS:
                    if (current.templateExercise.targetWeight > 0) {
                        detailsBuilder.append(" • ").append(current.templateExercise.targetWeight).append(" kg");
                    }
                    detailsBuilder.append(" • ").append(current.templateExercise.targetReps).append(" reps");
                    break;
                case BODYWEIGHT_REPS:
                    detailsBuilder.append(" • ").append(current.templateExercise.targetReps).append(" reps");
                    break;
                case DURATION:
                    detailsBuilder.append(" • ").append(current.templateExercise.targetDuration).append("s");
                    break;
                case DISTANCE_TIME:
                    if (current.templateExercise.targetDistance > 0) {
                        detailsBuilder.append(" • ").append(current.templateExercise.targetDistance).append(" km");
                    }
                    detailsBuilder.append(" • ").append(current.templateExercise.targetDuration).append("s");
                    break;
            }
            
            textExerciseDetails.setText(detailsBuilder.toString());
            textTimerLabel.setVisibility(View.INVISIBLE);
            textTimerValue.setText("00:00");
            buttonNext.setText("Next Set");
        }
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Quit Training?")
                .setMessage("Are you sure you want to interrupt your workout?")
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
                    Intent mainIntent = new Intent(ActiveTrainingActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(mainIntent);
                    
                    Intent statsIntent = new Intent(ActiveTrainingActivity.this, StatisticsActivity.class);
                    statsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(statsIntent);
                    finish();
                })
                .show();
    }
}
