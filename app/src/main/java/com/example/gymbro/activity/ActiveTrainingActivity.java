package com.example.gymbro.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gymbro.BuildConfig;
import com.example.gymbro.R;
import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.entity.MeasureType;
import com.example.gymbro.db.entity.SessionExercise;
import com.example.gymbro.db.entity.SessionSet;
import com.example.gymbro.db.entity.WorkoutSession;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;
import com.example.gymbro.network.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import coil.ComponentRegistry;
import coil.ImageLoader;
import coil.decode.GifDecoder;
import coil.decode.ImageDecoderDecoder;
import coil.request.ImageRequest;

public class ActiveTrainingActivity extends AppCompatActivity {

    private static final String TAG = "ActiveTraining";
    private TextView textExerciseName, textMuscleGroup, textExerciseDetails, textTimeDisplay;
    private View layoutInputs, layoutTimerContainer, layoutActionArea;
    private ImageView imageExerciseGif;
    private Button buttonNext, buttonStopTimer, buttonRestPlus, buttonRestMinus, buttonSkipSet, buttonAddSet;
    
    private NumberInputView inputWeight, inputReps, inputDistance, inputDuration;

    private AppDatabase db;
    private List<TemplateExerciseWithDetails> exercises = new ArrayList<>();
    private int currentExerciseIndex = 0;
    private int currentSet = 1;
    private boolean isResting = false;
    private ImageLoader gifLoader;
    private static final String PROXY_GIF_URL = "http://" + BuildConfig.PROXY_HOST_LAN + ":" + BuildConfig.PROXY_PORT + "/api/image?exerciseId=";

    // Data collection for history
    private List<SessionSet> performedSets = new ArrayList<>();
    private Map<Integer, Integer> originalTargetSetsMap = new HashMap<>();

    // Timer logic
    private long startTime = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean isTimerRunning = false;
    private int elapsedSeconds = 0;
    private int restSecondsRemaining = 0;

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

        // Setup Hold-to-Confirm buttons with 2 seconds duration
        setupHoldButton(buttonNext, R.color.green_button, R.color.green_button_light, this::handleNext);
        setupHoldButton(buttonStopTimer, R.color.red_button, R.color.red_button_light, this::stopStopwatch);
        setupHoldButton(buttonSkipSet, R.color.grey_button, R.color.grey_button_light, this::skipSetAction);
        
        buttonAddSet.setOnClickListener(v -> addExtraSet());
        
        buttonRestPlus.setOnClickListener(v -> adjustRest(10));
        buttonRestMinus.setOnClickListener(v -> adjustRest(-10));

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
        textTimeDisplay = findViewById(R.id.textTimeDisplay);
        imageExerciseGif = findViewById(R.id.imageExerciseGif);
        buttonNext = findViewById(R.id.buttonNext);
        buttonStopTimer = findViewById(R.id.buttonStopTimer);
        layoutInputs = findViewById(R.id.layoutInputs);
        layoutActionArea = findViewById(R.id.layoutActionArea);
        
        layoutTimerContainer = findViewById(R.id.layoutTimerContainer);
        buttonRestPlus = findViewById(R.id.buttonRestPlus);
        buttonRestMinus = findViewById(R.id.buttonRestMinus);

        buttonSkipSet = findViewById(R.id.buttonSkipSet);
        buttonAddSet = findViewById(R.id.buttonAddSet);

