package com.example.miles.engine

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.Locale

data class TargetZoneConfig(
    val paceAlertEnabled: Boolean = true,
    val targetPaceSecPerKm: Double? = 330.0, // e.g. 5:30 min/km
    val paceToleranceSec: Double = 25.0,     // ±25s tolerance
    val speedAlertEnabled: Boolean = false,
    val minSpeedKmh: Double? = 8.0,
    val maxSpeedKmh: Double? = 16.0,
    val hrAlertEnabled: Boolean = true,
    val minHeartRateBpm: Int? = 120,
    val maxHeartRateBpm: Int? = 175,
    val hapticEnabled: Boolean = true,
    val voiceEnabled: Boolean = true
)

sealed class WorkoutAlert {
    data class PaceAlert(val message: String, val isTooSlow: Boolean) : WorkoutAlert()
    data class SpeedAlert(val message: String, val isTooSlow: Boolean) : WorkoutAlert()
    data class HeartRateAlert(val message: String, val isTooHigh: Boolean, val bpm: Int) : WorkoutAlert()
}

class WorkoutTargetAlertManager(
    private val context: Context,
    private val ttsManager: NavigationTtsManager? = null
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var config: TargetZoneConfig = TargetZoneConfig()
    private var lastAlertTimeMs: Long = 0L
    private val alertCooldownMs = 15_000L // Don't annoy user more than once every 15s

    fun evaluateMetrics(
        paceSecPerKm: Double,
        speedKmh: Double,
        heartRateBpm: Int?
    ): WorkoutAlert? {
        val now = System.currentTimeMillis()
        if (now - lastAlertTimeMs < alertCooldownMs) return null

        // 1. Evaluate Heart Rate
        if (config.hrAlertEnabled && heartRateBpm != null && heartRateBpm > 40) {
            val maxHr = config.maxHeartRateBpm ?: 180
            val minHr = config.minHeartRateBpm ?: 110
            if (heartRateBpm > maxHr) {
                lastAlertTimeMs = now
                val alert = WorkoutAlert.HeartRateAlert(
                    message = "Heart rate elevated: $heartRateBpm bpm (Limit: $maxHr)",
                    isTooHigh = true,
                    bpm = heartRateBpm
                )
                triggerAlert(alert.message, isWarning = true)
                return alert
            } else if (heartRateBpm < minHr) {
                lastAlertTimeMs = now
                val alert = WorkoutAlert.HeartRateAlert(
                    message = "Heart rate below target zone: $heartRateBpm bpm",
                    isTooHigh = false,
                    bpm = heartRateBpm
                )
                triggerAlert(alert.message, isWarning = false)
                return alert
            }
        }

        // 2. Evaluate Pace (only when moving at least 3 km/h to prevent zero-speed false alarms)
        if (config.paceAlertEnabled && config.targetPaceSecPerKm != null && speedKmh > 3.0) {
            val target = config.targetPaceSecPerKm!!
            val tolerance = config.paceToleranceSec
            if (paceSecPerKm > (target + tolerance)) {
                // Slower than acceptable range
                lastAlertTimeMs = now
                val diffSec = (paceSecPerKm - target).toInt()
                val alert = WorkoutAlert.PaceAlert(
                    message = "Pace is slow (+${diffSec}s vs target). Pick up the tempo!",
                    isTooSlow = true
                )
                triggerAlert(alert.message, isWarning = false)
                return alert
            } else if (paceSecPerKm < (target - tolerance) && paceSecPerKm > 100.0) {
                // Faster than acceptable range
                lastAlertTimeMs = now
                val diffSec = (target - paceSecPerKm).toInt()
                val alert = WorkoutAlert.PaceAlert(
                    message = "Pace is fast (-${diffSec}s vs target). Settle into target rhythm.",
                    isTooSlow = false
                )
                triggerAlert(alert.message, isWarning = false)
                return alert
            }
        }

        // 3. Evaluate Speed
        if (config.speedAlertEnabled && speedKmh > 1.0) {
            val minSpeed = config.minSpeedKmh ?: 5.0
            val maxSpeed = config.maxSpeedKmh ?: 25.0
            if (speedKmh < minSpeed) {
                lastAlertTimeMs = now
                val alert = WorkoutAlert.SpeedAlert(
                    message = "Speed below target: ${String.format(Locale.US, "%.1f", speedKmh)} km/h",
                    isTooSlow = true
                )
                triggerAlert(alert.message, isWarning = false)
                return alert
            } else if (speedKmh > maxSpeed) {
                lastAlertTimeMs = now
                val alert = WorkoutAlert.SpeedAlert(
                    message = "Speed above target limit: ${String.format(Locale.US, "%.1f", speedKmh)} km/h",
                    isTooSlow = false
                )
                triggerAlert(alert.message, isWarning = true)
                return alert
            }
        }

        return null
    }

    private fun triggerAlert(message: String, isWarning: Boolean) {
        if (config.hapticEnabled && vibrator != null && vibrator.hasVibrator()) {
            val pattern = if (isWarning) {
                longArrayOf(0, 300, 150, 300) // Double buzz for warning
            } else {
                longArrayOf(0, 200, 100, 200) // Short rhythm
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }

        if (config.voiceEnabled) {
            ttsManager?.announceWorkoutAlert(message)
        }
    }
}
