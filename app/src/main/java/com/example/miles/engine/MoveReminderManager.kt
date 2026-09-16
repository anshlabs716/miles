package com.example.miles.engine

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MoveReminderManager(
    private val context: Context,
    private val preferences: MilesPreferences
) {
    companion object {
        const val CHANNEL_ID = "miles_move_reminders"
        const val NOTIFICATION_ID = 2048
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var reminderJob: Job? = null

    init {
        createNotificationChannel()
        startReminderLoop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Get Up & Move Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Periodic alerts to get up, stretch, and keep your daily step streak active"
                enableVibration(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun startReminderLoop() {
        reminderJob?.cancel()
        reminderJob = scope.launch {
            while (isActive) {
                val prefs = preferences.userPreferences.value
                val intervalMs = (prefs.moveReminderIntervalMinutes.coerceAtLeast(15)) * 60 * 1000L
                delay(intervalMs)

                if (preferences.userPreferences.value.moveReminderEnabled) {
                    sendMoveNotification(
                        title = "Time to Move! 🏃",
                        message = preferences.userPreferences.value.moveReminderCustomText
                    )
                }
            }
        }
    }

    fun scheduleNextReminder() = startReminderLoop()

    fun sendTestReminder() {
        val prefs = preferences.userPreferences.value
        sendMoveNotification(
            title = "Get Up & Move (Test) 🏃",
            message = prefs.moveReminderCustomText.ifBlank { "Time to stretch and get moving! Take 250 steps." }
        )
    }

    private fun sendMoveNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
}
