package com.example.miles.engine

import com.example.miles.data.model.ActivityType
import com.example.miles.data.model.GpsPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reviewer found workouts reporting invented steps, invented max speed and
 * invented elevation after a recovery restore. These tests pin down the real
 * behaviour so none of it can come back.
 */
class WorkoutMetricsTest {

    private fun point(
        lat: Double,
        lng: Double,
        alt: Double = 0.0,
        speed: Float = 0f,
        ts: Long
    ) = GpsPoint(latitude = lat, longitude = lng, altitude = alt, speed = speed, timestamp = ts)

    @Test
    fun `steps without a sensor stay at zero`() {
        // Emulator with no step sensor: distance must never become steps.
        assertEquals(0, WorkoutMetrics.stepsFor(ActivityType.RUNNING, 0))
        assertEquals(0, WorkoutMetrics.stepsFor(ActivityType.WALKING, 0))
    }

    @Test
    fun `cycling never reports steps`() {
        // 1.43 km ride: distance/0.76 used to invent ~1880 "steps".
        assertEquals(0, WorkoutMetrics.stepsFor(ActivityType.CYCLING, 1887))
        assertEquals(0, WorkoutMetrics.stepsFor(ActivityType.CYCLING, 0))
    }

    @Test
    fun `real sensor steps are reported untouched`() {
        assertEquals(1894, WorkoutMetrics.stepsFor(ActivityType.RUNNING, 1894))
        assertEquals(42, WorkoutMetrics.stepsFor(ActivityType.HIKING, 42))
    }

    @Test
    fun `negative sensor readings are clamped to zero`() {
        assertEquals(0, WorkoutMetrics.stepsFor(ActivityType.WALKING, -5))
    }

    @Test
    fun `average speed needs both distance and time`() {
        assertEquals(0.0, WorkoutMetrics.avgSpeedKmh(0.0, 60_000L), 0.001)
        assertEquals(0.0, WorkoutMetrics.avgSpeedKmh(1000.0, 0L), 0.001)
        // 1.43 km in 4 minutes is about 21.5 km/h
        assertEquals(21.45, WorkoutMetrics.avgSpeedKmh(1430.0, 240_000L), 0.2)
    }

    @Test
    fun `max speed comes from the track not from the average`() {
        // Straight line north: 0.0005 degrees latitude is ~55.5 m, covered in 15 s,
        // so the real speed is ~13.3 km/h. (Legs are clamped at 15 s by design, so a
        // longer pause between fixes must not turn into a speed spike.)
        val legs = mutableListOf(point(47.0, 15.0, ts = 0L))
        for (i in 1..10) {
            legs.add(point(47.0 + i * 0.0005, 15.0, ts = i * 15_000L))
        }
        val max = WorkoutMetrics.maxSpeedKmh(legs)
        assertTrue("expected ~13 km/h, got $max", max in 12.0..15.0)
    }

    @Test
    fun `a long pause between fixes does not become a speed spike`() {
        // Same 55 m apart, but 5 minutes apart: the 15 s clamp bounds the result at
        // 55.5 m / 15 s instead of pretending the rider did 8 km/h over the gap.
        val paused = listOf(
            point(47.0, 15.0, ts = 0L),
            point(47.0005, 15.0, ts = 300_000L)
        )
        val max = WorkoutMetrics.maxSpeedKmh(paused)
        assertTrue("expected ~13 km/h after clamp, got $max", max in 12.0..15.0)
    }

    @Test
    fun `jittery points do not become speed`() {
        val jitter = listOf(
            point(47.0, 15.0, ts = 0L),
            // 1 mm of movement in 10 ms would be a absurd speed if not filtered
            point(47.0, 15.00000001, ts = 10L),
            point(47.0, 15.0, ts = 20L)
        )
        assertEquals(0.0, WorkoutMetrics.maxSpeedKmh(jitter), 0.001)
    }

