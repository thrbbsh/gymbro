package com.example.gymbro.db.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.gymbro.db.entity.WorkoutSession;
import com.example.gymbro.db.entity.WorkoutTemplate;
import com.example.gymbro.db.entity.SessionExercise;

import java.util.List;

public class WorkoutSessionWithDetails {
    @Embedded
    public WorkoutSession session;

    @Relation(
            parentColumn = "templateId",
            entityColumn = "id"
    )
    public WorkoutTemplate template;

    @Relation(
            entity = SessionExercise.class,
            parentColumn = "id",
            entityColumn = "sessionId"
    )
    public List<SessionExerciseWithSets> exercises;
}
