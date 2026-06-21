package com.example.traintracker;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "stations", indices = {@Index(value = {"name"}, unique = true)})
public class Station {
    @PrimaryKey(autoGenerate = true)
    public Integer id;
    
    public String name;

    public Station() {}

    public Station(String name) {
        this.name = name;
    }
}