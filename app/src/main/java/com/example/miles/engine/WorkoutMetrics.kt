package com.example.miles.engine

import com.example.miles.data.model.ActivityType
import com.example.miles.data.model.GpsPoint
import com.example.miles.data.repository.MilesRepository
import kotlin.math.abs

/**
 * Pure, testable derivations for workout metrics.
 *
 * Everything here is computed from measurements the device actually recorded:
 * real sensor step counts and the real GPS track. Nothing in this object turns
 * distance into steps, or average speed into "max" speed.
 */
object WorkoutMetrics {

    /**
     * Steps shown for a workout.
     *
     * Only the hardware step sensor may produce a step count: distance is never
     * converted into steps, so a device without a step sensor shows 0. Cycling
     * shows 0 as well, because pedalling is not steps however far you ride.
     */
    fun stepsFor(activityType: ActivityType, sensorSteps: Int): Int =
        if (activityType == ActivityType.CYCLING) 0 else sensorSteps.coerceAtLeast(0)

    /** Distance actually travelled along the recorded track. */
    fun distanceFromPoints(points: List<GpsPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val cur = points[i]
            total += MilesRepository.calculateDistanceMeters(prev.latitude, prev.longitude, cur.latitude, cur.longitude)
        }
        return total
    }

    /** Average speed for a distance covered in an elapsed time. 0 when either is unknown. */
    fun avgSpeedKmh(distanceMeters: Double, elapsedMillis: Long): Double {
        val elapsedSec = elapsedMillis / 1000.0
        if (distanceMeters <= 0.0 || elapsedSec <= 0.0) return 0.0
        return (distanceMeters / elapsedSec) * 3.6
    }

    /**
     * Highest speed the track actually shows: the fastest leg, using the reported
     * location speed when the device provides one and distance/time when it does not.
     * Legs shorter than 0.35 m or spanning under 100 ms are GPS jitter, not speed.
     */
    fun maxSpeedKmh(points: List<GpsPoint>): Double {
        if (points.size < 2) return points.firstOrNull()?.let { (it.speed * 3.6).toDouble() } ?: 0.0
        var maxMps = 0.0
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val cur = points[i]
            val legDist = MilesRepository.calculateDistanceMeters(prev.latitude, prev.longitude, cur.latitude, cur.longitude)
            if (legDist < 0.35) continue
            val timeDeltaSec = ((cur.timestamp - prev.timestamp) / 1000.0).coerceIn(0.1, 15.0)
            val reported = if (cur.speed > 0.2f) cur.speed.toDouble() else 0.0
            val derived = legDist / timeDeltaSec
            maxMps = maxOf(maxMps, reported, derived)
        }
        return maxMps * 3.6
    }

    /**
     * Elevation gain/loss over the track using the same 0.6 m deadband as the live
     * engine, so a recovered workout matches what it would have shown live.
     */
    fun elevationProfile(points: List<GpsPoint>): Pair<Double, Double> {
        var gain = 0.0
        var loss = 0.0
        for (i in 1 until points.size) {
            val diff = points[i].altitude - points[i - 1].altitude
            if (diff > 0.6) gain += diff
            else if (diff < -0.6) loss += abs(diff)
        }
        return gain to loss
    }

    /**
     * Fastest sustained pace over a real segment of the track: the best rolling
     * window of [targetMeters] metres (1 km for normal workouts, shorter tracks
     * fall back to their longest usable window). Returns null when the track is
     * too short or has no timestamps to measure against.
     */
    fun bestPaceSecPerKm(points: List<GpsPoint>, totalDistanceMeters: Double, targetMeters: Double = 1000.0): Double? {
        if (points.size < 2 || totalDistanceMeters <= 0.0) return null

        val prefix = DoubleArray(points.size)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val cur = points[i]
            prefix[i] = prefix[i - 1] + MilesRepository.calculateDistanceMeters(prev.latitude, prev.longitude, cur.latitude, cur.longitude)
        }

        // Short tracks still deserve a real best pace: measure their best window
        // instead of giving up, so a 700 m effort is compared over 700 m.
        val target = if (totalDistanceMeters >= targetMeters) targetMeters else totalDistanceMeters * 0.8
        if (target <= 0.0) return null

        var best: Double? = null
        var start = 0
        for (end in 1 until points.size) {
            while (start < end - 1 && prefix[end] - prefix[start + 1] >= target) start++
            val windowDist = prefix[end] - prefix[start]
            if (windowDist < target) continue
            val windowMillis = points[end].timestamp - points[start].timestamp
            if (windowMillis <= 0L) continue
            val pace = (windowMillis / 1000.0) / (windowDist / 1000.0)
            if (best == null || pace < best) best = pace
        }
        return best
    }
}
