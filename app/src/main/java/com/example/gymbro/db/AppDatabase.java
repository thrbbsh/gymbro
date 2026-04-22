package com.example.gymbro.db;

import android.content.Context;

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
}, version = 1)
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
                            .addCallback(new Callback() {
                                @Override
                                @NonNull
                                public void onCreate(SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // Run prepopulation in a background thread
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        prepopulateDatabase(getDatabase(context));
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Prepopulates the database with initial exercises and workout templates.
     * This will be used as a mock/starting point before ExerciseDB API integration.
     */
    private static void prepopulateDatabase(AppDatabase db) {
        ExerciseDao exerciseDao = db.exerciseDao();
        WorkoutDao workoutDao = db.workoutDao();
        HistoryDao historyDao = db.historyDao();

        // exerciseDao: 10 basic exercises, 6 muscle groups
        long exBenchPress = exerciseDao.insert(new Exercise("Bench Press", "Chest"));
        long exPushUps = exerciseDao.insert(new Exercise("Push-ups", "Chest"));
        long exSquats = exerciseDao.insert(new Exercise("Squats", "Legs"));
        long exLunges = exerciseDao.insert(new Exercise("Lunges", "Legs"));
        long exDeadlift = exerciseDao.insert(new Exercise("Deadlift", "Back"));
        long exPullUps = exerciseDao.insert(new Exercise("Pull-ups", "Back"));
        long exOverheadPress = exerciseDao.insert(new Exercise("Overhead Press", "Shoulders"));
        long exPlank = exerciseDao.insert(new Exercise("Plank", "Core"));
        long exCrunches = exerciseDao.insert(new Exercise("Crunches", "Core"));
        long exBicepCurls = exerciseDao.insert(new Exercise("Bicep Curls", "Arms"));
        
        // workoutDao: 3 Basic Workout Templates
        long tplFullBody = workoutDao.insertTemplate(new WorkoutTemplate("Full Body Basics"));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplFullBody, (int)exSquats, 3, 10, 0));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplFullBody, (int)exBenchPress, 3, 10, 0));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplFullBody, (int)exDeadlift, 3, 5, 0));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplFullBody, (int)exPlank, 3, 0, 60));

        long tplUpperBody = workoutDao.insertTemplate(new WorkoutTemplate("Upper Body Focus"));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplUpperBody, (int)exPullUps, 3, 8, 0));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplUpperBody, (int)exOverheadPress, 3, 10, 0));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplUpperBody, (int)exPushUps, 3, 15, 0));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplUpperBody, (int)exBicepCurls, 3, 12, 0));

        long tplLowerCore = workoutDao.insertTemplate(new WorkoutTemplate("Lower Body & Core"));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplLowerCore, (int)exSquats, 3, 12, 0));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplLowerCore, (int)exLunges, 3, 10, 0));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplLowerCore, (int)exPlank, 3, 0, 60));
        workoutDao.insertTemplateExercise(new TemplateExercise((int)tplLowerCore, (int)exCrunches, 3, 20, 0));
    }
}