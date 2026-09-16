package com.example.miles.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MoveReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != MoveReminderManager.ACTION_MOVE_REMINDER) return
        MoveReminderManager(context, com.example.miles.data.local.MilesPreferences(context)).sendScheduledReminder()
    }
}
