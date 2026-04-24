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
        holder.editDuration.setText(String.valueOf(item.templateExercise.targetDuration));
        holder.editRest.setText(String.valueOf(item.templateExercise.restSeconds));

        if (item.templateExercise.targetDuration <= 0) {
            holder.layoutDuration.setVisibility(View.GONE);
        } else {
            holder.layoutDuration.setVisibility(View.VISIBLE);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDelete(item);
            }
        });

        holder.setsWatcher = new SimpleTextWatcher(s -> {
            if (s.isEmpty()) {
                holder.editSets.setError("Cannot be empty");
                return;
            }
            int val = parseSafe(s);
            if (val <= 0 || val > 99) {
                holder.editSets.setError("1-99");
                return;
            }
            item.templateExercise.targetSets = val;
            actionListener.onUpdate(item);
        });
        holder.repsWatcher = new SimpleTextWatcher(s -> {
            if (s.isEmpty()) {
                holder.editReps.setError("Cannot be empty");
                return;
            }
            int val = parseSafe(s);
            if (val <= 0 || val > 999) {
                holder.editReps.setError("1-999");
                return;
            }
            item.templateExercise.targetReps = val;
            actionListener.onUpdate(item);
        });
        holder.durationWatcher = new SimpleTextWatcher(s -> {
            if (s.isEmpty()) {
                holder.editDuration.setError("Cannot be empty");
                return;
            }
            int val = parseSafe(s);
            if (val < 0 || val > 3600) {
                holder.editDuration.setError("Max 3600s");
                return;
            }
            item.templateExercise.targetDuration = val;
            actionListener.onUpdate(item);
        });
        holder.restWatcher = new SimpleTextWatcher(s -> {
            if (s.isEmpty()) {
                holder.editRest.setError("Cannot be empty");
                return;
            }
            int val = parseSafe(s);
            if (val < 0 || val > 999) {
                holder.editRest.setError("Max 999s");
                return;
            }
            item.templateExercise.restSeconds = val;
            actionListener.onUpdate(item);
        });

        holder.editSets.addTextChangedListener(holder.setsWatcher);
        holder.editReps.addTextChangedListener(holder.repsWatcher);
        holder.editDuration.addTextChangedListener(holder.durationWatcher);
        holder.editRest.addTextChangedListener(holder.restWatcher);
    }

    private int parseSafe(String s) {
        try {
            return Integer.parseInt(s);
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
        EditText editSets, editReps, editDuration, editRest;
        View layoutDuration;
        ImageButton btnDelete;
        
        TextWatcher setsWatcher, repsWatcher, durationWatcher, restWatcher;

        public EditViewHolder(View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textViewExerciseName);
            editSets = itemView.findViewById(R.id.editTextSets);
            editReps = itemView.findViewById(R.id.editTextReps);
            editDuration = itemView.findViewById(R.id.editTextDuration);
            editRest = itemView.findViewById(R.id.editTextRest);
            layoutDuration = itemView.findViewById(R.id.layoutDuration);
            btnDelete = itemView.findViewById(R.id.buttonDeleteExercise);
        }

        void clearWatchers() {
            if (setsWatcher != null) editSets.removeTextChangedListener(setsWatcher);
            if (repsWatcher != null) editReps.removeTextChangedListener(repsWatcher);
            if (durationWatcher != null) editDuration.removeTextChangedListener(durationWatcher);
            if (restWatcher != null) editRest.removeTextChangedListener(restWatcher);
            
            editSets.setError(null);
            editReps.setError(null);
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
