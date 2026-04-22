package com.example.gymbro.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_templates")
public class WorkoutTemplate {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;

    public WorkoutTemplate(String name) {
        this.name = name;
    }
}