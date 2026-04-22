package com.example.gymbro;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.db.AppDatabase;
import com.example.gymbro.db.model.WorkoutSessionWithTemplate;
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
    private List<WorkoutSessionWithTemplate> allSessions = new ArrayList<>();

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
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        textEmptyHistory = findViewById(R.id.textEmptyHistory);
        calendarView = findViewById(R.id.calendarView);
        
        // Fix dark/light theme visibility
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            calendarView.setArrowColor(Color.WHITE);
        } else {
            calendarView.setArrowColor(Color.BLACK);
        }

        calendarView.setHeaderTextAppearance(R.style.CalendarHeaderCustom);
        calendarView.setDateTextAppearance(R.style.CalendarDateCustom);
        calendarView.setWeekDayTextAppearance(R.style.CalendarDateCustom);

        // 2. Force English labels for months and weekdays
        calendarView.setTitleFormatter(new MonthArrayTitleFormatter(new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        }));
        calendarView.setWeekDayFormatter(new ArrayWeekDayFormatter(new String[]{
                "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
        }));

        // Set Monday as first day
        calendarView.state().edit()
                .setFirstDayOfWeek(Calendar.MONDAY)
                .commit();

        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            filterHistoryByDate(date.getYear(), date.getMonth(), date.getDay());
        });

        loadAllHistory();
    }

    private void loadAllHistory() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Get sessions along with their template names
            allSessions = db.historyDao().getAllSessionsWithTemplate();
            
            // Sort sessions by time
            Collections.sort(allSessions, Comparator.comparingLong(s -> s.session.date));
            
            HashSet<CalendarDay> workoutDates = new HashSet<>();
            for (WorkoutSessionWithTemplate wrapper : allSessions) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(wrapper.session.date);
                workoutDates.add(CalendarDay.from(cal));
            }

            Calendar today = Calendar.getInstance();
            runOnUiThread(() -> {
                calendarView.addDecorator(new EventDecorator(Color.parseColor("#4CAF50"), workoutDates));
                calendarView.setSelectedDate(CalendarDay.today());
                filterHistoryByDate(
                    today.get(Calendar.YEAR),
                    today.get(Calendar.MONTH),
                    today.get(Calendar.DAY_OF_MONTH)
                );
            });
        });
    }

    private void filterHistoryByDate(int year, int month, int dayOfMonth) {
        List<HistoryAdapter.HistoryItem> filteredItems = new ArrayList<>();
        SimpleDateFormat dateSdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.US);

        for (WorkoutSessionWithTemplate wrapper : allSessions) {
            Calendar sessionCal = Calendar.getInstance();
            sessionCal.setTimeInMillis(wrapper.session.date);

            if (sessionCal.get(Calendar.YEAR) == year &&
                sessionCal.get(Calendar.MONTH) == month &&
                sessionCal.get(Calendar.DAY_OF_MONTH) == dayOfMonth) {

                String dateStr = dateSdf.format(new Date(wrapper.session.date));
                String timeStr = timeSdf.format(new Date(wrapper.session.date));
                
                // Now we get the actual template name from the relation
                String name = wrapper.template != null ? wrapper.template.name : "Unknown Workout";
                
                filteredItems.add(new HistoryAdapter.HistoryItem(name, dateStr, timeStr, "Completed"));
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
