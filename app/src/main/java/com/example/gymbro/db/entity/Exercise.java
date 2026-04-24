package com.example.gymbro.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.SerializedName;

@Entity(tableName = "exercises")
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @SerializedName("name")
    public String name;

    @SerializedName("target")
    public String target;

    @SerializedName("bodyPart")
    public String bodyPart;

    @SerializedName("equipment")
    public String equipment;

    @SerializedName("gifUrl")
    public String gifUrl;

    public Exercise() {}

    public Exercise(String name, String target, String bodyPart, String equipment, String gifUrl) {
        this.name = name;
        this.target = target;
        this.bodyPart = bodyPart;
        this.equipment = equipment;
        this.gifUrl = gifUrl;
    }
}
