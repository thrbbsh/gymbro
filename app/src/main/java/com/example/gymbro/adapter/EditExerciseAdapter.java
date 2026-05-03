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

        // Visibility based on MeasureType
        final MeasureType type = item.exercise.measureType != null ? item.exercise.measureType : MeasureType.WEIGHT_REPS;

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
                // Reps are hidden for Duration (Plank)
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

        // Validation logic
        holder.setsWatcher = new SimpleTextWatcher(s -> {
            int val = parseSafeInt(s);
            if (val >= 1 && val <= 50) {
                item.templateExercise.targetSets = val;
                actionListener.onUpdate(item);
            } else {
                holder.editSets.setError("1-50");
            }
        });

        holder.repsWatcher = new SimpleTextWatcher(s -> {
            int val = parseSafeInt(s);
            int minReps = (type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) ? 0 : 1;
            if (val >= minReps && val <= 999) {
                item.templateExercise.targetReps = val;
                actionListener.onUpdate(item);
            } else {
                holder.editReps.setError("Min " + minReps);
            }
        });

        holder.weightWatcher = new SimpleTextWatcher(s -> {
            double val = parseSafeDouble(s);
            if (val >= 0.5 && val <= 999.9) {
                item.templateExercise.targetWeight = val;
                actionListener.onUpdate(item);
            } else {
                holder.editWeight.setError("Min 0.5");
            }
        });

        holder.distanceWatcher = new SimpleTextWatcher(s -> {
            double val = parseSafeDouble(s);
            if (val >= 0.05 && val <= 999.9) {
                item.templateExercise.targetDistance = val;
                actionListener.onUpdate(item);
            } else {
                holder.editDistance.setError("Min 0.05");
            }
        });

        holder.durationWatcher = new SimpleTextWatcher(s -> {
            int val = parseSafeInt(s);
            if (val >= 5 && val <= 14400) {
                item.templateExercise.targetDuration = val;
                actionListener.onUpdate(item);
            } else {
                holder.editDuration.setError("Min 5s");
            }
        });

        holder.restWatcher = new SimpleTextWatcher(s -> {
            int val = parseSafeInt(s);
            if (val >= 0 && val <= 999) {
                item.templateExercise.restSeconds = val;
                actionListener.onUpdate(item);
            } else {
                holder.editRest.setError("0-999");
            }
        });

        // Manually trigger initial validation to show errors if values are 0
        validateInitial(holder, item, type);

        holder.editSets.addTextChangedListener(holder.setsWatcher);
        holder.editReps.addTextChangedListener(holder.repsWatcher);
        holder.editWeight.addTextChangedListener(holder.weightWatcher);
        holder.editDistance.addTextChangedListener(holder.distanceWatcher);
        holder.editDuration.addTextChangedListener(holder.durationWatcher);
        holder.editRest.addTextChangedListener(holder.restWatcher);
    }

    private void validateInitial(EditViewHolder holder, TemplateExerciseWithDetails item, MeasureType type) {
        if (item.templateExercise.targetSets < 1) holder.editSets.setError("1-50");
        
        int minReps = (type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) ? 0 : 1;
        if (item.templateExercise.targetReps < minReps) holder.editReps.setError("Min " + minReps);
        
        if (type == MeasureType.WEIGHT_REPS && item.templateExercise.targetWeight < 0.5) 
            holder.editWeight.setError("Min 0.5");
            
        if (type == MeasureType.DISTANCE_TIME && item.templateExercise.targetDistance < 0.05) 
            holder.editDistance.setError("Min 0.05");
            
        if ((type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) && item.templateExercise.targetDuration < 5) 
            holder.editDuration.setError("Min 5s");
    }

    public boolean isAllValid() {
        if (items == null) return true;
        for (TemplateExerciseWithDetails item : items) {
            MeasureType type = item.exercise.measureType != null ? item.exercise.measureType : MeasureType.WEIGHT_REPS;
            if (item.templateExercise.targetSets < 1 || item.templateExercise.targetSets > 50) return false;
            
            int minReps = (type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) ? 0 : 1;
            if (item.templateExercise.targetReps < minReps || item.templateExercise.targetReps > 999) return false;
            
            if (type == MeasureType.WEIGHT_REPS && (item.templateExercise.targetWeight < 0.5 || item.templateExercise.targetWeight > 999.9)) return false;
            if (type == MeasureType.DISTANCE_TIME && (item.templateExercise.targetDistance < 0.05 || item.templateExercise.targetDistance > 999.9)) return false;
            if ((type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) && (item.templateExercise.targetDuration < 5 || item.templateExercise.targetDuration > 14400)) return false;
            if (item.templateExercise.restSeconds < 0 || item.templateExercise.restSeconds > 999) return false;
        }
        return true;
    }

    private int parseSafeInt(String s) {
        try {
            return s == null || s.isEmpty() ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double parseSafeDouble(String s) {
        try {
            return s == null || s.isEmpty() ? 0 : Double.parseDouble(s.replace(',', '.'));
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
