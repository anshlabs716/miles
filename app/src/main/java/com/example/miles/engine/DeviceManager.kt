package com.example.miles.engine

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Real Bluetooth LE sensor discovery/connection layer. No simulated devices are inserted. */
enum class DeviceSourceType(val displayName: String, val category: String) {
    PHONE_GPS("Built-in Phone GNSS", "Location"),
    EXTERNAL_GNSS("External Bluetooth GPS", "Location"),
    PHONE_STEP_COUNTER("Built-in Pedometer", "Motion"),
    BLE_HEART_RATE("Bluetooth HR Strap", "Heart Rate"),
    WEAR_OS_SENSOR("Wear OS Smartwatch", "Wearable"),
    BLE_CYCLING_SENSOR("Cycling Speed/Cadence", "Cycling"),
    BLE_FOOT_POD("Running Foot Pod", "Motion")
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
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? get() = adapter?.bluetoothLeScanner
    private val gattConnections = mutableMapOf<String, BluetoothGatt>()

    private val _sources = MutableStateFlow<List<ConnectedSource>>(emptyList())
    val sources: StateFlow<List<ConnectedSource>> = _sources.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    val isScanningBle: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _heartRateBpm = MutableStateFlow<Int?>(null)
    val heartRateBpm: StateFlow<Int?> = _heartRateBpm.asStateFlow()

    private val _hasHeartRateCapability = MutableStateFlow(false)
    val hasHeartRateCapability: StateFlow<Boolean> = _hasHeartRateCapability.asStateFlow()

    private val _isBluetoothTunnelling = MutableStateFlow(false)
    val isBluetoothTunnelling: StateFlow<Boolean> = _isBluetoothTunnelling.asStateFlow()

    private val _sensorUpdateFrequencyHz = MutableStateFlow(1)
    val sensorUpdateFrequencyHz: StateFlow<Int> = _sensorUpdateFrequencyHz.asStateFlow()

    private val _gattPacketsReceived = MutableStateFlow(0L)
    val gattPacketsReceived: StateFlow<Long> = _gattPacketsReceived.asStateFlow()

    private val _lastBleError = MutableStateFlow<String?>(null)
    val lastBleError: StateFlow<String?> = _lastBleError.asStateFlow()

