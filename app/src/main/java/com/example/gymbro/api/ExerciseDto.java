package com.example.gymbro.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ExerciseDto {
    @SerializedName("id")
    public String apiId;

    @SerializedName("name")
    public String name;

    @SerializedName("bodyPart")
    public String bodyPart;

    @SerializedName("equipment")
    public String equipment;

    @SerializedName("gifUrl")
    public String gifUrl;

    @SerializedName("target")
    public String target;

    @SerializedName("secondaryMuscles")
    public List<String> secondaryMuscles;

    @SerializedName("instructions")
    public List<String> instructions;
}
