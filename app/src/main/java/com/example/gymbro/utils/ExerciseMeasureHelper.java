package com.example.gymbro.utils;

import com.example.gymbro.db.entity.MeasureType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExerciseMeasureHelper {

    // Exception map: key = apiId (String), value = MeasureType
    private static final Map<String, MeasureType> EXCEPTIONS = new HashMap<>();

    static {
        // Add specific exercises here if they are identified incorrectly
        // Example: EXCEPTIONS.put("0001", MeasureType.BODYWEIGHT_REPS);
        EXCEPTIONS.put("0684", MeasureType.DISTANCE_TIME); // run (equipment)
        EXCEPTIONS.put("0685", MeasureType.DISTANCE_TIME); // run
    }

    public static MeasureType guessMeasureType(String apiId, String name, String equipment, List<String> instructions) {
        // 1. Check against exceptions list
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
        // Keywords for static exercises or stretching
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
