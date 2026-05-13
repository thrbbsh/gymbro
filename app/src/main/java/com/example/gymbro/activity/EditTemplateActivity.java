package com.example.gymbro.activity;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gymbro.R;
import com.example.gymbro.adapter.EditExerciseAdapter;
import com.example.gymbro.adapter.SearchExerciseAdapter;
import com.example.gymbro.db.entity.Exercise;
import com.example.gymbro.db.model.TemplateExerciseWithDetails;
import com.example.gymbro.viewmodel.EditTemplateViewModel;

import java.util.ArrayList;
import java.util.List;

public class EditTemplateActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditExerciseAdapter adapter;
    private EditText editTextTemplateName;
    private EditTemplateViewModel viewModel;
    private int templateId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_template);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(EditTemplateViewModel.class);

        recyclerView = findViewById(R.id.recyclerViewEditExercises);
        editTextTemplateName = findViewById(R.id.editTextTemplateName);
        Button buttonAddExercise = findViewById(R.id.buttonAddExercise);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);

        if (templateId != -1) {
            viewModel.loadTemplateData(templateId);
        }

        setupObservers();
        setupNameChangeListener();
        buttonAddExercise.setOnClickListener(v -> showAddExerciseDialog());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (adapter != null && !adapter.isAllValid()) {
                    Toast.makeText(EditTemplateActivity.this, "Please fix the errors before saving", Toast.LENGTH_SHORT).show();
                } else if (editTextTemplateName.getText().toString().trim().isEmpty()) {
                    editTextTemplateName.setError("Name is required");
                    Toast.makeText(EditTemplateActivity.this, "Template name is required", Toast.LENGTH_SHORT).show();
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.getTemplate().observe(this, template -> {
            if (template != null && !editTextTemplateName.isFocused()) {
                editTextTemplateName.setText(template.name);
            }
        });

        viewModel.getExercises().observe(this, exercises -> {
            if (adapter == null) {
                adapter = new EditExerciseAdapter(new ArrayList<>(exercises), new EditExerciseAdapter.OnExerciseActionListener() {
                    @Override
                    public void onDelete(TemplateExerciseWithDetails item) {
                        viewModel.deleteExercise(item.templateExercise);
                    }

                    @Override
                    public void onUpdate(TemplateExerciseWithDetails item) {
                        viewModel.updateExercise(item.templateExercise);
                    }
                });
                recyclerView.setAdapter(adapter);
            } else {
                adapter.setItems(new ArrayList<>(exercises));
            }
        });
    }

    private void setupNameChangeListener() {
        editTextTemplateName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String newName = s.toString().trim();
                if (!newName.isEmpty()) {
                    viewModel.updateTemplateName(newName);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showAddExerciseDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_exercise, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        EditText searchInput = view.findViewById(R.id.editTextSearchExercise);
        RecyclerView allExercisesRecycler = view.findViewById(R.id.recyclerViewAllExercises);
        Button btnClose = view.findViewById(R.id.buttonCloseDialog);

        allExercisesRecycler.setLayoutManager(new LinearLayoutManager(this));
        allExercisesRecycler.setHasFixedSize(true);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        viewModel.getAllExercises().observe(this, allExercises -> {
            SearchExerciseAdapter searchAdapter = new SearchExerciseAdapter(allExercises, exercise -> {
                viewModel.addExerciseToTemplate(templateId, exercise);
                dialog.dismiss();
            });
            allExercisesRecycler.setAdapter(searchAdapter);

            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchAdapter.filter(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        });
        
        viewModel.loadAllExercises();
        dialog.show();
    }
}
