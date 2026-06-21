package com.example.traintracker;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "distances")
public class Distance {
    @PrimaryKey(autoGenerate = true)
    public Integer id;
    
    public String station1;
    public String station2;
    public Integer km;

    public Distance() {}

    public Distance(String station1, String station2, Integer km) {
        this.station1 = station1;
        this.station2 = station2;
        this.km = km;
    }
}