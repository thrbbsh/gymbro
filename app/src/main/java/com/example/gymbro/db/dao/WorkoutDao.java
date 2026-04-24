package com.example.gymbro.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.gymbro.db.entity.TemplateExercise;
import com.example.gymbro.db.entity.WorkoutTemplate;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;

import java.util.List;

@Dao
public interface WorkoutDao {
    @Insert
    long insertTemplate(WorkoutTemplate template);

    @Update
    void updateTemplate(WorkoutTemplate template);

    @Delete
    void deleteTemplate(WorkoutTemplate template);

    @Insert
    void insertTemplateExercise(TemplateExercise templateExercise);

    @Update
    void updateTemplateExercise(TemplateExercise templateExercise);

    @Delete
    void deleteTemplateExercise(TemplateExercise templateExercise);

    @Query("SELECT * FROM workout_templates")
    List<WorkoutTemplate> getAllTemplates();

    @Query("SELECT * FROM workout_templates WHERE id = :id LIMIT 1")
    WorkoutTemplate getTemplateById(int id);

    @Query("SELECT * FROM workout_templates WHERE name = :name LIMIT 1")
    WorkoutTemplate getTemplateByName(String name);

    @Query("SELECT COUNT(*) FROM template_exercises WHERE templateId = :templateId")
    int getExerciseCountForTemplate(int templateId);

    @Transaction
    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId")
    List<TemplateExerciseWithDetails> getExercisesForTemplateWithDetails(int templateId);
}
