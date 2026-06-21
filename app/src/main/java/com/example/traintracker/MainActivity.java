package com.example.traintracker;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Spinner spinnerStart, spinnerEnd;
    RadioGroup radioGroupType;
    EditText etTrainNumber;
    Button btnSave, btnHistory, btnStats, btnTimer, btnSelectDate, btnStartTime, btnEndTime, btnExport, btnImport, btnStationManager;
    TextView tvTotalKm, tvCalculatedDuration, tvManualCalculated;
    CheckBox cbManualMode;
    LinearLayout layoutTimer, layoutManualInputs;
    TrainDao trainDao;
    SharedPreferences prefs;
    int liveDurationMinutes = 0;
    String selectedDateString = "";
    Calendar calStartManual, calEndManual;

    // Variabile pentru a salva luna si anul selectate manual
    int manualMonth = -1;
    int manualYear = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trainDao = AppDatabase.getInstance(this).trainDao();
        prefs = getSharedPreferences("TrainAppPrefs", MODE_PRIVATE);

        spinnerStart = findViewById(R.id.spinnerStart);
        spinnerEnd = findViewById(R.id.spinnerEnd);
        radioGroupType = findViewById(R.id.radioGroupType);
        etTrainNumber = findViewById(R.id.etTrainNumber);

        btnSave = findViewById(R.id.btnSave);
        btnHistory = findViewById(R.id.btnHistory);
        btnStats = findViewById(R.id.btnStats);
        btnTimer = findViewById(R.id.btnTimer);
        btnStationManager = findViewById(R.id.btnStationManager);

        btnExport = findViewById(R.id.btnExport);
        btnImport = findViewById(R.id.btnImport);

        btnSelectDate = findViewById(R.id.btnSelectDate);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndTime = findViewById(R.id.btnEndTime);
        tvManualCalculated = findViewById(R.id.tvManualCalculated);

        tvTotalKm = findViewById(R.id.tvTotalKm);
        tvCalculatedDuration = findViewById(R.id.tvCalculatedDuration);

        cbManualMode = findViewById(R.id.cbManualMode);
        layoutTimer = findViewById(R.id.layoutTimer);
        layoutManualInputs = findViewById(R.id.layoutManualInputs);

        // Initializam luna si anul cu valorile curente
        Calendar currentCal = Calendar.getInstance();
        manualMonth = currentCal.get(Calendar.MONTH);
        manualYear = currentCal.get(Calendar.YEAR);
        selectedDateString = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(currentCal.getTime());

        refreshTotalKm();
        checkTimerState();
        loadStationsIntoSpinners();

        cbManualMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                layoutTimer.setVisibility(View.GONE);
                layoutManualInputs.setVisibility(View.VISIBLE);
                btnSave.setText(getString(R.string.btn_save_manual));
                calStartManual = null;
                calEndManual = null;
                tvManualCalculated.setText(getString(R.string.msg_set_times));
                btnStartTime.setText(getString(R.string.btn_time_start));
                btnEndTime.setText(getString(R.string.btn_time_end));
            } else {
                layoutTimer.setVisibility(View.VISIBLE);
                layoutManualInputs.setVisibility(View.GONE);
                btnSave.setText(getString(R.string.btn_save_trip));
                Calendar cal = Calendar.getInstance();
                manualMonth = cal.get(Calendar.MONTH);
                manualYear = cal.get(Calendar.YEAR);
                selectedDateString = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime());
            }
        });

        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnStartTime.setOnClickListener(v -> showTimePicker(true));
        btnEndTime.setOnClickListener(v -> showTimePicker(false));

        btnTimer.setOnClickListener(v -> handleTimer());
        btnSave.setOnClickListener(v -> saveTrip());
        btnHistory.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HistoryActivity.class)));
        btnStats.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StatsActivity.class)));
        btnStationManager.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StationManagerActivity.class)));

        btnExport.setOnClickListener(v -> startExport());
        btnImport.setOnClickListener(v -> startImport());
    }

    private void loadStationsIntoSpinners() {
        String currentStart = spinnerStart.getSelectedItem() != null ? spinnerStart.getSelectedItem().toString() : null;
        String currentEnd = spinnerEnd.getSelectedItem() != null ? spinnerEnd.getSelectedItem().toString() : null;

        List<String> combinedStations = new ArrayList<>();
        combinedStations.add(getString(R.string.bucuresti));
        combinedStations.add(getString(R.string.targoviste));
        combinedStations.add(getString(R.string.titu));
        combinedStations.add(getString(R.string.gaesti));
        combinedStations.add(getString(R.string.pitesti));
        combinedStations.add(getString(R.string.vulcana_pandele));
        combinedStations.add(getString(R.string.pietrosita));

        List<String> dynamicStations = trainDao.getAllStationNames();
        for (String s : dynamicStations) {
            if (!combinedStations.contains(s)) {
                combinedStations.add(s);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, combinedStations);
        spinnerStart.setAdapter(adapter);
        spinnerEnd.setAdapter(adapter);

        if (currentStart != null && combinedStations.contains(currentStart)) {
            spinnerStart.setSelection(adapter.getPosition(currentStart));
        }
        if (currentEnd != null && combinedStations.contains(currentEnd)) {
            spinnerEnd.setSelection(adapter.getPosition(currentEnd));
        }
    }

    private final ActivityResultLauncher<Intent> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) performExport(uri);
                }
            });

    private final ActivityResultLauncher<Intent> importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) performImport(uri);
                }
            });

    private void startExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        String fileName = "TrainBackup_" + new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()) + ".db";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        exportLauncher.launch(intent);
    }

    private void startImport() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Import Backup")
                .setMessage("Atenție! Importul va înlocui toate datele actuale (călătorii și stații) cu cele din fișierul de backup. Ești sigur?")
                .setPositiveButton("Da, Importă", (dialog, which) -> {
                    Toast.makeText(this, getString(R.string.msg_select_db), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    importLauncher.launch(intent);
                })
                .setNegativeButton("Anulează", null)
                .show();
    }

    private void performExport(Uri uri) {
        try {
            AppDatabase.destroyInstance();
            File dbFile = getDatabasePath("TrainTrips.db");
            if (!dbFile.exists()) {
                Toast.makeText(this, getString(R.string.msg_no_export_data), Toast.LENGTH_SHORT).show();
                return;
            }
            FileInputStream fis = new FileInputStream(dbFile);
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
            FileOutputStream fos = new FileOutputStream(pfd.getFileDescriptor());

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) fos.write(buffer, 0, length);

            fos.close();
            pfd.close();
            fis.close();
            trainDao = AppDatabase.getInstance(this).trainDao();
            Toast.makeText(this, getString(R.string.msg_backup_success), Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.msg_export_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void performImport(Uri uri) {
        File tempFile = new File(getCacheDir(), "import_temp.db");
        try {
            // 1. Copy URI to temp file
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) return;
            FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) fos.write(buffer, 0, len);
            fos.close();
            fis.close();
            pfd.close();

            // 2. Open temp file with raw SQLite
            SQLiteDatabase db = SQLiteDatabase.openDatabase(tempFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            
            // 3. Clear current database before importing (to avoid duplicates)
            // This clears all tables (trips, stations, distances) for a clean restore
            AppDatabase.getInstance(this).clearAllTables();

            // 4. Check for 'trips' table and import rows
            Cursor cursor = db.rawQuery("SELECT * FROM trips", null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Trip trip = new Trip();
                    trip.start = getStringFromCursor(cursor, "start");
                    trip.stop = getStringFromCursor(cursor, "stop");
                    trip.type = getStringFromCursor(cursor, "type");
                    trip.number = getStringFromCursor(cursor, "number");
                    trip.km = getIntFromCursor(cursor, "km");
                    trip.duration = getIntFromCursor(cursor, "duration");
                    trip.tripDate = getStringFromCursor(cursor, "trip_date");
                    trip.startTime = getStringFromCursor(cursor, "start_time");
                    trip.endTime = getStringFromCursor(cursor, "end_time");
                    trip.month = getIntFromCursor(cursor, "month");
                    trip.year = getIntFromCursor(cursor, "year");
                    
                    trainDao.insertTrip(trip);
                }
                cursor.close();
            }

            // 5. Import stations
            Cursor stationCursor = db.rawQuery("SELECT * FROM stations", null);
            if (stationCursor != null) {
                while (stationCursor.moveToNext()) {
                    String name = getStringFromCursor(stationCursor, "name");
                    if (name != null) {
                        trainDao.insertStation(new Station(name));
                    }
                }
                stationCursor.close();
            }
            
            // 6. Import distances
            Cursor distanceCursor = db.rawQuery("SELECT * FROM distances", null);
            if (distanceCursor != null) {
                while (distanceCursor.moveToNext()) {
                    trainDao.insertDistance(new Distance(
                            getStringFromCursor(distanceCursor, "station1"),
                            getStringFromCursor(distanceCursor, "station2"),
                            getIntFromCursor(distanceCursor, "km")
                    ));
                }
                distanceCursor.close();
            }

            db.close();
            tempFile.delete();
            
            refreshTotalKm();
            loadStationsIntoSpinners();
            Toast.makeText(this, getString(R.string.msg_import_success), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Eroare import: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (tempFile.exists()) tempFile.delete();
        }
    }
    
    private String getStringFromCursor(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx != -1 ? c.getString(idx) : null;
    }
    
    private Integer getIntFromCursor(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return (idx != -1 && !c.isNull(idx)) ? c.getInt(idx) : null;
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog d = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(year, month, day);

            // Salvam luna si anul pe care le-a ales utilizatorul manual
            manualYear = year;
            manualMonth = month;

            selectedDateString = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(sel.getTime());
            btnSelectDate.setText(getString(R.string.btn_date_format, selectedDateString));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.show();
    }

    private void showTimePicker(boolean isStart) {
        final Calendar c = Calendar.getInstance();
        TimePickerDialog t = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar temp = Calendar.getInstance();
            temp.set(Calendar.HOUR_OF_DAY, hourOfDay);
            temp.set(Calendar.MINUTE, minute);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String timeStr = sdf.format(temp.getTime());

            if (isStart) {
                calStartManual = temp;
                btnStartTime.setText(timeStr);
            } else {
                calEndManual = temp;
                btnEndTime.setText(timeStr);
            }
            calculateManualDuration();
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
        t.show();
    }

    private void calculateManualDuration() {
        if (calStartManual != null && calEndManual != null) {
            long diff = calEndManual.getTimeInMillis() - calStartManual.getTimeInMillis();

            // Fix pentru cursele de noapte
            if (diff < 0) {
                diff += 24 * 60 * 60 * 1000L;
            }

            int min = (int) (diff / (1000 * 60));
            tvManualCalculated.setText(getString(R.string.msg_duration_calc, min));
        }
    }

    private void saveTrip() {
        String start = spinnerStart.getSelectedItem().toString();
        String end = spinnerEnd.getSelectedItem().toString();
        String number = etTrainNumber.getText().toString();

        if (number.isEmpty()) { etTrainNumber.setError(getString(R.string.msg_error_train_num)); return; }
        if (start.equals(end)) { Toast.makeText(this, getString(R.string.msg_error_stations), Toast.LENGTH_LONG).show(); return; }

        int durationToSave;
        String startTimeStr, endTimeStr;
        int monthToSave;
        int yearToSave;

        if (cbManualMode.isChecked()) {
            if (calStartManual == null || calEndManual == null) {
                Toast.makeText(this, getString(R.string.msg_error_select_times), Toast.LENGTH_LONG).show();
                return;
            }

            long diff = calEndManual.getTimeInMillis() - calStartManual.getTimeInMillis();
            // Fix pentru cursele de noapte
            if (diff < 0) {
                diff += 24 * 60 * 60 * 1000L;
            }

            if (diff <= 0) {
                Toast.makeText(this, getString(R.string.msg_error_invalid_time), Toast.LENGTH_LONG).show();
                return;
            }

            durationToSave = (int) (diff / (1000 * 60));
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            startTimeStr = sdf.format(calStartManual.getTime());
            endTimeStr = sdf.format(calEndManual.getTime());

            // Folosim luna si anul selectate
            monthToSave = manualMonth;
            yearToSave = manualYear;

        } else {
            long startTimeMillis = prefs.getLong("START_TIME", 0);
            if (startTimeMillis != 0) {
                Toast.makeText(this, getString(R.string.msg_error_stop_timer), Toast.LENGTH_LONG).show();
                return;
            }
            if (liveDurationMinutes == 0) {
                Toast.makeText(this, getString(R.string.msg_error_use_timer), Toast.LENGTH_LONG).show();
                return;
            }
            durationToSave = liveDurationMinutes;
            long now = System.currentTimeMillis();
            long startApprox = now - (liveDurationMinutes * 60 * 1000L);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            endTimeStr = sdf.format(new Date(now));
            startTimeStr = sdf.format(new Date(startApprox));

            Calendar cal = Calendar.getInstance();
            selectedDateString = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime());
            monthToSave = cal.get(Calendar.MONTH);
            yearToSave = cal.get(Calendar.YEAR);
        }

        int selectedId = radioGroupType.getCheckedRadioButtonId();
        RadioButton radioButton = findViewById(selectedId);
        String type = radioButton.getText().toString();
        int km = calculateDistance(start, end);

        if (km == 0) {
            Toast.makeText(this, getString(R.string.msg_error_unknown_route), Toast.LENGTH_LONG).show();
            return;
        }

        // Am adaugat luna si anul ca parametri
        Trip trip = new Trip();
        trip.start = start;
        trip.stop = end;
        trip.type = type;
        trip.number = number;
        trip.km = km;
        trip.duration = durationToSave;
        trip.tripDate = selectedDateString;
        trip.startTime = startTimeStr;
        trip.endTime = endTimeStr;
        trip.month = monthToSave;
        trip.year = yearToSave;

        trainDao.insertTrip(trip);
        Toast.makeText(this, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show();

        etTrainNumber.setText("");
        liveDurationMinutes = 0;
        tvCalculatedDuration.setVisibility(View.GONE);
        cbManualMode.setChecked(false);

        refreshTotalKm();
        updateWidget();
    }

    private void handleTimer() {
        long startTime = prefs.getLong("START_TIME", 0);
        if (startTime == 0) {
            if (liveDurationMinutes > 0) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Atenție!")
                        .setMessage("Ai deja un timp cronometrat de " + liveDurationMinutes + " minute care nu a fost salvat. Dacă pornești un cronometru nou, acest timp va fi pierdut. Ești sigur?")
                        .setPositiveButton("Da, pornește", (dialog, which) -> {
                            startActiveTimer();
                        })
                        .setNegativeButton("Anulează", null)
                        .show();
            } else {
                startActiveTimer();
            }
        } else {
            long now = System.currentTimeMillis();
            long diffMillis = now - startTime;
            liveDurationMinutes = (int) (diffMillis / (1000 * 60));
            if (liveDurationMinutes == 0) liveDurationMinutes = 1;

            prefs.edit().remove("START_TIME").apply();
            checkTimerState();
            tvCalculatedDuration.setText(getString(R.string.msg_duration_calc_manual, liveDurationMinutes));
            tvCalculatedDuration.setVisibility(View.VISIBLE);
        }
    }

    private void startActiveTimer() {
        long now = System.currentTimeMillis();
        prefs.edit().putLong("START_TIME", now).apply();
        checkTimerState();
        Toast.makeText(this, getString(R.string.msg_timer_started), Toast.LENGTH_SHORT).show();
        tvCalculatedDuration.setVisibility(View.GONE);
        liveDurationMinutes = 0;
    }

    private void checkTimerState() {
        long startTime = prefs.getLong("START_TIME", 0);
        if (startTime != 0) {
            btnTimer.setText(getString(R.string.btn_stop_timer));
            btnTimer.setBackgroundTintList(getColorStateList(android.R.color.holo_red_light));
        } else {
            btnTimer.setText(getString(R.string.btn_start_timer));
            btnTimer.setBackgroundTintList(getColorStateList(android.R.color.holo_green_light));
        }
    }

    private void updateWidget() {
        Intent intent = new Intent(this, TrainWidget.class);
        intent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = android.appwidget.AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new android.content.ComponentName(getApplication(), TrainWidget.class));
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void refreshTotalKm() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        Integer kmObj = trainDao.getTotalKmForYear(currentYear);
        int totalKm = kmObj != null ? kmObj : 0;
        Integer durObj = trainDao.getTotalDurationForYear(currentYear);
        int totalMinutes = durObj != null ? durObj : 0;
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        tvTotalKm.setText(getString(R.string.lbl_total_year, currentYear, totalKm, hours, mins));
    }

    private int calculateDistance(String s1, String s2) {
        Integer dynamicKm = trainDao.getDistance(s1, s2);
        if (dynamicKm == null) dynamicKm = 0;

        if (dynamicKm > 0) {
            return dynamicKm;
        }

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

    @Override
    protected void onResume() {
        super.onResume();
        checkTimerState();
        refreshTotalKm();
        loadStationsIntoSpinners();
    }
}