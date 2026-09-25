package com.example.miles.engine

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Real daily-goal streaks — no hardcoded numbers.
 *
 * A day "counts" when the user's real data met their real step goal. Because
 * Android's step counter keeps no history, past days are judged from recorded
 * activities (which store their real step counts); today additionally uses the
 * live sensor total. Today not being met yet never breaks a streak — it is
 * only broken once a full day passes without meeting the goal.
 */
object StreakCalculator {

    /** yyyy-MM-dd key for an epoch-millis timestamp (local time). */
    fun dayKey(epochMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(epochMs))

    fun todayKey(nowMs: Long = System.currentTimeMillis()): String = dayKey(nowMs)

    /**
     * Consecutive goal-meeting days ending today (or yesterday, if today is
     * still in progress).
     */
    fun currentStreak(
        metDays: Set<String>,
        todayKey: String,
        maxLookbackDays: Int = 365
    ): Int {
        if (metDays.isEmpty()) return 0
        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getDefault()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Start from today when it's met, otherwise from yesterday.
        if (dayKey(cal.timeInMillis) !in metDays) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        var streak = 0
        repeat(maxLookbackDays) {
            if (dayKey(cal.timeInMillis) !in metDays) return streak
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    /** Days that met the step goal, from recorded activities (real step counts). */
    fun metDaysFromActivities(
        activitySteps: Collection<Pair<Long, Int>>,
        stepGoal: Int,
        extraDays: Set<String> = emptySet()
    ): Set<String> {
        if (stepGoal <= 0) return extraDays
        val perDay = HashMap<String, Int>()
        activitySteps.forEach { (startTime, steps) ->
            val key = dayKey(startTime)
            perDay[key] = (perDay[key] ?: 0) + steps
        }
        return extraDays + perDay.filterValues { it >= stepGoal }.keys
    }

    /** Human label for the dashboard chip. */
    fun streakLabel(streak: Int): String = when {
        streak <= 0 -> "No streak yet"
        streak == 1 -> "1 Day Streak"
        else -> "$streak Day Streak"
    }
}
