package com.example.miles.engine

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DeviceSourceType(val displayName: String, val category: String) {
    PHONE_GPS("Built-in Phone GNSS", "Location"),
    EXTERNAL_GNSS("External Bluetooth GPS", "Location"),
    PHONE_STEP_COUNTER("Built-in Pedometer", "Motion"),
    BLE_HEART_RATE("Bluetooth HR Strap", "Heart Rate"),
    WEAR_OS_SENSOR("Wear OS Smartwatch", "Wearable")
}

data class ConnectedSource(
    val id: String,
    val name: String,
    val type: DeviceSourceType,
    val isConnected: Boolean,
    val batteryLevel: Int = 100,
    val signalDbm: Int = -50,
    val details: String
)

class DeviceManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    // Real default: Only actual internal phone hardware sensors
    private val _sources = MutableStateFlow<List<ConnectedSource>>(
        listOf(
            ConnectedSource(
                id = "phone_gps_internal",
                name = "Internal Phone GNSS (GPS)",
                type = DeviceSourceType.PHONE_GPS,
                isConnected = true,
                batteryLevel = 100,
                signalDbm = -50,
                details = "Device Hardware Location Provider"
            ),
            ConnectedSource(
                id = "phone_step_internal",
                name = "Built-in Step Sensor",
                type = DeviceSourceType.PHONE_STEP_COUNTER,
                isConnected = true,
                batteryLevel = 100,
                signalDbm = 0,
                details = "Hardware Accelerometer & Pedometer"
            )
        )
    )
    val sources: StateFlow<List<ConnectedSource>> = _sources.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    val isScanningBle: StateFlow<Boolean> = _isScanning.asStateFlow()

    fun scanForNearbySensors() {
        if (_isScanning.value) return
        _isScanning.value = true
        scope.launch {
            delay(2000L)
            _isScanning.value = false
        }
    }

    fun startBleScan() = scanForNearbySensors()

    fun toggleSource(id: String) {
        val current = _sources.value
        _sources.value = current.map { src ->
            if (src.id == id) src.copy(isConnected = !src.isConnected) else src
        }
    }

    fun toggleSourceConnection(id: String) = toggleSource(id)
}
