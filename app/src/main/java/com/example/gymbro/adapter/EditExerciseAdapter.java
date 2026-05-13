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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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

    public void setItems(List<TemplateExerciseWithDetails> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
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
        
        final MeasureType type = item.exercise.measureType != null ? item.exercise.measureType : MeasureType.WEIGHT_REPS;

        // Visibility logic
        holder.layoutWeight.setVisibility(type == MeasureType.WEIGHT_REPS ? View.VISIBLE : View.GONE);
        holder.layoutReps.setVisibility((type == MeasureType.WEIGHT_REPS || type == MeasureType.BODYWEIGHT_REPS) ? View.VISIBLE : View.GONE);
        holder.layoutDuration.setVisibility((type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) ? View.VISIBLE : View.GONE);
        holder.layoutDistance.setVisibility(type == MeasureType.DISTANCE_TIME ? View.VISIBLE : View.GONE);

        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(item);
        });

        int minReps = (type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) ? 0 : 1;

        // Updated minimums: Weight 1.0, Reps 1.0 (for reps types), Distance 0.1, Duration 5
        bindInt(holder.editSets, item.templateExercise.targetSets, 1, 50, "1-50", v -> item.templateExercise.targetSets = v, item, holder);
        bindInt(holder.editReps, item.templateExercise.targetReps, minReps, 999, "Min " + minReps, v -> item.templateExercise.targetReps = v, item, holder);
        bindDouble(holder.editWeight, item.templateExercise.targetWeight, 1.0, 999.9, "Min 1.0", v -> item.templateExercise.targetWeight = v, item, holder);
        bindDouble(holder.editDistance, item.templateExercise.targetDistance, 0.1, 999.9, "Min 0.1", v -> item.templateExercise.targetDistance = v, item, holder);
        bindInt(holder.editDuration, item.templateExercise.targetDuration, 5, 14400, "Min 5s", v -> item.templateExercise.targetDuration = v, item, holder);
        bindInt(holder.editRest, item.templateExercise.restSeconds, 0, 999, "0-999", v -> item.templateExercise.restSeconds = v, item, holder);
    }

    private void bindInt(EditText et, int value, int min, int max, String error, Consumer<Integer> setter, TemplateExerciseWithDetails item, EditViewHolder holder) {
        et.setText(String.valueOf(value));
        if (value < min || value > max) et.setError(error);
        
        TextWatcher watcher = new SimpleTextWatcher(s -> {
            int val = parseSafeInt(s);
            if (val >= min && val <= max) {
                setter.accept(val);
                actionListener.onUpdate(item);
                et.setError(null);
            } else {
                et.setError(error);
            }
        });
        holder.addWatcher(et, watcher);
    }

    private void bindDouble(EditText et, double value, double min, double max, String error, Consumer<Double> setter, TemplateExerciseWithDetails item, EditViewHolder holder) {
        et.setText(String.valueOf(value));
        if (value < min || value > max) et.setError(error);

        TextWatcher watcher = new SimpleTextWatcher(s -> {
            double val = parseSafeDouble(s);
            if (val >= min && val <= max) {
                setter.accept(val);
                actionListener.onUpdate(item);
                et.setError(null);
            } else {
                et.setError(error);
            }
        });
        holder.addWatcher(et, watcher);
    }

    public boolean isAllValid() {
        if (items == null) return true;
        for (TemplateExerciseWithDetails item : items) {
            MeasureType type = item.exercise.measureType != null ? item.exercise.measureType : MeasureType.WEIGHT_REPS;
            if (item.templateExercise.targetSets < 1 || item.templateExercise.targetSets > 50) return false;

            int minReps = (type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) ? 0 : 1;
            if (item.templateExercise.targetReps < minReps || item.templateExercise.targetReps > 999) return false;

            if (type == MeasureType.WEIGHT_REPS && (item.templateExercise.targetWeight < 1.0 || item.templateExercise.targetWeight > 999.9)) return false;
            if (type == MeasureType.DISTANCE_TIME && (item.templateExercise.targetDistance < 0.1 || item.templateExercise.targetDistance > 999.9)) return false;
            if ((type == MeasureType.DURATION || type == MeasureType.DISTANCE_TIME) && (item.templateExercise.targetDuration < 5 || item.templateExercise.targetDuration > 14400)) return false;
            if (item.templateExercise.restSeconds < 0 || item.templateExercise.restSeconds > 999) return false;
        }
        return true;
    }

    private int parseSafeInt(String s) {
        try { return s == null || s.isEmpty() ? 0 : Integer.parseInt(s); }
        catch (NumberFormatException e) { return 0; }
    }

    private double parseSafeDouble(String s) {
        try { return s == null || s.isEmpty() ? 0 : Double.parseDouble(s.replace(',', '.')); }
        catch (NumberFormatException e) { return 0; }
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class EditViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        EditText editSets, editReps, editWeight, editDistance, editDuration, editRest;
        View layoutWeight, layoutReps, layoutDistance, layoutDuration;
        ImageButton btnDelete;
        
        private final Map<EditText, TextWatcher> watchers = new HashMap<>();

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

        void addWatcher(EditText et, TextWatcher watcher) {
            watchers.put(et, watcher);
            et.addTextChangedListener(watcher);
        }

        void clearWatchers() {
            for (Map.Entry<EditText, TextWatcher> entry : watchers.entrySet()) {
                entry.getKey().removeTextChangedListener(entry.getValue());
                entry.getKey().setError(null);
            }
            watchers.clear();
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
