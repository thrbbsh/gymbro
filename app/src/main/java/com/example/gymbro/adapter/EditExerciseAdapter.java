package com.example.gymbro.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;

import java.util.List;

public class EditExerciseAdapter extends RecyclerView.Adapter<EditExerciseAdapter.EditViewHolder> {
    private List<TemplateExerciseWithDetails> items;

    public EditExerciseAdapter(List<TemplateExerciseWithDetails> items) {
        this.items = items;
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

        // Show/hide duration field based on targetDuration value
        if (item.templateExercise.targetDuration <= 0) {
            holder.layoutDuration.setVisibility(View.GONE);
        } else {
            holder.layoutDuration.setVisibility(View.VISIBLE);
        }

        holder.btnDelete.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Delete Exercise functionality coming soon", Toast.LENGTH_SHORT).show();
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
