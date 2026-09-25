package com.example.miles.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Real-data calorie maths: personal weight, activity burn, everyday steps. */
class CalorieEstimatorTest {

    @Test
    fun `weight is clamped to a safe range`() {
        assertEquals(30.0, CalorieEstimator.clampWeight(5), 0.001)
        assertEquals(70.0, CalorieEstimator.clampWeight(70), 0.001)
        assertEquals(250.0, CalorieEstimator.clampWeight(9999), 0.001)
    }

    @Test
    fun `long hard session burns more than a short one`() {
        val short = CalorieEstimator.activityCalories(
            met = 8.0, weightKg = 70, elapsedSeconds = 600,
            distanceMeters = 1000.0, steps = 1200, perMetreRate = 0.065
        )
        val long = CalorieEstimator.activityCalories(
            met = 8.0, weightKg = 70, elapsedSeconds = 3600,
            distanceMeters = 6000.0, steps = 7200, perMetreRate = 0.065
        )
        assertTrue(long > short)
    }

    @Test
    fun `heavier user burns more for identical work`() {
        val light = CalorieEstimator.activityCalories(
            met = 7.0, weightKg = 55, elapsedSeconds = 1800,
            distanceMeters = 3000.0, steps = 3600, perMetreRate = 0.065
        )
        val heavy = CalorieEstimator.activityCalories(
            met = 7.0, weightKg = 95, elapsedSeconds = 1800,
            distanceMeters = 3000.0, steps = 3600, perMetreRate = 0.065
        )
        assertTrue(heavy > light)
    }

    @Test
    fun `a real workout always reports at least one kcal after ten seconds`() {
        val cals = CalorieEstimator.activityCalories(
            met = 3.5, weightKg = 70, elapsedSeconds = 11,
            distanceMeters = 0.0, steps = 0, perMetreRate = 0.045
        )
        assertTrue("should never report 0 kcal for real movement, was $cals", cals >= 1)
    }

    @Test
    fun `a fresh start reports zero rather than fake calories`() {
        val cals = CalorieEstimator.activityCalories(
            met = 3.5, weightKg = 70, elapsedSeconds = 0,
            distanceMeters = 0.0, steps = 0, perMetreRate = 0.045
        )
        assertEquals(0, cals)
    }

    @Test
    fun `ten thousand everyday steps gives a realistic burn for 70kg`() {
        val kcal = CalorieEstimator.dailyActiveFromSteps(10_000, 70)
        assertTrue("10k steps should be 380-480 kcal, was $kcal", kcal in 380..480)
    }

    @Test
    fun `everyday calories scale with steps and body weight`() {
        val tenK = CalorieEstimator.dailyActiveFromSteps(10_000, 70)
        val fiveK = CalorieEstimator.dailyActiveFromSteps(5_000, 70)
        val heavy = CalorieEstimator.dailyActiveFromSteps(10_000, 100)
        assertTrue(fiveK < tenK)
        assertTrue(heavy > tenK)
    }

    @Test
    fun `no everyday steps means no calories`() {
        assertEquals(0, CalorieEstimator.dailyActiveFromSteps(0, 70))
        assertEquals(0, CalorieEstimator.dailyActiveFromSteps(-10, 70))
    }
}