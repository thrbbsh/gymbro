package com.example.gymbro.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "exercises")
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String name;
    public String primaryMuscle;

    public Exercise(String name, String primaryMuscle) {
        this.name = name;
        this.primaryMuscle = primaryMuscle;
    }
}