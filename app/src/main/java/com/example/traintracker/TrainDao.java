package com.example.traintracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TrainDao {
    // Trips
    @Insert
    void insertTrip(Trip trip);

    @Update
    void updateTrip(Trip trip);

    @Query("DELETE FROM trips WHERE id = :id")
    void deleteTrip(int id);

    @Query("SELECT * FROM trips ORDER BY id DESC")
    List<Trip> getAllTrips();

    @Query("SELECT SUM(km) FROM trips WHERE year = :year")
    Integer getTotalKmForYear(int year);

    @Query("SELECT SUM(duration) FROM trips WHERE year = :year")
    Integer getTotalDurationForYear(int year);

    @Query("SELECT SUM(km) FROM trips WHERE month = :month AND year = :year")
    Integer getKmByMonthAndYear(int month, int year);

    @Query("SELECT SUM(duration) FROM trips WHERE month = :month AND year = :year")
    Integer getDurationByMonthAndYear(int month, int year);

    @Query("SELECT DISTINCT year FROM trips ORDER BY year DESC")
    List<Integer> getAvailableYears();

    // Stations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertStation(Station station);

    @Query("SELECT name FROM stations ORDER BY name ASC")
    List<String> getAllStationNames();

    // Distances
    @Query("SELECT km FROM distances WHERE (station1 = :s1 AND station2 = :s2) OR (station1 = :s2 AND station2 = :s1) LIMIT 1")
    Integer getDistance(String s1, String s2);

    @Query("DELETE FROM distances WHERE (station1 = :s1 AND station2 = :s2) OR (station1 = :s2 AND station2 = :s1)")
    void deleteDistance(String s1, String s2);

    @Insert
    void insertDistance(Distance distance);
}