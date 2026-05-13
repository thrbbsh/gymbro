package com.example.gymbro.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.gymbro.db.entity.SessionExercise;
import com.example.gymbro.db.entity.SessionSet;
import com.example.gymbro.db.entity.WorkoutSession;
import com.example.gymbro.db.model.WorkoutSessionWithDetails;

import java.util.List;

@Dao
public abstract class HistoryDao {
    @Insert
    public abstract long insertSession(WorkoutSession session);

    @Insert
    public abstract long insertSessionExercise(SessionExercise sessionExercise);

    @Insert
    public abstract void insertSessionSet(SessionSet sessionSet);

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    public abstract List<WorkoutSessionWithDetails> getAllSessionsWithDetails();

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE date >= :start AND date < :end ORDER BY date ASC")
    public abstract List<WorkoutSessionWithDetails> getSessionsInPeriod(long start, long end);

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE date >= :start AND date < :end")
    public abstract int getSessionCount(long start, long end);

    @Query("SELECT SUM(s.weight * s.reps) FROM session_sets s " +
           "JOIN session_exercises e ON s.sessionExerciseId = e.id " +
           "JOIN workout_sessions w ON e.sessionId = w.id " +
           "WHERE w.date >= :start AND w.date < :end AND s.isSkipped = 0")
    public abstract Double getTotalTonnage(long start, long end);

    @Query("SELECT SUM(s.distance) FROM session_sets s " +
           "JOIN session_exercises e ON s.sessionExerciseId = e.id " +
           "JOIN workout_sessions w ON e.sessionId = w.id " +
           "WHERE w.date >= :start AND w.date < :end AND s.isSkipped = 0")
    public abstract Double getTotalDistance(long start, long end);

    @Query("SELECT SUM(s.duration) FROM session_sets s " +
           "JOIN session_exercises e ON s.sessionExerciseId = e.id " +
           "JOIN workout_sessions w ON e.sessionId = w.id " +
           "WHERE w.date >= :start AND w.date < :end AND s.isSkipped = 0")
    public abstract Long getTotalDuration(long start, long end);
}
