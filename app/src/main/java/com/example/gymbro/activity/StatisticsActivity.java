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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.EventDecorator;
import com.example.gymbro.R;
import com.example.gymbro.adapter.HistoryAdapter;
import com.example.gymbro.db.model.WorkoutSessionWithDetails;
import com.example.gymbro.viewmodel.StatisticsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.format.ArrayWeekDayFormatter;
import com.prolificinteractive.materialcalendarview.format.MonthArrayTitleFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private TextView textEmptyHistory;
    private MaterialCalendarView calendarView;
    private BarChart barChart;
    private Spinner spinnerChartType, spinnerChartPeriod;
    private final String[] chartTypes = {"Sessions", "Tonnage", "Distance", "Duration"};
    private final String[] periodTypes = {"Daily", "Weekly"};

    private TextView valStatsSessions, valStatsTonnage, valStatsDistance, valStatsTime, textSummaryTitle;

    private StatisticsViewModel viewModel;
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

        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        initViews();
        setupCalendar();
        setupChart();
        setupSpinners();
        setupObservers();

        viewModel.loadAllSessions();
    }

    private void initViews() {
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

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupObservers() {
        viewModel.getAllSessions().observe(this, sessions -> {
            HashSet<CalendarDay> workoutDates = new HashSet<>();
            for (WorkoutSessionWithDetails wrapper : sessions) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(wrapper.session.date);
                workoutDates.add(CalendarDay.from(cal));
            }
            calendarView.addDecorator(new EventDecorator(Color.parseColor("#4CAF50"), workoutDates));
            
            Calendar today = Calendar.getInstance();
            viewModel.filterSessionsByDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
            refreshMonthData();
        });

        viewModel.getFilteredSessions().observe(this, sessions -> {
            if (sessions.isEmpty()) {
                textEmptyHistory.setVisibility(View.VISIBLE);
                recyclerViewHistory.setVisibility(View.GONE);
            } else {
                textEmptyHistory.setVisibility(View.GONE);
                recyclerViewHistory.setVisibility(View.VISIBLE);
                
                List<HistoryAdapter.HistoryItem> items = new ArrayList<>();
                SimpleDateFormat dateSdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
                SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.US);
                
                for (WorkoutSessionWithDetails details : sessions) {
                    items.add(new HistoryAdapter.HistoryItem(details, 
                            dateSdf.format(new Date(details.session.date)), 
                            timeSdf.format(new Date(details.session.date))));
                }
                recyclerViewHistory.setAdapter(new HistoryAdapter(items));
            }
        });

        viewModel.getSummaryStats().observe(this, stats -> {
            if (stats != null) {
                valStatsSessions.setText(String.valueOf(stats.count));
                valStatsTonnage.setText(String.format(Locale.US, "%.0f kg", stats.tonnage));
                valStatsDistance.setText(String.format(Locale.US, "%.1f km", stats.distance));
                valStatsTime.setText((stats.duration / 60) + " min");
            }
        });

        viewModel.getChartData().observe(this, data -> {
            BarDataSet dataSet = new BarDataSet(data.entries, chartTypes[spinnerChartType.getSelectedItemPosition()]);
            dataSet.setColor(Color.parseColor("#4CAF50"));
            dataSet.setDrawValues(false);

            barChart.setData(new BarData(dataSet));
            XAxis xAxis = barChart.getXAxis();
            final int periodIndex = spinnerChartPeriod.getSelectedItemPosition();
            
            xAxis.setValueFormatter(new IndexAxisValueFormatter(data.labels) {
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    if (index < 0 || index >= data.labels.size()) return "";
                    if (periodIndex == 0) {
                        int day = index + 1;
                        if (day == 1 || day == 7 || day == 14 || day == 21 || day == 28) return data.labels.get(index);
                        return "";
                    }
                    return data.labels.get(index);
                }
            });
            xAxis.setAxisMinimum(-0.5f);
            xAxis.setAxisMaximum(data.labels.size() - 0.5f);
            xAxis.setLabelCount(data.labels.size(), false);
            barChart.fitScreen();
            barChart.animateY(800);
            barChart.invalidate();
        });
    }

    private void setupCalendar() {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        calendarView.setArrowColor(currentNightMode == Configuration.UI_MODE_NIGHT_YES ? Color.WHITE : Color.BLACK);
        calendarView.setHeaderTextAppearance(R.style.CalendarHeaderCustom);
        calendarView.setDateTextAppearance(R.style.CalendarDateCustom);
        calendarView.setWeekDayTextAppearance(R.style.CalendarDateCustom);
        calendarView.setTitleFormatter(new MonthArrayTitleFormatter(new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"}));
        calendarView.setWeekDayFormatter(new ArrayWeekDayFormatter(new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}));
        calendarView.setPagingEnabled(false);
        calendarView.state().edit().setFirstDayOfWeek(Calendar.MONDAY).commit();
        calendarView.setSelectedDate(CalendarDay.today());

        calendarView.setOnDateChangedListener((widget, date, selected) -> 
            viewModel.filterSessionsByDate(date.getYear(), date.getMonth(), date.getDay()));

        calendarView.setOnMonthChangedListener((widget, date) -> {
            summaryCal.set(Calendar.YEAR, date.getYear());
            summaryCal.set(Calendar.MONTH, date.getMonth());
            summaryCal.set(Calendar.DAY_OF_MONTH, 1);
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.US);
            textSummaryTitle.setText(sdf.format(summaryCal.getTime()));
            refreshMonthData();
        });
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
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { refreshMonthData(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spinnerChartType.setOnItemSelectedListener(listener);
        spinnerChartPeriod.setOnItemSelectedListener(listener);
    }

    private void refreshMonthData() {
        viewModel.updateMonthData(summaryCal, spinnerChartType.getSelectedItemPosition(), spinnerChartPeriod.getSelectedItemPosition());
    }
}