        inputWeight = new NumberInputView(findViewById(R.id.inputWeight), "Weight", 0.0, 999.9, 2.5);
        inputReps = new NumberInputView(findViewById(R.id.inputReps), "Reps", 0.0, 999.0, 1.0);
        inputDistance = new NumberInputView(findViewById(R.id.inputDistance), "Dist", 0.0, 999.9, 0.1);
        inputDuration = new NumberInputView(findViewById(R.id.inputDuration), "Dur", 0.0, 14400.0, 1.0);
    }

    private void setupHoldButton(Button button, int normalColorRes, int lightColorRes, Runnable action) {
        int normalColor = ContextCompat.getColor(this, normalColorRes);
        int lightColor = ContextCompat.getColor(this, lightColorRes);
        float cornerRadius = 12 * getResources().getDisplayMetrics().density;

        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setColor(normalColor);
        normalBg.setCornerRadius(cornerRadius);

        GradientDrawable lightBg = new GradientDrawable();
        lightBg.setColor(lightColor);
        lightBg.setCornerRadius(cornerRadius);

        GradientDrawable progressDrawable = new GradientDrawable();
        progressDrawable.setColor(normalColor);
        progressDrawable.setCornerRadius(cornerRadius);
        
        ClipDrawable clipProgress = new ClipDrawable(progressDrawable, Gravity.START, ClipDrawable.HORIZONTAL);
        LayerDrawable progressLayer = new LayerDrawable(new Drawable[]{lightBg, clipProgress});

        button.setBackground(normalBg);

        button.setOnTouchListener(new View.OnTouchListener() {
            private ValueAnimator animator;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    button.setBackground(progressLayer);
                    animator = ValueAnimator.ofInt(0, 10000);
                    animator.setDuration(2000); // 2 seconds hold time
                    animator.addUpdateListener(animation -> {
                        int level = (int) animation.getAnimatedValue();
                        progressLayer.setLevel(level);
                        button.invalidate();
                    });
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (progressLayer.getLevel() >= 10000) {
                                reset();
                                v.performClick();
                                action.run();
                            }
                        }
                    });
                    animator.start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    reset();
                    return true;
                }
                return false;
            }

            private void reset() {
                if (animator != null) animator.cancel();
                progressLayer.setLevel(0);
                button.setBackground(normalBg);
                button.invalidate();
            }
        });
    }

    private void recordCurrentSet(boolean skipped) {
        TemplateExerciseWithDetails current = exercises.get(currentExerciseIndex);
        int originalTarget = originalTargetSetsMap.getOrDefault(currentExerciseIndex, current.templateExercise.targetSets);
        boolean isExtra = currentSet > originalTarget;
        
        // Don't record if both extra and skipped
        if (isExtra && skipped) return;

        SessionSet set = new SessionSet(
                0, // temp
                currentSet,
                (int) inputReps.getValue(),
                inputWeight.getValue(),
                (int) inputDuration.getValue(),
                inputDistance.getValue(),
                isExtra,
                skipped
        );
        // We use set.id to temporarily store the exercise index for mapping during save
        set.id = currentExerciseIndex;
        performedSets.add(set);
    }

    private void skipSetAction() {
        if (exercises.isEmpty()) return;
        TemplateExerciseWithDetails current = exercises.get(currentExerciseIndex);
        if (isResting) {
            handleNext(); // Skips rest
        } else {
            // Move to rest/next set without saving data
            if (currentSet >= current.templateExercise.targetSets && currentExerciseIndex >= exercises.size() - 1) {
                finishWorkout();
            } else {
                isResting = true;
                updateUI();
            }
        }
    }

    private void addExtraSet() {
        if (exercises.isEmpty()) return;
        TemplateExerciseWithDetails current = exercises.get(currentExerciseIndex);
        // Store original target if not already stored
        if (!originalTargetSetsMap.containsKey(currentExerciseIndex)) {
            originalTargetSetsMap.put(currentExerciseIndex, current.templateExercise.targetSets);
        }
        current.templateExercise.targetSets++;
        updateUI();
        Toast.makeText(this, "Extra set added", Toast.LENGTH_SHORT).show();
    }

    private void loadExercises(int templateId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            exercises = db.workoutDao().getExercisesForTemplateWithDetails(templateId);
            for (int i = 0; i < exercises.size(); i++) {
                originalTargetSetsMap.put(i, exercises.get(i).templateExercise.targetSets);
            }
            runOnUiThread(this::updateUI);
        });
    }

    private void finishWorkout() {
        new AlertDialog.Builder(this)
                .setTitle("Workout Completed!")
                .setMessage("Saving results...")
                .setCancelable(false)
                .show();

        Executors.newSingleThreadExecutor().execute(() -> {
            int templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);
            long sessionId = db.historyDao().insertSession(new WorkoutSession(templateId, System.currentTimeMillis()));

            for (int i = 0; i < exercises.size(); i++) {
                TemplateExerciseWithDetails ex = exercises.get(i);
                SessionExercise sessionEx = new SessionExercise((int) sessionId, ex.exercise.apiId, 0, 0, 0.0, 0, 0.0, 0);
                long sessionExId = db.historyDao().insertSessionExercise(sessionEx);

                for (SessionSet set : performedSets) {
                    if (set.id == i) { // Mapping by temp index
                        set.sessionExerciseId = (int) sessionExId;
                        set.id = 0; // Reset for auto-gen
                        db.historyDao().insertSessionSet(set);
                    }
                }
            }

            runOnUiThread(() -> {
                Intent mainIntent = new Intent(ActiveTrainingActivity.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mainIntent);
                Intent statsIntent = new Intent(ActiveTrainingActivity.this, StatisticsActivity.class);
                statsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(statsIntent);
                finish();
            });
        });
    }

    private void handleNext() {
        if (exercises.isEmpty()) return;
        
        if (!isResting && !validateCurrentInputs()) {
            Toast.makeText(this, "Check your inputs", Toast.LENGTH_SHORT).show();
            return;
        }

        TemplateExerciseWithDetails current = exercises.get(currentExerciseIndex);

        if (isResting) {
            stopRestTimer();
            startNextSet(current);
        } else {
            recordCurrentSet(false);
            if (isTimerRunning) {
                stopStopwatch();
                return;
            }
            if (currentSet >= current.templateExercise.targetSets && currentExerciseIndex == exercises.size() - 1) {
                finishWorkout();
            } else {
                isResting = true;
                updateUI();
            }
        }
    }

    private boolean validateCurrentInputs() {
        TemplateExerciseWithDetails current = exercises.get(currentExerciseIndex);
        MeasureType type = current.exercise.measureType != null ? current.exercise.measureType : MeasureType.WEIGHT_REPS;

        switch (type) {
            case WEIGHT_REPS:
                return inputWeight.isValid() && inputReps.isValid();
            case BODYWEIGHT_REPS:
                return inputReps.isValid();
            case DURATION:
                return inputDuration.isValid();
            case DISTANCE_TIME:
                return inputDistance.isValid() && inputDuration.isValid();
            default:
                return true;
        }
    }

    private void startNextSet(TemplateExerciseWithDetails current) {
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
    }

    private void updateUI() {
        if (currentExerciseIndex >= exercises.size()) return;

        TemplateExerciseWithDetails current = exercises.get(currentExerciseIndex);
        MeasureType type = current.exercise.measureType != null ? current.exercise.measureType : MeasureType.WEIGHT_REPS;

        if (isResting) {
            stopStopwatch();
            textExerciseName.setText("Rest Period");
            textMuscleGroup.setVisibility(View.GONE);
            imageExerciseGif.setVisibility(View.GONE);
            findViewById(R.id.layoutInputsContainer).setVisibility(View.GONE);
            buttonStopTimer.setVisibility(View.GONE);

            // Hide extra actions during rest
            buttonSkipSet.setVisibility(View.GONE);
            buttonAddSet.setVisibility(View.GONE);
            
            if (currentSet < current.templateExercise.targetSets) {
                textExerciseDetails.setText("Next: " + current.exercise.name + " (Set " + (currentSet + 1) + ")");
            } else if (currentExerciseIndex + 1 < exercises.size()) {
                TemplateExerciseWithDetails nextEx = exercises.get(currentExerciseIndex + 1);
                textExerciseDetails.setText("Next: " + nextEx.exercise.name + " (Set 1)");
            }

            layoutTimerContainer.setVisibility(View.VISIBLE);
            buttonRestPlus.setVisibility(View.VISIBLE);
            buttonRestMinus.setVisibility(View.VISIBLE);
            startRestTimer(current.templateExercise.restSeconds);

            buttonNext.setVisibility(View.VISIBLE);
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

            layoutTimerContainer.setVisibility(View.GONE);
            buttonRestPlus.setVisibility(View.GONE);
            buttonRestMinus.setVisibility(View.GONE);
            stopRestTimer();

            setupInputs(current, type);
            
            // Show extra actions during exercise
            buttonSkipSet.setVisibility(View.VISIBLE);
            buttonAddSet.setVisibility(View.VISIBLE);

            if (type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) {
                startStopwatch();
                buttonStopTimer.setVisibility(View.VISIBLE);
                buttonNext.setVisibility(View.GONE);
            } else {
                buttonStopTimer.setVisibility(View.GONE);
                buttonNext.setVisibility(View.VISIBLE);
                buttonNext.setText("Next Set");
            }

            // Target description string
            StringBuilder detailsBuilder = new StringBuilder();
            detailsBuilder.append("Set ").append(currentSet).append("/").append(current.templateExercise.targetSets);
            switch (type) {
                case WEIGHT_REPS:
                    if (current.templateExercise.targetWeight > 0) detailsBuilder.append(" • ").append(current.templateExercise.targetWeight).append(" kg");
                    detailsBuilder.append(" • ").append(current.templateExercise.targetReps).append(" reps");
                    break;
                case BODYWEIGHT_REPS:
                    detailsBuilder.append(" • ").append(current.templateExercise.targetReps).append(" reps");
                    break;
                case DURATION:
                    detailsBuilder.append(" • ").append(current.templateExercise.targetDuration).append("s");
                    break;
                case DISTANCE_TIME:
                    if (current.templateExercise.targetDistance > 0) detailsBuilder.append(" • ").append(current.templateExercise.targetDistance).append(" km");
                    detailsBuilder.append(" • ").append((int)current.templateExercise.targetDuration).append("s");
                    break;
            }
            textExerciseDetails.setText(detailsBuilder.toString());
        }
    }

    private void setupInputs(TemplateExerciseWithDetails item, MeasureType type) {
        findViewById(R.id.layoutInputsContainer).setVisibility(View.VISIBLE);
        inputWeight.hide();
        inputReps.hide();
        inputDistance.hide();
        inputDuration.hide();

        switch (type) {
            case WEIGHT_REPS:
                inputWeight.show(item.templateExercise.targetWeight);
                inputReps.show(item.templateExercise.targetReps);
                break;
            case BODYWEIGHT_REPS:
                inputReps.show(item.templateExercise.targetReps);
                break;
            case DURATION:
                break;
            case DISTANCE_TIME:
                inputDistance.show(item.templateExercise.targetDistance);
                break;
        }
    }

    private void startStopwatch() {
        if (isTimerRunning) return;
        isTimerRunning = true;
        startTime = System.currentTimeMillis();

        layoutTimerContainer.setVisibility(View.VISIBLE);
        buttonRestPlus.setVisibility(View.GONE);
        buttonRestMinus.setVisibility(View.GONE);

        buttonStopTimer.setVisibility(View.VISIBLE);
        buttonNext.setVisibility(View.GONE);
        timerHandler.post(timerRunnable);
    }

    private void stopStopwatch() {
        if (!isTimerRunning) return;
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);

        layoutTimerContainer.setVisibility(View.GONE);
        buttonStopTimer.setVisibility(View.GONE);
        buttonNext.setVisibility(View.VISIBLE);
        buttonNext.setText("Next Set");

        // Finalize duration input after stop
        inputDuration.show((double) elapsedSeconds);
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            elapsedSeconds = (int) (millis / 1000);
            textTimeDisplay.setText(elapsedSeconds + "s");
            timerHandler.postDelayed(this, 100);
        }
    };

    private void startRestTimer(int seconds) {
        restSecondsRemaining = seconds;
        textTimeDisplay.setText(restSecondsRemaining + "s");
        timerHandler.removeCallbacks(restRunnable);
        timerHandler.postDelayed(restRunnable, 1000);
    }

    private void stopRestTimer() {
        timerHandler.removeCallbacks(restRunnable);
    }

    private void adjustRest(int delta) {
        restSecondsRemaining += delta;
        if (restSecondsRemaining < 0) restSecondsRemaining = 0;
        textTimeDisplay.setText(restSecondsRemaining + "s");
    }

    private Runnable restRunnable = new Runnable() {
        @Override
        public void run() {
            if (restSecondsRemaining > 0) {
                restSecondsRemaining--;
                textTimeDisplay.setText(restSecondsRemaining + "s");
                timerHandler.postDelayed(this, 1000);
            } else {
                handleNext();
            }
        }
    };

    private void initGifLoader() {
        ComponentRegistry.Builder componentsBuilder = new ComponentRegistry.Builder();
        if (Build.VERSION.SDK_INT >= 28) {
            componentsBuilder.add(new ImageDecoderDecoder.Factory());
        } else {
            componentsBuilder.add(new GifDecoder.Factory());
        }
        gifLoader = new ImageLoader.Builder(this)
                .okHttpClient(RetrofitClient.getOkHttpClient(this))
                .components(componentsBuilder.build())
                .build();
    }

    private void loadGif(String apiId) {
        if (apiId == null || apiId.isEmpty()) return;
        String url = PROXY_GIF_URL + apiId;
        ImageRequest request = new ImageRequest.Builder(this)
                .data(url).target(imageExerciseGif).crossfade(true).build();
        gifLoader.enqueue(request);
    }

    private class NumberInputView {
        private final View root;
        private final EditText editText;
        private final TextView label;
        private double min, max, step;

        public NumberInputView(View root, String labelText, double min, double max, double step) {
            this.root = root;
            this.min = min;
            this.max = max;
            this.step = step;
            this.label = root.findViewById(R.id.textLabel);
            if (this.label != null) {
                this.label.setText(labelText);
            }
            this.editText = root.findViewById(R.id.editTextValue);

            root.findViewById(R.id.btnUp).setOnClickListener(v -> adjust(true));
            root.findViewById(R.id.btnDown).setOnClickListener(v -> adjust(false));

            this.editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validate();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        public void show(double initialValue) {
            root.setVisibility(View.VISIBLE);
            setValue(initialValue);
        }

        public void hide() {
            root.setVisibility(View.GONE);
        }

        public void setValue(double val) {
            if (step >= 1.0) {
                editText.setText(String.valueOf((int) val));
            } else {
                editText.setText(String.format(Locale.US, "%.2f", val));
            }
        }

        public double getValue() {
            String s = editText.getText().toString().replace(',', '.');
            try {
                return s.isEmpty() ? 0 : Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public boolean isValid() {
            double val = getValue();
            return val >= min && val <= max;
        }

        private void validate() {
            if (!isValid()) {
                editText.setError("Range: " + min + "-" + max);
            } else {
                editText.setError(null);
            }
        }

        private void adjust(boolean up) {
            double current = getValue();
            double next = up ? (current + step) : (current - step);
            if (next >= min && next <= max) {
                setValue(next);
            }
        }
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Quit Training?")
                .setMessage("Are you sure you want to interrupt your workout?")
                .setPositiveButton("Yes, Quit", (dialog, which) -> finish())
                .setNegativeButton("No, Continue", null).show();
    }
}
