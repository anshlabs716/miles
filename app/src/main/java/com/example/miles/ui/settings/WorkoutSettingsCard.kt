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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.miles.ui.theme.LiquidGlassCard

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

            Spacer(modifier = Modifier.height(12.dp))

            // Heart Rate Zone Alarm
            SettingRow(
                title = "Max HR Zone Alarm",
                subtitle = "Triggers urgent vibration & alert when exceeding BPM limit",
                checked = userPrefs.hrZoneAlarmEnabled,
                onCheckedChange = { preferences.updatePreferences { p -> p.copy(hrZoneAlarmEnabled = it) } }
            )

            if (userPrefs.hrZoneAlarmEnabled) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Alarm Trigger Threshold: ${userPrefs.hrZoneAlarmBpm} BPM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = userPrefs.hrZoneAlarmBpm.toFloat(),
                    onValueChange = { preferences.updatePreferences { p -> p.copy(hrZoneAlarmBpm = it.toInt()) } },
                    valueRange = 130f..210f,
                    steps = 15
                )
            }

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
