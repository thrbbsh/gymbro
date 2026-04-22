package com.example.gymbro.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;

import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

    private List<TemplateExerciseWithDetails> items;

    public ExerciseAdapter(List<TemplateExerciseWithDetails> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise_details, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        TemplateExerciseWithDetails item = items.get(position);
        
        // Exercise Info
        holder.textViewName.setText(item.exercise.name);
        holder.textViewMuscle.setText(item.exercise.primaryMuscle);
        
        // Target Info (Sets/Reps or Duration)
        StringBuilder target = new StringBuilder();
        if (item.templateExercise.targetSets > 0) {
            target.append(item.templateExercise.targetSets).append(" sets");
            if (item.templateExercise.targetReps > 0) {
                target.append(" x ").append(item.templateExercise.targetReps).append(" reps");
            }
        } else if (item.templateExercise.targetDuration > 0) {
            target.append("Duration: ").append(item.templateExercise.targetDuration).append("s");
        }
        
        holder.textViewTarget.setText(target.toString());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewMuscle, textViewTarget;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewExName);
            textViewMuscle = itemView.findViewById(R.id.textViewExMuscle);
            textViewTarget = itemView.findViewById(R.id.textViewExTarget);
        }
    }
}