package com.example.miles.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads the phone's own optical heart-rate sensor (TYPE_HEART_RATE).
 *
 * This is what makes the BODY_SENSORS permission meaningful: without it the app
 * asked for body sensors and then never read one. Readings are only emitted when
 * Android has actually granted the permission, and nothing is invented when the
 * device has no heart-rate hardware.
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
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.BODY_SENSORS) ==
            PackageManager.PERMISSION_GRANTED

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
