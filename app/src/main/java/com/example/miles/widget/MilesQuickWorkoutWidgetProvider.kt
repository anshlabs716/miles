package com.example.miles.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class MilesQuickWorkoutWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_QUICK_WIDGET, Context.MODE_PRIVATE)
        val isRecording = prefs.getBoolean(KEY_IS_RECORDING, false)
        val sportName = prefs.getString(KEY_CURRENT_SPORT, "READY") ?: "READY"

        for (appWidgetId in appWidgetIds) {
            val views = buildRemoteViews(context, isRecording, sportName)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        const val PREFS_QUICK_WIDGET = "miles_quick_widget_prefs"
        const val KEY_IS_RECORDING = "is_recording"
        const val KEY_CURRENT_SPORT = "current_sport"
        const val EXTRA_START_SPORT = "extra_quick_start_sport"

        fun updateQuickWorkoutWidgets(context: Context, isRecording: Boolean, sportName: String) {
            val prefs = context.getSharedPreferences(PREFS_QUICK_WIDGET, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_IS_RECORDING, isRecording)
                .putString(KEY_CURRENT_SPORT, sportName)
                .apply()

            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, MilesQuickWorkoutWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            for (id in appWidgetIds) {
                val views = buildRemoteViews(context, isRecording, sportName)
                appWidgetManager.updateAppWidget(id, views)
            }
        }

        private fun buildRemoteViews(context: Context, isRecording: Boolean, sportName: String): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_miles_quick_workout)

            if (isRecording) {
                views.setTextViewText(R.id.widget_quick_status, "● $sportName RECORDING")
            } else {
                views.setTextViewText(R.id.widget_quick_status, "READY")
            }

            // Button 1: Run
            views.setOnClickPendingIntent(
                R.id.widget_btn_run,
                createStartSportPendingIntent(context, "RUNNING", 101)
            )

            // Button 2: Walk
            views.setOnClickPendingIntent(
                R.id.widget_btn_walk,
                createStartSportPendingIntent(context, "WALKING", 102)
            )

            // Button 3: Ride
            views.setOnClickPendingIntent(
                R.id.widget_btn_ride,
                createStartSportPendingIntent(context, "CYCLING", 103)
            )

            // Root click opens dashboard
            val rootIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_root_quick,
                PendingIntent.getActivity(context, 100, rootIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            return views
        }

        private fun createStartSportPendingIntent(context: Context, sport: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_START_SPORT, sport)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
