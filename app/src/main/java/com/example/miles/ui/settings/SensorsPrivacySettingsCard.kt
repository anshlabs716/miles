package com.example.miles.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.local.UserPreferences
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.PrivacyZoneEntity
import com.example.miles.data.repository.format
import com.example.miles.ui.theme.LiquidGlassCard

@Composable
fun SensorsSettingsCard(
    preferences: MilesPreferences,
    userPrefs: UserPreferences,
    onOpenDevices: () -> Unit,
    vibrator: Vibrator?
) {
    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var isIgnoringBatteryOpt by remember {
        mutableStateOf(powerManager != null && powerManager.isIgnoringBatteryOptimizations(context.packageName))
    }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "HARDWARE SENSORS & TUNNELING",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // GPS Route Positioning Hardware Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GPS Route Positioning", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = if (userPrefs.gpsSensorEnabled) "High-accuracy GNSS & Fused Location active" else "GPS disabled (indoor treadmill mode)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = userPrefs.gpsSensorEnabled,
                    onCheckedChange = { preferences.setSensorHardwareControls(gpsEnabled = it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Motion Step Sensor Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hardware Step Counter Sensor", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = if (userPrefs.stepSensorHardwareEnabled) "Motion coprocessor step tracking active" else "Hardware step tracking paused",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = userPrefs.stepSensorHardwareEnabled,
                    onCheckedChange = { preferences.setSensorHardwareControls(stepHwEnabled = it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bluetooth Sensor Tunneling Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bluetooth Sensor Tunneling", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = if (userPrefs.bluetoothTunnelingEnabled) "Tunneling BLE telemetry into active workout feed" else "Bluetooth sensor tunneling disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = userPrefs.bluetoothTunnelingEnabled,
                    onCheckedChange = { preferences.setSensorHardwareControls(btTunnelEnabled = it) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Heart Rate BLE Sensor Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Heart Rate Sensor Stream", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = if (userPrefs.heartRateSensorEnabled) "Optical / chest strap BLE monitoring active" else "Heart rate stream off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = userPrefs.heartRateSensorEnabled,
                    onCheckedChange = { preferences.setSensorHardwareControls(hrEnabled = it) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Refresh Rate (How fast it refreshes)
            Text(
                text = "Sensor Sampling & Refresh Rate",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Controls how fast GPS and telemetry refresh during activity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            val refreshOptions = listOf(
                250L to "250ms (Ultra)",
                500L to "500ms (High)",
                1000L to "1s (Normal)",
                2000L to "2s (Eco)",
                5000L to "5s (Saver)"
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(refreshOptions) { (rateMs, label) ->
                    val isSelected = userPrefs.sensorRefreshRateMs == rateMs
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                preferences.setSensorHardwareControls(refreshRateMs = rateMs)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Battery Optimization Exemption Status Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isIgnoringBatteryOpt) Color(0xFF00E676).copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isIgnoringBatteryOpt) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = if (isIgnoringBatteryOpt) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Unrestricted Battery",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isIgnoringBatteryOpt) "Active • Tracking won't be killed" else "Tap to request exemption upfront",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!isIgnoringBatteryOpt) {
                    Button(
                        onClick = {
                            runCatching {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }.onFailure {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Allow", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // BLE Peripherals & Wear Companion
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bluetooth LE & Wear OS Watch", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = "Pair chest HR monitors, Stryd footpods & sync with Wear OS tile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(onClick = onOpenDevices) {
                    Icon(Icons.Default.Watch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Devices", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Custom Notification & Move Reminder Configuration
            var customReminderText by remember(userPrefs.moveReminderCustomText) {
                mutableStateOf(userPrefs.moveReminderCustomText)
            }
            var reminderEditing by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Custom Movement Notification", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = if (userPrefs.moveReminderEnabled) "Active every ${userPrefs.moveReminderIntervalMinutes}m • Custom prompt"
                               else "Sedentary alert notification is off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = userPrefs.moveReminderEnabled,
                    onCheckedChange = { isEnabled ->
                        preferences.setMoveReminderSettings(
                            enabled = isEnabled,
                            intervalMinutes = userPrefs.moveReminderIntervalMinutes,
                            customMessage = customReminderText
                        )
                    }
                )
            }

            if (userPrefs.moveReminderEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Notification Message",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = customReminderText,
                    onValueChange = {
                        customReminderText = it
                        reminderEditing = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Custom Notification Text") },
                    placeholder = { Text("e.g. Time to stretch and conquer today's miles!") },
                    trailingIcon = {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interval: ${userPrefs.moveReminderIntervalMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                customReminderText = "Time to stretch and get moving! Take 250 steps."
                                preferences.setMoveReminderSettings(
                                    enabled = true,
                                    intervalMinutes = userPrefs.moveReminderIntervalMinutes,
                                    customMessage = customReminderText
                                )
                                reminderEditing = false
                                Toast.makeText(context, "Notification message reset to default", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Reset", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                val savedText = customReminderText.trim().ifEmpty { "Time to stretch and get moving! Take 250 steps." }
                                preferences.setMoveReminderSettings(
                                    enabled = true,
                                    intervalMinutes = userPrefs.moveReminderIntervalMinutes,
                                    customMessage = savedText
                                )
                                reminderEditing = false
                                Toast.makeText(context, "Custom notification message saved!", Toast.LENGTH_SHORT).show()
                            },
                            enabled = reminderEditing
                        ) {
                            Text("Save", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Haptic Feedback Test
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tactile Haptics Verification", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = "Tests vibration feedback for auto-pause, splits and HR alarms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { triggerHaptic(vibrator) }
                ) {
                    Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Buzz", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun PrivacySettingsCard(
    privacyZones: List<PrivacyZoneEntity>,
    trashCount: Int,
    onAddZone: () -> Unit,
    onDeleteZone: (String) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenTrash: () -> Unit,
    onWipeData: () -> Unit,
    onResetDefaults: () -> Unit
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "PRIVACY, DATA STORAGE & EXPORTS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Privacy Zones Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("GPS Privacy Zones", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = "Excludes points within radius around home/office from exports",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onAddZone) {
                    Icon(Icons.Default.Add, contentDescription = "Add Zone", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (privacyZones.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "No privacy zones configured. Tap + to add a privacy perimeter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    privacyZones.forEach { zone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(zone.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = "Radius: ${zone.radiusMeters.format(0)}m (${zone.latitude.format(4)}, ${zone.longitude.format(4)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDeleteZone(zone.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Backups & Trash Bin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenBackup,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SQLite Backup", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onOpenTrash,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trash Bin ($trashCount)", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reset & Wipe
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Defaults", fontSize = 11.sp)
                }

                Button(
                    onClick = onWipeData,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wipe All Data", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun StudioShortcutCard(onOpenStudio: () -> Unit) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MILES STUDIO & DEV LABS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Comprehensive nerdy diagnostics, Kalman tuning & serial telemetry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Includes live WGS-84 Vincenty geodesy solvers, Daniels & Gilbert VDOT calculators, real-time NMEA 0183 serial terminal stream, Kalman covariance tuning, raw SQLite inspection & high-precision tick chronometers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onOpenStudio,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open MILES Studio", fontWeight = FontWeight.Bold)
            }
        }
    }
}
