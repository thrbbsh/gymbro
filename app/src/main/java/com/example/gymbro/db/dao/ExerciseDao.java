package com.example.gymbro.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.gymbro.db.entity.Exercise;

import java.util.List;

@Dao
public interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Exercise> exercises);

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    List<Exercise> getAllExercises();
    
    @Query("SELECT * FROM exercises WHERE target = :muscle ORDER BY name ASC")
    List<Exercise> getExercisesByMuscle(String muscle);

    @Query("SELECT COUNT(*) FROM exercises")
    int getExerciseCount();

    @Query("SELECT * FROM exercises WHERE name LIKE :name LIMIT 1")
    Exercise findByName(String name);
}
