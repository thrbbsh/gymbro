package com.example.gymbro.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.db.entity.Exercise;

import java.util.ArrayList;
import java.util.List;

public class SearchExerciseAdapter extends RecyclerView.Adapter<SearchExerciseAdapter.ViewHolder> {

    private List<Exercise> exercises;
    private List<Exercise> filteredList;
    private final OnExerciseClickListener listener;

    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise);
    }

    public SearchExerciseAdapter(List<Exercise> exercises, OnExerciseClickListener listener) {
        this.exercises = exercises;
        this.filteredList = new ArrayList<>(exercises);
        this.listener = listener;
    }

    public void filter(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(exercises);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (Exercise exercise : exercises) {
                if (exercise.name.toLowerCase().contains(lowerCaseQuery) ||
                    exercise.target.toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(exercise);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = filteredList.get(position);
        holder.text1.setText(exercise.name);
        holder.text2.setText(exercise.target + " | " + exercise.bodyPart);
        holder.itemView.setOnClickListener(v -> listener.onExerciseClick(exercise));
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        ViewHolder(View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }
}
