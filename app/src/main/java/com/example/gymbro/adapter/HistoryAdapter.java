package com.example.gymbro.adapter;

import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.db.entity.SessionSet;
import com.example.gymbro.db.model.SessionExerciseWithSets;
import com.example.gymbro.db.model.WorkoutSessionWithDetails;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<HistoryItem> historyList;

    public static class HistoryItem {
        public final WorkoutSessionWithDetails sessionDetails;
        public final String dateStr;
        public final String timeStr;
        public boolean isExpanded = false;

        public HistoryItem(WorkoutSessionWithDetails sessionDetails, String dateStr, String timeStr) {
            this.sessionDetails = sessionDetails;
            this.dateStr = dateStr;
            this.timeStr = timeStr;
        }
    }

    public HistoryAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        WorkoutSessionWithDetails details = item.sessionDetails;

        holder.textName.setText(details.template != null ? details.template.name : "Unknown Workout");
        holder.textDate.setText(item.dateStr);
        holder.textTime.setText(item.timeStr);

        // Update Toggle Text and Icon
        holder.textShowDetails.setText(item.isExpanded ? "Hide details" : "Show details");
        holder.textShowDetails.setCompoundDrawablesWithIntrinsicBounds(0, 0, 
                item.isExpanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down, 0);
        holder.textShowDetails.setCompoundDrawablePadding(8);

        holder.layoutDetailsContainer.setVisibility(item.isExpanded ? View.VISIBLE : View.GONE);

        holder.textShowDetails.setOnClickListener(v -> {
            item.isExpanded = !item.isExpanded;
            notifyItemChanged(position);
        });

        if (item.isExpanded) {
            populateDetails(holder.layoutDetailsContainer, details.exercises);
        }
    }

    private void populateDetails(LinearLayout container, List<SessionExerciseWithSets> exercises) {
        container.removeAllViews();
        if (exercises == null) return;

        for (SessionExerciseWithSets exWithSets : exercises) {
            // Exercise Name Header
            TextView exTitle = new TextView(container.getContext());
            exTitle.setText(exWithSets.exercise != null ? exWithSets.exercise.name : "Exercise");
            exTitle.setTypeface(null, Typeface.BOLD);
            exTitle.setPadding(0, 16, 0, 4);
            exTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            exTitle.setTextColor(ContextCompat.getColor(container.getContext(), R.color.black));
            container.addView(exTitle);

            // Sets
            if (exWithSets.sets != null) {
                for (SessionSet set : exWithSets.sets) {
                    // Skip if it's both extra and skipped
                    if (set.isExtra && set.isSkipped) continue;

                    StringBuilder sb = new StringBuilder();
                    sb.append("Set ").append(set.setNumber).append(": ");

                    int textColor;
                    if (set.isSkipped) {
                        sb.append("Skipped");
                        textColor = 0xFF9E9E9E; // Grey
                    } else {
                        List<String> metrics = new ArrayList<>();
                        if (set.weight > 0) metrics.add(set.weight + " kg");
                        if (set.reps > 0) metrics.add(set.reps + " reps");
                        if (set.duration > 0) metrics.add(set.duration + "s");
                        if (set.distance > 0) metrics.add(set.distance + " km");
                        
                        if (metrics.isEmpty()) {
                            sb.append("Completed");
                        } else {
                            sb.append(TextUtils.join(" • ", metrics));
                        }
                        
                        if (set.isExtra) {
                            sb.append(" (Extra Set)");
                            textColor = 0xFF1976D2; // Material Blue for Extra Sets
                        } else {
                            textColor = 0xFF000000; // Black for normal sets
                        }
                    }

                    TextView setInfo = new TextView(container.getContext());
                    setInfo.setText(sb.toString());
                    setInfo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    setInfo.setPadding(24, 2, 0, 2);
                    setInfo.setTextColor(textColor);
                    container.addView(setInfo);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textDate, textTime, textResult, textShowDetails;
        LinearLayout layoutDetailsContainer;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textHistoryName);
            textDate = itemView.findViewById(R.id.textHistoryDate);
            textTime = itemView.findViewById(R.id.textHistoryTime);
            textResult = itemView.findViewById(R.id.textHistoryResult);
            textShowDetails = itemView.findViewById(R.id.textShowDetails);
            layoutDetailsContainer = itemView.findViewById(R.id.layoutDetailsContainer);
        }
    }
}
