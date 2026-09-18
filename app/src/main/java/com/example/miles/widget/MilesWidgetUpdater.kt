package com.example.miles.widget

import android.content.Context

object MilesWidgetUpdater {

    fun updateAllWidgets(
        context: Context,
        calories: Int,
        calGoal: Int,
        steps: Int,
        stepGoal: Int,
        activeMin: Int,
        activeGoal: Int,
        hrBpm: Int? = null,
        isWatchConnected: Boolean = false,
        isRecording: Boolean = false,
        sportName: String = "READY"
    ) {
        runCatching {
            MilesRingsWidgetProvider.updateRingsWidgets(
                context = context,
                calories = calories,
                calGoal = calGoal,
                steps = steps,
                stepGoal = stepGoal,
                activeMin = activeMin,
                activeGoal = activeGoal,
                hrBpm = hrBpm,
                isWatchConnected = isWatchConnected
            )
            MilesQuickWorkoutWidgetProvider.updateQuickWorkoutWidgets(
                context = context,
                isRecording = isRecording,
                sportName = sportName
            )
        }
    }

    fun syncWorkoutState(context: Context, isRecording: Boolean, sportName: String, calories: Int, durationMin: Int) {
        runCatching {
            MilesQuickWorkoutWidgetProvider.updateQuickWorkoutWidgets(context, isRecording, sportName)
        }
    }
}
