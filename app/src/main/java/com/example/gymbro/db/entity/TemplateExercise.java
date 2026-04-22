package com.example.gymbro.db.entity;

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
                        parentColumns = "id",
                        childColumns = "exerciseId",
                        onDelete = ForeignKey.CASCADE)
        })
public class TemplateExercise {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int templateId;
    public int exerciseId;
    
    public int targetSets;
    public int targetReps;
    public int targetDuration; // Время в секундах (например, для планки)

    public TemplateExercise(int templateId, int exerciseId, int targetSets, int targetReps, int targetDuration) {
        this.templateId = templateId;
        this.exerciseId = exerciseId;
        this.targetSets = targetSets;
        this.targetReps = targetReps;
        this.targetDuration = targetDuration;
    }
}