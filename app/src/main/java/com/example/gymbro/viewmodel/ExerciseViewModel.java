package com.example.gymbro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymbro.db.entity.WorkoutTemplate;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;
import com.example.gymbro.repository.WorkoutRepository;

import java.util.List;

public class ExerciseViewModel extends AndroidViewModel {
    private final WorkoutRepository repository;

    private final MutableLiveData<List<TemplateExerciseWithDetails>> exercises = new MutableLiveData<>();
    private final MutableLiveData<WorkoutTemplate> template = new MutableLiveData<>();

    public ExerciseViewModel(@NonNull Application application) {
        super(application);
        this.repository = WorkoutRepository.getInstance(application);
    }

    public LiveData<List<TemplateExerciseWithDetails>> getExercises() {
        return exercises;
    }

    public LiveData<WorkoutTemplate> getTemplate() {
        return template;
    }

    public void loadData(int templateId) {
        repository.getExercisesForTemplate(templateId, exercises::postValue);
        repository.getTemplateById(templateId, template::postValue);
    }
}
