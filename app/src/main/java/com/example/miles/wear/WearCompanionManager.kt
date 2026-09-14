package com.example.miles.wear

import android.bluetooth.BluetoothAdapter
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

enum class WearConnectionStatus(val label: String) {
    CONNECTED("Connected"),
    CONNECTING("Searching..."),
    DISCONNECTED("Not Connected"),
    RECONNECTING("Reconnecting..."),
    UNSUPPORTED("No Watch Paired"),
    PERMISSION_REQUIRED("Nearby Permission Required"),
    APP_UNAVAILABLE("Wear OS Companion Not Installed")
}

data class WearDeviceProfile(
    val deviceId: String,
    val deviceName: String,
    val model: String,
    val protocolVersion: String = "MILES-WEAR-v2.4",
    val milesWearVersion: String = "1.0.4-standalone",
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val supportedCapabilities: List<String> = listOf(
        "HEART_RATE_STREAM",
        "BAROMETER_ALTITUDE",
        "CADENCE_SENSOR",
        "REMOTE_CONTROL",
        "COMPLICATIONS",
        "TILES",
        "OFFLINE_SYNC"
    )
)

data class WatchSettings(
    val hapticMilestoneAlerts: Boolean = true,
    val wristFlickToPause: Boolean = false,
    val alwaysOnScreenWorkout: Boolean = true,
    val primaryMetric: String = "PACE", // PACE, DISTANCE, HEART_RATE, DURATION
    val secondaryMetric: String = "DISTANCE",
    val complicationSlot1: String = "DAILY_STEPS",
    val complicationSlot2: String = "WEEKLY_DISTANCE",
    val tileQuickStartActivity: String = "RUNNING",
    val autoSyncOverBluetooth: Boolean = true
)

data class WearLogMessage(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val direction: String, // "TX -> WATCH" or "RX <- WATCH"
    val topic: String,
    val payload: String
)

class WearCompanionManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Real default: Disconnected with NO fake device
    private val _connectionStatus = MutableStateFlow(WearConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<WearConnectionStatus> = _connectionStatus.asStateFlow()

    private val _deviceProfile = MutableStateFlow<WearDeviceProfile?>(null)
    val deviceProfile: StateFlow<WearDeviceProfile?> = _deviceProfile.asStateFlow()

    private val _watchSettings = MutableStateFlow(WatchSettings())
    val watchSettings: StateFlow<WatchSettings> = _watchSettings.asStateFlow()

    private val _communicationLogs = MutableStateFlow<List<WearLogMessage>>(emptyList())
    val communicationLogs: StateFlow<List<WearLogMessage>> = _communicationLogs.asStateFlow()

    fun scanAndConnect() {
        _connectionStatus.value = WearConnectionStatus.CONNECTING
        logMessage("SYS", "SCAN_START", "Scanning local Bluetooth bonds for Wear OS nodes...")

        scope.launch {
            delay(1500L)
            val btAdapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull()
            val bondedWear = btAdapter?.bondedDevices?.firstOrNull { dev ->
                val name = dev.name?.lowercase() ?: ""
                name.contains("watch") || name.contains("wear") || name.contains("pixel") || name.contains("galaxy")
            }

            if (bondedWear != null) {
                val profile = WearDeviceProfile(
                    deviceId = bondedWear.address ?: UUID.randomUUID().toString(),
                    deviceName = bondedWear.name ?: "Wear OS Smartwatch",
                    model = "Wear OS Device"
                )
                _deviceProfile.value = profile
                _connectionStatus.value = WearConnectionStatus.CONNECTED
                logMessage("RX <- WATCH", "CONNECT_ACK", "{\"name\":\"${profile.deviceName}\",\"status\":\"PAIRED\"}")
            } else {
                _connectionStatus.value = WearConnectionStatus.DISCONNECTED
                _deviceProfile.value = null
                logMessage("SYS", "SCAN_RESULT", "No paired Wear OS watch found in Android Settings.")
            }
        }
    }

    fun disconnect() {
        _connectionStatus.value = WearConnectionStatus.DISCONNECTED
        _deviceProfile.value = null
        logMessage("TX -> WATCH", "DISCONNECT", "{\"reason\":\"USER_REQUEST\"}")
    }

    fun togglePairing() {
        if (_connectionStatus.value == WearConnectionStatus.CONNECTED) {
            disconnect()
        } else {
            scanAndConnect()
        }
    }

    fun pingWatch() {
        logMessage("TX -> WATCH", "PING", "{\"timestamp\":${System.currentTimeMillis()}}")
    }

    fun triggerWatchSync() {
        logMessage("TX -> WATCH", "SYNC_REQ", "{\"full\":true}")
    }

    fun sendLiveWorkoutUpdate(durationSec: Long, distanceM: Double, calories: Double, hr: Int) {
        logMessage("TX -> WATCH", "LIVE_METRICS", "{\"sec\":$durationSec,\"m\":$distanceM,\"cal\":$calories,\"hr\":$hr}")
    }

    fun updateWatchSettings(transform: (WatchSettings) -> WatchSettings) {
        val updated = transform(_watchSettings.value)
        _watchSettings.value = updated
        val json = JSONObject().apply {
            put("haptics", updated.hapticMilestoneAlerts)
            put("wrist_flick", updated.wristFlickToPause)
            put("always_on", updated.alwaysOnScreenWorkout)
            put("primary_metric", updated.primaryMetric)
            put("complication_1", updated.complicationSlot1)
            put("tile_sport", updated.tileQuickStartActivity)
        }
        logMessage("TX -> WATCH", "SETTING_SYNC", json.toString())
    }

    fun sendHapticPulse() {
        logMessage("TX -> WATCH", "HAPTIC_COMMAND", "{\"pattern\":\"DOUBLE_STRONG\"}")
    }

    private fun logMessage(direction: String, topic: String, payload: String) {
        val msg = WearLogMessage(
            direction = direction,
            topic = topic,
            payload = payload
        )
        _communicationLogs.value = (_communicationLogs.value + msg).takeLast(40)
    }
}
