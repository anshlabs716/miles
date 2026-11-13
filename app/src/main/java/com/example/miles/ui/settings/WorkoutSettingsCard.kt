package com.example.miles.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.DistanceUnit
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.local.UserPreferences
import com.example.miles.data.repository.format
import com.example.miles.engine.WorkoutTargetAlertManager
import com.example.miles.ui.theme.LiquidGlassCard
import kotlin.math.roundToInt

@Composable
fun WorkoutSettingsCard(
    userPrefs: UserPreferences,
    preferences: MilesPreferences
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "WORKOUT HUD & RUNTIME ENGINE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Distance Unit
            Text("Distance Unit", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DistanceUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = userPrefs.unit == unit,
                        onClick = { preferences.setUnit(unit) },
                        label = { Text(unit.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-pause toggle & sensitivity
            SettingRow(
                title = "Smart Auto-Pause",
                subtitle = "Halts chronometer when speed drops below threshold",
                checked = userPrefs.autoPause,
                onCheckedChange = { preferences.setAutoPause(it) }
            )

            if (userPrefs.autoPause) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Auto-Pause Speed Threshold: ${userPrefs.autoPauseSensitivityKmh.format(1)} km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = userPrefs.autoPauseSensitivityKmh,
                    onValueChange = { preferences.updatePreferences { p -> p.copy(autoPauseSensitivityKmh = it) } },
                    valueRange = 0.5f..4.0f,
                    steps = 6
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pre-workout Countdown
            Text("Pre-Workout Countdown", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 3, 5, 10).forEach { seconds ->
                    FilterChip(
                        selected = userPrefs.countdownSeconds == seconds,
                        onClick = { preferences.updatePreferences { it.copy(countdownSeconds = seconds) } },
                        label = { Text(if (seconds == 0) "Immediate" else "${seconds}s") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cadence Metronome
            SettingRow(
                title = "Audible Cadence Metronome",
                subtitle = "Rhythmic audio ticks to train stride cadence",
                checked = userPrefs.cadenceMetronomeEnabled,
                onCheckedChange = { preferences.updatePreferences { p -> p.copy(cadenceMetronomeEnabled = it) } }
            )

            if (userPrefs.cadenceMetronomeEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Target Stride Rate: ${userPrefs.targetCadenceSpm} SPM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = userPrefs.targetCadenceSpm.toFloat(),
                    onValueChange = { preferences.updatePreferences { p -> p.copy(targetCadenceSpm = it.toInt()) } },
                    valueRange = 140f..200f,
                    steps = 11
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TargetZoneAlertSettings(userPrefs = userPrefs, preferences = preferences)

            Spacer(modifier = Modifier.height(12.dp))

            // Live Activities, Smart DND & Battery Saver
            SettingRow(
                title = "Live Activity Ongoing Notification",
                subtitle = "Persistent lockscreen telemetry and workout quick actions",
                checked = userPrefs.liveActivities,
                onCheckedChange = { preferences.updatePreferences { p -> p.copy(liveActivities = it) } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingRow(
                title = "Smart Do Not Disturb (DND)",
                subtitle = "Automatically suppress non-urgent calls during workouts",
                checked = userPrefs.smartDnd,
                onCheckedChange = { preferences.updatePreferences { p -> p.copy(smartDnd = it) } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingRow(
                title = "Battery Saver Low-Power GPS Sampling",
                subtitle = "Throttles background sensor queries during ultra-distance runs",
                checked = userPrefs.batterySaverTracking,
                onCheckedChange = { preferences.updatePreferences { p -> p.copy(batterySaverTracking = it) } }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // High Precision Tick Interval
            Text("HUD Chronometer Frequency", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50L to "50ms (20fps)", 100L to "100ms (10fps)", 250L to "250ms", 500L to "500ms").forEach { (ms, label) ->
                    FilterChip(
                        selected = userPrefs.counterIntervalMs == ms,
                        onClick = { preferences.updatePreferences { it.copy(counterIntervalMs = ms) } },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

/**
 * Live target-zone alerts evaluated during a workout by WorkoutTargetAlertManager:
 * heart-rate zone, pace window around the HUD's target pace, and a speed window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetZoneAlertSettings(
    userPrefs: UserPreferences,
    preferences: MilesPreferences
) {
    Text(
        text = "Target Zone Alerts",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    )
    Text(
        text = "Live cues during a workout when you drift outside your target zone " +
            "(at most one every ${WorkoutTargetAlertManager.ALERT_COOLDOWN_MS / 1000}s)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Heart rate zone
    SettingRow(
        title = "Heart Rate Zone Alert",
        subtitle = "Alert when heart rate leaves the zone below",
        checked = userPrefs.hrAlertEnabled,
        onCheckedChange = { preferences.updatePreferences { p -> p.copy(hrAlertEnabled = it) } }
    )
    if (userPrefs.hrAlertEnabled) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Zone: ${userPrefs.hrAlertMinBpm}–${userPrefs.hrAlertMaxBpm} BPM",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RangeSlider(
            value = userPrefs.hrAlertMinBpm.toFloat()..userPrefs.hrAlertMaxBpm.toFloat(),
            onValueChange = { range ->
                preferences.updatePreferences { p ->
                    p.copy(hrAlertMinBpm = range.start.toInt(), hrAlertMaxBpm = range.endInclusive.toInt())
                }
            },
            valueRange = 80f..210f,
            steps = 25
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Pace window around the HUD target pace
    SettingRow(
        title = "Pace Alert",
        subtitle = "Alert when pace drifts from the target pace set in the workout HUD",
        checked = userPrefs.paceAlertEnabled,
        onCheckedChange = { preferences.updatePreferences { p -> p.copy(paceAlertEnabled = it) } }
    )
    if (userPrefs.paceAlertEnabled) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tolerance: ±${userPrefs.paceAlertToleranceSec}s per km",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = userPrefs.paceAlertToleranceSec.toFloat(),
            onValueChange = { preferences.updatePreferences { p -> p.copy(paceAlertToleranceSec = it.toInt()) } },
            valueRange = 5f..60f,
            steps = 10
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Speed window
    SettingRow(
        title = "Speed Alert",
        subtitle = "Alert when speed leaves the range below (useful for cycling)",
        checked = userPrefs.speedAlertEnabled,
        onCheckedChange = { preferences.updatePreferences { p -> p.copy(speedAlertEnabled = it) } }
    )
    if (userPrefs.speedAlertEnabled) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Range: ${userPrefs.speedAlertMinKmh.format(0)}–${userPrefs.speedAlertMaxKmh.format(0)} km/h",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RangeSlider(
            value = userPrefs.speedAlertMinKmh..userPrefs.speedAlertMaxKmh,
            onValueChange = { range ->
                preferences.updatePreferences { p ->
                    p.copy(
                        speedAlertMinKmh = range.start.roundToInt().toFloat(),
                        speedAlertMaxKmh = range.endInclusive.roundToInt().toFloat()
                    )
                }
            },
            valueRange = 2f..60f,
            steps = 57
        )
    }

    // How alerts are delivered (only relevant when at least one alert is on)
    if (userPrefs.hrAlertEnabled || userPrefs.paceAlertEnabled || userPrefs.speedAlertEnabled) {
        Spacer(modifier = Modifier.height(10.dp))
        SettingRow(
            title = "Vibrate on Alert",
            checked = userPrefs.targetAlertHaptic,
            onCheckedChange = { preferences.updatePreferences { p -> p.copy(targetAlertHaptic = it) } }
        )
        Spacer(modifier = Modifier.height(6.dp))
        SettingRow(
            title = "Speak Alert",
            checked = userPrefs.targetAlertVoice,
            onCheckedChange = { preferences.updatePreferences { p -> p.copy(targetAlertVoice = it) } }
        )
    }
}
