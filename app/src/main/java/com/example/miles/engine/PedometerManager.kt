package com.example.miles.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.example.miles.data.local.MilesPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class PedometerManager(
    private val context: Context,
    private val preferences: MilesPreferences
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val prefs = context.getSharedPreferences("miles_pedometer", Context.MODE_PRIVATE)

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()
    private val _sensorStatus = MutableStateFlow("Starting…")
    val sensorStatus: StateFlow<String> = _sensorStatus.asStateFlow()
    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()
    private val _sensorAvailable = MutableStateFlow(false)
    val sensorAvailable: StateFlow<Boolean> = _sensorAvailable.asStateFlow()

    var onStepDetected: ((delta: Int) -> Unit)? = null

    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    private var lastCounterValue = -1f
    private var todayDate = currentDate()
    private var lastMagnitude = 0.0
    private var lastStepTimeMs = 0L

    init {
        setupSensors()
        loadTodayState()
        observeSettings()
    }

    private fun currentDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun hasActivityPermission(): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    private fun loadTodayState() {
        val today = currentDate()
        todayDate = today
        val storedDate = prefs.getString(KEY_DATE, null)
        if (storedDate != today) {
            prefs.edit()
                .putString(KEY_DATE, today)
                .putInt(KEY_TODAY_STEPS, 0)
                .putFloat(KEY_DAY_BASELINE, -1f)
                .apply()
            _todaySteps.value = 0
            lastCounterValue = prefs.getFloat(KEY_LAST_COUNTER, -1f)
        } else {
            _todaySteps.value = prefs.getInt(KEY_TODAY_STEPS, 0).coerceAtLeast(0)
            lastCounterValue = prefs.getFloat(KEY_LAST_COUNTER, -1f)
        }
    }

    private fun persistSteps(value: Int) {
        val safe = value.coerceAtLeast(0)
        _todaySteps.value = safe
        prefs.edit()
            .putString(KEY_DATE, todayDate)
            .putInt(KEY_TODAY_STEPS, safe)
            .putFloat(KEY_LAST_COUNTER, lastCounterValue)
            .apply()
        runCatching { com.example.miles.widget.MilesWidgetUpdater.updateAll(context) }
    }

    private fun setupSensors() {
        val sm = sensorManager ?: run {
            _sensorStatus.value = "Motion sensors unavailable"
            return
        }
        stepCounterSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        _sensorAvailable.value = stepCounterSensor != null || stepDetectorSensor != null || accelSensor != null
    }

    private fun observeSettings() {
        scope.launch {
            preferences.userPreferences.collect { userPrefs ->
                if (userPrefs.stepSensorHardwareEnabled) registerSensors()
                else {
                    unregisterSensors()
                    _sensorStatus.value = "Step sensor OFF"
                }
            }
        }
    }

    fun registerSensors() {
        val sm = sensorManager ?: return
        _permissionGranted.value = hasActivityPermission()
        if (!_permissionGranted.value) {
            unregisterSensors()
            _sensorStatus.value = "Activity recognition permission required"
            return
        }
        if (!preferences.userPreferences.value.stepSensorHardwareEnabled) {
            unregisterSensors()
            _sensorStatus.value = "Step sensor OFF"
            return
        }
        unregisterSensors()
        when {
            stepCounterSensor != null -> {
                sm.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
                _sensorStatus.value = "Hardware step counter active"
            }
            stepDetectorSensor != null -> {
                sm.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
                _sensorStatus.value = "Hardware step detector active"
            }
            accelSensor != null -> {
                sm.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
                _sensorStatus.value = "Accelerometer fallback active"
            }
            else -> _sensorStatus.value = "No supported motion sensor"
        }
    }

    fun unregisterSensors() {
        sensorManager?.unregisterListener(this)
    }

    fun startTracking() = registerSensors()
    fun stopTracking() = unregisterSensors()

    fun onPermissionStateChanged() {
        _permissionGranted.value = hasActivityPermission()
        if (_permissionGranted.value) registerSensors()
        else {
            unregisterSensors()
            _sensorStatus.value = "Activity recognition permission required"
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val nowDate = currentDate()
        if (nowDate != todayDate) loadTodayState()
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> handleStepCounter(event.values.firstOrNull() ?: return)
            Sensor.TYPE_STEP_DETECTOR -> if ((event.values.firstOrNull() ?: 0f) >= 1f) {
                persistSteps(_todaySteps.value + 1)
                onStepDetected?.invoke(1)
            }
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
        }
    }

    private fun handleStepCounter(totalSinceBoot: Float) {
        if (lastCounterValue < 0f) {
            lastCounterValue = totalSinceBoot
            prefs.edit()
                .putFloat(KEY_LAST_COUNTER, lastCounterValue)
                .putFloat(KEY_DAY_BASELINE, totalSinceBoot)
                .apply()
            return
        }
        if (totalSinceBoot < lastCounterValue) {
            lastCounterValue = totalSinceBoot
            prefs.edit().putFloat(KEY_LAST_COUNTER, lastCounterValue).apply()
            return
        }
        val delta = (totalSinceBoot - lastCounterValue).toInt().coerceAtLeast(0)
        if (delta > 0) {
            persistSteps(_todaySteps.value + delta)
            onStepDetected?.invoke(delta)
        }
        lastCounterValue = totalSinceBoot
        prefs.edit().putFloat(KEY_LAST_COUNTER, lastCounterValue).apply()
    }

    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values.getOrNull(0)?.toDouble() ?: return
        val y = event.values.getOrNull(1)?.toDouble() ?: return
        val z = event.values.getOrNull(2)?.toDouble() ?: return
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()
        val sensitivity = preferences.userPreferences.value.stepSensitivityThreshold.coerceIn(0.7f, 2.5f)
        val threshold = 1.4 * sensitivity
        if (magnitude - lastMagnitude > threshold && now - lastStepTimeMs >= 300L) {
            lastStepTimeMs = now
            persistSteps(_todaySteps.value + 1)
            onStepDetected?.invoke(1)
        }
        lastMagnitude = magnitude
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    fun addManualSteps(count: Int) = persistSteps((_todaySteps.value + count).coerceAtLeast(0))

    fun resetDailySteps() {
        lastCounterValue = -1f
        todayDate = currentDate()
        prefs.edit()
            .putString(KEY_DATE, todayDate)
            .putInt(KEY_TODAY_STEPS, 0)
            .putFloat(KEY_DAY_BASELINE, -1f)
            .apply()
        _todaySteps.value = 0
    }

    companion object {
        private const val KEY_DATE = "step_date"
        private const val KEY_TODAY_STEPS = "today_steps"
        private const val KEY_DAY_BASELINE = "day_baseline"
        private const val KEY_LAST_COUNTER = "last_counter"
    }
}