package com.example.miles.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** Real goal streaks — no hardcoded numbers. */
class StreakCalculatorTest {

    private fun dayAtOffset(daysAgo: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeZone = TimeZone.getDefault()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }
        return cal.timeInMillis
    }

    @Test
    fun `no met days means no streak`() {
        val today = StreakCalculator.todayKey()
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), today))
    }

    @Test
    fun `consecutive met days ending today build a streak`() {
        val met = setOf(
            StreakCalculator.dayKey(dayAtOffset(0)),
            StreakCalculator.dayKey(dayAtOffset(1)),
            StreakCalculator.dayKey(dayAtOffset(2))
        )
        assertEquals(3, StreakCalculator.currentStreak(met, StreakCalculator.todayKey()))
    }

    @Test
    fun `today not met yet does not break a streak`() {
        val met = setOf(
            StreakCalculator.dayKey(dayAtOffset(1)),
            StreakCalculator.dayKey(dayAtOffset(2))
        )
        assertEquals(2, StreakCalculator.currentStreak(met, StreakCalculator.todayKey()))
    }

    @Test
    fun `a missed yesterday ends the streak`() {
        val met = setOf(
            StreakCalculator.dayKey(dayAtOffset(2)),
            StreakCalculator.dayKey(dayAtOffset(3))
        )
        assertEquals(0, StreakCalculator.currentStreak(met, StreakCalculator.todayKey()))
    }

    @Test
    fun `only days that met the step goal count`() {
        val goal = 8000
        val metDays = StreakCalculator.metDaysFromActivities(
            activitySteps = listOf(
                dayAtOffset(0) to 9_000,   // met
                dayAtOffset(1) to 2_000,   // not met
                dayAtOffset(2) to 12_000   // met
            ),
            stepGoal = goal
        )
        assertEquals(2, metDays.size)
        assertTrue(StreakCalculator.dayKey(dayAtOffset(0)) in metDays)
        assertTrue(StreakCalculator.dayKey(dayAtOffset(1)) !in metDays)
        assertEquals(1, StreakCalculator.currentStreak(metDays, StreakCalculator.todayKey()))
    }

    @Test
    fun `multiple activities in a day add up toward the goal`() {
        val metDays = StreakCalculator.metDaysFromActivities(
            activitySteps = listOf(dayAtOffset(0) to 5_000, dayAtOffset(0) to 4_000),
            stepGoal = 8_000
        )
        assertEquals(1, metDays.size)
    }

    @Test
    fun `a zero or negative goal never fakes a streak`() {
        val metDays = StreakCalculator.metDaysFromActivities(
            activitySteps = listOf(dayAtOffset(0) to 1),
            stepGoal = 0
        )
        assertTrue(metDays.isEmpty())
    }

    @Test
    fun `streak label reads naturally`() {
        assertEquals("No streak yet", StreakCalculator.streakLabel(0))
        assertEquals("1 Day Streak", StreakCalculator.streakLabel(1))
        assertEquals("7 Day Streak", StreakCalculator.streakLabel(7))
    }
}
