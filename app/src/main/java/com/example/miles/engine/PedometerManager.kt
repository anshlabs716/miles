package com.example.miles.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val prefs = context.getSharedPreferences("miles_pedometer", Context.MODE_PRIVATE)

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _sensorStatus = MutableStateFlow("Initializing...")
    val sensorStatus: StateFlow<String> = _sensorStatus.asStateFlow()

    private var stepCounterSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var accelSensor: Sensor? = null

    private var initialSystemStepCount = -1f
    private var todayDateStr = getCurrentDateString()

    // Accelerometer peak detection fallback
    private var lastAccelMagnitude = 0.0
    private var lastPeakTimeMs = 0L

    init {
        loadPersistedDailySteps()
        setupSensors()

        // Observe user settings (if hardware sensor disabled, unregister)
        scope.launch {
            preferences.userPreferences.collect { userPrefs ->
                if (userPrefs.stepSensorHardwareEnabled) {
                    registerSensors()
                } else {
                    unregisterSensors()
                    _sensorStatus.value = "Hardware Step Sensor Paused (User Setting)"
                }
            }
        }
    }

    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun loadPersistedDailySteps() {
        val today = getCurrentDateString()
        todayDateStr = today
        val savedDate = prefs.getString("step_date", "")
        if (savedDate != today) {
            // New day: reset today's accumulator
            prefs.edit()
                .putString("step_date", today)
                .putInt("step_count", 0)
                .putFloat("initial_system_steps", -1f)
                .apply()
            _todaySteps.value = 0
            initialSystemStepCount = -1f
        } else {
            val savedSteps = prefs.getInt("step_count", 0)
            initialSystemStepCount = prefs.getFloat("initial_system_steps", -1f)
            _todaySteps.value = savedSteps
        }
    }

    private fun persistSteps(steps: Int) {
        _todaySteps.value = steps
        prefs.edit()
            .putString("step_date", todayDateStr)
            .putInt("step_count", steps)
            .putFloat("initial_system_steps", initialSystemStepCount)
            .apply()
    }

    private fun setupSensors() {
        val sm = sensorManager ?: run {
            _sensorStatus.value = "No SensorManager"
            return
        }

        stepCounterSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (stepCounterSensor != null) {
            _sensorStatus.value = "Hardware Step Counter Active"
        } else if (stepDetectorSensor != null) {
            _sensorStatus.value = "Hardware Step Detector Active"
        } else if (accelSensor != null) {
            _sensorStatus.value = "Motion Accelerometer Pedometer Active"
        } else {
            _sensorStatus.value = "No motion hardware detected"
        }
    }

    fun registerSensors() {
        val sm = sensorManager ?: return
        unregisterSensors()

        if (stepCounterSensor != null) {
            sm.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
            _sensorStatus.value = "Hardware Step Counter Active"
        } else if (stepDetectorSensor != null) {
            sm.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
            _sensorStatus.value = "Hardware Step Detector Active"
        } else if (accelSensor != null) {
            sm.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
            _sensorStatus.value = "Motion Accelerometer Active"
        }
    }

    fun unregisterSensors() {
        sensorManager?.unregisterListener(this)
    }

    fun startTracking() = registerSensors()
    fun stopTracking() = unregisterSensors()

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // Daily rollover check
        val today = getCurrentDateString()
        if (today != todayDateStr) {
            loadPersistedDailySteps()
        }

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalStepsSinceBoot = event.values[0]
                if (initialSystemStepCount < 0f || totalStepsSinceBoot < initialSystemStepCount) {
                    initialSystemStepCount = totalStepsSinceBoot
                    prefs.edit().putFloat("initial_system_steps", initialSystemStepCount).apply()
                }
                val delta = (totalStepsSinceBoot - initialSystemStepCount).toInt().coerceAtLeast(0)
                val base = prefs.getInt("base_steps", 0)
                persistSteps(base + delta)
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    persistSteps(_todaySteps.value + 1)
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                // Fallback motion step estimation
                val x = event.values[0].toDouble()
                val y = event.values[1].toDouble()
                val z = event.values[2].toDouble()
                val magnitude = sqrt(x * x + y * y + z * z)
                val now = System.currentTimeMillis()

                // Peak threshold (gravity ~9.8, walking strike ~11.5 - 13.5 m/s^2)
                val delta = magnitude - lastAccelMagnitude
                val userThreshold = preferences.userPreferences.value.stepSensitivityThreshold.toDouble()
                val threshold = 1.6 * userThreshold

                if (delta > threshold && now - lastPeakTimeMs > 280L) {
                    lastPeakTimeMs = now
                    persistSteps(_todaySteps.value + 1)
                }
                lastAccelMagnitude = magnitude
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun addManualSteps(count: Int) {
        val next = (_todaySteps.value + count).coerceAtLeast(0)
        persistSteps(next)
    }

    fun resetDailySteps() {
        initialSystemStepCount = -1f
        persistSteps(0)
    }
}
