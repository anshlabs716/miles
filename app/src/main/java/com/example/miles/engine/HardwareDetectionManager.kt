package com.example.miles.engine

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DeviceHardwareProfile {
    STANDARD_PHONE,
    TABLET_WITH_GPS,
    TABLET_WITHOUT_GPS
}

enum class ActiveTrackingProtocol(
    val title: String,
    val shortLabel: String,
    val description: String
) {
    STANDARD_GPS(
        title = "Standard Satellite GNSS Protocol",
        shortLabel = "GNSS / GPS",
        description = "High-accuracy standalone hardware satellite GNSS + motion pedometer coprocessor."
    ),
    TABLET_FALLBACK_PROTOCOL(
        title = "Tablet Sensor & Network Protocol",
        shortLabel = "Tablet Protocol",
        description = "Optimized for Wi-Fi / Cell location, BLE heart-rate/footpod tunnels, on-board 3-axis accelerometer wave pedometer, and Health Connect sync."
    )
}

data class HardwareDetectionReport(
    val isTablet: Boolean,
    val hasGpsHardware: Boolean,
    val hasNetworkLocation: Boolean,
    val hasHardwareStepCounter: Boolean,
    val hasHardwareAccelerometer: Boolean,
    val hasBluetoothLe: Boolean,
    val hasWifi: Boolean,
    val hardwareProfile: DeviceHardwareProfile,
    val recommendedProtocol: ActiveTrackingProtocol,
    val summaryMessage: String
)

class HardwareDetectionManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _report = MutableStateFlow(inspectHardware())
    val report: StateFlow<HardwareDetectionReport> = _report.asStateFlow()

    fun inspectHardware(): HardwareDetectionReport {
        // 1. Detect if running on a Tablet / large screen device
        val configuration = appContext.resources.configuration
        val screenLayout = configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
        val isLargeScreen = screenLayout >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
        val isTabletByWidth = configuration.smallestScreenWidthDp >= 600
        val isTablet = isLargeScreen || isTabletByWidth

        // 2. Detect GPS hardware feature
        val hasGpsFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        val hasGpsProvider = runCatching {
            locationManager?.allProviders?.contains(LocationManager.GPS_PROVIDER) == true
        }.getOrDefault(false)
        val hasProperGps = hasGpsFeature && hasGpsProvider

        // 3. Detect Wi-Fi / Network location availability
        val hasNetworkFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK)
        val hasNetworkProvider = runCatching {
            locationManager?.allProviders?.contains(LocationManager.NETWORK_PROVIDER) == true
        }.getOrDefault(false)
        val hasNetworkLocation = hasNetworkFeature || hasNetworkProvider

        // 4. Detect hardware step sensors vs raw accelerometer
        val hasStepCounter = packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER) ||
                (sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null)
        val hasAccelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

        // 5. Detect Bluetooth LE
        val hasBle = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

        // 6. Detect Wi-Fi connectivity / capability
        val hasWifi = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)

        val profile = when {
            isTablet && !hasProperGps -> DeviceHardwareProfile.TABLET_WITHOUT_GPS
            isTablet && hasProperGps -> DeviceHardwareProfile.TABLET_WITH_GPS
            else -> DeviceHardwareProfile.STANDARD_PHONE
        }

        val recommendedProtocol = if (profile == DeviceHardwareProfile.TABLET_WITHOUT_GPS) {
            ActiveTrackingProtocol.TABLET_FALLBACK_PROTOCOL
        } else {
            ActiveTrackingProtocol.STANDARD_GPS
        }

        val summaryMessage = when (profile) {
            DeviceHardwareProfile.TABLET_WITHOUT_GPS ->
                "Tablet detected without hardware GPS. Falling back to Tablet Protocol: Wi-Fi assisted location, motion sensors, Bluetooth peripherals & Health Connect."
            DeviceHardwareProfile.TABLET_WITH_GPS ->
                "Tablet detected with dedicated hardware GPS. Satellite telemetry and fused sensors active."
            DeviceHardwareProfile.STANDARD_PHONE ->
                "Phone detected with standard GPS and motion coprocessors."
        }

        val result = HardwareDetectionReport(
            isTablet = isTablet,
            hasGpsHardware = hasProperGps,
            hasNetworkLocation = hasNetworkLocation,
            hasHardwareStepCounter = hasStepCounter,
            hasHardwareAccelerometer = hasAccelerometer,
            hasBluetoothLe = hasBle,
            hasWifi = hasWifi,
            hardwareProfile = profile,
            recommendedProtocol = recommendedProtocol,
            summaryMessage = summaryMessage
        )
        _report.value = result
        return result
    }

    fun isWifiConnected(): Boolean = runCatching {
        val net = connectivityManager?.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(net) ?: return false
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }.getOrDefault(false)
}
