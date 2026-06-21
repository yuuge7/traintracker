package com.example.traintracker;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.List;

public class StatsActivity extends AppCompatActivity {
    Spinner spinnerMonths, spinnerYears;
    TextView tvStatsKm, tvStatsTime;
    TrainDao trainDao;
    String[] months;
    int selectedMonthIndex = 0;
    int selectedYear = 0;

    /**
     * Called when the activity is first created.
     * This method initializes the activity's user interface, populates the month names array from
     * string resources, and initializes various UI components and the database helper.
     * It also calls methods to set up the month and year spinners.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        months = new String[]{
                getString(R.string.lbl_all_year),
                getString(R.string.lbl_january), getString(R.string.lbl_february), getString(R.string.lbl_march),
                getString(R.string.lbl_april), getString(R.string.lbl_may), getString(R.string.lbl_june),
                getString(R.string.lbl_july), getString(R.string.lbl_august), getString(R.string.lbl_september),
                getString(R.string.lbl_october), getString(R.string.lbl_november), getString(R.string.lbl_december)
        };

        trainDao = AppDatabase.getInstance(this).trainDao();
        spinnerMonths = findViewById(R.id.spinnerMonths);
        spinnerYears = findViewById(R.id.spinnerYears);
        tvStatsKm = findViewById(R.id.tvStatsKm);
        tvStatsTime = findViewById(R.id.tvStatsTime);

        setupMonthSpinner();
        setupYearSpinner();
    }

    /**
     * Configures and initializes the month selection spinner.
     * <p>
     * This method sets up an {@link ArrayAdapter} with the list of months (including an "All Year" option)
     * and attaches it to the {@code spinnerMonths} widget. It automatically selects the current month
     * by default. An {@link AdapterView.OnItemSelectedListener} is also attached to detect when the
     * user selects a different month, which triggers an update of the displayed statistics by calling
     * {@link #updateStats()}.
     * </p>
     */
    private void setupMonthSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, months);
        adapter.setDropDownViewResource(R.layout.spinner_item);

        spinnerMonths.setAdapter(adapter);
        selectedMonthIndex = Calendar.getInstance().get(Calendar.MONTH) + 1;
        spinnerMonths.setSelection(selectedMonthIndex);

        spinnerMonths.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            /**
             * Callback method to be invoked when an item in this view has been
             * selected. This implementation is called when the user selects a month from the spinner.
             * <p>
             * It checks if the newly selected position is different from the currently
             * selected one to avoid redundant updates. If the selection has changed, it updates
             * the {@code selectedMonthIndex} and calls {@link #updateStats()} to refresh the
             * displayed statistics for the newly selected month.
             *
             * @param parent The AdapterView where the selection happened.
             * @param view The view within the AdapterView that was clicked.
             * @param position The position of the view in the adapter.
             * @param id The row id of the item that is selected.
             */
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view == null) return;

                if (selectedMonthIndex != position) {
                    selectedMonthIndex = position;
                    updateStats();
                }
            }

            /**
             * Callback method to be invoked when the selection disappears from this
             * view. This can happen when the adapter becomes empty or when the selection
             * is cleared. The default implementation is a no-op, as no specific
             * action is required in this scenario.
             *
             * @param parent The AdapterView that now contains no selected item.
             */
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Configures and initializes the year selection spinner.
     * <p>
     * This method fetches the list of years for which there is available data from the database
     * using {@link TrainDao#getAvailableYears()}. It then populates the {@code spinnerYears}
     * with these years using an {@link ArrayAdapter}. By default, the first year in the list is
     * selected. An {@link AdapterView.OnItemSelectedListener} is set up to handle user selections,
     * updating the {@code selectedYear} and triggering a call to {@link #updateStats()} to refresh
     * the displayed statistics whenever a new year is chosen.
     * </p>
     */
    private void setupYearSpinner() {
        List<Integer> yearsInt = trainDao.getAvailableYears();
        List<String> years = new java.util.ArrayList<>();
        for (Integer y : yearsInt) years.add(String.valueOf(y));

        String currentYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        if (!years.contains(currentYear)) {
            years.add(0, currentYear);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, years);
        adapter.setDropDownViewResource(R.layout.spinner_item);

        spinnerYears.setAdapter(adapter);

        if (!years.isEmpty()) {
            selectedYear = Integer.parseInt(years.get(0));
        }

        spinnerYears.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            /**
             * Callback method to be invoked when an item in this view has been
             * selected. This listener is triggered when the user selects a year from the
             * spinner. It updates the {@code selectedYear} with the newly chosen value
             * and calls {@link #updateStats()} to refresh the displayed statistics.
             *
             * @param parent The AdapterView where the selection happened.
             * @param view The view within the AdapterView that was clicked.
             * @param position The position of the view in the adapter.
             * @param id The row id of the item that is selected.
             */
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String yearString = (String) parent.getItemAtPosition(position);
                selectedYear = Integer.parseInt(yearString);
                updateStats();
            }
            /**
             * Callback method to be invoked when the selection disappears from this
             * view. This can happen when the adapter becomes empty or when the selection
             * is cleared. This implementation is intentionally left empty as no action
             * is required when nothing is selected in the spinner.
             *
             * @param parent The AdapterView that now contains no selected item.
             */
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Fetches and displays travel statistics based on the selected month and year.
     * <p>
     * This method queries the database to get the total kilometers traveled and total minutes spent
     * on trips. It behaves differently based on the {@code selectedMonthIndex}:
     * <ul>
     *     <li>If {@code selectedMonthIndex} is 0 (representing "All Year"), it iterates through all
     *     12 months, summing up the stats for the {@code selectedYear}.</li>
     *     <li>Otherwise, it fetches the stats only for the specific month and year selected. Note that
     *     the database uses a 0-based index for months, so 1 is subtracted from {@code selectedMonthIndex}.</li>
     * </ul>
     * After fetching the data, it updates the {@code tvStatsKm} and {@code tvStatsTime} TextViews
     * with the formatted results.
     * </p>
     */
    private void updateStats() {
        int km = 0, minutes = 0;

        if (selectedMonthIndex == 0) {
            Integer kmObj = trainDao.getTotalKmForYear(selectedYear);
            km = kmObj != null ? kmObj : 0;
            Integer durObj = trainDao.getTotalDurationForYear(selectedYear);
            minutes = durObj != null ? durObj : 0;
        } else {
            int dbMonthIndex = selectedMonthIndex - 1;
            Integer kmObj = trainDao.getKmByMonthAndYear(dbMonthIndex, selectedYear);
            km = kmObj != null ? kmObj : 0;
            Integer durObj = trainDao.getDurationByMonthAndYear(dbMonthIndex, selectedYear);
            minutes = durObj != null ? durObj : 0;
        }

        tvStatsKm.setText(getString(R.string.stats_km, km));
        tvStatsTime.setText(formatTime(minutes));
    }

    /**
     * Formats a total number of minutes into a human-readable string "X hours Y mins".
     * <p>
     * Converts the given duration in minutes into a structured format.
     * If the total minutes is 0, it returns a specific string indicating zero minutes.
     * Otherwise, it calculates the number of hours and remaining minutes and formats them
     * using the {@code R.string.stats_time} string resource.
     *
     * @param totalMinutes The total duration in minutes to be formatted.
     * @return A formatted string representing the time in hours and minutes (e.g., "5 hours 23 mins"),
     *         or a string for zero minutes if the input is 0.
     */
    private String formatTime(int totalMinutes) {
        if (totalMinutes == 0) return getString(R.string.stats_zero_min);

        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;

        return getString(R.string.stats_time, hours, mins);
    }
}