package com.example.gymbro.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.db.entity.MeasureType;
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
        
        holder.textViewName.setText(item.exercise.name);
        
        // Format Primary and Secondary Muscles with |
        StringBuilder muscles = new StringBuilder(item.exercise.target);
        if (item.exercise.secondaryMuscles != null && !item.exercise.secondaryMuscles.isEmpty()) {
            muscles.append(" | ");
            muscles.append(TextUtils.join(", ", item.exercise.secondaryMuscles));
        }
        holder.textViewMuscle.setText(muscles.toString());
        
        // Format target info based on MeasureType
        MeasureType type = item.exercise.measureType;
        if (type == null) type = MeasureType.WEIGHT_REPS;
        
        StringBuilder target = new StringBuilder();
        int sets = item.templateExercise.targetSets;
        
        switch (type) {
            case WEIGHT_REPS:
                target.append(sets).append(" sets x ").append(item.templateExercise.targetReps).append(" reps");
                if (item.templateExercise.targetWeight > 0) {
                    target.append(" @ ").append(item.templateExercise.targetWeight).append(" kg");
                }
                break;
            case BODYWEIGHT_REPS:
                target.append(sets).append(" sets x ").append(item.templateExercise.targetReps).append(" reps (Bodyweight)");
                break;
            case DURATION:
                target.append(sets).append(" sets x ").append(item.templateExercise.targetDuration).append("s (Hold)");
                break;
            case DISTANCE_TIME:
                if (item.templateExercise.targetDistance > 0) {
                    target.append(item.templateExercise.targetDistance).append(" km in ");
                }
                target.append(item.templateExercise.targetDuration).append("s");
                break;
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
