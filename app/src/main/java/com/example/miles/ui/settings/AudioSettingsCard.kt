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
import androidx.compose.material.icons.filled.VolumeUp
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
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.local.UserPreferences
import com.example.miles.data.repository.format
import com.example.miles.ui.theme.LiquidGlassCard

@Composable
fun AudioSettingsCard(
    userPrefs: UserPreferences,
    preferences: MilesPreferences
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "AUDIO COACH & VOICE ANNOUNCEMENTS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            SettingRow(
                title = "Voice Announcements (TTS Engine)",
                subtitle = "Synthesized vocal splits through headphones or speaker",
                checked = userPrefs.voiceAnnouncements,
                onCheckedChange = { preferences.setVoiceAnnouncements(it) }
            )

            if (userPrefs.voiceAnnouncements) {
                Spacer(modifier = Modifier.height(14.dp))

                Text("Split Announce Interval", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.5f to "Every 500m", 1.0f to "Every 1km", 2.0f to "Every 2km", 5.0f to "Every 5km").forEach { (km, label) ->
                        FilterChip(
                            selected = userPrefs.audioCueIntervalKm == km,
                            onClick = { preferences.updatePreferences { it.copy(audioCueIntervalKm = km) } },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Coach Audio Volume: ${userPrefs.audioCueVolumePercent}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = userPrefs.audioCueVolumePercent.toFloat(),
                    onValueChange = { preferences.updatePreferences { p -> p.copy(audioCueVolumePercent = it.toInt()) } },
                    valueRange = 10f..100f,
                    steps = 8
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Speech Rate: ${userPrefs.audioSpeechRate.format(1)}x",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = userPrefs.audioSpeechRate,
                    onValueChange = { preferences.updatePreferences { p -> p.copy(audioSpeechRate = it) } },
                    valueRange = 0.5f..2.0f,
                    steps = 6
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Spoken Metrics in Split Callout", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))

                SettingRow(
                    title = "Announce Average Pace",
                    subtitle = "e.g., 'Pace 4 minutes 35 seconds per kilometer'",
                    checked = userPrefs.audioAnnouncePace,
                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(audioAnnouncePace = it) } }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingRow(
                    title = "Announce Heart Rate & Zone",
                    subtitle = "e.g., 'Heart rate 158 beats per minute, Zone 3'",
                    checked = userPrefs.audioAnnounceHeartRate,
                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(audioAnnounceHeartRate = it) } }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingRow(
                    title = "Announce Split Distance",
                    subtitle = "e.g., 'Distance 5 kilometers completed'",
                    checked = userPrefs.audioAnnounceDistance,
                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(audioAnnounceDistance = it) } }
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingRow(
                    title = "Announce Cadence & Stride Rate",
                    subtitle = "e.g., 'Average cadence 172 steps per minute'",
                    checked = userPrefs.audioAnnounceCadence,
                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(audioAnnounceCadence = it) } }
                )
            }
        }
    }
}
