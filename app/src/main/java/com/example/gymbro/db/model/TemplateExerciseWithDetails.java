package com.example.gymbro.db.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.entity.TemplateExercise;

public class TemplateExerciseWithDetails {
    @Embedded
    public TemplateExercise templateExercise;

    @Relation(
            parentColumn = "exerciseId",
            entityColumn = "apiId"
    )
    public Exercise exercise;
}
