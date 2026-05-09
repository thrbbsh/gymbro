package com.example.gymbro.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.gymbro.db.entity.SessionExercise;
import com.example.gymbro.db.entity.SessionSet;
import com.example.gymbro.db.entity.WorkoutSession;
import com.example.gymbro.db.model.WorkoutSessionWithDetails;
import com.example.gymbro.db.model.WorkoutSessionWithTemplate;

import java.util.List;

@Dao
public abstract class HistoryDao {
    @Insert
    public abstract long insertSessionInternal(WorkoutSession session);
    
    public long insertSession(WorkoutSession session) {
        if (session.date > System.currentTimeMillis()) {
            return -1;
        }
        return insertSessionInternal(session);
    }

    @Insert
    public abstract long insertSessionExercise(SessionExercise sessionExercise);

    @Insert
    public abstract void insertSessionSet(SessionSet sessionSet);

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    public abstract List<WorkoutSession> getAllSessions();

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    public abstract List<WorkoutSessionWithTemplate> getAllSessionsWithTemplate();

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    public abstract List<WorkoutSessionWithDetails> getAllSessionsWithDetails();

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId")
    public abstract List<SessionExercise> getExercisesForSession(int sessionId);

    @Query("SELECT * FROM session_sets WHERE sessionExerciseId = :sessionExerciseId")
    public abstract List<SessionSet> getSetsForExercise(int sessionExerciseId);
}
