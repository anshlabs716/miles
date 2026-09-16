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
import androidx.compose.material.icons.filled.Map
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
fun MapsSettingsCard(
    userPrefs: UserPreferences,
    preferences: MilesPreferences
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "MAPS, GPS & SATELLITE TILES",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Map Layer selection
            Text("Default Map Renderer", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "OPEN_STREET_MAP" to "OSM Standard",
                    "DARK_VECTOR" to "AMOLED Dark",
                    "TOPO_OUTDOOR" to "Topographic",
                    "SATELLITE" to "Satellite"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = userPrefs.defaultMapLayer == mode,
                        onClick = { preferences.updatePreferences { it.copy(defaultMapLayer = mode) } },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Map Heading Rotation
            Text("Map Orientation Mode", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("NORTH_UP" to "Fixed North Up", "HEADING_UP" to "Course Heading Up").forEach { (mode, label) ->
                    FilterChip(
                        selected = userPrefs.mapHeadingRotationMode == mode,
                        onClick = { preferences.updatePreferences { it.copy(mapHeadingRotationMode = mode) } },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Polyline styling
            Text("GPS Breadcrumb Color Style", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("SPEED_GRADIENT" to "Speed Heatmap", "HR_GRADIENT" to "HR Zones", "SOLID_CYAN" to "Cyan Glow").forEach { (mode, label) ->
                    FilterChip(
                        selected = userPrefs.mapPolylineColorMode == mode,
                        onClick = { preferences.updatePreferences { it.copy(mapPolylineColorMode = mode) } },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SettingRow(
                title = "Auto-Center & Follow Runner",
                subtitle = "Smoothly centers camera on current GPS location",
                checked = userPrefs.mapAutoFollow,
                onCheckedChange = { preferences.updatePreferences { p -> p.copy(mapAutoFollow = it) } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingRow(
                title = "Keep Screen Awakened During HUD Map",
                subtitle = "Prevents display timeout while workout map is active",
                checked = userPrefs.mapKeepScreenOn,
                onCheckedChange = { preferences.updatePreferences { p -> p.copy(mapKeepScreenOn = it) } }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "GPS Outlier Noise Filter: < ±${userPrefs.minGpsAccuracyFilterMeters.format(0)}m accuracy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = userPrefs.minGpsAccuracyFilterMeters,
                onValueChange = { preferences.updatePreferences { p -> p.copy(minGpsAccuracyFilterMeters = it) } },
                valueRange = 5f..50f,
                steps = 8
            )
        }
    }
}
