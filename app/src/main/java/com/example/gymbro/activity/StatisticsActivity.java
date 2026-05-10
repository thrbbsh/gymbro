package com.example.gymbro.activity;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.EventDecorator;
import com.example.gymbro.R;
import com.example.gymbro.adapter.HistoryAdapter;
import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.entity.SessionSet;
import com.example.gymbro.db.model.SessionExerciseWithSets;
import com.example.gymbro.db.model.WorkoutSessionWithDetails;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter;
import com.prolificinteractive.materialcalendarview.format.MonthArrayTitleFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class StatisticsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private TextView textEmptyHistory;
    private MaterialCalendarView calendarView;
    private HistoryAdapter adapter;
    private AppDatabase db;
    private List<WorkoutSessionWithDetails> allSessions = new ArrayList<>();

    private BarChart barChart;
    private Spinner spinnerChartType, spinnerChartPeriod;
    private final String[] chartTypes = {"Sessions", "Tonnage", "Distance", "Duration"};
    private final String[] periodTypes = {"Daily", "Weekly"};

    // Summary TextViews
    private TextView valStatsSessions, valStatsTonnage, valStatsDistance, valStatsTime, textSummaryTitle;

    private Calendar summaryCal = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistics);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = AppDatabase.getDatabase(this);
        
        // Bind Summary Views
        valStatsSessions = findViewById(R.id.valStatsSessions);
        valStatsTonnage = findViewById(R.id.valStatsTonnage);
        valStatsDistance = findViewById(R.id.valStatsDistance);
        valStatsTime = findViewById(R.id.valStatsTime);
        textSummaryTitle = findViewById(R.id.textSummaryTitle);

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        textEmptyHistory = findViewById(R.id.textEmptyHistory);
        calendarView = findViewById(R.id.calendarView);
        barChart = findViewById(R.id.barChart);
        spinnerChartType = findViewById(R.id.spinnerChartType);
        spinnerChartPeriod = findViewById(R.id.spinnerChartPeriod);

        setupCalendar();
        setupChart();
        setupSpinners();
        setupNavigation();

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            filterHistoryByDate(date.getYear(), date.getMonth(), date.getDay());
        });

        loadAllHistory();
    }

    private void setupCalendar() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        calendarView.setArrowColor(currentNightMode == Configuration.UI_MODE_NIGHT_YES ? Color.WHITE : Color.BLACK);

        calendarView.setHeaderTextAppearance(R.style.CalendarHeaderCustom);
        calendarView.setDateTextAppearance(R.style.CalendarDateCustom);
        calendarView.setWeekDayTextAppearance(R.style.CalendarDateCustom);

        calendarView.setTitleFormatter(new MonthArrayTitleFormatter(new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        }));
        calendarView.setWeekDayFormatter(new ArrayWeekDayFormatter(new String[]{
                "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
        }));

        // Static calendar: disable paging between months via swipe
        calendarView.setPagingEnabled(false);

        calendarView.state().edit()
                .setFirstDayOfWeek(Calendar.MONDAY)
                .commit();
    }

    private void setupChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false); 
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setDragEnabled(false); 

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.GRAY);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f);
        leftAxis.setGranularityEnabled(true);

        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
    }

    private void setupSpinners() {
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, chartTypes);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartType.setAdapter(typeAdapter);

        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, periodTypes);
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartPeriod.setAdapter(periodAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerChartType.setOnItemSelectedListener(listener);
        spinnerChartPeriod.setOnItemSelectedListener(listener);
    }

    private void setupNavigation() {
        // Navigation is handled solely by the calendar arrows now.
        // Synchronizing stats and chart when the calendar month changes.
        calendarView.setOnMonthChangedListener((widget, date) -> {
            summaryCal.set(Calendar.YEAR, date.getYear());
            summaryCal.set(Calendar.MONTH, date.getMonth());
            summaryCal.set(Calendar.DAY_OF_MONTH, 1);
            updateSummaryStats();
            updateChart();
        });
    }

    private void loadAllHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            allSessions = db.historyDao().getAllSessionsWithDetails();
            Collections.sort(allSessions, Comparator.comparingLong(s -> s.session.date));
            
            HashSet<CalendarDay> workoutDates = new HashSet<>();
            for (WorkoutSessionWithDetails wrapper : allSessions) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(wrapper.session.date);
                workoutDates.add(CalendarDay.from(cal));
            }

            runOnUiThread(() -> {
                calendarView.addDecorator(new EventDecorator(Color.parseColor("#4CAF50"), workoutDates));
                calendarView.setSelectedDate(CalendarDay.today());
                
                Calendar today = Calendar.getInstance();
                filterHistoryByDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
                updateSummaryStats();
                updateChart();
            });
        });
    }

    private void updateSummaryStats() {
        int year = summaryCal.get(Calendar.YEAR);
        int month = summaryCal.get(Calendar.MONTH);
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.US);
        textSummaryTitle.setText(sdf.format(summaryCal.getTime()));

        int sessionsCount = 0;
        double totalTonnage = 0;
        double totalDistance = 0;
        long totalDuration = 0;

        Calendar sessionCal = Calendar.getInstance();
        for (WorkoutSessionWithDetails session : allSessions) {
            sessionCal.setTimeInMillis(session.session.date);
            if (sessionCal.get(Calendar.MONTH) == month && sessionCal.get(Calendar.YEAR) == year) {
                sessionsCount++;
                
                for (SessionExerciseWithSets ex : session.exercises) {
                    if (ex.sets != null) {
                        for (SessionSet set : ex.sets) {
                            if (!set.isSkipped) {
                                totalTonnage += (set.weight * set.reps);
                                totalDistance += set.distance;
                                totalDuration += set.duration;
                            }
                        }
                    }
                }
            }
        }

        valStatsSessions.setText(String.valueOf(sessionsCount));
        valStatsTonnage.setText(String.format(Locale.US, "%.0f kg", totalTonnage));
        valStatsDistance.setText(String.format(Locale.US, "%.1f km", totalDistance));
        valStatsTime.setText((totalDuration / 60) + " min");
    }

    private void updateChart() {
        if (allSessions == null || barChart == null) return;

        int typeIndex = spinnerChartType.getSelectedItemPosition();
        int periodIndex = spinnerChartPeriod.getSelectedItemPosition();

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Float> rawValues = new ArrayList<>();

        if (periodIndex == 0) { // Daily
            Calendar c = (Calendar) summaryCal.clone();
            c.set(Calendar.DAY_OF_MONTH, 1);
            int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            
            for (int d = 1; d <= daysInMonth; d++) {
                c.set(Calendar.DAY_OF_MONTH, d);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                long dayStart = c.getTimeInMillis();
                
                Calendar cNext = (Calendar) c.clone();
                cNext.add(Calendar.DAY_OF_MONTH, 1);
                long dayEnd = cNext.getTimeInMillis();
                
                float val = 0;
                for (WorkoutSessionWithDetails session : allSessions) {
                    if (session.session.date >= dayStart && session.session.date < dayEnd) {
                        val += getSessionValue(session, typeIndex);
                    }
                }
                rawValues.add(val);
                labels.add(String.valueOf(d));
            }
        } else { // Weekly (Monday in this month)
            Calendar c = (Calendar) summaryCal.clone();
            c.setFirstDayOfWeek(Calendar.MONDAY);
            c.set(Calendar.DAY_OF_MONTH, 1);
            int maxDays = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.US);
            
            for (int d = 1; d <= maxDays; d++) {
                c.set(Calendar.DAY_OF_MONTH, d);
                if (c.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                    c.set(Calendar.HOUR_OF_DAY, 0);
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    long weekStart = c.getTimeInMillis();
                    String startStr = sdf.format(c.getTime());
                    
                    Calendar cEnd = (Calendar) c.clone();
                    cEnd.add(Calendar.DAY_OF_WEEK, 6);
                    cEnd.set(Calendar.HOUR_OF_DAY, 23);
                    cEnd.set(Calendar.MINUTE, 59);
                    cEnd.set(Calendar.SECOND, 59);
                    long weekEnd = cEnd.getTimeInMillis();
                    String endStr = sdf.format(cEnd.getTime());
                    
                    labels.add(startStr + "-" + endStr);
                    
                    float val = 0;
                    for (WorkoutSessionWithDetails session : allSessions) {
                        if (session.session.date >= weekStart && session.session.date <= weekEnd) {
                            val += getSessionValue(session, typeIndex);
                        }
                    }
                    rawValues.add(val);
                }
            }
        }

        if (rawValues.isEmpty()) {
            barChart.clear();
            return;
        }

        for (int i = 0; i < rawValues.size(); i++) {
            entries.add(new BarEntry(i, rawValues.get(i)));
        }

        BarDataSet dataSet = new BarDataSet(entries, chartTypes[typeIndex]);
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setDrawValues(false);

        barChart.setData(new BarData(dataSet));
        
        XAxis xAxis = barChart.getXAxis();
        // Custom formatter to show labels only for specific days (1, 7, 14, 21, 28)
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels) {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index < 0 || index >= labels.size()) return "";
                if (periodIndex == 0) { // Daily logic: labels index 0 is day 1
                    int day = index + 1;
                    if (day == 1 || day == 7 || day == 14 || day == 21 || day == 28) {
                        return labels.get(index);
                    }
                    return "";
                }
                return labels.get(index);
            }
        });

        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(labels.size() - 0.5f);
        
        // Static chart: reset any zoom to show all bars on screen
        barChart.fitScreen();
        
        // Ensure the chart evaluates enough labels to show our chosen ones
        xAxis.setLabelCount(labels.size(), false);
        
        barChart.animateY(800);
        barChart.invalidate();
    }

    private float getSessionValue(WorkoutSessionWithDetails session, int typeIndex) {
        float val = 0;
        if (typeIndex == 0) return 1f;
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
        return val;
    }

    private void filterHistoryByDate(int year, int month, int dayOfMonth) {
        List<HistoryAdapter.HistoryItem> filteredItems = new ArrayList<>();
        SimpleDateFormat dateSdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.US);

        for (WorkoutSessionWithDetails details : allSessions) {
            Calendar sessionCal = Calendar.getInstance();
            sessionCal.setTimeInMillis(details.session.date);

            if (sessionCal.get(Calendar.YEAR) == year &&
                sessionCal.get(Calendar.MONTH) == month &&
                sessionCal.get(Calendar.DAY_OF_MONTH) == dayOfMonth) {

                String dateStr = dateSdf.format(new Date(details.session.date));
                String timeStr = timeSdf.format(new Date(details.session.date));

                filteredItems.add(new HistoryAdapter.HistoryItem(details, dateStr, timeStr));
            }
        }

        if (filteredItems.isEmpty()) {
            textEmptyHistory.setVisibility(View.VISIBLE);
            recyclerViewHistory.setVisibility(View.GONE);
        } else {
            textEmptyHistory.setVisibility(View.GONE);
            recyclerViewHistory.setVisibility(View.VISIBLE);
            adapter = new HistoryAdapter(filteredItems);
            recyclerViewHistory.setAdapter(adapter);
        }
    }
}
