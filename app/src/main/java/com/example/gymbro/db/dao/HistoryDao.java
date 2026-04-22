package com.example.gymbro.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.gymbro.db.entity.SessionExercise;
import com.example.gymbro.db.entity.WorkoutSession;

import java.util.List;

@Dao
public interface HistoryDao {
    @Insert
    long insertSession(WorkoutSession session);

    @Insert
    void insertSessionExercise(SessionExercise sessionExercise);

    @Query("SELECT * FROM workout_sessions ORDER BY date DESC")
    List<WorkoutSession> getAllSessions();

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId")
    List<SessionExercise> getExercisesForSession(int sessionId);
}