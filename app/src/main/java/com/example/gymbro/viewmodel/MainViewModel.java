package com.example.gymbro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymbro.db.entity.WorkoutTemplate;
import com.example.gymbro.repository.WorkoutRepository;

import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private final WorkoutRepository repository;
    private final MutableLiveData<List<WorkoutTemplate>> templates = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);
    private final MutableLiveData<String> syncError = new MutableLiveData<>(null);

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = WorkoutRepository.getInstance(application);
    }

    public LiveData<List<WorkoutTemplate>> getTemplates() {
        return templates;
    }

    public LiveData<Boolean> isSyncing() {
        return isSyncing;
    }

    public LiveData<String> getSyncError() {
        return syncError;
    }

    public void loadTemplates() {
        repository.getAllTemplates(templates::postValue);
    }

    public void createTemplate(String name) {
        repository.insertTemplate(name, id -> loadTemplates());
    }

    public void deleteTemplate(WorkoutTemplate template) {
        repository.deleteTemplate(template, this::loadTemplates);
    }

    public void syncExercises() {
        repository.getExerciseCount(count -> {
            if (count > 100) {
                isSyncing.postValue(false);
                return;
            }

            isSyncing.postValue(true);
            syncError.postValue(null);
            
            repository.syncExercises(getApplication(), new WorkoutRepository.SyncCallback() {
                @Override
                public void onSuccess() {
                    isSyncing.postValue(false);
                    loadTemplates();
                }

                @Override
                public void onError(String message) {
                    isSyncing.postValue(false);
                    syncError.postValue(message);
                }
            });
        });
    }
}
