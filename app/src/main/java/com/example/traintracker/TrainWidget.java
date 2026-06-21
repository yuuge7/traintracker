package com.example.traintracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;

import java.util.Calendar;

public class TrainWidget extends AppWidgetProvider {
    private static final String ACTION_TOGGLE_TIMER = "com.example.traintracker.ACTION_TOGGLE_TIMER";

    /**
     * Called in response to the {@link AppWidgetManager#ACTION_APPWIDGET_UPDATE} broadcast intent,
     * as well as when a new widget is created. This method is responsible for updating all
     * instances of the TrainTracker widget.
     * <p>
     * It iterates through all the widget IDs provided and calls {@link #updateAppWidget(Context, AppWidgetManager, int)}
     * for each one to refresh its content.
     *
     * @param context The Context in which this receiver is running.
     * @param appWidgetManager A manager for updating AppWidget state.
     * @param appWidgetIds The IDs of the app widgets that need to be updated.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * Updates the layout and data for a specific app widget instance.
     * <p>
     * This method fetches the latest train statistics (total kilometers and duration for the current year)
     * from the database and updates the widget's text views. It also configures the main action
     * button's text, color, and behavior based on whether a trip timer is currently running.
     *
     * @param context          The context in which the receiver is running.
     * @param appWidgetManager The AppWidgetManager instance to use for updating the widget.
     * @param appWidgetId      The ID of the app widget instance to update.
     */
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        TrainDao trainDao = AppDatabase.getInstance(context).trainDao();
        SharedPreferences prefs = context.getSharedPreferences("TrainAppPrefs", Context.MODE_PRIVATE);

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        Integer kmObj = trainDao.getTotalKmForYear(currentYear);
        int totalKm = kmObj != null ? kmObj : 0;
        Integer durObj = trainDao.getTotalDurationForYear(currentYear);
        int totalMinutes = durObj != null ? durObj : 0;
        long startTime = prefs.getLong("START_TIME", 0);
        boolean isRunning = (startTime != 0);

        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;

        String durationText = context.getString(R.string.stats_time, hours, mins);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        views.setTextViewText(R.id.widgetYear, String.valueOf(currentYear));

        views.setTextViewText(R.id.widgetKm, context.getString(R.string.stats_km, totalKm));
        views.setTextViewText(R.id.widgetDuration, durationText);

        if (isRunning) {
            views.setTextViewText(R.id.widgetBtnAction, context.getString(R.string.widget_btn_stop));
            views.setInt(R.id.widgetBtnAction, "setBackgroundColor", Color.parseColor("#D32F2F"));

            views.setTextViewText(R.id.widgetStatusText, context.getString(R.string.widget_running));
            views.setTextColor(R.id.widgetStatusText, Color.parseColor("#D32F2F"));

            Intent intentOpen = new Intent(context, MainActivity.class);
            intentOpen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingOpen = PendingIntent.getActivity(context, 0, intentOpen, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetBtnAction, pendingOpen);

        } else {
            views.setTextViewText(R.id.widgetBtnAction, context.getString(R.string.widget_btn_start));
            views.setInt(R.id.widgetBtnAction, "setBackgroundColor", Color.parseColor("#4CAF50")); // Verde

            views.setTextViewText(R.id.widgetStatusText, context.getString(R.string.widget_ready));
            views.setTextColor(R.id.widgetStatusText, Color.parseColor("#4CAF50"));

            Intent intentStart = new Intent(context, TrainWidget.class);
            intentStart.setAction(ACTION_TOGGLE_TIMER);
            PendingIntent pendingStart = PendingIntent.getBroadcast(context, 0, intentStart, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetBtnAction, pendingStart);
        }

        views.setTextViewText(R.id.widgetBtnManual, context.getString(R.string.widget_btn_manual));
        Intent intentManual = new Intent(context, MainActivity.class);
        intentManual.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingManual = PendingIntent.getActivity(context, 1, intentManual, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetBtnManual, pendingManual);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * This method is called by the system when a broadcast Intent is received.
     * It's a standard entry point for broadcast receivers.
     * <p>
     * This implementation first calls the superclass method to handle standard widget
     * lifecycle events. It then checks if the received intent's action is
     * {@code ACTION_TOGGLE_TIMER}. If it is, this indicates the user has clicked
     * the "Start" button on the widget. The method then records the current time
     * as the trip's start time in SharedPreferences and triggers a widget update
     * to reflect the new "running" state.
     *
     * @param context The Context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_TOGGLE_TIMER.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("TrainAppPrefs", Context.MODE_PRIVATE);
            long now = System.currentTimeMillis();
            prefs.edit().putLong("START_TIME", now).apply();

            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), TrainWidget.class.getName());
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] ids = appWidgetManager.getAppWidgetIds(thisAppWidget);
            onUpdate(context, appWidgetManager, ids);
        }
    }
}