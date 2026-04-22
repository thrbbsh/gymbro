package com.example.gymbro.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_sessions",
        foreignKeys = @ForeignKey(entity = WorkoutTemplate.class,
                parentColumns = "id",
                childColumns = "templateId",
                onDelete = ForeignKey.CASCADE))
public class WorkoutSession {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int templateId;
    public long date; // Timestamp

    public WorkoutSession(int templateId, long date) {
        this.templateId = templateId;
        this.date = date;
    }
}