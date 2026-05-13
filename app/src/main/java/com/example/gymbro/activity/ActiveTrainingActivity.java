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
import androidx.lifecycle.ViewModelProvider;

import com.example.gymbro.BuildConfig;
import com.example.gymbro.R;
import com.example.gymbro.db.entity.MeasureType;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;
import com.example.gymbro.network.RetrofitClient;
import com.example.gymbro.viewmodel.ActiveTrainingViewModel;

import java.util.Locale;

import coil.ComponentRegistry;
import coil.ImageLoader;
import coil.decode.GifDecoder;
import coil.decode.ImageDecoderDecoder;
import coil.request.ImageRequest;

public class ActiveTrainingActivity extends AppCompatActivity {

    private TextView textExerciseName, textMuscleGroup, textExerciseDetails, textTimeDisplay;
    private View layoutTimerContainer, layoutInputsContainer;
    private ImageView imageExerciseGif;
    private Button buttonNext, buttonStopTimer, buttonRestPlus, buttonRestMinus, buttonSkipSet, buttonAddSet;
    
    private NumberInputView inputWeight, inputReps, inputDistance, inputDuration;

    private ActiveTrainingViewModel viewModel;
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

        viewModel = new ViewModelProvider(this).get(ActiveTrainingViewModel.class);

        initViews();
        initGifLoader();
        setupObservers();

