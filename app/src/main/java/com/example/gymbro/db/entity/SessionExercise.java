package com.example.gymbro.db.entity;

import androidx.annotation.NonNull;
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
                        parentColumns = "apiId",
                        childColumns = "exerciseId",
                        onDelete = ForeignKey.CASCADE)
        })
public class SessionExercise {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int sessionId;
    
    @NonNull
    public String exerciseId;
    
    public int actualSets;
    public int actualReps;
    public int actualDuration;
    public int restSeconds;

    public SessionExercise(int sessionId, @NonNull String exerciseId, int actualSets, int actualReps, int actualDuration, int restSeconds) {
        this.sessionId = sessionId;
        this.exerciseId = exerciseId;
        this.actualSets = actualSets;
        this.actualReps = actualReps;
        this.actualDuration = actualDuration;
        this.restSeconds = restSeconds;
    }
}
