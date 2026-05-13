package com.example.gymbro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.entity.TemplateExercise;
import com.example.gymbro.db.entity.WorkoutTemplate;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;
import com.example.gymbro.repository.WorkoutRepository;

import java.util.List;

public class EditTemplateViewModel extends AndroidViewModel {
    private final WorkoutRepository repository;

    private final MutableLiveData<WorkoutTemplate> template = new MutableLiveData<>();
    private final MutableLiveData<List<TemplateExerciseWithDetails>> exercises = new MutableLiveData<>();
    private final MutableLiveData<List<Exercise>> allExercises = new MutableLiveData<>();

    public EditTemplateViewModel(@NonNull Application application) {
        super(application);
        repository = WorkoutRepository.getInstance(application);
    }

    public LiveData<WorkoutTemplate> getTemplate() { return template; }
    public LiveData<List<TemplateExerciseWithDetails>> getExercises() { return exercises; }
    public LiveData<List<Exercise>> getAllExercises() { return allExercises; }

    public void loadTemplateData(int templateId) {
        repository.getTemplateById(templateId, template::postValue);
        loadExercises(templateId);
    }

    public void loadExercises(int templateId) {
        repository.getExercisesForTemplate(templateId, exercises::postValue);
    }

    public void updateTemplateName(String newName) {
        WorkoutTemplate t = template.getValue();
        if (t != null) {
            t.name = newName;
            repository.updateTemplate(t);
        }
    }

    public void deleteExercise(TemplateExercise te) {
        repository.deleteTemplateExercise(te, () -> loadExercises(te.templateId));
    }

    public void updateExercise(TemplateExercise te) {
        repository.updateTemplateExercise(te);
    }

    public void loadAllExercises() {
        repository.getAllExercises(allExercises::postValue);
    }

    public void addExerciseToTemplate(int templateId, Exercise exercise) {
        TemplateExercise newEx = new TemplateExercise(templateId, exercise.apiId, 0, 0, 0.0, 0, 0.0, 60);
        repository.insertTemplateExercise(newEx, () -> loadExercises(templateId));
    }
}
