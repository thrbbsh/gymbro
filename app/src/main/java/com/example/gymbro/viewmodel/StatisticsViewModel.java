package com.example.gymbro.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gymbro.db.entity.SessionSet;
import com.example.gymbro.db.model.SessionExerciseWithSets;
import com.example.gymbro.db.model.WorkoutSessionWithDetails;
import com.example.gymbro.repository.WorkoutRepository;
import com.github.mikephil.charting.data.BarEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatisticsViewModel extends AndroidViewModel {
    private final WorkoutRepository repository;
    private final MutableLiveData<List<WorkoutSessionWithDetails>> allSessions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<WorkoutSessionWithDetails>> filteredSessions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<WorkoutRepository.StatsData> summaryStats = new MutableLiveData<>();
    private final MutableLiveData<ChartData> chartData = new MutableLiveData<>();

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        repository = WorkoutRepository.getInstance(application);
    }

    public LiveData<List<WorkoutSessionWithDetails>> getAllSessions() { return allSessions; }
    public LiveData<List<WorkoutSessionWithDetails>> getFilteredSessions() { return filteredSessions; }
    public LiveData<WorkoutRepository.StatsData> getSummaryStats() { return summaryStats; }
    public LiveData<ChartData> getChartData() { return chartData; }

    public void loadAllSessions() {
        repository.getAllSessionsWithDetails(allSessions::postValue);
    }

    public void filterSessionsByDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        long end = start + 24 * 60 * 60 * 1000L;
        repository.getSessionsInPeriod(start, end, filteredSessions::postValue);
    }

    public void updateMonthData(Calendar summaryCal, int typeIndex, int periodIndex) {
        Calendar cal = (Calendar) summaryCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        
        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.MONTH, 1);
        long end = endCal.getTimeInMillis();

        repository.getSummaryStats(start, end, summaryStats::postValue);
        prepareChartData(summaryCal, typeIndex, periodIndex);
    }

    private void prepareChartData(Calendar summaryCal, int typeIndex, int periodIndex) {
        Calendar cal = (Calendar) summaryCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        long start = getStartOfDay(cal);
        Calendar endCal = (Calendar) cal.clone();
        endCal.add(Calendar.MONTH, 1);
        long end = endCal.getTimeInMillis();

        repository.getSessionsInPeriod(start, end, sessions -> {
            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            if (periodIndex == 0) { // Daily
                int daysInMonth = summaryCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                for (int d = 1; d <= daysInMonth; d++) {
                    float val = 0;
                    long dayStart = getStartOfDayForDay(summaryCal, d);
                    long dayEnd = dayStart + 24 * 60 * 60 * 1000L;
                    for (WorkoutSessionWithDetails s : sessions) {
                        if (s.session.date >= dayStart && s.session.date < dayEnd) {
                            val += getSessionValue(s, typeIndex);
                        }
                    }
                    entries.add(new BarEntry(d - 1, val));
                    labels.add(String.valueOf(d));
                }
            } else { // Weekly
                int maxDays = summaryCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.US);
                int weekIdx = 0;
                for (int d = 1; d <= maxDays; d++) {
                    Calendar c = (Calendar) summaryCal.clone();
                    c.set(Calendar.DAY_OF_MONTH, d);
                    if (c.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                        float val = 0;
                        long weekStart = getStartOfDay(c);
                        long weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000L;
                        for (WorkoutSessionWithDetails s : sessions) {
                            if (s.session.date >= weekStart && s.session.date < weekEnd) {
                                val += getSessionValue(s, typeIndex);
                            }
                        }
                        entries.add(new BarEntry(weekIdx++, val));
                        labels.add(sdf.format(c.getTime()) + "-" + sdf.format(new java.util.Date(weekEnd - 1000)));
                    }
                }
            }
            chartData.postValue(new ChartData(entries, labels));
        });
    }

    private long getStartOfDay(Calendar cal) {
        Calendar c = (Calendar) cal.clone();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getStartOfDayForDay(Calendar monthCal, int day) {
        Calendar c = (Calendar) monthCal.clone();
        c.set(Calendar.DAY_OF_MONTH, day);
        return getStartOfDay(c);
    }

    private float getSessionValue(WorkoutSessionWithDetails session, int typeIndex) {
        if (typeIndex == 0) return 1f;
        float val = 0;
        if (session.exercises != null) {
            for (SessionExerciseWithSets ex : session.exercises) {
                if (ex.sets != null) {
                    for (SessionSet set : ex.sets) {
                        if (!set.isSkipped) {
                            if (typeIndex == 1) val += (float)(set.weight * set.reps);
                            else if (typeIndex == 2) val += (float)set.distance;
                            else if (typeIndex == 3) val += (float)set.duration / 60f;
                        }
                    }
                }
            }
        }
        return val;
    }

    public static class ChartData {
        public final List<BarEntry> entries;
        public final List<String> labels;
        public ChartData(List<BarEntry> entries, List<String> labels) { this.entries = entries; this.labels = labels; }
    }
}
