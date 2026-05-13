package com.example.gymbro.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.adapter.WorkoutAdapter;
import com.example.gymbro.db.entity.WorkoutTemplate;
import com.example.gymbro.utils.ExerciseMeasureHelper;
import com.example.gymbro.viewmodel.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textLoading;
    private View syncOverlay;
    private View syncLoadingLayout;
    private View syncErrorLayout;
    private TextView textSyncError;
    private WorkoutAdapter adapter;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ExerciseMeasureHelper.loadExceptions(this);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initViews();
        setupObservers();

        viewModel.syncExercises();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewTemplates);
        textLoading = findViewById(R.id.textLoadingTemplates);
        syncOverlay = findViewById(R.id.syncOverlay);
        syncLoadingLayout = findViewById(R.id.syncLoadingLayout);
        syncErrorLayout = findViewById(R.id.syncErrorLayout);
        textSyncError = findViewById(R.id.textSyncError);
        FloatingActionButton fabAddTemplate = findViewById(R.id.fabAddTemplate);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button btnStatistics = findViewById(R.id.buttonStats);
        btnStatistics.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        Button btnCommunity = findViewById(R.id.buttonCommunity);
        btnCommunity.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CommunityActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnRetrySync).setOnClickListener(v -> viewModel.syncExercises());

        fabAddTemplate.setOnClickListener(v -> showAddTemplateDialog());
    }

    private void setupObservers() {
        viewModel.getTemplates().observe(this, templates -> {
            textLoading.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            
            adapter = new WorkoutAdapter(templates, new WorkoutAdapter.OnTemplateActionListener() {
                @Override
                public void onTemplateClick(WorkoutTemplate template) {
                    Intent intent = new Intent(MainActivity.this, ExerciseActivity.class);
                    intent.putExtra("TEMPLATE_ID", template.id);
                    intent.putExtra("TEMPLATE_NAME", template.name);
                    startActivity(intent);
                }

                @Override
                public void onTemplateDelete(WorkoutTemplate template) {
                    showDeleteConfirmation(template);
                }
            });
            recyclerView.setAdapter(adapter);
        });

        viewModel.isSyncing().observe(this, syncing -> {
            syncOverlay.setVisibility(syncing ? View.VISIBLE : View.GONE);
            syncLoadingLayout.setVisibility(syncing ? View.VISIBLE : View.GONE);
            if (syncing) syncErrorLayout.setVisibility(View.GONE);
        });

        viewModel.getSyncError().observe(this, error -> {
            if (error != null) {
                syncOverlay.setVisibility(View.VISIBLE);
                syncLoadingLayout.setVisibility(View.GONE);
                syncErrorLayout.setVisibility(View.VISIBLE);
                textSyncError.setText(error);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadTemplates();
    }

    private void showAddTemplateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("New Workout Template");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint("Template Name");
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        builder.setView(input);
        input.setPadding(padding, padding, padding, padding);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                viewModel.createTemplate(name);
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteConfirmation(WorkoutTemplate template) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Template")
                .setMessage("Are you sure you want to delete '" + template.name + "'?")
                .setPositiveButton("Delete", (dialog, which) -> viewModel.deleteTemplate(template))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