    @Test
    fun `a single point has no meaningful max speed`() {
        assertEquals(0.0, WorkoutMetrics.maxSpeedKmh(listOf(point(47.0, 15.0, ts = 0L))), 0.001)
        assertEquals(0.0, WorkoutMetrics.maxSpeedKmh(emptyList()), 0.001)
    }

    @Test
    fun `elevation gain and loss use the same deadband as live tracking`() {
        val track = listOf(
            point(47.0, 15.0, alt = 100.0, ts = 0L),
            point(47.001, 15.0, alt = 110.0, ts = 10_000L),  // +10 m counted
            point(47.002, 15.0, alt = 109.8, ts = 20_000L),  // -0.2 m is noise, ignored
            point(47.003, 15.0, alt = 95.0, ts = 30_000L),   // -14.8 m counted
            point(47.004, 15.0, alt = 105.0, ts = 40_000L)   // +10 m counted
        )
        val (gain, loss) = WorkoutMetrics.elevationProfile(track)
        assertEquals(20.0, gain, 0.001)
        assertEquals(14.8, loss, 0.001)
    }

    @Test
    fun `flat track has no elevation gain`() {
        val track = listOf(
            point(47.0, 15.0, alt = 100.0, ts = 0L),
            point(47.001, 15.0, alt = 100.1, ts = 10_000L),
            point(47.002, 15.0, alt = 99.9, ts = 20_000L)
        )
        val (gain, loss) = WorkoutMetrics.elevationProfile(track)
        assertEquals(0.0, gain, 0.001)
        assertEquals(0.0, loss, 0.001)
    }

    @Test
    fun `no track means no distance`() {
        assertEquals(0.0, WorkoutMetrics.distanceFromPoints(emptyList()), 0.001)
        assertEquals(0.0, WorkoutMetrics.distanceFromPoints(listOf(point(47.0, 15.0, ts = 0L))), 0.001)
    }

    @Test
    fun `distance follows the recorded track`() {
        val track = (0..10).map { i -> point(47.0 + i * 0.001, 15.0, ts = i * 10_000L) }
        val distance = WorkoutMetrics.distanceFromPoints(track)
        // 10 steps of ~111 m
        assertTrue("expected ~1110 m, was $distance", distance in 1050.0..1170.0)
    }

    @Test
    fun `best pace is a real segment not a fraction of the average`() {
        val track = (0..20).map { i -> point(47.0 + i * 0.001, 15.0, ts = i * 60_000L) }
        // 20 legs of 111 m, 60 s each = 9:00 /km pace throughout
        val best = WorkoutMetrics.bestPaceSecPerKm(track, totalDistanceMeters = WorkoutMetrics.distanceFromPoints(track))
        assertTrue("expected ~540 s/km, was $best", best != null && best in 520.0..560.0)
    }

    @Test
    fun `best pace is null when there is nothing to measure`() {
        assertNull(WorkoutMetrics.bestPaceSecPerKm(emptyList(), 1000.0))
        assertNull(WorkoutMetrics.bestPaceSecPerKm(listOf(point(47.0, 15.0, ts = 0L)), 1000.0))
        assertNull(WorkoutMetrics.bestPaceSecPerKm(listOf(point(47.0, 15.0, ts = 0L)), 0.0))
    }

    @Test
    fun `heart rate average and peak only ever reflect real samples`() {
        val tracker = HeartRateTracker()
        // No sensor reading: no average, no peak, never an invented +20.
        assertEquals(0, tracker.average())
        assertEquals(0, tracker.peak())
        assertEquals(0, tracker.sampleCount())

        tracker.record(142)
        tracker.record(150)
        tracker.record(130)
        assertEquals(3, tracker.sampleCount())
        assertEquals(140, tracker.average())
        assertEquals(150, tracker.peak())

        // Physically impossible readings are ignored rather than averaged in.
        tracker.record(500)
        tracker.record(10)
        assertEquals(3, tracker.sampleCount())
        assertEquals(140, tracker.average())
        assertEquals(150, tracker.peak())

        // A new workout starts from a clean slate.
        tracker.reset()
        assertEquals(0, tracker.average())
        assertEquals(0, tracker.peak())
    }
}
