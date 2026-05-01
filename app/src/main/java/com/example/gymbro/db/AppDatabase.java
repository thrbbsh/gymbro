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

import java.util.concurrent.Executors;

@Database(entities = {
        Exercise.class,
        WorkoutTemplate.class,
        TemplateExercise.class,
        WorkoutSession.class,
        SessionExercise.class
}, version = 18, exportSchema = false) // Updated version to 18
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
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        prepopulateTemplates(INSTANCE);
                                    });
                                }

                                @Override
                                public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                                    super.onDestructiveMigration(db);
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        prepopulateTemplates(INSTANCE);
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void prepopulateTemplates(AppDatabase db) {
        if (db == null) return;
        WorkoutDao workoutDao = db.workoutDao();
        ExerciseDao exerciseDao = db.exerciseDao();

        int fullBodyId = getOrCreateTemplate(workoutDao, "Full Body Basics");
        int upperBodyId = getOrCreateTemplate(workoutDao, "Upper Body Focus");
        int lowerBodyId = getOrCreateTemplate(workoutDao, "Lower Body & Core");

        if (workoutDao.getExerciseCountForTemplate(fullBodyId) == 0) {
            addExerciseToTemplateByName(exerciseDao, workoutDao, fullBodyId, "push up", 3, 12);
            addExerciseToTemplateByName(exerciseDao, workoutDao, fullBodyId, "squat", 3, 15);
        }
        
        if (workoutDao.getExerciseCountForTemplate(upperBodyId) == 0) {
            addExerciseToTemplateByName(exerciseDao, workoutDao, upperBodyId, "bench press", 4, 8);
            addExerciseToTemplateByName(exerciseDao, workoutDao, upperBodyId, "pull up", 3, 10);
        }
        
        if (workoutDao.getExerciseCountForTemplate(lowerBodyId) == 0) {
            addExerciseToTemplateByName(exerciseDao, workoutDao, lowerBodyId, "deadlift", 3, 8);
            addExerciseToTemplateByName(exerciseDao, workoutDao, lowerBodyId, "leg press", 3, 12);
        }
    }

    private static int getOrCreateTemplate(WorkoutDao dao, String name) {
        WorkoutTemplate template = dao.getTemplateByName(name);
        if (template != null) return template.id;
        return (int) dao.insertTemplate(new WorkoutTemplate(name));
    }

    private static void addExerciseToTemplateByName(ExerciseDao exDao, WorkoutDao workDao, int tId, String name, int sets, int reps) {
        Exercise ex = exDao.findByName("%" + name + "%");
        if (ex != null) {
            workDao.insertTemplateExercise(new TemplateExercise(tId, ex.apiId, sets, reps, 0, 60));
        }
    }
}
