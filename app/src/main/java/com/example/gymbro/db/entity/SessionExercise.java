package com.example.gymbro.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "session_exercises",
        foreignKeys = {
                @ForeignKey(entity = WorkoutSession.class,
                        parentColumns = "id",
                        childColumns = "sessionId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Exercise.class,
                        parentColumns = "id",
                        childColumns = "exerciseId",
                        onDelete = ForeignKey.CASCADE)
        })
public class SessionExercise {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int sessionId;
    public int exerciseId;
    
    public int actualSets;
    public int actualReps;
    public int actualDuration; // Time in seconds

    public SessionExercise(int sessionId, int exerciseId, int actualSets, int actualReps, int actualDuration) {
        this.sessionId = sessionId;
        this.exerciseId = exerciseId;
        this.actualSets = actualSets;
        this.actualReps = actualReps;
        this.actualDuration = actualDuration;
    }
}