package com.example.traintracker;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "trips")
public class Trip {
    @PrimaryKey(autoGenerate = true)
    @Nullable
    public Integer id;

    @Nullable
    public String start;
    @Nullable
    public String stop;
    @Nullable
    public String type;
    @Nullable
    public String number;
    
    @Nullable
    public Integer km;
    @Nullable
    public Integer duration;
    
    @ColumnInfo(name = "trip_date")
    @Nullable
    public String tripDate;
    
    @ColumnInfo(name = "start_time")
    @Nullable
    public String startTime;
    
    @ColumnInfo(name = "end_time")
    @Nullable
    public String endTime;
    
    @Nullable
    public Integer month;
    @Nullable
    public Integer year;

    // Derived fields for UI
    @Ignore
    public String route;
    @Ignore
    public String details;
    @Ignore
    public String kmFormatted;
    @Ignore
    public String durationText;
    @Ignore
    public long timestamp;

    public Trip() {}

    public void formatFields() {
        this.route = start + " -> " + stop;
        this.details = type + " " + number + " • " + tripDate + "\n" + startTime + " - " + endTime;
        this.kmFormatted = (km != null ? km : 0) + " km";

        if (duration != null && duration > 0) {
            this.durationText = duration + " min";
        } else {
            this.durationText = "--";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
            Date parsedDate = sdf.parse(tripDate + " " + startTime);
            this.timestamp = parsedDate != null ? parsedDate.getTime() : 0;
        } catch (Exception e) {
            this.timestamp = 0;
        }
    }
}