    val pairedWatch: StateFlow<ConnectedSource?> =
        _sources
            .map { list ->
                list.firstOrNull {
                    it.type == DeviceSourceType.WEAR_OS_SENSOR && it.isConnected
                }
            }
            .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)

    fun refreshPairedDevices() {
        checkBondedDevices()
        if (!_isScanning.value) scanForNearbySensors()
    }

    fun onWatchHeartRateReceived(bpm: Int) {
        _heartRateBpm.value = bpm.coerceIn(20, 240)
        _hasHeartRateCapability.value = true
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addScanResult(result)
        }
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::addScanResult)
        }
        override fun onScanFailed(errorCode: Int) {
            _lastBleError.value = "BLE scan failed ($errorCode)"
            _isScanning.value = false
        }
    }

    init {
        addInternalSources()
        checkBondedDevices()
    }

    private fun hasScanPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

    private fun hasConnectPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun updateHrCapability() {
        _hasHeartRateCapability.value = _sources.value.any { 
            (it.type == DeviceSourceType.BLE_HEART_RATE || it.type == DeviceSourceType.WEAR_OS_SENSOR) && 
            (it.isConnected || it.id.startsWith("bonded_"))
        }
    }

    fun checkBondedDevices() {
        if (!hasConnectPermission()) return
        val bonded = runCatching { adapter?.bondedDevices }.getOrNull().orEmpty()
        val detected = bonded.mapNotNull { device ->
            val name = (device.name ?: "").lowercase()
            val type = when {
                name.contains("watch") || name.contains("wear") || name.contains("pixel watch") || name.contains("galaxy watch") || name.contains("fitbit") -> DeviceSourceType.WEAR_OS_SENSOR
                name.contains("heart") || name.contains("hrm") || name.contains("polar") || name.contains("wahoo") -> DeviceSourceType.BLE_HEART_RATE
                name.contains("cadence") || name.contains("speed") || name.contains("cycl") -> DeviceSourceType.BLE_CYCLING_SENSOR
                name.contains("pod") || name.contains("stride") || name.contains("foot") -> DeviceSourceType.BLE_FOOT_POD
                name.contains("gps") || name.contains("garmin") -> DeviceSourceType.EXTERNAL_GNSS
                else -> null
            }
            type?.let {
                ConnectedSource(
                    id = "bonded_${device.address}",
                    name = device.name ?: "Bonded Device",
                    type = it,
                    isConnected = true,
                    signalDbm = -45,
                    details = "Android paired ${it.category}"
                )
            }
        }
        if (detected.isNotEmpty()) {
            val existingIds = _sources.value.map { it.id }.toSet()
            val newSources = detected.filterNot { it.id in existingIds }
            if (newSources.isNotEmpty()) {
                _sources.value = _sources.value + newSources
            }
        }
        updateHrCapability()
    }

    private fun addInternalSources() {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
        val gps = ConnectedSource(
            id = "phone_gps_internal",
            name = "Internal Phone GNSS",
            type = DeviceSourceType.PHONE_GPS,
            isConnected = true,
            details = "Android hardware location provider"
        )
        val stepsAvailable = sm?.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_COUNTER) != null ||
            sm?.getDefaultSensor(android.hardware.Sensor.TYPE_STEP_DETECTOR) != null
        val step = ConnectedSource(
            id = "phone_step_internal",
            name = "Built-in Step Sensor",
            type = DeviceSourceType.PHONE_STEP_COUNTER,
            isConnected = stepsAvailable,
            details = if (stepsAvailable) "Hardware step counter/detector" else "No hardware step counter detected"
        )
        _sources.value = listOf(gps, step)
        updateHrCapability()
    }

    private fun addScanResult(result: ScanResult) {
        val device = result.device
        val id = "ble_${device.address}"
        val name = runCatching {
            if (hasConnectPermission()) device.name ?: "Unnamed BLE sensor" else "BLE sensor"
        }.getOrDefault("BLE sensor")
        val type = classify(result)
        val details = when (type) {
            DeviceSourceType.BLE_HEART_RATE -> "BLE Heart Rate Service detected"
            DeviceSourceType.EXTERNAL_GNSS -> "External GNSS / navigation sensor"
            DeviceSourceType.WEAR_OS_SENSOR -> "Wear OS / smartwatch sensor"
            DeviceSourceType.BLE_CYCLING_SENSOR -> "Cycling Speed/Cadence sensor"
            DeviceSourceType.BLE_FOOT_POD -> "Running foot pod / stride sensor"
            else -> "Bluetooth LE sensor"
        }
        val source = ConnectedSource(id, name, type, false, signalDbm = result.rssi, details = details)
        _sources.value = _sources.value.filterNot { it.id == id } + source
        updateHrCapability()
    }

    private fun classify(result: ScanResult): DeviceSourceType {
        val uuids = result.scanRecord?.serviceUuids.orEmpty().map { it.uuid }
        val name = runCatching {
            if (hasConnectPermission()) result.device.name.orEmpty().lowercase() else ""
        }.getOrDefault("")
        return when {
            HEART_RATE_SERVICE in uuids || name.contains("heart") || name.contains("hrm") || name.contains("polar") || name.contains("wahoo") -> DeviceSourceType.BLE_HEART_RATE
            CYCLING_SPEED_CADENCE in uuids || name.contains("cadence") || name.contains("speed") || name.contains("cycl") -> DeviceSourceType.BLE_CYCLING_SENSOR
            RUNNING_SPEED_CADENCE in uuids || name.contains("pod") || name.contains("stride") || name.contains("foot") -> DeviceSourceType.BLE_FOOT_POD
            name.contains("pixel watch") || name.contains("wear os") || name.contains("galaxy watch") || name.contains("watch") || name.contains("fitbit") -> DeviceSourceType.WEAR_OS_SENSOR
            name.contains("gps") || name.contains("gnss") || name.contains("garmin") -> DeviceSourceType.EXTERNAL_GNSS
            else -> DeviceSourceType.BLE_HEART_RATE
        }
    }

    fun scanForNearbySensors() {
        if (_isScanning.value || !hasScanPermission()) {
            if (!hasScanPermission()) _lastBleError.value = "Bluetooth scan permission required"
            return
        }
        val bleScanner = scanner ?: run {
            _lastBleError.value = "Bluetooth LE unavailable"
            return
        }
        _isScanning.value = true
        _lastBleError.value = null
        runCatching {
            bleScanner.startScan(
                null,
                ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                scanCallback
            )
        }.onFailure {
            _lastBleError.value = it.message ?: "Unable to start BLE scan"
            _isScanning.value = false
            return
        }
        scope.launch {
            delay(10_000L)
            stopBleScan()
        }
    }

    fun startBleScan() = scanForNearbySensors()

    fun stopBleScan() {
        if (!hasScanPermission()) return
        runCatching { scanner?.stopScan(scanCallback) }
        _isScanning.value = false
    }

    fun toggleSource(id: String) {
        val source = _sources.value.firstOrNull { it.id == id } ?: return
        if (source.type == DeviceSourceType.PHONE_GPS || source.type == DeviceSourceType.PHONE_STEP_COUNTER) {
            _sources.value = _sources.value.map { if (it.id == id) it.copy(isConnected = !it.isConnected) else it }
            return
        }
        if (!source.isConnected) connect(source) else disconnect(id)
    }

    fun toggleSourceConnection(id: String) = toggleSource(id)

    private fun connect(source: ConnectedSource) {
        if (!hasConnectPermission()) {
            _lastBleError.value = "Bluetooth connect permission required"
            return
        }
        val device = runCatching { adapter?.getRemoteDevice(source.id.removePrefix("ble_")) }.getOrNull()
        if (device == null) {
            _lastBleError.value = "Bluetooth device unavailable"
            return
        }
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    gattConnections[source.id] = gatt
                    _sources.value = _sources.value.map { if (it.id == source.id) it.copy(isConnected = true) else it }
                    gatt.discoverServices()
                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                    gattConnections.remove(source.id)
                    _sources.value = _sources.value.map { if (it.id == source.id) it.copy(isConnected = false) else it }
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    _lastBleError.value = "BLE connection failed ($status)"
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) return
                val hrService = gatt.getService(HEART_RATE_SERVICE)
                val hrCharacteristic = hrService?.getCharacteristic(HEART_RATE_MEASUREMENT)
                if (hrCharacteristic != null && source.type == DeviceSourceType.BLE_HEART_RATE) {
                    gatt.setCharacteristicNotification(hrCharacteristic, true)
                    val descriptor = hrCharacteristic.getDescriptor(CLIENT_CONFIG)
                    if (descriptor != null) {
                        descriptor.value = android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(descriptor)
                    }
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                _gattPacketsReceived.value++
                if (characteristic.uuid == HEART_RATE_MEASUREMENT) {
                    val flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) ?: return
                    val format = if ((flags and 0x01) == 0) BluetoothGattCharacteristic.FORMAT_UINT8 else BluetoothGattCharacteristic.FORMAT_UINT16
                    val offset = 1
                    val bpm = characteristic.getIntValue(format, offset) ?: return
                    _heartRateBpm.value = bpm.coerceIn(20, 240)
                    _hasHeartRateCapability.value = true
                }
            }
        }
        runCatching { device.connectGatt(context, false, callback) }
            .onFailure { _lastBleError.value = it.message ?: "Unable to connect" }
    }

    private fun disconnect(id: String) {
        gattConnections.remove(id)?.let { gatt ->
            runCatching { gatt.disconnect() }
            runCatching { gatt.close() }
        }
        _sources.value = _sources.value.map { if (it.id == id) it.copy(isConnected = false) else it }
        updateHrCapability()
    }

    fun toggleBluetoothTunnelling() {
        _isBluetoothTunnelling.value = !_isBluetoothTunnelling.value
    }

    fun setSensorUpdateFrequency(hz: Int) {
        _sensorUpdateFrequencyHz.value = hz.coerceIn(1, 20)
    }

    fun close() {
        stopBleScan()
        gattConnections.values.forEach { runCatching { it.disconnect() }; runCatching { it.close() } }
        gattConnections.clear()
    }

    companion object {
        val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CYCLING_SPEED_CADENCE: UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
        val RUNNING_SPEED_CADENCE: UUID = UUID.fromString("00001814-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
