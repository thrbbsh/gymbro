package com.example.gymbro.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.gymbro.db.converter.StringListConverter;
import java.util.List;

@Entity(tableName = "exercises")
@TypeConverters(StringListConverter.class)
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String apiId; // ID from ExerciseDB API
    public String name;
    public String bodyPart;
    public String equipment;
    public String gifUrl;
    public String target;
    
    public List<String> secondaryMuscles;
    public List<String> instructions;

    public Exercise() {}

    public Exercise(String name, String bodyPart) {
        this.name = name;
        this.bodyPart = bodyPart;
    }
}