        int templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);
        if (templateId != -1) {
            viewModel.loadExercises(templateId);
        }

        setupHoldButton(buttonNext, R.color.green_button, R.color.green_button_light, this::handleNextAction);
        setupHoldButton(buttonStopTimer, R.color.red_button, R.color.red_button_light, viewModel::stopStopwatch);
        setupHoldButton(buttonSkipSet, R.color.grey_button, R.color.grey_button_light, this::skipSetAction);
        
        buttonAddSet.setOnClickListener(v -> viewModel.addExtraSet());
        buttonRestPlus.setOnClickListener(v -> viewModel.adjustRest(10));
        buttonRestMinus.setOnClickListener(v -> viewModel.adjustRest(-10));

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
        layoutTimerContainer = findViewById(R.id.layoutTimerContainer);
        layoutInputsContainer = findViewById(R.id.layoutInputsContainer);
        buttonRestPlus = findViewById(R.id.buttonRestPlus);
        buttonRestMinus = findViewById(R.id.buttonRestMinus);
        buttonSkipSet = findViewById(R.id.buttonSkipSet);
        buttonAddSet = findViewById(R.id.buttonAddSet);

        // Set minimums: Weight 1.0, Reps 1.0, Distance 0.1, Duration 5
        inputWeight = new NumberInputView(findViewById(R.id.inputWeight), "Weight", 1.0, 999.9, 1.0);
        inputReps = new NumberInputView(findViewById(R.id.inputReps), "Reps", 1.0, 999.0, 1.0);
        inputDistance = new NumberInputView(findViewById(R.id.inputDistance), "Dist", 0.1, 999.9, 0.1);
        inputDuration = new NumberInputView(findViewById(R.id.inputDuration), "Dur", 5.0, 14400.0, 1.0);
    }

    private void setupObservers() {
        viewModel.getExercises().observe(this, list -> updateUIState());
        viewModel.getCurrentExerciseIndex().observe(this, idx -> updateUIState());
        viewModel.getCurrentSet().observe(this, set -> updateUIState());
        viewModel.isResting().observe(this, resting -> updateUIState());
        viewModel.getIsStopwatchRunning().observe(this, running -> updateUIState());

        viewModel.getElapsedSeconds().observe(this, seconds -> {
            if (!Boolean.TRUE.equals(viewModel.isResting().getValue())) {
                textTimeDisplay.setText(seconds + "s");
                TemplateExerciseWithDetails current = getCurrentExercise();
                if (current != null && (current.exercise.measureType == MeasureType.DURATION || current.exercise.measureType == MeasureType.DISTANCE_TIME)) {
                    inputDuration.setValue(seconds);
                }
            }
        });

        viewModel.getRestSecondsRemaining().observe(this, seconds -> {
            if (Boolean.TRUE.equals(viewModel.isResting().getValue())) {
                textTimeDisplay.setText(seconds + "s");
                if (seconds != null && seconds == 0) {
                    handleNextAction();
                }
            }
        });
    }

    private void updateUIState() {
        TemplateExerciseWithDetails current = getCurrentExercise();
        if (current == null) return;

        boolean isResting = Boolean.TRUE.equals(viewModel.isResting().getValue());
        MeasureType type = current.exercise.measureType != null ? current.exercise.measureType : MeasureType.WEIGHT_REPS;

        if (isResting) {
            textExerciseName.setText("Rest Period");
            textMuscleGroup.setVisibility(View.GONE);
            imageExerciseGif.setVisibility(View.GONE);
            layoutInputsContainer.setVisibility(View.GONE);
            buttonStopTimer.setVisibility(View.GONE);
            buttonSkipSet.setVisibility(View.GONE);
            buttonAddSet.setVisibility(View.GONE);
            layoutTimerContainer.setVisibility(View.VISIBLE);
            buttonRestPlus.setVisibility(View.VISIBLE);
            buttonRestMinus.setVisibility(View.VISIBLE);
            buttonNext.setText("Skip Rest");
            buttonNext.setVisibility(View.VISIBLE);
            
            int set = viewModel.getCurrentSet().getValue();
            if (set < current.templateExercise.targetSets) {
                textExerciseDetails.setText("Next: " + current.exercise.name + " (Set " + (set + 1) + ")");
            } else {
                int nextIdx = viewModel.getCurrentExerciseIndex().getValue() + 1;
                if (nextIdx < viewModel.getExercises().getValue().size()) {
                    textExerciseDetails.setText("Next: " + viewModel.getExercises().getValue().get(nextIdx).exercise.name);
                }
            }
        } else {
            textExerciseName.setText(current.exercise.name);
            textMuscleGroup.setVisibility(View.VISIBLE);
            imageExerciseGif.setVisibility(View.VISIBLE);
            loadGif(current.exercise.apiId);
            layoutInputsContainer.setVisibility(View.VISIBLE);
            buttonSkipSet.setVisibility(View.VISIBLE);
            buttonAddSet.setVisibility(View.VISIBLE);
            layoutTimerContainer.setVisibility((type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) ? View.VISIBLE : View.GONE);
            buttonRestPlus.setVisibility(View.GONE);
            buttonRestMinus.setVisibility(View.GONE);

            setupInputs(current, type);

            if (type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) {
                boolean isRunning = Boolean.TRUE.equals(viewModel.getIsStopwatchRunning().getValue());
                if (isRunning) {
                    buttonStopTimer.setVisibility(View.VISIBLE);
                    buttonNext.setVisibility(View.GONE);
                    inputDuration.hide();
                } else {
                    buttonStopTimer.setVisibility(View.GONE);
                    buttonNext.setVisibility(View.VISIBLE);
                    buttonNext.setText("Next Set");
                    
                    Integer elapsed = viewModel.getElapsedSeconds().getValue();
                    inputDuration.show(elapsed != null ? elapsed : 0);
                    
                    // Issue 5: Auto-start if it was reset and not running
                    if (elapsed != null && elapsed == 0) {
                        viewModel.startStopwatch();
                    }
                }
            } else {
                buttonStopTimer.setVisibility(View.GONE);
                buttonNext.setVisibility(View.VISIBLE);
                buttonNext.setText("Next Set");
            }
            
            textExerciseDetails.setText(String.format(Locale.US, "Set %d/%d", viewModel.getCurrentSet().getValue(), current.templateExercise.targetSets));
        }
    }

    private void handleNextAction() {
        if (Boolean.TRUE.equals(viewModel.isResting().getValue())) {
            viewModel.handleNext(this::finishWorkout, null);
        } else {
            if (!validateCurrentInputs()) {
                Toast.makeText(this, "Check inputs", Toast.LENGTH_SHORT).show();
                return;
            }
            if (inputDuration.getValue() == 0) {
                inputDuration.setValue(viewModel.getElapsedSeconds().getValue());
            }

            viewModel.recordSet((int) inputReps.getValue(), inputWeight.getValue(), (int) inputDuration.getValue(), inputDistance.getValue(), false);
            
            TemplateExerciseWithDetails current = getCurrentExercise();
            viewModel.handleNext(this::finishWorkout, () -> viewModel.startRestTimer(current.templateExercise.restSeconds));
        }
    }

    private void skipSetAction() {
        if (Boolean.TRUE.equals(viewModel.isResting().getValue())) {
            handleNextAction();
        } else {
            viewModel.recordSet(0, 0, 0, 0, true);
            TemplateExerciseWithDetails current = getCurrentExercise();
            viewModel.handleNext(this::finishWorkout, () -> viewModel.startRestTimer(current.templateExercise.restSeconds));
        }
    }

    private TemplateExerciseWithDetails getCurrentExercise() {
        int idx = viewModel.getCurrentExerciseIndex().getValue() != null ? viewModel.getCurrentExerciseIndex().getValue() : 0;
        if (viewModel.getExercises().getValue() != null && idx < viewModel.getExercises().getValue().size()) {
            return viewModel.getExercises().getValue().get(idx);
        }
        return null;
    }

    private void finishWorkout() {
        new AlertDialog.Builder(this).setTitle("Workout Completed!").setMessage("Saving...").setCancelable(false).show();
        viewModel.finishWorkout(getIntent().getIntExtra("TEMPLATE_ID", -1), () -> runOnUiThread(() -> {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            startActivity(new Intent(this, StatisticsActivity.class));
            finish();
        }));
    }

    private void setupInputs(TemplateExerciseWithDetails item, MeasureType type) {
        inputWeight.hide(); inputReps.hide(); inputDistance.hide(); inputDuration.hide();
        switch (type) {
            case WEIGHT_REPS: 
                inputWeight.show(item.templateExercise.targetWeight); 
                inputWeight.setMin(1.0);
                inputReps.show(item.templateExercise.targetReps); 
                inputReps.setMin(1.0);
                break;
            case BODYWEIGHT_REPS: 
                inputReps.show(item.templateExercise.targetReps); 
                inputReps.setMin(1.0);
                break;
            case DISTANCE_TIME: 
                inputDistance.show(item.templateExercise.targetDistance); 
                inputDistance.setMin(0.1);
                inputDuration.setMin(5.0);
                break;
            case DURATION:
                inputDuration.setMin(5.0);
                break;
        }
    }

    private boolean validateCurrentInputs() {
        TemplateExerciseWithDetails current = getCurrentExercise();
        if (current == null) return true;
        MeasureType type = current.exercise.measureType != null ? current.exercise.measureType : MeasureType.WEIGHT_REPS;
        switch (type) {
            case WEIGHT_REPS: return inputWeight.isValid() && inputReps.isValid();
            case BODYWEIGHT_REPS: return inputReps.isValid();
            case DURATION: return true;
            case DISTANCE_TIME: return inputDistance.isValid();
            default: return true;
        }
    }

    private void setupHoldButton(Button button, int normalColorRes, int lightColorRes, Runnable action) {
        int normalColor = ContextCompat.getColor(this, normalColorRes);
        int lightColor = ContextCompat.getColor(this, lightColorRes);
        float cornerRadius = 12 * getResources().getDisplayMetrics().density;
        GradientDrawable normalBg = new GradientDrawable(); normalBg.setColor(normalColor); normalBg.setCornerRadius(cornerRadius);
        GradientDrawable lightBg = new GradientDrawable(); lightBg.setColor(lightColor); lightBg.setCornerRadius(cornerRadius);
        GradientDrawable progressDrawable = new GradientDrawable(); progressDrawable.setColor(normalColor); progressDrawable.setCornerRadius(cornerRadius);
        ClipDrawable clipProgress = new ClipDrawable(progressDrawable, Gravity.START, ClipDrawable.HORIZONTAL);
        LayerDrawable progressLayer = new LayerDrawable(new Drawable[]{lightBg, clipProgress});
        button.setBackground(normalBg);
        button.setOnTouchListener(new View.OnTouchListener() {
            private ValueAnimator animator;
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    button.setBackground(progressLayer);
                    animator = ValueAnimator.ofInt(0, 10000); animator.setDuration(2000);
                    animator.addUpdateListener(a -> { progressLayer.setLevel((int) a.getAnimatedValue()); button.invalidate(); });
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override public void onAnimationEnd(Animator a) { if (progressLayer.getLevel() >= 10000) { reset(); action.run(); } }
                    });
                    animator.start(); return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) { reset(); return true; }
                return false;
            }
            private void reset() { if (animator != null) animator.cancel(); progressLayer.setLevel(0); button.setBackground(normalBg); button.invalidate(); }
        });
    }

    private void initGifLoader() {
        ComponentRegistry.Builder cb = new ComponentRegistry.Builder();
        if (Build.VERSION.SDK_INT >= 28) cb.add(new ImageDecoderDecoder.Factory()); else cb.add(new GifDecoder.Factory());
        gifLoader = new ImageLoader.Builder(this).okHttpClient(RetrofitClient.getOkHttpClient(this)).components(cb.build()).build();
    }

    private void loadGif(String apiId) {
        if (apiId == null) return;
        gifLoader.enqueue(new ImageRequest.Builder(this).data(PROXY_GIF_URL + apiId).target(imageExerciseGif).crossfade(true).build());
    }

    private void showExitConfirmation() {
        new AlertDialog.Builder(this).setTitle("Quit Training?").setMessage("Interrupt?").setPositiveButton("Yes", (d, w) -> finish()).setNegativeButton("No", null).show();
    }

    private class NumberInputView {
        private final View root; private final EditText editText; private final TextView label; private double min, max, step;
        public NumberInputView(View root, String labelText, double min, double max, double step) {
            this.root = root; this.min = min; this.max = max; this.step = step;
            this.label = root.findViewById(R.id.textLabel); if (this.label != null) this.label.setText(labelText);
            this.editText = root.findViewById(R.id.editTextValue);
            root.findViewById(R.id.btnUp).setOnClickListener(v -> adjust(true));
            root.findViewById(R.id.btnDown).setOnClickListener(v -> adjust(false));
            this.editText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validate(); }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        public void setMin(double min) { this.min = min; validate(); }
        public void show(double val) { root.setVisibility(View.VISIBLE); setValue(val); }
        public void hide() { root.setVisibility(View.GONE); }
        public void setValue(double val) { editText.setText(step >= 1.0 ? String.valueOf((int) val) : String.format(Locale.US, "%.2f", val)); }
        public double getValue() { try { String s = editText.getText().toString().replace(',', '.'); return s.isEmpty() ? 0 : Double.parseDouble(s); } catch (Exception e) { return 0; } }
        public boolean isValid() { double v = getValue(); return v >= min && v <= max; }
        private void validate() { editText.setError(isValid() ? null : "Range: " + min + "-" + max); }
        private void adjust(boolean up) { double n = up ? (getValue() + step) : (getValue() - step); if (n >= min && n <= max) setValue(n); }
    }
}
