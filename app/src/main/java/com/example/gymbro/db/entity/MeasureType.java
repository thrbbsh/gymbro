package com.example.gymbro.db.entity;

public enum MeasureType {
    WEIGHT_REPS,      // Weight + Reps (Barbell, Dumbbell)
    BODYWEIGHT_REPS,  // Reps only (Push-ups, Crunches)
    DURATION,         // Time (Plank, Stretching, Hold)
    DISTANCE_TIME     // Distance + Time (Running, Walking)
}
