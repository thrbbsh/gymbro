package com.example.gymbro.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.gymbro.db.entity.Exercise;

import java.util.List;

@Dao
public interface ExerciseDao {
    @Insert
    long insert(Exercise exercise);

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    List<Exercise> getAllExercises();
    
    @Query("SELECT * FROM exercises WHERE primaryMuscle = :muscle ORDER BY name ASC")
    List<Exercise> getExercisesByMuscle(String muscle);
}