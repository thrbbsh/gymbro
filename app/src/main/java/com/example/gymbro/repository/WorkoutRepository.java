package com.example.gymbro.repository;

import android.content.Context;

import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.entity.SessionExercise;
import com.example.gymbro.db.entity.SessionSet;
import com.example.gymbro.db.entity.TemplateExercise;
import com.example.gymbro.db.entity.WorkoutSession;
import com.example.gymbro.db.entity.WorkoutTemplate;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;
import com.example.gymbro.db.model.WorkoutSessionWithDetails;
import com.example.gymbro.network.RetrofitClient;
import com.example.gymbro.utils.ExerciseMeasureHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class WorkoutRepository {
    private static WorkoutRepository INSTANCE;
    private final AppDatabase db;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private WorkoutRepository(Context context) {
        db = AppDatabase.getDatabase(context);
    }

    public static synchronized WorkoutRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new WorkoutRepository(context);
        }
        return INSTANCE;
    }

    // --- Templates ---
    public void getAllTemplates(Callback<List<WorkoutTemplate>> callback) {
        executor.execute(() -> callback.onResult(db.workoutDao().getAllTemplates()));
    }

    public void getTemplateById(int id, Callback<WorkoutTemplate> callback) {
        executor.execute(() -> callback.onResult(db.workoutDao().getTemplateById(id)));
    }

    public void insertTemplate(String name, Callback<Long> callback) {
        executor.execute(() -> callback.onResult(db.workoutDao().insertTemplate(new WorkoutTemplate(name))));
    }

    public void updateTemplate(WorkoutTemplate template) {
        executor.execute(() -> db.workoutDao().updateTemplate(template));
    }

    public void deleteTemplate(WorkoutTemplate template, Runnable onComplete) {
        executor.execute(() -> {
            db.workoutDao().deleteTemplate(template);
            onComplete.run();
        });
    }

    public void getExercisesForTemplate(int templateId, Callback<List<TemplateExerciseWithDetails>> callback) {
        executor.execute(() -> callback.onResult(db.workoutDao().getExercisesForTemplateWithDetails(templateId)));
    }

    public void insertTemplateExercise(TemplateExercise te, Runnable onComplete) {
        executor.execute(() -> {
            db.workoutDao().insertTemplateExercise(te);
            onComplete.run();
        });
    }

    public void updateTemplateExercise(TemplateExercise te) {
        executor.execute(() -> db.workoutDao().updateTemplateExercise(te));
    }

    public void deleteTemplateExercise(TemplateExercise te, Runnable onComplete) {
        executor.execute(() -> {
            db.workoutDao().deleteTemplateExercise(te);
            onComplete.run();
        });
    }

    // --- Exercises & Sync ---
    public void getAllExercises(Callback<List<Exercise>> callback) {
        executor.execute(() -> callback.onResult(db.exerciseDao().getAllExercises()));
    }

    public void getExerciseCount(Callback<Integer> callback) {
        executor.execute(() -> callback.onResult(db.exerciseDao().getExerciseCount()));
    }

    public void syncExercises(Context context, SyncCallback callback) {
        RetrofitClient.getApiService(context).getExercises().enqueue(new retrofit2.Callback<List<Exercise>>() {
            @Override
            public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveExercises(response.body(), callback);
                } else {
                    callback.onError("Sync failed: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<List<Exercise>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private void saveExercises(List<Exercise> exercises, SyncCallback callback) {
        executor.execute(() -> {
            for (Exercise ex : exercises) {
                ex.measureType = ExerciseMeasureHelper.guessMeasureType(
                        ex.apiId, ex.name, ex.equipment, ex.instructions
                );
            }
            db.exerciseDao().insertAll(exercises);
            AppDatabase.prepopulateTemplates(db);
            callback.onSuccess();
        });
    }

    // --- History & Stats ---
    public void getAllSessionsWithDetails(Callback<List<WorkoutSessionWithDetails>> callback) {
        executor.execute(() -> callback.onResult(db.historyDao().getAllSessionsWithDetails()));
    }

    public void getSessionsInPeriod(long start, long end, Callback<List<WorkoutSessionWithDetails>> callback) {
        executor.execute(() -> callback.onResult(db.historyDao().getSessionsInPeriod(start, end)));
    }

    public void getSummaryStats(long start, long end, Callback<StatsData> callback) {
        executor.execute(() -> {
            int count = db.historyDao().getSessionCount(start, end);
            Double tonnage = db.historyDao().getTotalTonnage(start, end);
            Double distance = db.historyDao().getTotalDistance(start, end);
            Long duration = db.historyDao().getTotalDuration(start, end);
            callback.onResult(new StatsData(count, tonnage != null ? tonnage : 0, distance != null ? distance : 0, duration != null ? duration : 0));
        });
    }

    public void saveWorkoutSession(int templateId, List<SessionExerciseData> exerciseData, Runnable onComplete) {
        executor.execute(() -> {
            long sessionId = db.historyDao().insertSession(new WorkoutSession(templateId, System.currentTimeMillis()));
            for (SessionExerciseData data : exerciseData) {
                long sessionExId = db.historyDao().insertSessionExercise(new SessionExercise((int) sessionId, data.exerciseApiId));
                for (SessionSet set : data.sets) {
                    set.sessionExerciseId = (int) sessionExId;
                    set.id = 0;
                    db.historyDao().insertSessionSet(set);
                }
            }
            onComplete.run();
        });
    }

    // --- Interfaces & Data Models ---
    public interface Callback<T> { void onResult(T result); }
    public interface SyncCallback { void onSuccess(); void onError(String message); }

    public static class SessionExerciseData {
        public String exerciseApiId;
        public List<SessionSet> sets;
        public SessionExerciseData(String exerciseApiId, List<SessionSet> sets) {
            this.exerciseApiId = exerciseApiId;
            this.sets = sets;
        }
    }

    public static class StatsData {
        public final int count;
        public final double tonnage;
        public final double distance;
        public final long duration;
        public StatsData(int count, double tonnage, double distance, long duration) {
            this.count = count; this.tonnage = tonnage; this.distance = distance; this.duration = duration;
        }
    }
}
