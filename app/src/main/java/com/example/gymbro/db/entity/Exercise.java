package com.example.gymbro.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.gymbro.db.Converters;
import com.google.gson.annotations.SerializedName;
import java.util.List;

@Entity(tableName = "exercises")
public class Exercise {
    
    @PrimaryKey
    @NonNull
    @SerializedName("id")
    public String apiId;

    @SerializedName("name")
    public String name;

    @SerializedName("target")
    public String target;

    @SerializedName("secondaryMuscles")
    @TypeConverters(Converters.class)
    public List<String> secondaryMuscles;

    @SerializedName("bodyPart")
    public String bodyPart;

    @SerializedName("equipment")
    public String equipment;

    public String gifUrl;

    public Exercise() {
        this.apiId = "";
    }

    public Exercise(@NonNull String apiId, String name, String target, String bodyPart, String equipment) {
        this.apiId = apiId;
        this.name = name;
        this.target = target;
        this.bodyPart = bodyPart;
        this.equipment = equipment;
    }
}
