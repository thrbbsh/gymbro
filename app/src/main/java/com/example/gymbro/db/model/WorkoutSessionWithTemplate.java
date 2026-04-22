package com.example.gymbro.db.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.gymbro.db.entity.WorkoutSession;
import com.example.gymbro.db.entity.WorkoutTemplate;

public class WorkoutSessionWithTemplate {
    @Embedded
    public WorkoutSession session;

    @Relation(
            parentColumn = "templateId",
            entityColumn = "id"
    )
    public WorkoutTemplate template;
}
