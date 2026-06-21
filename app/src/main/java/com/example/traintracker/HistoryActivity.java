package com.example.traintracker;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    TrainDao trainDao;
    ArrayList<Trip> allTrips;
    ArrayList<Trip> currentFilteredTrips;
    TripAdapter adapter;

    ListView listView;
    EditText etSearch;
    TextView tvFilterStats;
    Button btnSortDuration, btnSortDistance;

    boolean isAscendingDuration = true;
    boolean isAscendingDistance = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        listView = findViewById(R.id.listViewHistory);
        etSearch = findViewById(R.id.etSearch);
        tvFilterStats = findViewById(R.id.tvFilterStats);
        btnSortDuration = findViewById(R.id.btnSortDuration);
        btnSortDistance = findViewById(R.id.btnSortDistance);

        trainDao = AppDatabase.getInstance(this).trainDao();

        loadData();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTrips(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSortDuration.setOnClickListener(v -> {
            if (currentFilteredTrips == null || currentFilteredTrips.isEmpty()) return;
            Collections.sort(currentFilteredTrips, (t1, t2) -> {
                if (isAscendingDuration) return Integer.compare(t1.duration, t2.duration);
                else return Integer.compare(t2.duration, t1.duration);
            });
            isAscendingDuration = !isAscendingDuration;
            btnSortDuration.setText(isAscendingDuration ? "Min 🔼" : "Min 🔽");
            btnSortDistance.setText("Km ↕️");
            adapter.notifyDataSetChanged();
        });

        btnSortDistance.setOnClickListener(v -> {
            if (currentFilteredTrips == null || currentFilteredTrips.isEmpty()) return;
            Collections.sort(currentFilteredTrips, (t1, t2) -> {
                if (isAscendingDistance) return Integer.compare(t1.km, t2.km);
                else return Integer.compare(t2.km, t1.km);
            });
            isAscendingDistance = !isAscendingDistance;
            btnSortDistance.setText(isAscendingDistance ? "Km 🔼" : "Km 🔽");
            btnSortDuration.setText("Min ↕️");
            adapter.notifyDataSetChanged();
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Trip selectedTrip = currentFilteredTrips.get(position);
            showEditDialog(selectedTrip);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            Trip selectedTrip = currentFilteredTrips.get(position);
            showDeleteDialog(selectedTrip);
            return true;
        });
    }

    private void loadData() {
        allTrips = new ArrayList<>(trainDao.getAllTrips());
        for (Trip trip : allTrips) {
            trip.formatFields();
        }
        filterTrips(etSearch.getText().toString());
    }

    private void filterTrips(String query) {
        currentFilteredTrips = new ArrayList<>();
        int totalKm = 0;
        int totalMinutes = 0;

        // --- NOU: Curățăm textul căutat de diacritice și îl facem litere mici ---
        String cleanQuery = removeDiacritics(query).toLowerCase().trim();

        for (Trip t : allTrips) {
            // Curățăm și textele din baza de date înainte să le comparăm
            String cleanRoute = removeDiacritics(t.route).toLowerCase();
            String cleanDetails = removeDiacritics(t.details).toLowerCase();

            if (cleanRoute.contains(cleanQuery) || cleanDetails.contains(cleanQuery)) {

                currentFilteredTrips.add(t);
                totalKm += t.km;
                totalMinutes += t.duration;
            }
        }

        Collections.sort(currentFilteredTrips, (t1, t2) -> Long.compare(t2.timestamp, t1.timestamp));

        adapter = new TripAdapter(this, currentFilteredTrips);
        listView.setAdapter(adapter);

        btnSortDuration.setText("Min ↕️");
        btnSortDistance.setText("Km ↕️");
        isAscendingDuration = true;
        isAscendingDistance = true;

        updateStatsUI(totalKm, totalMinutes);
    }

    private void updateStatsUI(int totalKm, int totalMinutes) {
        String timeString;
        if (totalMinutes == 0) timeString = getString(R.string.stats_zero_min);
        else timeString = getString(R.string.stats_time, totalMinutes / 60, totalMinutes % 60);

        tvFilterStats.setText(getString(R.string.filter_stats_text, totalKm, timeString));
    }

    private void showEditDialog(final Trip trip) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_trip, null);

        RadioGroup rgType = view.findViewById(R.id.rgEditType);
        EditText etNumber = view.findViewById(R.id.etEditNumber);
        Button btnDate = view.findViewById(R.id.btnEditDate);
        Button btnStart = view.findViewById(R.id.btnEditStartTime);
        Button btnEnd = view.findViewById(R.id.btnEditEndTime);

        if (trip.type != null) {
            if (trip.type.equals("R-E")) ((RadioButton)view.findViewById(R.id.rbEditRE)).setChecked(true);
            else if (trip.type.equals("IR")) ((RadioButton)view.findViewById(R.id.rbEditIR)).setChecked(true);
            else if (trip.type.equals("IC")) ((RadioButton)view.findViewById(R.id.rbEditIC)).setChecked(true);
            else if (trip.type.equals("IRN")) ((RadioButton)view.findViewById(R.id.rbEditIRN)).setChecked(true);
            else ((RadioButton)view.findViewById(R.id.rbEditR)).setChecked(true);
        }

        etNumber.setText(trip.number);
        btnDate.setText(trip.tripDate);
        btnStart.setText(trip.startTime);
        btnEnd.setText(trip.endTime);

        final Calendar calStart = Calendar.getInstance();
        final Calendar calEnd = Calendar.getInstance();
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

        try {
            Date d = sdfDate.parse(trip.tripDate);
            if (d != null) {
                calStart.setTime(d);
                calEnd.setTime(d);
            }
            Date s = sdfTime.parse(trip.startTime);
            if (s != null) {
                Calendar t = Calendar.getInstance(); t.setTime(s);
                calStart.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
                calStart.set(Calendar.MINUTE, t.get(Calendar.MINUTE));
            }
            Date e = sdfTime.parse(trip.endTime);
            if (e != null) {
                Calendar t = Calendar.getInstance(); t.setTime(e);
                calEnd.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
                calEnd.set(Calendar.MINUTE, t.get(Calendar.MINUTE));
            }
        } catch (Exception ignored) {}

        btnDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (vw, y, m, d) -> {
                calStart.set(y, m, d);
                calEnd.set(y, m, d);
                btnDate.setText(sdfDate.format(calStart.getTime()));
            }, calStart.get(Calendar.YEAR), calStart.get(Calendar.MONTH), calStart.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnStart.setOnClickListener(v -> {
            new TimePickerDialog(this, (vw, h, m) -> {
                calStart.set(Calendar.HOUR_OF_DAY, h);
                calStart.set(Calendar.MINUTE, m);
                btnStart.setText(sdfTime.format(calStart.getTime()));
            }, calStart.get(Calendar.HOUR_OF_DAY), calStart.get(Calendar.MINUTE), true).show();
        });

        btnEnd.setOnClickListener(v -> {
            new TimePickerDialog(this, (vw, h, m) -> {
                calEnd.set(Calendar.HOUR_OF_DAY, h);
                calEnd.set(Calendar.MINUTE, m);
                btnEnd.setText(sdfTime.format(calEnd.getTime()));
            }, calEnd.get(Calendar.HOUR_OF_DAY), calEnd.get(Calendar.MINUTE), true).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Editează Cursa")
                .setView(view)
                .setPositiveButton("Salvează", (dialog, which) -> {
                    int selectedId = rgType.getCheckedRadioButtonId();
                    RadioButton rbSelected = view.findViewById(selectedId);
                    String nType = rbSelected.getText().toString();

                    String nNum = etNumber.getText().toString().trim();

                    if(nNum.isEmpty()) {
                        Toast.makeText(HistoryActivity.this, "Numărul trenului este obligatoriu!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int nKm = trip.km;

                    long diff = calEnd.getTimeInMillis() - calStart.getTimeInMillis();
                    if (diff < 0) diff += 24 * 60 * 60 * 1000L;
                    int nDur = (int) (diff / (1000 * 60));

                    String nDateStr = sdfDate.format(calStart.getTime());
                    String nStartStr = sdfTime.format(calStart.getTime());
                    String nEndStr = sdfTime.format(calEnd.getTime());
                    int nMonth = calStart.get(Calendar.MONTH);
                    int nYear = calStart.get(Calendar.YEAR);

                    trip.type = nType;
                    trip.number = nNum;
                    trip.km = nKm;
                    trip.duration = nDur;
                    trip.tripDate = nDateStr;
                    trip.startTime = nStartStr;
                    trip.endTime = nEndStr;
                    trip.month = nMonth;
                    trip.year = nYear;

                    trainDao.updateTrip(trip);
                    Toast.makeText(HistoryActivity.this, "Cursa a fost actualizată!", Toast.LENGTH_SHORT).show();

                    loadData();
                    updateWidget();
                })
                .setNegativeButton("Anulează", null)
                .show();
    }

    private void showDeleteDialog(final Trip trip) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_title))
                .setMessage(getString(R.string.dialog_delete_msg, trip.route, trip.km))
                .setPositiveButton(getString(R.string.dialog_btn_yes), (dialog, which) -> {
                    trainDao.deleteTrip(trip.id);
                    Toast.makeText(HistoryActivity.this, getString(R.string.msg_deleted), Toast.LENGTH_SHORT).show();
                    loadData();
                    updateWidget();
                })
                .setNegativeButton(getString(R.string.dialog_btn_no), null)
                .show();
    }

    private void updateWidget() {
        Intent intent = new Intent(this, TrainWidget.class);
        intent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = android.appwidget.AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new android.content.ComponentName(getApplication(), TrainWidget.class));
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    // --- METODA MAGICĂ PENTRU ELIMINAREA DIACRITICELOR ---
    private String removeDiacritics(String str) {
        if (str == null) return "";
        // Descompune caracterele (ex: 'ș' devine 's' + accent)
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        // Șterge toate accentele
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}