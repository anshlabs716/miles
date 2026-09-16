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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.ColorVisionMode
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.local.UserPreferences
import com.example.miles.ui.theme.LiquidGlassCard

@Composable
fun AccessibilitySettingsCard(
    userPrefs: UserPreferences,
    preferences: MilesPreferences
) {
    val acc = userPrefs.accessibility

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "ACCESSIBILITY & VISION SUITE",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            SettingRow(
                title = "Larger Dynamic Display Typography",
                subtitle = "Scales metrics, paces, and labels by +18% across all cards",
                checked = acc.largerText,
                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(largerText = it) } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingRow(
                title = "Expanded Touch Targets (48dp+ Minimum)",
                subtitle = "Enlarges interactive controls for running gloves and high cadence",
                checked = acc.largeTouchTargets,
                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(largeTouchTargets = it) } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingRow(
                title = "Bold High-Contrast Weights",
                subtitle = "Applies heavier font weights for rapid outdoor glances",
                checked = acc.boldText,
                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(boldText = it) } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingRow(
                title = "High-Luminance Contrast Edges",
                subtitle = "Enhances borders, cards, and text against dark backgrounds",
                checked = acc.highContrast,
                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(highContrast = it) } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingRow(
                title = "Reduced Motion & Kinetic Dampening",
                subtitle = "Replaces spring transitions with instant fades",
                checked = acc.reducedMotion,
                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(reducedMotion = it) } }
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingRow(
                title = "Disable UI Animations Completely",
                subtitle = "Zero layout animations for maximum power savings",
                checked = acc.disableAnimations,
                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(disableAnimations = it) } }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Color Vision Mode
            Text("Color Vision Deficiency Palette", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(
                text = acc.colorVisionMode.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorVisionMode.entries.chunked(2).forEach { rowModes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowModes.forEach { mode ->
                            FilterChip(
                                selected = acc.colorVisionMode == mode,
                                onClick = { preferences.updateAccessibility { a -> a.copy(colorVisionMode = mode) } },
                                label = { Text(mode.label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
