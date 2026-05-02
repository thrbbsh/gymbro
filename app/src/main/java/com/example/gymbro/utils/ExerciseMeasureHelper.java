package com.example.gymbro.utils;

import android.content.Context;
import android.util.Log;

import com.example.gymbro.db.entity.MeasureType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExerciseMeasureHelper {

    private static final String TAG = "ExerciseMeasureHelper";
    private static Map<String, MeasureType> EXCEPTIONS = new HashMap<>();
    public static void loadExceptions(Context context) {
        try {
            InputStream is = context.getAssets().open("measure_exceptions.json");
            InputStreamReader reader = new InputStreamReader(is);
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> rawMap = new Gson().fromJson(reader, type);
            
            EXCEPTIONS = new HashMap<>();
            if (rawMap != null) {
                for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                    try {
                        EXCEPTIONS.put(entry.getKey(), MeasureType.valueOf(entry.getValue()));
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Invalid MeasureType in JSON for ID " + entry.getKey() + ": " + entry.getValue());
                    }
                }
            }
            reader.close();
            Log.d(TAG, "Loaded " + EXCEPTIONS.size() + " exceptions from JSON");
        } catch (Exception e) {
            Log.e(TAG, "Error loading measure_exceptions.json", e);
        }
    }

    public static MeasureType guessMeasureType(String apiId, String name, String equipment, List<String> instructions) {
        // 1. Check against exceptions list (loaded from JSON)
        if (EXCEPTIONS.containsKey(apiId)) {
            return EXCEPTIONS.get(apiId);
        }

        String nameLower = name != null ? name.toLowerCase() : "";
        String equipmentLower = equipment != null ? equipment.toLowerCase() : "";
        
        StringBuilder sb = new StringBuilder();
        if (instructions != null) {
            for (String s : instructions) sb.append(s).append(" ");
        }
        String fullInstructions = sb.toString().toLowerCase();

        // 2. Check for DURATION (Time-based exercises)
        String[] durationKeywords = {"plank", "hold", "stretch", "static", "hang", "stay", "isometric"};
        for (String keyword : durationKeywords) {
            if (nameLower.contains(keyword)) return MeasureType.DURATION;
        }
        
        if (fullInstructions.contains("hold for") || fullInstructions.contains("maintain position")) {
            return MeasureType.DURATION;
        }

        // 3. Check for CARDIO (Distance + Time)
        String[] cardioKeywords = {"run", "walk", "sprint", "climb", "cycle", "rowing", "cardio", "jump rope"};
        for (String keyword : cardioKeywords) {
            if (nameLower.contains(keyword)) return MeasureType.DISTANCE_TIME;
        }

        // 4. Separate REPS into "bodyweight" and "weighted"
        if (equipmentLower.equals("body weight") || equipmentLower.contains("assisted")) {
            return MeasureType.BODYWEIGHT_REPS;
        }

        // Default - Weight + Reps
        return MeasureType.WEIGHT_REPS;
    }
}
