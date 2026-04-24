package com.example.gymbro.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.gymbro.db.entity.TemplateExercise;
import com.example.gymbro.db.entity.WorkoutTemplate;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;

import java.util.List;

@Dao
public interface WorkoutDao {
    @Insert
    long insertTemplate(WorkoutTemplate template);

    @Insert
    void insertTemplateExercise(TemplateExercise templateExercise);

    @Query("SELECT * FROM workout_templates")
    List<WorkoutTemplate> getAllTemplates();

    @Query("SELECT * FROM workout_templates WHERE name = :name LIMIT 1")
    WorkoutTemplate getTemplateByName(String name);

    @Query("SELECT COUNT(*) FROM template_exercises WHERE templateId = :templateId")
    int getExerciseCountForTemplate(int templateId);

    @Transaction
    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId")
    List<TemplateExerciseWithDetails> getExercisesForTemplateWithDetails(int templateId);
}
