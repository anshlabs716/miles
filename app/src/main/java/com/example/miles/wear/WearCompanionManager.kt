package com.example.miles.wear

import android.bluetooth.BluetoothManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

enum class WearConnectionStatus(val label: String) {
    CONNECTED("MILES Watch App Connected"),
    PAIRED("Watch Paired"),
    CONNECTING("Checking paired watches..."),
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
    val milesWearVersion: String = "not installed",
    val batteryPercent: Int = -1,
    val isCharging: Boolean = false,
    val supportedCapabilities: List<String> = listOf(
        "ANDROID_BOND_DETECTION",
        "PHONE_SIDE_SYNC_READY",
        "HEART_RATE_STREAM",
        "BAROMETER_ALTITUDE",
        "CADENCE_SENSOR",
        "REMOTE_CONTROL",
        "OFFLINE_SYNC"
    )
)

data class WatchSettings(
    val hapticMilestoneAlerts: Boolean = true,
    val wristFlickToPause: Boolean = false,
    val alwaysOnScreenWorkout: Boolean = true,
    val primaryMetric: String = "PACE",
    val secondaryMetric: String = "DISTANCE",
    val complicationSlot1: String = "DAILY_STEPS",
    val complicationSlot2: String = "WEEKLY_DISTANCE",
    val tileQuickStartActivity: String = "RUNNING",
    val autoSyncOverBluetooth: Boolean = true
)

data class WearLogMessage(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val direction: String,
    val topic: String,
    val payload: String
)

/**
 * Phone-side Wear integration. This class deliberately does not pretend that a
 * watch-side MILES APK exists. Android pairing is detected for real; app-level
 * sync becomes available once the future Wear companion is installed.
 */
class WearCompanionManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

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
        val adapter = bluetoothManager?.adapter
        val bondedWear = runCatching {
            adapter?.bondedDevices?.firstOrNull { device ->
                val name = device.name?.lowercase().orEmpty()
                name.contains("watch") || name.contains("wear") || name.contains("pixel watch") || name.contains("galaxy watch")
            }
        }.getOrNull()

        if (bondedWear == null) {
            _deviceProfile.value = null
            _connectionStatus.value = WearConnectionStatus.DISCONNECTED
            logMessage("SYS", "PAIR_SCAN", "No paired Wear OS-style watch found. Pair the watch in Android Bluetooth settings first.")
            return
        }

        val profile = WearDeviceProfile(
            deviceId = bondedWear.address ?: "unknown",
            deviceName = bondedWear.name ?: "Paired smartwatch",
            model = "Android paired wearable",
            milesWearVersion = "not installed"
        )
        _deviceProfile.value = profile
        _connectionStatus.value = WearConnectionStatus.PAIRED
        logMessage("SYS", "PAIR_FOUND", JSONObject().apply {
            put("name", profile.deviceName)
            put("addressKnown", profile.deviceId != "unknown")
            put("milesAppInstalled", false)
            put("syncReady", true)
        }.toString())
    }

    fun disconnect() {
        _connectionStatus.value = WearConnectionStatus.DISCONNECTED
        _deviceProfile.value = null
        logMessage("SYS", "PAIR_CLEAR", "Phone-side watch selection cleared; Android Bluetooth pairing is unchanged.")
    }

    fun togglePairing() {
        if (_deviceProfile.value != null) disconnect() else scanAndConnect()
    }

    fun pingWatch() {
        if (_connectionStatus.value == WearConnectionStatus.CONNECTED) {
            logMessage("TX -> WATCH", "PING", "{\"timestamp\":${System.currentTimeMillis()}}")
        } else {
            logMessage("SYS", "PING_SKIPPED", "No MILES watch-side connection exists yet.")
        }
    }

    fun triggerWatchSync() {
        if (_connectionStatus.value == WearConnectionStatus.CONNECTED) {
            logMessage("TX -> WATCH", "SYNC_REQ", "{\"full\":true}")
        } else {
            logMessage("SYS", "SYNC_QUEUED", "Watch is paired, but a MILES Wear APK is required for app-level sync.")
        }
    }

    fun sendLiveWorkoutUpdate(durationSec: Long, distanceM: Double, calories: Double, hr: Int) {
        if (_connectionStatus.value == WearConnectionStatus.CONNECTED) {
            logMessage("TX -> WATCH", "LIVE_METRICS", "{\"sec\":$durationSec,\"m\":$distanceM,\"cal\":$calories,\"hr\":$hr}")
        }
    }

    fun updateWatchSettings(transform: (WatchSettings) -> WatchSettings) {
        val updated = transform(_watchSettings.value)
        _watchSettings.value = updated
        val json = JSONObject().apply {
            put("haptics", updated.hapticMilestoneAlerts)
            put("wrist_flick", updated.wristFlickToPause)
            put("always_on", updated.alwaysOnScreenWorkout)
            put("primary_metric", updated.primaryMetric)
            put("secondary_metric", updated.secondaryMetric)
            put("complication_1", updated.complicationSlot1)
            put("complication_2", updated.complicationSlot2)
            put("tile_sport", updated.tileQuickStartActivity)
            put("auto_sync", updated.autoSyncOverBluetooth)
        }
        if (_connectionStatus.value == WearConnectionStatus.CONNECTED) logMessage("TX -> WATCH", "SETTING_SYNC", json.toString())
    }

    fun sendHapticPulse() {
        if (_connectionStatus.value == WearConnectionStatus.CONNECTED) logMessage("TX -> WATCH", "HAPTIC_COMMAND", "{\"pattern\":\"DOUBLE_STRONG\"}")
    }

    private fun logMessage(direction: String, topic: String, payload: String) {
        _communicationLogs.value = (_communicationLogs.value + WearLogMessage(direction = direction, topic = topic, payload = payload)).takeLast(40)
    }
}
