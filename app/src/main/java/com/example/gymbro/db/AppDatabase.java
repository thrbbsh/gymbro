package com.example.gymbro.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.gymbro.db.dao.ExerciseDao;
import com.example.gymbro.db.dao.HistoryDao;
import com.example.gymbro.db.dao.WorkoutDao;
import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.entity.SessionExercise;
import com.example.gymbro.db.entity.TemplateExercise;
import com.example.gymbro.db.entity.WorkoutSession;
import com.example.gymbro.db.entity.WorkoutTemplate;

import java.util.List;
import java.util.concurrent.Executors;

@Database(entities = {
        Exercise.class,
        WorkoutTemplate.class,
        TemplateExercise.class,
        WorkoutSession.class,
        SessionExercise.class
}, version = 19, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ExerciseDao exerciseDao();
    public abstract WorkoutDao workoutDao();
    public abstract HistoryDao historyDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "gym_bro_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d("DB_PREPOP", "Database onCreate triggered");
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        prepopulateTemplates(getDatabase(context));
                                    });
                                }

                                @Override
                                public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                                    super.onDestructiveMigration(db);
                                    Log.d("DB_PREPOP", "Database onDestructiveMigration triggered");
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        prepopulateTemplates(getDatabase(context));
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void prepopulateTemplates(AppDatabase db) {
        if (db == null) return;
        
        WorkoutDao workoutDao = db.workoutDao();

        List<WorkoutTemplate> existing = workoutDao.getAllTemplates();
        if (!existing.isEmpty()) {
            Log.d("DB_PREPOP", "Templates already exist.");
            return;
        }

        long fullBodyId = workoutDao.insertTemplate(new WorkoutTemplate("Full Body Basics"));
        long upperBodyId = workoutDao.insertTemplate(new WorkoutTemplate("Upper Body Focus"));
        long lowerBodyId = workoutDao.insertTemplate(new WorkoutTemplate("Lower Body & Core"));

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                int attempts = 0;
                while (attempts < 60) {
                    if (db.exerciseDao().getCount() >= 10) break;
                    Thread.sleep(1000);
                    attempts++;
                }

                if (db.exerciseDao().getCount() > 0) {
                    addExercisesToTemplates(db, (int)fullBodyId, (int)upperBodyId, (int)lowerBodyId);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private static void addExercisesToTemplates(AppDatabase db, int fullBodyId, int upperBodyId, int lowerBodyId) {
        ExerciseDao exerciseDao = db.exerciseDao();
        WorkoutDao workoutDao = db.workoutDao();
        List<Exercise> all = exerciseDao.getAllExercises();
        
        // 1. Full Body Basics
        addIfExists(workoutDao, all, fullBodyId, "3/4 sit-up", 3, 15, 0, 60);
        addIfExists(workoutDao, all, fullBodyId, "assisted chest dip", 3, 10, 0, 90);
        addIfExists(workoutDao, all, fullBodyId, "air bike", 3, 20, 0, 60);

        // 2. Upper Body Focus
        addIfExists(workoutDao, all, upperBodyId, "lateral pulldown", 4, 12, 0, 90);
        addIfExists(workoutDao, all, upperBodyId, "assisted chest dip", 3, 10, 0, 90);

        // 3. Lower Body & Core
        addIfExists(workoutDao, all, lowerBodyId, "hanging knee raise", 3, 15, 0, 60);
        addIfExists(workoutDao, all, lowerBodyId, "lying leg raise", 3, 12, 0, 60);
        addIfExists(workoutDao, all, lowerBodyId, "heel touchers", 3, 20, 0, 45);
    }

    private static void addIfExists(WorkoutDao dao, List<Exercise> list, int tId, String name, int sets, int reps, int dur, int rest) {
        for (Exercise e : list) {
            if (e.name.toLowerCase().contains(name.toLowerCase())) {
                dao.insertTemplateExercise(new TemplateExercise(tId, e.id, sets, reps, dur, rest));
                Log.d("DB_PREPOP", "Matched: " + name + " -> " + e.name);
                return;
            }
        }
        Log.w("DB_PREPOP", "No match found for: " + name);
    }
}
