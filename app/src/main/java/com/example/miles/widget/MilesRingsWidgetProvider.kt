package com.example.miles.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.util.Locale

class MilesRingsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
        val calories = prefs.getInt(KEY_CALORIES, 0)
        val calGoal = prefs.getInt(KEY_CAL_GOAL, 500).coerceAtLeast(100)
        val steps = prefs.getInt(KEY_STEPS, 0)
        val stepGoal = prefs.getInt(KEY_STEP_GOAL, 8000).coerceAtLeast(1000)
        val activeMin = prefs.getInt(KEY_ACTIVE_MIN, 0)
        val activeGoal = prefs.getInt(KEY_ACTIVE_GOAL, 45).coerceAtLeast(10)
        val hrBpm = prefs.getInt(KEY_HR_BPM, -1).takeIf { it > 0 }
        val isWatchConnected = prefs.getBoolean(KEY_WATCH_CONNECTED, false)

        for (appWidgetId in appWidgetIds) {
            val views = buildRemoteViews(context, calories, calGoal, steps, stepGoal, activeMin, activeGoal, hrBpm, isWatchConnected)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        const val PREFS_WIDGET = "miles_widget_prefs"
        const val KEY_CALORIES = "widget_calories"
        const val KEY_CAL_GOAL = "widget_cal_goal"
        const val KEY_STEPS = "widget_steps"
        const val KEY_STEP_GOAL = "widget_step_goal"
        const val KEY_ACTIVE_MIN = "widget_active_min"
        const val KEY_ACTIVE_GOAL = "widget_active_goal"
        const val KEY_HR_BPM = "widget_hr_bpm"
        const val KEY_WATCH_CONNECTED = "widget_watch_connected"

        fun updateRingsWidgets(
            context: Context,
            calories: Int,
            calGoal: Int,
            steps: Int,
            stepGoal: Int,
            activeMin: Int,
            activeGoal: Int,
            hrBpm: Int?,
            isWatchConnected: Boolean
        ) {
            val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_CALORIES, calories)
                .putInt(KEY_CAL_GOAL, calGoal)
                .putInt(KEY_STEPS, steps)
                .putInt(KEY_STEP_GOAL, stepGoal)
                .putInt(KEY_ACTIVE_MIN, activeMin)
                .putInt(KEY_ACTIVE_GOAL, activeGoal)
                .putInt(KEY_HR_BPM, hrBpm ?: -1)
                .putBoolean(KEY_WATCH_CONNECTED, isWatchConnected)
                .apply()

            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, MilesRingsWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            for (id in appWidgetIds) {
                val views = buildRemoteViews(context, calories, calGoal, steps, stepGoal, activeMin, activeGoal, hrBpm, isWatchConnected)
                appWidgetManager.updateAppWidget(id, views)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            calories: Int,
            calGoal: Int,
            steps: Int,
            stepGoal: Int,
            activeMin: Int,
            activeGoal: Int,
            hrBpm: Int?,
            isWatchConnected: Boolean
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_miles_rings)

            // Primary Hero: Steps Count (Large primary number)
            views.setTextViewText(R.id.widget_steps_hero_text, String.format(Locale.getDefault(), "%,d", steps))
            views.setTextViewText(R.id.widget_step_goal_sub, "• of ${String.format(Locale.getDefault(), "%,d", stepGoal)}")
            views.setTextViewText(R.id.widget_calories_badge, "🔥 $calories kcal")

            // Progress percentages (0 to 100)
            val stepProgress = ((steps.toFloat() / stepGoal) * 100).toInt().coerceIn(0, 100)
            val calProgress = ((calories.toFloat() / calGoal) * 100).toInt().coerceIn(0, 100)
            val minProgress = ((activeMin.toFloat() / activeGoal) * 100).toInt().coerceIn(0, 100)

            views.setProgressBar(R.id.widget_step_progress, 100, stepProgress, false)
            views.setProgressBar(R.id.widget_cal_progress, 100, calProgress, false)
            views.setProgressBar(R.id.widget_min_progress, 100, minProgress, false)

            // Bottom metric values: Calories breakdown, Active Minutes, Heart Rate
            views.setTextViewText(R.id.widget_calories_text, "${String.format(Locale.getDefault(), "%,d", calories)} / ${String.format(Locale.getDefault(), "%,d", calGoal)} kcal")
            views.setTextViewText(R.id.widget_active_text, "${activeMin}m active")

            if (isWatchConnected || (hrBpm != null && hrBpm > 0)) {
                views.setViewVisibility(R.id.widget_hr_text, View.VISIBLE)
                val hrText = if (hrBpm != null && hrBpm > 0) "❤️ $hrBpm bpm" else "⌚ Watch OK"
                views.setTextViewText(R.id.widget_hr_text, hrText)
                views.setTextViewText(R.id.widget_watch_badge, "⌚ LIVE")
            } else {
                views.setViewVisibility(R.id.widget_hr_text, View.GONE)
                views.setTextViewText(R.id.widget_watch_badge, "PHONE")
            }

            // Open app on click
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root_rings, pendingIntent)

            return views
        }
    }
}
