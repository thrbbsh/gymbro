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
import java.util.Locale;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {

    private final List<TemplateExerciseWithDetails> items;

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
        holder.textViewMuscle.setText(getFormattedMuscles(item));
        holder.textViewTarget.setText(getFormattedTarget(item));
    }

    private String getFormattedMuscles(TemplateExerciseWithDetails item) {
        StringBuilder muscles = new StringBuilder(item.exercise.target);
        if (item.exercise.secondaryMuscles != null && !item.exercise.secondaryMuscles.isEmpty()) {
            muscles.append(" | ").append(TextUtils.join(", ", item.exercise.secondaryMuscles));
        }
        return muscles.toString();
    }

    private String getFormattedTarget(TemplateExerciseWithDetails item) {
        MeasureType type = item.exercise.measureType != null ? item.exercise.measureType : MeasureType.WEIGHT_REPS;
        int sets = item.templateExercise.targetSets;
        int reps = item.templateExercise.targetReps;
        
        switch (type) {
            case WEIGHT_REPS:
                String weightStr = item.templateExercise.targetWeight > 0 ? String.format(Locale.US, " @ %.1f kg", item.templateExercise.targetWeight) : "";
                return String.format(Locale.US, "%d sets x %d reps%s", sets, reps, weightStr);
            case BODYWEIGHT_REPS:
                return String.format(Locale.US, "%d sets x %d reps (Bodyweight)", sets, reps);
            case DURATION:
                return String.format(Locale.US, "%d sets x %ds (Hold)", sets, item.templateExercise.targetDuration);
            case DISTANCE_TIME:
                String distStr = item.templateExercise.targetDistance > 0 ? String.format(Locale.US, "%.1f km in ", item.templateExercise.targetDistance) : "";
                return String.format(Locale.US, "%s%ds", distStr, item.templateExercise.targetDuration);
            default:
                return "";
        }
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
