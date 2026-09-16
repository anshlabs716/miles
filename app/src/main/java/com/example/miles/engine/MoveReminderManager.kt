package com.example.miles.engine

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.miles.data.local.MilesPreferences

/** Schedules reminders with AlarmManager so they survive process death. */
class MoveReminderManager(
    private val context: Context,
    private val preferences: MilesPreferences
) {
    companion object {
        const val CHANNEL_ID = "miles_move_reminders"
        const val NOTIFICATION_ID = 2048
        const val REQUEST_CODE = 2048
        const val ACTION_MOVE_REMINDER = "com.aistudio.miles.track.action.MOVE_REMINDER"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    init { createNotificationChannel() }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Get Up & Move Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Optional reminders to stretch and take a movement break"
                }
            )
        }
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, MoveReminderReceiver::class.java).setAction(ACTION_MOVE_REMINDER)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun scheduleNextReminder() {
        cancel()
        val prefs = preferences.userPreferences.value
        if (!prefs.moveReminderEnabled) return
        val intervalMinutes = prefs.moveReminderIntervalMinutes.coerceIn(15, 24 * 60)
        val triggerAt = System.currentTimeMillis() + intervalMinutes * 60_000L
        runCatching {
            alarmManager?.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                intervalMinutes * 60_000L,
                pendingIntent()
            )
        }
    }

    fun cancel() {
        runCatching { alarmManager?.cancel(pendingIntent()) }
    }

    fun refreshSchedule() = scheduleNextReminder()

    fun sendTestReminder() {
        val prefs = preferences.userPreferences.value
        sendMoveNotification(
            "MILES move reminder",
            prefs.moveReminderCustomText.ifBlank { "Take a movement break and stretch." }
        )
    }

    fun sendScheduledReminder() {
        val prefs = preferences.userPreferences.value
        if (prefs.moveReminderEnabled) {
            sendMoveNotification(
                "MILES move reminder",
                prefs.moveReminderCustomText.ifBlank { "Take a movement break and stretch." }
            )
        }
    }

    private fun sendMoveNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationManager?.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
        )
    }
}
