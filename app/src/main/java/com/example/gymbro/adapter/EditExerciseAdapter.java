package com.example.gymbro.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.db.entity.MeasureType;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;

import java.util.List;

public class EditExerciseAdapter extends RecyclerView.Adapter<EditExerciseAdapter.EditViewHolder> {
    private List<TemplateExerciseWithDetails> items;
    private final OnExerciseActionListener actionListener;

    public interface OnExerciseActionListener {
        void onDelete(TemplateExerciseWithDetails item);
        void onUpdate(TemplateExerciseWithDetails item);
    }

    public EditExerciseAdapter(List<TemplateExerciseWithDetails> items, OnExerciseActionListener actionListener) {
        this.items = items;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public EditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise_edit, parent, false);
        return new EditViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EditViewHolder holder, int position) {
        TemplateExerciseWithDetails item = items.get(position);
        
        holder.clearWatchers();

        holder.textName.setText(item.exercise.name);
        holder.editSets.setText(String.valueOf(item.templateExercise.targetSets));
        holder.editReps.setText(String.valueOf(item.templateExercise.targetReps));
        holder.editWeight.setText(String.valueOf(item.templateExercise.targetWeight));
        holder.editDistance.setText(String.valueOf(item.templateExercise.targetDistance));
        holder.editDuration.setText(String.valueOf(item.templateExercise.targetDuration));
        holder.editRest.setText(String.valueOf(item.templateExercise.restSeconds));

        // Show/Hide fields based on MeasureType
        MeasureType type = item.exercise.measureType;
        if (type == null) type = MeasureType.WEIGHT_REPS;

        holder.layoutWeight.setVisibility(View.GONE);
        holder.layoutReps.setVisibility(View.GONE);
        holder.layoutDistance.setVisibility(View.GONE);
        holder.layoutDuration.setVisibility(View.GONE);

        switch (type) {
            case WEIGHT_REPS:
                holder.layoutWeight.setVisibility(View.VISIBLE);
                holder.layoutReps.setVisibility(View.VISIBLE);
                break;
            case BODYWEIGHT_REPS:
                holder.layoutReps.setVisibility(View.VISIBLE);
                break;
            case DURATION:
                holder.layoutDuration.setVisibility(View.VISIBLE);
                break;
            case DISTANCE_TIME:
                holder.layoutDistance.setVisibility(View.VISIBLE);
                holder.layoutDuration.setVisibility(View.VISIBLE);
                break;
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(item);
            }
        });

        holder.setsWatcher = new SimpleTextWatcher(s -> {
            int val = parseSafeInt(s);
            item.templateExercise.targetSets = val;
            actionListener.onUpdate(item);
        });
        holder.repsWatcher = new SimpleTextWatcher(s -> {
            int val = parseSafeInt(s);
            item.templateExercise.targetReps = val;
            actionListener.onUpdate(item);
        });
        holder.weightWatcher = new SimpleTextWatcher(s -> {
            double val = parseSafeDouble(s);
            item.templateExercise.targetWeight = val;
            actionListener.onUpdate(item);
        });
        holder.distanceWatcher = new SimpleTextWatcher(s -> {
            double val = parseSafeDouble(s);
            item.templateExercise.targetDistance = val;
            actionListener.onUpdate(item);
        });
        holder.durationWatcher = new SimpleTextWatcher(s -> {
            int val = parseSafeInt(s);
            item.templateExercise.targetDuration = val;
            actionListener.onUpdate(item);
        });
        holder.restWatcher = new SimpleTextWatcher(s -> {
            int val = parseSafeInt(s);
            item.templateExercise.restSeconds = val;
            actionListener.onUpdate(item);
        });

        holder.editSets.addTextChangedListener(holder.setsWatcher);
        holder.editReps.addTextChangedListener(holder.repsWatcher);
        holder.editWeight.addTextChangedListener(holder.weightWatcher);
        holder.editDistance.addTextChangedListener(holder.distanceWatcher);
        holder.editDuration.addTextChangedListener(holder.durationWatcher);
        holder.editRest.addTextChangedListener(holder.restWatcher);
    }

    private int parseSafeInt(String s) {
        try {
            return s.isEmpty() ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseSafeDouble(String s) {
        try {
            return s.isEmpty() ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class EditViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        EditText editSets, editReps, editWeight, editDistance, editDuration, editRest;
        View layoutWeight, layoutReps, layoutDistance, layoutDuration;
        ImageButton btnDelete;
        
        TextWatcher setsWatcher, repsWatcher, weightWatcher, distanceWatcher, durationWatcher, restWatcher;

        public EditViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textViewExerciseName);
            editSets = itemView.findViewById(R.id.editTextSets);
            editReps = itemView.findViewById(R.id.editTextReps);
            editWeight = itemView.findViewById(R.id.editTextWeight);
            editDistance = itemView.findViewById(R.id.editTextDistance);
            editDuration = itemView.findViewById(R.id.editTextDuration);
            editRest = itemView.findViewById(R.id.editTextRest);
            
            layoutWeight = itemView.findViewById(R.id.layoutWeight);
            layoutReps = itemView.findViewById(R.id.layoutReps);
            layoutDistance = itemView.findViewById(R.id.layoutDistance);
            layoutDuration = itemView.findViewById(R.id.layoutDuration);
            
            btnDelete = itemView.findViewById(R.id.buttonDeleteExercise);
        }

        void clearWatchers() {
            if (setsWatcher != null) editSets.removeTextChangedListener(setsWatcher);
            if (repsWatcher != null) editReps.removeTextChangedListener(repsWatcher);
            if (weightWatcher != null) editWeight.removeTextChangedListener(weightWatcher);
            if (distanceWatcher != null) editDistance.removeTextChangedListener(distanceWatcher);
            if (durationWatcher != null) editDuration.removeTextChangedListener(durationWatcher);
            if (restWatcher != null) editRest.removeTextChangedListener(restWatcher);
            
            editSets.setError(null);
            editReps.setError(null);
            editWeight.setError(null);
            editDistance.setError(null);
            editDuration.setError(null);
            editRest.setError(null);
        }
    }

    private static class SimpleTextWatcher implements TextWatcher {
        interface OnChanged { void onTextChanged(String s); }
        private final OnChanged callback;
        SimpleTextWatcher(OnChanged callback) { this.callback = callback; }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { callback.onTextChanged(s.toString()); }
        @Override
        public void afterTextChanged(Editable s) {}
    }
}
