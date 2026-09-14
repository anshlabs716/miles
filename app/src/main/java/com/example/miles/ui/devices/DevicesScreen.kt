package com.example.miles.ui.devices

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.miles.engine.ConnectedSource
import com.example.miles.engine.DeviceManager
import com.example.miles.engine.DeviceSourceType
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel
import com.example.miles.wear.WearCompanionManager
import com.example.miles.wear.WearConnectionStatus

@Composable
fun DevicesScreen(
    deviceManager: DeviceManager,
    wearCompanion: WearCompanionManager
) {
    val context = LocalContext.current
    val sources by deviceManager.sources.collectAsState()
    val isScanning by deviceManager.isScanningBle.collectAsState()
    val wearStatus by wearCompanion.connectionStatus.collectAsState()
    val wearProfile by wearCompanion.deviceProfile.collectAsState()
    val watchSettings by wearCompanion.watchSettings.collectAsState()
    val wearLogs by wearCompanion.communicationLogs.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Sensors & Hardware, 1: Wear OS Companion

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Hardware & Devices",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage external GNSS, Bluetooth HR, and future Wear OS pairing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sensors & Sources", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Wear OS Companion", fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (selectedTab == 0) {
            // SENSORS & HARDWARE TAB
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Sources (${sources.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FilledTonalButton(
                        onClick = { deviceManager.startBleScan() },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanning...")
                        } else {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan BLE")
                        }
                    }
                }
            }

            items(sources) { src ->
                SensorSourceItem(
                    source = src,
                    onToggle = { deviceManager.toggleSourceConnection(src.id) }
                )
            }

            item {
                LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Smart Fallback System",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "If an External GNSS or Bluetooth sensor loses connection, MILES automatically falls back to built-in phone sensors instantly without dropping a single track point.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // WEAR OS COMPANION TAB
            item {
                // Wear OS Connection Hero
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Watch, contentDescription = "Watch", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = wearProfile?.deviceName ?: "No Watch Paired",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Status: ${wearStatus.label} • Battery: ${wearProfile?.batteryPercent ?: 0}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (wearStatus == WearConnectionStatus.CONNECTED) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { wearCompanion.togglePairing() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (wearStatus == WearConnectionStatus.CONNECTED) Color(0xFFFF2D55) else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(if (wearStatus == WearConnectionStatus.CONNECTED) "Unpair" else "Pair")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Protocol Version: ${wearProfile?.protocolVersion} • Standalone Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Actions: Ping & Sync
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            wearCompanion.pingWatch()
                            Toast.makeText(context, "Ping sent to watch!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ping Watch")
                    }

                    FilledTonalButton(
                        onClick = {
                            wearCompanion.triggerWatchSync()
                            Toast.makeText(context, "Triggered offline data sync!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Request Sync")
                    }
                }
            }

            // Watch Settings Toggles
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "WATCH BEHAVIOR & SETTINGS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SettingToggleRow(
                            title = "Haptic Milestone Alerts",
                            subtitle = "Vibrate wrist every kilometer split or milestone",
                            checked = watchSettings.hapticMilestoneAlerts,
                            onCheckedChange = { checked ->
                                wearCompanion.updateWatchSettings { it.copy(hapticMilestoneAlerts = checked) }
                            }
                        )

                        SettingToggleRow(
                            title = "Wrist Flick to Pause",
                            subtitle = "Quick wrist rotation pauses/resumes recording",
                            checked = watchSettings.wristFlickToPause,
                            onCheckedChange = { checked ->
                                wearCompanion.updateWatchSettings { it.copy(wristFlickToPause = checked) }
                            }
                        )

                        SettingToggleRow(
                            title = "Always-On Workout Screen",
                            subtitle = "Keep metrics visible during workout on watch face",
                            checked = watchSettings.alwaysOnScreenWorkout,
                            onCheckedChange = { checked ->
                                wearCompanion.updateWatchSettings { it.copy(alwaysOnScreenWorkout = checked) }
                            }
                        )
                    }
                }
            }

            // Complications & Tiles Configuration
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "COMPLICATIONS & TILES CONFIGURATION",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Complication Slot 1: ${watchSettings.complicationSlot1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Complication Slot 2: ${watchSettings.complicationSlot2}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Quick Start Tile Activity: ${watchSettings.tileQuickStartActivity}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Wear Protocol Communication Log
            item {
                LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "WEAR OS PROTOCOL PACKET LOG",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            wearLogs.takeLast(5).forEach { log ->
                                Text(
                                    text = "[${log.direction}] ${log.topic}: ${log.payload}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SensorSourceItem(
    source: ConnectedSource,
    onToggle: () -> Unit
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                val icon = when (source.type) {
                    DeviceSourceType.PHONE_GPS -> Icons.Default.GpsFixed
                    DeviceSourceType.EXTERNAL_GNSS -> Icons.Default.Sensors
                    DeviceSourceType.PHONE_STEP_COUNTER -> Icons.Default.Smartphone
                    DeviceSourceType.BLE_HEART_RATE -> Icons.Default.Favorite
                    DeviceSourceType.WEAR_OS_SENSOR -> Icons.Default.Watch
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (source.isConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = source.name,
                        tint = if (source.isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${source.details} • Batt: ${source.batteryLevel}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = source.isConnected,
                onCheckedChange = { onToggle() }
            )
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
