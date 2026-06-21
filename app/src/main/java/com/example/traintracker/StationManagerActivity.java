package com.example.traintracker;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StationManagerActivity extends AppCompatActivity {

    TrainDao trainDao;
    EditText etNewStation;
    Button btnAddStation, btnSaveAllDistances;
    Spinner spinnerBaseStation;
    LinearLayout layoutDistancesContainer;

    Map<String, EditText> distanceInputs = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_manager);

        trainDao = AppDatabase.getInstance(this).trainDao();

        etNewStation = findViewById(R.id.etNewStation);
        btnAddStation = findViewById(R.id.btnAddStation);
        spinnerBaseStation = findViewById(R.id.spinnerBaseStation);
        layoutDistancesContainer = findViewById(R.id.layoutDistancesContainer);
        btnSaveAllDistances = findViewById(R.id.btnSaveAllDistances);

        loadStationsIntoSpinner(null);

        spinnerBaseStation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedStation = parent.getItemAtPosition(position).toString();
                buildDistancesList(selectedStation);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAddStation.setOnClickListener(v -> {
            String newStation = etNewStation.getText().toString().trim();
            if (newStation.isEmpty()) {
                Toast.makeText(this, "Introdu un nume de stație!", Toast.LENGTH_SHORT).show();
                return;
            }

            Station station = new Station(newStation);
            long id = trainDao.insertStation(station);
            boolean success = id != -1;
            if (success) {
                Toast.makeText(this, "Stație adăugată cu succes!", Toast.LENGTH_SHORT).show();
                etNewStation.setText("");
                loadStationsIntoSpinner(newStation);
            } else {
                Toast.makeText(this, "Stația există deja în listă!", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveAllDistances.setOnClickListener(v -> {
            if (spinnerBaseStation.getSelectedItem() == null) return;
            String baseStation = spinnerBaseStation.getSelectedItem().toString();

            int savedCount = 0;

            for (Map.Entry<String, EditText> entry : distanceInputs.entrySet()) {
                String otherStation = entry.getKey();
                String kmStr = entry.getValue().getText().toString().trim();

                if (!kmStr.isEmpty()) {
                    int km = Integer.parseInt(kmStr);
                    trainDao.deleteDistance(baseStation, otherStation);
                    trainDao.insertDistance(new Distance(baseStation, otherStation, km));
                    savedCount++;
                }
            }

            if (savedCount > 0) {
                Toast.makeText(this, savedCount + " distanțe au fost salvate!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Nu ai completat nicio distanță nouă.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> getCombinedStations() {
        List<String> combined = new ArrayList<>();
        combined.add(getString(R.string.bucuresti));
        combined.add(getString(R.string.targoviste));
        combined.add(getString(R.string.titu));
        combined.add(getString(R.string.gaesti));
        combined.add(getString(R.string.pitesti));
        combined.add(getString(R.string.vulcana_pandele));
        combined.add(getString(R.string.pietrosita));

        List<String> dynamicStations = trainDao.getAllStationNames();
        for (String s : dynamicStations) {
            if (!combined.contains(s)) {
                combined.add(s);
            }
        }
        return combined;
    }

    private void loadStationsIntoSpinner(String stationToSelect) {
        List<String> allStations = getCombinedStations();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, allStations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBaseStation.setAdapter(adapter);

        if (stationToSelect != null && allStations.contains(stationToSelect)) {
            spinnerBaseStation.setSelection(adapter.getPosition(stationToSelect));
        }
    }

    private void buildDistancesList(String baseStation) {
        layoutDistancesContainer.removeAllViews();
        distanceInputs.clear();

        List<String> allStations = getCombinedStations();

        int textColorPrimary = ContextCompat.getColor(this, R.color.text_primary);
        int textColorSecondary = ContextCompat.getColor(this, R.color.text_secondary);

        for (String otherStation : allStations) {
            if (otherStation.equals(baseStation)) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 16, 0, 16);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvStation = new TextView(this);
            tvStation.setText(otherStation);
            tvStation.setTextSize(16f);
            tvStation.setTextColor(textColorPrimary);

            LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvStation.setLayoutParams(tvParams);

            EditText etKm = new EditText(this);
            etKm.setHint("Km");
            etKm.setInputType(InputType.TYPE_CLASS_NUMBER);
            etKm.setGravity(Gravity.CENTER);

            etKm.setTextColor(textColorPrimary);
            etKm.setHintTextColor(textColorSecondary);
            etKm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_blue)));

            LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(dpToPx(80), LinearLayout.LayoutParams.WRAP_CONTENT);
            etKm.setLayoutParams(etParams);

            // INTEGRARE DE PRECOMPLETARE CORECTĂ (Verifică DB + Fallback pe Hardcoded)
            int existingKm = getDistanceFromDB(baseStation, otherStation);
            if (existingKm > 0) {
                etKm.setText(String.valueOf(existingKm));
            }

            row.addView(tvStation);
            row.addView(etKm);
            layoutDistancesContainer.addView(row);

            distanceInputs.put(otherStation, etKm);
        }
    }

    private int getDistanceFromDB(String s1, String s2) {
        Integer kmObj = trainDao.getDistance(s1, s2);
        int km = kmObj != null ? kmObj : 0;

        // 2. NOU: Dacă e 0, verificăm dacă este una dintre distanțele hardcodate originale
        if (km == 0) {
            km = getHardcodedDistance(s1, s2);
        }
        return km;
    }

    // Listă secundară de verificare pentru a precompleta distanțele de bază
    private int getHardcodedDistance(String s1, String s2) {
        if (isRoute(s1, s2, getString(R.string.bucuresti), getString(R.string.targoviste))) return 80;
        if (isRoute(s1, s2, getString(R.string.targoviste), getString(R.string.titu))) return 32;
        if (isRoute(s1, s2, getString(R.string.bucuresti), getString(R.string.titu))) return 48;
        if (isRoute(s1, s2, getString(R.string.targoviste), getString(R.string.vulcana_pandele))) return 13;
        if (isRoute(s1, s2, getString(R.string.targoviste), getString(R.string.pietrosita))) return 35;
        if (isRoute(s1, s2, getString(R.string.vulcana_pandele), getString(R.string.pietrosita))) return 22;
        if (isRoute(s1, s2, getString((R.string.titu)), getString(R.string.pitesti))) return 60;
        if (isRoute(s1, s2, getString(R.string.bucuresti), getString(R.string.pitesti))) return 108;
        if (isRoute(s1, s2, getString(R.string.titu), getString(R.string.gaesti))) return 22;
        if (isRoute(s1, s2, getString(R.string.gaesti), getString(R.string.pitesti))) return 38;
        if (isRoute(s1, s2, getString(R.string.bucuresti), getString(R.string.gaesti))) return 70;
        if (isRoute(s1, s2, getString(R.string.bucuresti), getString(R.string.pietrosita))) return 115;
        if (isRoute(s1, s2, getString(R.string.bucuresti), getString(R.string.vulcana_pandele))) return 93;
        return 0;
    }

    private boolean isRoute(String s1, String s2, String f1, String f2) {
        return (s1.equals(f1) && s2.equals(f2)) || (s1.equals(f2) && s2.equals(f1));
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}