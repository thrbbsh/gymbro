package com.example.gymbro.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "session_sets",
        foreignKeys = @ForeignKey(entity = SessionExercise.class,
                parentColumns = "id",
                childColumns = "sessionExerciseId",
                onDelete = ForeignKey.CASCADE))
public class SessionSet {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int sessionExerciseId;
    
    public int setNumber;
    public int reps;
    public double weight;
    public int duration;
    public double distance;
    public boolean isExtra;
    public boolean isSkipped;

    public SessionSet(int sessionExerciseId, int setNumber, int reps, double weight, int duration, double distance, boolean isExtra, boolean isSkipped) {
        this.sessionExerciseId = sessionExerciseId;
        this.setNumber = setNumber;
        this.reps = reps;
        this.weight = weight;
        this.duration = duration;
        this.distance = distance;
        this.isExtra = isExtra;
        this.isSkipped = isSkipped;
    }
}
