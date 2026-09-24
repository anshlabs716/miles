package com.example.miles.wear

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    val protocolVersion: String = "MILES-LAN-v1",
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
 * Phone-side Wear integration — **no Google Play Services**.
 *
 * The watch is found on the local network with a small UDP beacon and talked
 * to over TCP JSON frames ([LanTransport]), so live metrics, remote control,
 * watch HR/cadence streaming and the offline queue are all real. Android
 * Bluetooth pairing detection is still shown as a fallback signal.
 */
class WearCompanionManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionStatus = MutableStateFlow(WearConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<WearConnectionStatus> = _connectionStatus.asStateFlow()

    private val _deviceProfile = MutableStateFlow<WearDeviceProfile?>(null)
    val deviceProfile: StateFlow<WearDeviceProfile?> = _deviceProfile.asStateFlow()

    private val _watchSettings = MutableStateFlow(WatchSettings())
    val watchSettings: StateFlow<WatchSettings> = _watchSettings.asStateFlow()

    private val _offlineQueue = MutableStateFlow<List<JSONObject>>(emptyList())
    val offlineQueue: StateFlow<List<JSONObject>> = _offlineQueue.asStateFlow()

    private val _watchHeartRate = MutableStateFlow<Int?>(null)
    val watchHeartRate: StateFlow<Int?> = _watchHeartRate.asStateFlow()

    private val _watchCadence = MutableStateFlow<Int?>(null)
    val watchCadence: StateFlow<Int?> = _watchCadence.asStateFlow()

    private val _communicationLogs = MutableStateFlow<List<WearLogMessage>>(emptyList())
    val communicationLogs: StateFlow<List<WearLogMessage>> = _communicationLogs.asStateFlow()

    private var transport: LanTransport? = null
    private var lastWearPeerId: String? = null

    init {
        startLan()
    }

    // ---------------- local-network transport ----------------

    private fun startLan() {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val appVersion = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
        val lan = LanTransport(
            role = LanProtocol.ROLE_PHONE,
            selfId = "phone-$androidId",
            selfName = (Build.MANUFACTURER + " " + Build.MODEL).trim(),
            selfModel = Build.MODEL ?: "Android",
            appVersion = appVersion,
            scope = scope,
            batteryProvider = { currentBatteryPercent() },
            onMessage = { path, payload -> handleFromWatch(path, payload) },
            onPeersChanged = { peers -> updateFromPeers(peers) }
        )
        transport = lan
        lan.start()
    }

    private fun currentBatteryPercent(): Int = runCatching {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }.getOrDefault(-1)

    private fun updateFromPeers(peers: List<LanProtocol.Beacon>) {
        val watch = peers.firstOrNull { it.role == LanProtocol.ROLE_WEAR }
        if (watch != null) {
            val isNew = watch.id != lastWearPeerId
            lastWearPeerId = watch.id
            _deviceProfile.value = WearDeviceProfile(
                deviceId = watch.id,
                deviceName = watch.name,
                model = watch.model,
                milesWearVersion = watch.appVersion,
                batteryPercent = watch.battery
            )
            _connectionStatus.value = WearConnectionStatus.CONNECTED
            if (isNew) {
                logMessage(
                    "SYS", "LAN_FOUND", JSONObject().apply {
                        put("name", watch.name)
                        put("model", watch.model)
                        put("wearVersion", watch.appVersion)
                        put("battery", watch.battery)
                    }.toString()
                )
            }
        } else if (lastWearPeerId != null) {
            lastWearPeerId = null
            _connectionStatus.value = if (hasBondedWatch()) WearConnectionStatus.PAIRED else WearConnectionStatus.DISCONNECTED
            logMessage("SYS", "LAN_LOST", "MILES Wear left the local network")
        }
    }

    private fun handleFromWatch(path: String, payload: JSONObject) {
        when (path) {
            LanProtocol.PATH_HR_STREAM -> ingestWatchHeartRate(payload.optInt("bpm", 0))
            LanProtocol.PATH_CADENCE_STREAM -> ingestWatchCadence(payload.optInt("cadence", 0))
            LanProtocol.PATH_PING -> transport?.send(LanProtocol.ROLE_WEAR, LanProtocol.PATH_PONG, JSONObject().toString())
            LanProtocol.PATH_WORKOUT_CONTROL -> logMessage(
                "RX <- WATCH", "WORKOUT_CONTROL", JSONObject().apply {
                    put("action", payload.optString("action"))
                    put("workoutType", payload.optString("workoutType"))
                }.toString()
            )
            LanProtocol.PATH_FLUSH_QUEUE -> {
                val count = payload.optInt("count", 0)
                logMessage("RX <- WATCH", "QUEUE_FLUSH", "Received $count offline telemetry records from watch")
            }
        }
    }

    private fun sendToWatch(path: String, payloadJson: String): Boolean =
        transport?.send(LanProtocol.ROLE_WEAR, path, payloadJson) == true

    private fun pathForTopic(topic: String): String = when (topic) {
        "LIVE_METRICS" -> LanProtocol.PATH_PHONE_METRICS
        "SETTING_SYNC" -> LanProtocol.PATH_WATCH_SETTINGS
        "PING" -> LanProtocol.PATH_PING
        "SYNC_REQ" -> LanProtocol.PATH_SYNC_REQUEST
        else -> LanProtocol.PATH_PING
    }

    private fun hasBondedWatch(): Boolean = runCatching {
        val adapter = bluetoothManager?.adapter
        adapter?.bondedDevices?.any { device ->
            val name = device.name?.lowercase().orEmpty()
            name.contains("watch") || name.contains("wear") || name.contains("pixel watch") ||
                name.contains("galaxy watch") || name.contains("fitbit") || name.contains("garmin") ||
                name.contains("coros") || name.contains("suunto") || name.contains("amazfit")
        } == true
    }.getOrDefault(false)

    // ---------------- pairing (Android Bluetooth, real) ----------------

    fun scanAndConnect() {
        _connectionStatus.value = WearConnectionStatus.CONNECTING
        val adapter = bluetoothManager?.adapter
        val bondedWear = runCatching {
            adapter?.bondedDevices?.firstOrNull { device ->
                val name = device.name?.lowercase().orEmpty()
                name.contains("watch") || name.contains("wear") || name.contains("pixel watch") ||
                    name.contains("galaxy watch") || name.contains("fitbit") || name.contains("garmin") ||
                    name.contains("coros") || name.contains("suunto") || name.contains("amazfit")
            }
        }.getOrNull()

        if (bondedWear == null) {
            logMessage("SYS", "PAIR_SCAN", "No paired Wear OS or fitness watch found. Pair in Android Bluetooth settings first. LAN discovery keeps running either way.")
            // A watch on the same WiFi can still be connected without Bluetooth pairing
            if (!hasLanWatch()) {
                _deviceProfile.value = null
                _connectionStatus.value = WearConnectionStatus.DISCONNECTED
            }
            return
        }

        logMessage("SYS", "PAIR_FOUND", JSONObject().apply {
            put("name", bondedWear.name)
            put("addressKnown", bondedWear.address != null)
            put("syncReady", true)
        }.toString())
        if (!hasLanWatch()) _connectionStatus.value = WearConnectionStatus.PAIRED
        syncAfterReconnect()
    }

    private fun hasLanWatch(): Boolean = lastWearPeerId != null

    fun disconnect() {
        _deviceProfile.value = null
        _connectionStatus.value = WearConnectionStatus.DISCONNECTED
        logMessage("SYS", "PAIR_CLEAR", "Phone-side watch selection cleared; LAN discovery keeps running.")
    }

    fun togglePairing() {
        if (_deviceProfile.value != null) disconnect() else scanAndConnect()
    }

    // ---------------- sending ----------------

    fun queueForWatchSync(topic: String, payload: JSONObject) {
        if (_connectionStatus.value == WearConnectionStatus.CONNECTED) {
            val ok = sendToWatch(pathForTopic(topic), payload.toString())
            if (ok) {
                logMessage("TX -> WATCH", topic, payload.toString())
            } else {
                enqueueOffline(topic, payload)
            }
        } else {
            enqueueOffline(topic, payload)
        }
    }

    private fun enqueueOffline(topic: String, payload: JSONObject) {
        val queueItem = JSONObject().apply {
            put("topic", topic)
            put("timestamp", System.currentTimeMillis())
            put("data", payload)
        }
        _offlineQueue.value = (_offlineQueue.value + queueItem).takeLast(50)
        logMessage("OFFLINE_QUEUE", topic, "Enqueued for sync upon reconnect (pending: ${_offlineQueue.value.size})")
    }

    fun syncAfterReconnect() {
        val pending = _offlineQueue.value
        if (pending.isEmpty()) {
            logMessage("SYS", "SYNC", "Offline queue empty. Watch synchronization up to date.")
            return
        }
        if (_connectionStatus.value != WearConnectionStatus.CONNECTED) {
            logMessage("SYS", "SYNC_WAIT", "Watch not reachable yet; ${pending.size} items stay queued.")
            return
        }
        logMessage("TX -> WATCH", "FLUSH_OFFLINE_QUEUE", "Flushing ${pending.size} queued events to watch...")
        val stillPending = pending.filter { item ->
            val topic = item.optString("topic", "OFFLINE_EVENT")
            val data = item.optJSONObject("data")?.toString() ?: item.toString()
            val ok = sendToWatch(pathForTopic(topic), data)
            if (ok) logMessage("TX -> WATCH (SYNC)", topic, data)
            !ok
        }
        _offlineQueue.value = stillPending
        logMessage("SYS", "SYNC_COMPLETE", "Delivered ${pending.size - stillPending.size} of ${pending.size} offline items to watch.")
    }

    fun ingestWatchHeartRate(bpm: Int) {
        _watchHeartRate.value = bpm
        logMessage("RX <- WATCH", "HR_STREAM", "{\"bpm\":$bpm}")
    }

    fun ingestWatchCadence(rpm: Int) {
        _watchCadence.value = rpm
        logMessage("RX <- WATCH", "CADENCE_STREAM", "{\"cadence\":$rpm}")
    }

    fun pingWatch() {
        if (_connectionStatus.value == WearConnectionStatus.CONNECTED) {
            val payload = JSONObject().put("timestamp", System.currentTimeMillis())
            if (sendToWatch(LanProtocol.PATH_PING, payload.toString())) {
                logMessage("TX -> WATCH", "PING", payload.toString())
            } else {
                logMessage("SYS", "PING_FAILED", "Watch beacon seen but TCP send failed; will retry.")
            }
        } else {
            logMessage("SYS", "PING_SKIPPED", "No watch on the local network.")
        }
    }

    fun triggerWatchSync() {
        if (_connectionStatus.value == WearConnectionStatus.CONNECTED) {
            logMessage("TX -> WATCH", "SYNC_REQ", "{\"full\":true}")
            sendToWatch(LanProtocol.PATH_SYNC_REQUEST, "{\"full\":true}")
            syncAfterReconnect()
        } else {
            logMessage("SYS", "SYNC_QUEUED", "Watch not connected; queued until reconnect.")
        }
    }

    fun sendLiveWorkoutUpdate(durationSec: Long, distanceM: Double, calories: Double, hr: Int) {
        val payload = JSONObject().apply {
            put("sec", durationSec)
            put("m", distanceM)
            put("cal", calories)
            put("hr", hr)
        }
        queueForWatchSync("LIVE_METRICS", payload)
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
        if (_connectionStatus.value == WearConnectionStatus.CONNECTED) {
            val ok = sendToWatch(LanProtocol.PATH_WATCH_SETTINGS, json.toString())
            logMessage(if (ok) "TX -> WATCH" else "OFFLINE_QUEUE", "SETTING_SYNC", json.toString())
        }
    }

    fun sendHapticPulse() {
        // No haptic path in the LAN protocol; kept for UI compatibility
        logMessage("SYS", "HAPTIC_UNSUPPORTED", "Haptic commands are not part of the LAN protocol yet.")
    }

    private fun logMessage(direction: String, topic: String, payload: String) {
        _communicationLogs.value = (_communicationLogs.value + WearLogMessage(direction = direction, topic = topic, payload = payload)).takeLast(40)
    }
}