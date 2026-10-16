package com.example.miles.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads the phone's own optical heart-rate sensor (TYPE_HEART_RATE).
 *
 * This is what makes the body-sensor permission meaningful: without it the app
 * asked for it and then never read a sensor. Which permission guards the sensor
 * changed in Android 16 — see [heartRatePermission] — and this class checks the
 * right one for the system it is running on. Readings are only emitted when
 * Android has actually granted it, and nothing is invented when the device has
 * no heart-rate hardware.
 */
class DeviceHeartRateSensor(context: Context) : SensorEventListener {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    private val _readingBpm = MutableStateFlow<Int?>(null)
    val readingBpm: StateFlow<Int?> = _readingBpm.asStateFlow()

    private val _supported = MutableStateFlow(false)
    val supported: StateFlow<Boolean> = _supported.asStateFlow()

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private var registered = false

    /** True only when this device actually has heart-rate hardware. */
    fun hasHardware(): Boolean = sensor != null

    private fun permissionGrantedNow(): Boolean =
        ContextCompat.checkSelfPermission(appContext, heartRatePermission()) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Re-reads the permission state, and registers the sensor if it became
     * available. Called again after the permission dialog is answered, because
     * the first [start] happens before the user has had the chance to grant it.
     */
    fun recheckPermission() = start()

    fun start() {
        val hardware = sensor
        _supported.value = hardware != null
        _permissionGranted.value = permissionGrantedNow()
        if (hardware == null || registered) return
        if (!_permissionGranted.value) return
        registered = sensorManager?.registerListener(this, hardware, SensorManager.SENSOR_DELAY_NORMAL) == true
    }

    fun stop() {
        if (!registered) return
        sensorManager?.unregisterListener(this)
        registered = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val bpm = event?.values?.firstOrNull() ?: return
        val rounded = bpm.toInt()
        if (rounded in 20..240) _readingBpm.value = rounded
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/**
 * The permission Android enforces for [Sensor.TYPE_HEART_RATE] on a given API level.
 *
 * Android 16 (API 36) retired the broad BODY_SENSORS permission for heart rate and
 * moved it to the granular health permissions, so an app targeting SDK 36 must ask
 * for `android.permission.health.READ_HEART_RATE` there and for BODY_SENSORS below.
 * The literal is used instead of the constant because that class only exists from
 * API 34 onwards and would fail to load on older devices.
 */
fun heartRatePermission(sdkInt: Int = Build.VERSION.SDK_INT): String =
    if (sdkInt >= 36) "android.permission.health.READ_HEART_RATE"
    else Manifest.permission.BODY_SENSORS
