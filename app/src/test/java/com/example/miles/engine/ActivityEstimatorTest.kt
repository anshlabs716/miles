package com.example.miles.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Everyday distance from real steps + the user's real height. */
class ActivityEstimatorTest {

    @Test
    fun `stride grows with height and stays realistic`() {
        val short = ActivityEstimator.strideLengthMeters(150f)
        val average = ActivityEstimator.strideLengthMeters(175f)
        val tall = ActivityEstimator.strideLengthMeters(200f)
        assertTrue("taller should stride further", tall > average)
        assertTrue("shorter should stride less", average > short)
        // Everyday walking stride is roughly 0.5-1.0 m
        assertTrue(average in 0.5..0.9)
    }

    @Test
    fun `absurd heights are clamped to a sane range`() {
        assertEquals(
            ActivityEstimator.strideLengthMeters(120f),
            ActivityEstimator.strideLengthMeters(10f),
            0.0001
        )
        assertEquals(
            ActivityEstimator.strideLengthMeters(220f),
            ActivityEstimator.strideLengthMeters(900f),
            0.0001
        )
    }

    @Test
    fun `ten thousand steps is a realistic everyday distance`() {
        val meters = ActivityEstimator.distanceMetersFromSteps(10_000, 175f)
        // 10k steps at a normal stride is roughly 5-8 km
        assertTrue("10k steps should be 5-8 km, was $meters m", meters in 5_000.0..8_000.0)
    }

    @Test
    fun `no steps means no distance`() {
        assertEquals(0.0, ActivityEstimator.distanceMetersFromSteps(0, 175f), 0.0001)
        assertEquals(0.0, ActivityEstimator.distanceMetersFromSteps(-50, 175f), 0.0001)
    }

    @Test
    fun `workout steps are excluded so distance is never double counted`() {
        val total = 10_000
        val workout = 4_000
        assertEquals(6_000, ActivityEstimator.everydaySteps(total, workout))
        val meters = ActivityEstimator.everydayDistanceMeters(total, workout, 175f)
        val expected = ActivityEstimator.distanceMetersFromSteps(6_000, 175f)
        assertEquals(expected, meters, 0.0001)
    }

    @Test
    fun `everyday steps never go negative`() {
        assertEquals(0, ActivityEstimator.everydaySteps(500, 2_000))
        assertEquals(0.0, ActivityEstimator.everydayDistanceMeters(500, 2_000, 175f), 0.0001)
    }

    @Test
    fun `active minutes estimate uses a realistic walking cadence`() {
        // ~100 steps/min while walking
        assertEquals(75, ActivityEstimator.estimatedActiveMinutesFromSteps(7_557))
        assertEquals(10, ActivityEstimator.estimatedActiveMinutesFromSteps(1_000))
    }

    @Test
    fun `no steps means no estimated active minutes`() {
        assertEquals(0, ActivityEstimator.estimatedActiveMinutesFromSteps(0))
        assertEquals(0, ActivityEstimator.estimatedActiveMinutesFromSteps(-100))
    }
}
