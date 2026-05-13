package com.example.gymbro.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymbro.db.entity.MeasureType;
import com.example.gymbro.db.entity.SessionSet;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;
import com.example.gymbro.repository.WorkoutRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveTrainingViewModel extends AndroidViewModel {
    private final WorkoutRepository repository;
    
    private final MutableLiveData<List<TemplateExerciseWithDetails>> exercises = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> currentExerciseIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> currentSet = new MutableLiveData<>(1);
    private final MutableLiveData<Boolean> isResting = new MutableLiveData<>(false);
    
    // Stopwatch
    private final MutableLiveData<Integer> elapsedSeconds = new MutableLiveData<>(0);
    private long stopwatchStartTime = 0;
    private final MutableLiveData<Boolean> isStopwatchRunning = new MutableLiveData<>(false);
    
    // Rest Timer
    private final MutableLiveData<Integer> restSecondsRemaining = new MutableLiveData<>(0);
    private boolean isRestTimerRunning = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    
    // Map exercise index to list of performed sets
    private final Map<Integer, List<SessionSet>> performedSetsMap = new HashMap<>();
    private final Map<Integer, Integer> originalTargetSetsMap = new HashMap<>();

    public ActiveTrainingViewModel(@NonNull Application application) {
        super(application);
        repository = WorkoutRepository.getInstance(application);
    }

    public LiveData<List<TemplateExerciseWithDetails>> getExercises() { return exercises; }
    public LiveData<Integer> getCurrentExerciseIndex() { return currentExerciseIndex; }
    public LiveData<Integer> getCurrentSet() { return currentSet; }
    public LiveData<Boolean> isResting() { return isResting; }
    public LiveData<Integer> getElapsedSeconds() { return elapsedSeconds; }
    public LiveData<Integer> getRestSecondsRemaining() { return restSecondsRemaining; }
    public LiveData<Boolean> getIsStopwatchRunning() { return isStopwatchRunning; }

    public void loadExercises(int templateId) {
        repository.getExercisesForTemplate(templateId, result -> {
            exercises.postValue(result);
            for (int i = 0; i < result.size(); i++) {
                originalTargetSetsMap.put(i, result.get(i).templateExercise.targetSets);
            }
        });
    }

    public void startStopwatch() {
        if (Boolean.TRUE.equals(isStopwatchRunning.getValue())) return;
        isStopwatchRunning.setValue(true);
        int currentElapsed = (elapsedSeconds.getValue() != null) ? elapsedSeconds.getValue() : 0;
        stopwatchStartTime = System.currentTimeMillis() - currentElapsed * 1000L;
        handler.removeCallbacks(stopwatchRunnable);
        handler.post(stopwatchRunnable);
    }

    public void stopStopwatch() {
        isStopwatchRunning.setValue(false);
        handler.removeCallbacks(stopwatchRunnable);
    }

    private final Runnable stopwatchRunnable = new Runnable() {
        @Override
        public void run() {
            if (Boolean.TRUE.equals(isStopwatchRunning.getValue())) {
                int seconds = (int) ((System.currentTimeMillis() - stopwatchStartTime) / 1000);
                // Use setValue to avoid race conditions on Main Thread
                elapsedSeconds.setValue(seconds);
                handler.postDelayed(this, 500);
            }
        }
    };

    public void startRestTimer(int seconds) {
        if (seconds <= 0) {
            isResting.setValue(false);
            restSecondsRemaining.setValue(0);
            return;
        }
        isResting.setValue(true);
        restSecondsRemaining.setValue(seconds);
        isRestTimerRunning = true;
        handler.removeCallbacks(restRunnable);
        handler.postDelayed(restRunnable, 1000);
    }

    public void stopRestTimer() {
        isRestTimerRunning = false;
        handler.removeCallbacks(restRunnable);
    }
    
    public void adjustRest(int delta) {
        Integer current = restSecondsRemaining.getValue();
        int newVal = Math.max(0, (current != null ? current : 0) + delta);
        restSecondsRemaining.setValue(newVal);
    }

    private final Runnable restRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRestTimerRunning) {
                Integer remaining = restSecondsRemaining.getValue();
                if (remaining != null && remaining > 0) {
                    restSecondsRemaining.setValue(remaining - 1);
                    if (remaining - 1 > 0) {
                        handler.postDelayed(this, 1000);
                    } else {
                        isRestTimerRunning = false;
                        restSecondsRemaining.setValue(0);
                    }
                } else {
                    isRestTimerRunning = false;
                    restSecondsRemaining.setValue(0);
                }
            }
        }
    };

    public void recordSet(int reps, double weight, int duration, double distance, boolean skipped) {
        List<TemplateExerciseWithDetails> exList = exercises.getValue();
        Integer exIdx = currentExerciseIndex.getValue();
        Integer setNum = currentSet.getValue();
        
        if (exList == null || exIdx == null || setNum == null) return;
        
        TemplateExerciseWithDetails currentEx = exList.get(exIdx);
        MeasureType type = currentEx.exercise.measureType != null ? currentEx.exercise.measureType : MeasureType.WEIGHT_REPS;
        
        double finalWeight = (type == MeasureType.WEIGHT_REPS) ? weight : 0;
        int finalReps = (type == MeasureType.WEIGHT_REPS || type == MeasureType.BODYWEIGHT_REPS) ? reps : 0;
        int finalDuration = (type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) ? duration : 0;
        double finalDistance = (type == MeasureType.DISTANCE_TIME) ? distance : 0;

        int originalTarget = originalTargetSetsMap.getOrDefault(exIdx, currentEx.templateExercise.targetSets);
        boolean isExtra = setNum > originalTarget;
        
        if (isExtra && skipped) return;

        SessionSet set = new SessionSet(0, setNum, finalReps, finalWeight, finalDuration, finalDistance, isExtra, skipped);
        
        if (!performedSetsMap.containsKey(exIdx)) {
            performedSetsMap.put(exIdx, new ArrayList<>());
        }
        performedSetsMap.get(exIdx).add(set);
    }

    public void addExtraSet() {
        List<TemplateExerciseWithDetails> exList = exercises.getValue();
        Integer exIdx = currentExerciseIndex.getValue();
        if (exList == null || exIdx == null) return;
        
        TemplateExerciseWithDetails currentEx = exList.get(exIdx);
        currentEx.templateExercise.targetSets++;
        exercises.setValue(new ArrayList<>(exList)); 
    }

    public void handleNext(Runnable onFinish, Runnable onStartRest) {
        Boolean resting = isResting.getValue();
        if (Boolean.TRUE.equals(resting)) {
            stopRestTimer();
            isResting.setValue(false);
            advanceSet(onFinish);
        } else {
            List<TemplateExerciseWithDetails> exList = exercises.getValue();
            Integer exIdx = currentExerciseIndex.getValue();
            Integer setNum = currentSet.getValue();
            if (exList == null || exIdx == null || setNum == null) return;

            if (setNum >= exList.get(exIdx).templateExercise.targetSets && exIdx >= exList.size() - 1) {
                onFinish.run();
            } else {
                int restSeconds = exList.get(exIdx).templateExercise.restSeconds;
                if (restSeconds > 0) {
                    onStartRest.run();
                } else {
                    advanceSet(onFinish);
                }
            }
        }
    }

    private void advanceSet(Runnable onFinish) {
        List<TemplateExerciseWithDetails> exList = exercises.getValue();
        Integer exIdx = currentExerciseIndex.getValue();
        Integer setNum = currentSet.getValue();
        
        if (exList == null || exIdx == null || setNum == null) return;

        TemplateExerciseWithDetails currentEx = exList.get(exIdx);
        if (setNum < currentEx.templateExercise.targetSets) {
            currentSet.setValue(setNum + 1);
        } else {
            if (exIdx < exList.size() - 1) {
                currentExerciseIndex.setValue(exIdx + 1);
                currentSet.setValue(1);
            } else {
                onFinish.run();
                return;
            }
        }
        
        // Reset stopwatch state for new set
        stopStopwatch();
        elapsedSeconds.setValue(0);
        
        // Auto-start stopwatch if the NEW exercise/set is timed
        TemplateExerciseWithDetails nextEx = exList.get(currentExerciseIndex.getValue());
        MeasureType type = nextEx.exercise.measureType != null ? nextEx.exercise.measureType : MeasureType.WEIGHT_REPS;
        if (type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) {
            startStopwatch();
        }
    }
    
    public void finishWorkout(int templateId, Runnable onComplete) {
        List<WorkoutRepository.SessionExerciseData> dataList = new ArrayList<>();
        List<TemplateExerciseWithDetails> exList = exercises.getValue();
        if (exList == null) return;

        for (int i = 0; i < exList.size(); i++) {
            List<SessionSet> sets = performedSetsMap.get(i);
            if (sets != null && !sets.isEmpty()) {
                dataList.add(new WorkoutRepository.SessionExerciseData(exList.get(i).exercise.apiId, sets));
            }
        }
        repository.saveWorkoutSession(templateId, dataList, onComplete);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacksAndMessages(null);
    }
}
