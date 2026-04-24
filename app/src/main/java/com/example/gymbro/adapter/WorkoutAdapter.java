package com.example.gymbro.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.db.entity.WorkoutTemplate;

import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private List<WorkoutTemplate> templates;
    private OnTemplateActionListener listener;

    public interface OnTemplateActionListener {
        void onTemplateClick(WorkoutTemplate template);
        void onTemplateDelete(WorkoutTemplate template);
    }

    public WorkoutAdapter(List<WorkoutTemplate> templates, OnTemplateActionListener listener) {
        this.templates = templates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_template, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        WorkoutTemplate template = templates.get(position);
        holder.textViewName.setText(template.name);
        holder.itemView.setOnClickListener(v -> listener.onTemplateClick(template));
        holder.buttonDelete.setOnClickListener(v -> listener.onTemplateDelete(template));
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        ImageButton buttonDelete;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewTemplateName);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteTemplate);
        }
    }
}
