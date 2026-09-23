package com.example.miles.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.miles.data.local.MilesPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MilesWidgetSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        syncWidgetsNow(context)
        schedulePeriodicSync(context)
    }

    companion object {
        private const val ACTION_SYNC_WIDGETS = "com.example.miles.ACTION_SYNC_WIDGETS"
        private const val REQUEST_CODE = 4040

        fun schedulePeriodicSync(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, MilesWidgetSyncReceiver::class.java).apply {
                action = ACTION_SYNC_WIDGETS
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Trigger every 15 minutes
            val triggerAtMs = SystemClock.elapsedRealtime() + (15 * 60 * 1000L)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        }

        fun syncWidgetsNow(context: Context) {
            val prefs = MilesPreferences(context)
            CoroutineScope(Dispatchers.IO).launch {
                val userPrefs = prefs.userPreferences.firstOrNull() ?: return@launch
                val settingsPrefs = context.getSharedPreferences("miles_settings", Context.MODE_PRIVATE)
                val todayCal = settingsPrefs.getInt("today_workout_calories", 240)
                val todayMin = settingsPrefs.getInt("today_workout_duration_min", 32)
                val steps = settingsPrefs.getInt("today_step_count", 6420)

                MilesWidgetUpdater.updateAllWidgets(
                    context = context,
                    calories = todayCal,
                    calGoal = userPrefs.dailyCaloriesGoal,
                    steps = steps,
                    stepGoal = userPrefs.dailyStepGoal,
                    activeMin = todayMin,
                    activeGoal = userPrefs.dailyActiveMinutesGoal,
                    hrBpm = null,
                    isWatchConnected = false,
                    isRecording = false,
                    sportName = "READY"
                )
            }
        }
    }
}
