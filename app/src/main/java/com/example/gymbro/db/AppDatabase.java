package com.example.gymbro.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.gymbro.db.dao.ExerciseDao;
import com.example.gymbro.db.dao.HistoryDao;
import com.example.gymbro.db.dao.WorkoutDao;
import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.entity.SessionExercise;
import com.example.gymbro.db.entity.SessionSet;
import com.example.gymbro.db.entity.TemplateExercise;
import com.example.gymbro.db.entity.WorkoutSession;
import com.example.gymbro.db.entity.WorkoutTemplate;

import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.Executors;

@Database(entities = {
        Exercise.class,
        WorkoutTemplate.class,
        TemplateExercise.class,
        WorkoutSession.class,
        SessionExercise.class,
        SessionSet.class
}, version = 42, exportSchema = false)
@TypeConverters({Converters.class})
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
                                        prepopulateSessions(INSTANCE);
                                    });
                                }

                                @Override
                                public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                                    super.onDestructiveMigration(db);
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        prepopulateTemplates(INSTANCE);
                                        prepopulateSessions(INSTANCE);
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

    public static void prepopulateSessions(AppDatabase db) {
        if (db == null) return;
        HistoryDao historyDao = db.historyDao();
        WorkoutDao workoutDao = db.workoutDao();

        if (!historyDao.getAllSessions().isEmpty()) return;

        WorkoutTemplate fullBody = workoutDao.getTemplateByName("Full Body Basics");
        WorkoutTemplate upperBody = workoutDao.getTemplateByName("Upper Body Focus");

        if (fullBody == null || upperBody == null) {
            prepopulateTemplates(db);
            fullBody = workoutDao.getTemplateByName("Full Body Basics");
            upperBody = workoutDao.getTemplateByName("Upper Body Focus");
        }

        if (fullBody == null || upperBody == null) {
            Log.e("AppDatabase", "Failed to find templates for session prepopulation");
            return;
        }

        Random random = new Random();
        int[] months = {Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH};

        for (int i = 0; i < 15; i++) {
            Calendar cal = Calendar.getInstance();
            cal.set(2026, months[i % 3], 1 + random.nextInt(25));
            cal.set(Calendar.HOUR_OF_DAY, 8 + random.nextInt(12));
            cal.set(Calendar.MINUTE, random.nextInt(60));

            WorkoutTemplate template = (i % 2 == 0) ? fullBody : upperBody;

            WorkoutSession session = new WorkoutSession(template.id, cal.getTimeInMillis());
            historyDao.insertSessionInternal(session);
        }
        Log.d("AppDatabase", "Prepopulated 15 empty test sessions for Jan-Mar 2026");
    }

    private static int getOrCreateTemplate(WorkoutDao dao, String name) {
        WorkoutTemplate template = dao.getTemplateByName(name);
        if (template != null) return template.id;
        return (int) dao.insertTemplate(new WorkoutTemplate(name));
    }

    private static void addExerciseToTemplateByName(ExerciseDao exDao, WorkoutDao workDao, int tId, String name, int sets, int reps) {
        Exercise ex = exDao.findByName("%" + name + "%");
        if (ex != null) {
            workDao.insertTemplateExercise(new TemplateExercise(tId, ex.apiId, sets, reps, 0.0, 0, 0.0, 60));
        }
    }
}
