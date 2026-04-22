package com.example.gymbro.db.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.entity.TemplateExercise;

/**
 * A POJO class that combines TemplateExercise data with the corresponding Exercise details.
 */
public class TemplateExerciseWithDetails {
    @Embedded
    public TemplateExercise templateExercise;

    @Relation(
            parentColumn = "exerciseId",
            entityColumn = "id"
    )
    public Exercise exercise;
}