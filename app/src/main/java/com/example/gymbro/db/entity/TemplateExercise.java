package com.example.gymbro.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "template_exercises",
        foreignKeys = {
                @ForeignKey(entity = WorkoutTemplate.class,
                        parentColumns = "id",
                        childColumns = "templateId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Exercise.class,
                        parentColumns = "apiId",
                        childColumns = "exerciseId",
                        onDelete = ForeignKey.CASCADE)
        })
public class TemplateExercise {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int templateId;
    
    @NonNull
    public String exerciseId;
    
    public int targetSets;
    public int targetReps;
    public double targetWeight;
    public int targetDuration; // Integer as requested
    public double targetDistance;
    public int restSeconds;

    public TemplateExercise(int templateId, @NonNull String exerciseId, int targetSets, int targetReps, double targetWeight, int targetDuration, double targetDistance, int restSeconds) {
        this.templateId = templateId;
        this.exerciseId = exerciseId;
        this.targetSets = targetSets;
        this.targetReps = targetReps;
        this.targetWeight = targetWeight;
        this.targetDuration = targetDuration;
        this.targetDistance = targetDistance;
        this.restSeconds = restSeconds;
    }
}
