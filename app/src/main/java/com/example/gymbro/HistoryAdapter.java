package com.example.gymbro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<HistoryItem> historyList;

    public static class HistoryItem {
        public String name;
        public String date;
        public String time;
        public String result;

        public HistoryItem(String name, String date, String time, String result) {
            this.name = name;
            this.date = date;
            this.time = time;
            this.result = result;
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
        holder.textName.setText(item.name);
        holder.textDate.setText(item.date);
        holder.textTime.setText(item.time);
        holder.textResult.setText(item.result);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textDate, textTime, textResult;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textHistoryName);
            textDate = itemView.findViewById(R.id.textHistoryDate);
            textTime = itemView.findViewById(R.id.textHistoryTime);
            textResult = itemView.findViewById(R.id.textHistoryResult);
        }
    }
}