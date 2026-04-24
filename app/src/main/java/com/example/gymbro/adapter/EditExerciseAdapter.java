package com.example.gymbro.adapter;

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
    private final OnExerciseDeleteListener deleteListener;

    public interface OnExerciseDeleteListener {
        void onDelete(TemplateExerciseWithDetails item);
    }

    public EditExerciseAdapter(List<TemplateExerciseWithDetails> items, OnExerciseDeleteListener deleteListener) {
        this.items = items;
        this.deleteListener = deleteListener;
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
            if (deleteListener != null) {
                deleteListener.onDelete(item);
            }
        });
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
    }
}
