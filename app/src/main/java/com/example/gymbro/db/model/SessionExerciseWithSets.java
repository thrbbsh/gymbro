package com.example.gymbro.db.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.entity.SessionExercise;
import com.example.gymbro.db.entity.SessionSet;

import java.util.List;

public class SessionExerciseWithSets {
    @Embedded
    public SessionExercise sessionExercise;

    @Relation(
            parentColumn = "exerciseId",
            entityColumn = "apiId"
    )
    public Exercise exercise;

    @Relation(
            parentColumn = "id",
            entityColumn = "sessionExerciseId"
    )
    public List<SessionSet> sets;
}
