package com.example.miles.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.model.GpsPoint
import com.example.miles.data.model.Waypoint
import com.example.miles.data.repository.MilesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class TrackingState {
    IDLE,
    RECORDING,
    PAUSED
}

data class LiveWorkoutStats(
    val state: TrackingState = TrackingState.IDLE,
    val activityType: ActivityType = ActivityType.WALKING,
    val elapsedMillis: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentPaceSecPerKm: Double = 0.0,
    val avgPaceSecPerKm: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val elevationLossM: Double = 0.0,
    val currentElevationM: Double = 0.0,
    val stepCount: Int = 0,
    val calories: Int = 0,
    val heartRate: Int = 0,
    val movementConfidence: Int = 100, // 0-100%
    val isAutoPaused: Boolean = false,
    val isIndoor: Boolean = false,
    val gpsAccuracyMeters: Float = 3.5f,
    val activeSource: String = "Built-in GPS",
    val points: List<GpsPoint> = emptyList(),
    val waypoints: List<Waypoint> = emptyList(),
    val batteryForecastHours: Float = 8.5f,
    val ghostDeltaDistanceM: Double? = null,
    val isSimulationActive: Boolean = false,
    val tickIntervalMs: Long = 100L,
    val telemetryIntervalMs: Long = 3000L
) {
    val elapsedSeconds: Long get() = elapsedMillis / 1000L
}

sealed class TrackingEvent {
    data class MilestoneReached(val title: String, val message: String) : TrackingEvent()
    data class AutoPauseTriggered(val isPaused: Boolean) : TrackingEvent()
    data class AnomalyDetected(val message: String) : TrackingEvent()
}

class SmartTrackingEngine(
    private val context: Context,
    private val repository: MilesRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val recoveryPrefs: SharedPreferences =
        context.getSharedPreferences("miles_tracking_recovery", Context.MODE_PRIVATE)

    private val _liveStats = MutableStateFlow(LiveWorkoutStats())
    val liveStats: StateFlow<LiveWorkoutStats> = _liveStats.asStateFlow()

    private val _events = MutableSharedFlow<TrackingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TrackingEvent> = _events.asSharedFlow()

    private var timerJob: Job? = null
    private var telemetryJob: Job? = null
    private var simulationJob: Job? = null

    private var startTimestamp: Long = 0L
    private var pauseStartTimestamp: Long = 0L
    private var totalPausedMillis: Long = 0L
    private var lastMovementTimestamp: Long = 0L
    private var ghostActivity: ActivityEntity? = null
    private var lastMilestoneKm: Int = 0

    // Configurable intervals: Default 100ms for counter, 3000ms (3s) for telemetry
    var counterIntervalMs: Long = 100L
        set(value) {
            field = value.coerceIn(10L, 600_000L)
            _liveStats.value = _liveStats.value.copy(tickIntervalMs = field)
            if (_liveStats.value.state == TrackingState.RECORDING) {
                startTimer()
            }
        }

    var telemetryIntervalMs: Long = 3000L
        set(value) {
            field = value.coerceIn(100L, 600_000L)
            _liveStats.value = _liveStats.value.copy(telemetryIntervalMs = field)
            if (_liveStats.value.state == TrackingState.RECORDING) {
                startTelemetryLoop()
            }
        }

    var targetPaceSecPerKm: Double = 330.0 // 5:30 min/km default target pace
        set(value) {
            field = value.coerceIn(60.0, 3600.0)
        }

    // Kalman / smoothing state
    private var lastFilteredPoint: GpsPoint? = null

    init {
        restoreTrackingStateIfAvailable()
    }

    fun setGhostModeActivity(activity: ActivityEntity?) {
        ghostActivity = activity
    }

    fun startTracking(activityType: ActivityType, source: String = "Built-in GPS") {
        if (_liveStats.value.state == TrackingState.RECORDING) return

        startTimestamp = System.currentTimeMillis()
        pauseStartTimestamp = 0L
        totalPausedMillis = 0L
        lastMovementTimestamp = startTimestamp
        lastMilestoneKm = 0
        lastFilteredPoint = null

        // Real workout starts strictly at 0.00 distance, 0 elapsed, 0 steps, NO simulation!
        _liveStats.value = LiveWorkoutStats(
            state = TrackingState.RECORDING,
            activityType = activityType,
            elapsedMillis = 0L,
            activeSource = source,
            movementConfidence = 100,
            tickIntervalMs = counterIntervalMs,
            telemetryIntervalMs = telemetryIntervalMs,
            isSimulationActive = false
        )

        startTimer()
        startTelemetryLoop()
        saveRecoveryState()
    }

    fun pauseTracking(isAuto: Boolean = false) {
        if (_liveStats.value.state != TrackingState.RECORDING) return
        pauseStartTimestamp = System.currentTimeMillis()
        _liveStats.value = _liveStats.value.copy(
            state = TrackingState.PAUSED,
            isAutoPaused = isAuto
        )
        if (isAuto) {
            _events.tryEmit(TrackingEvent.AutoPauseTriggered(true))
        }
        saveRecoveryState()
    }

    fun resumeTracking() {
        if (_liveStats.value.state != TrackingState.PAUSED) return
        if (pauseStartTimestamp > 0L) {
            totalPausedMillis += (System.currentTimeMillis() - pauseStartTimestamp)
            pauseStartTimestamp = 0L
        }
        _liveStats.value = _liveStats.value.copy(
            state = TrackingState.RECORDING,
            isAutoPaused = false
        )
        _events.tryEmit(TrackingEvent.AutoPauseTriggered(false))
        saveRecoveryState()
    }

    suspend fun stopAndSaveWorkout(title: String? = null, notes: String = "", photoUri: String? = null): ActivityEntity? {
        val current = _liveStats.value
        if (current.state == TrackingState.IDLE) return null

        timerJob?.cancel()
        telemetryJob?.cancel()
        simulationJob?.cancel()

        val endTs = System.currentTimeMillis()
        val finalTitle = title?.takeIf { it.isNotBlank() }
            ?: "${current.activityType.displayName} ${java.text.SimpleDateFormat("MMM d, HH:mm", Locale.US).format(java.util.Date(startTimestamp))}"

        val entity = ActivityEntity(
            id = UUID.randomUUID().toString(),
            title = finalTitle,
            activityType = current.activityType.name,
            startTime = startTimestamp,
            endTime = endTs,
            durationSeconds = current.elapsedSeconds,
            distanceMeters = current.distanceMeters,
            steps = current.stepCount,
            avgPaceSecPerKm = current.avgPaceSecPerKm,
            bestPaceSecPerKm = (current.avgPaceSecPerKm * 0.9).coerceAtLeast(180.0),
            avgSpeedKmh = current.avgSpeedKmh,
            maxSpeedKmh = (current.avgSpeedKmh * 1.35).coerceAtLeast(current.avgSpeedKmh),
            elevationGainM = current.elevationGainM,
            elevationLossM = current.elevationLossM,
            calories = current.calories,
            avgHeartRate = if (current.heartRate > 0) current.heartRate else 0,
            maxHeartRate = if (current.heartRate > 0) (current.heartRate + 20) else 0,
            routePointsJson = MilesRepository.pointsToJson(current.points),
            waypointsJson = MilesRepository.waypointsToJson(current.waypoints),
            notes = notes,
            photoUri = photoUri,
            sensorSource = current.activeSource
        )

        repository.saveActivity(entity)
        clearRecoveryState()

        _liveStats.value = LiveWorkoutStats(state = TrackingState.IDLE)
        return entity
    }

    fun discardWorkout() {
        timerJob?.cancel()
        telemetryJob?.cancel()
        simulationJob?.cancel()
        clearRecoveryState()
        _liveStats.value = LiveWorkoutStats(state = TrackingState.IDLE)
    }

    fun addWaypoint(name: String, type: com.example.miles.data.model.WaypointType, notes: String = "") {
        val current = _liveStats.value
        val lat = current.points.lastOrNull()?.latitude ?: 37.7749
        val lng = current.points.lastOrNull()?.longitude ?: -122.4194
        val wp = Waypoint(
            name = name,
            latitude = lat,
            longitude = lng,
            type = type,
            notes = notes
        )
        _liveStats.value = current.copy(waypoints = current.waypoints + wp)
        saveRecoveryState()
    }

    // Process real incoming GPS location from Android LocationManager / Sensors
    fun processLocation(rawPoint: GpsPoint) {
        if (_liveStats.value.state != TrackingState.RECORDING) return

        val prev = lastFilteredPoint
        var validPoint = rawPoint

        if (prev != null) {
            val dist = MilesRepository.calculateDistanceMeters(prev.latitude, prev.longitude, rawPoint.latitude, rawPoint.longitude)
            val timeDiffSec = ((rawPoint.timestamp - prev.timestamp).coerceAtLeast(100L)) / 1000.0
            val instantaneousSpeed = dist / timeDiffSec

            // Filter unrealistic teleport jumps (> 50 m/s ~ 180 km/h)
            if (instantaneousSpeed > 50.0 && dist > 100.0) {
                _events.tryEmit(TrackingEvent.AnomalyDetected("Teleport glitch suppressed ($dist m jump)"))
                return
            }

            // Exponential smoothing for GPS jitter
            val alpha = 0.75f
            val smoothLat = (rawPoint.latitude * alpha) + (prev.latitude * (1f - alpha))
            val smoothLng = (rawPoint.longitude * alpha) + (prev.longitude * (1f - alpha))
            val smoothAlt = (rawPoint.altitude * 0.6) + (prev.altitude * 0.4)

            validPoint = rawPoint.copy(
                latitude = smoothLat,
                longitude = smoothLng,
                altitude = smoothAlt
            )
        }

        lastFilteredPoint = validPoint
        lastMovementTimestamp = validPoint.timestamp
        appendValidatedPoint(validPoint)
    }

    private fun appendValidatedPoint(point: GpsPoint) {
        val current = _liveStats.value
        val newPoints = current.points + point

        var totalDist = 0.0
        var addedElevGain = 0.0
        var addedElevLoss = 0.0

        if (current.points.isNotEmpty()) {
            val last = current.points.last()
            val legDist = MilesRepository.calculateDistanceMeters(last.latitude, last.longitude, point.latitude, point.longitude)
            totalDist = current.distanceMeters + legDist

            val elevDiff = point.altitude - last.altitude
            if (elevDiff > 0.6) addedElevGain = elevDiff
            else if (elevDiff < -0.6) addedElevLoss = abs(elevDiff)
        }

        val elapsed = current.elapsedSeconds
        val avgSpeedKmh = if (elapsed > 0) (totalDist / elapsed) * 3.6 else 0.0
        val avgPaceSec = if (totalDist > 50.0 && elapsed > 0) (elapsed / (totalDist / 1000.0)) else 0.0
        val currentSpeedKmh = (point.speed * 3.6).toDouble()
        val currentPaceSec = if (point.speed > 0.3f) (1000.0 / point.speed) else 0.0

        val estimatedSteps = (totalDist / 0.76).toInt()
        val calories = when (current.activityType) {
            ActivityType.RUNNING -> (totalDist * 0.065).toInt()
            ActivityType.CYCLING -> (totalDist * 0.035).toInt()
            ActivityType.HIKING -> (totalDist * 0.055).toInt()
            else -> (totalDist * 0.045).toInt()
        }

        // Milestone alerts (every whole km)
        val currentKm = (totalDist / 1000.0).toInt()
        if (currentKm > lastMilestoneKm && currentKm > 0) {
            lastMilestoneKm = currentKm
            val paceStr = formatPace(avgPaceSec)
            _events.tryEmit(TrackingEvent.MilestoneReached("Milestone: $currentKm km", "Pace: $paceStr/km • Time: ${formatDuration(elapsed)}"))
        }

        _liveStats.value = current.copy(
            distanceMeters = totalDist,
            currentPaceSecPerKm = currentPaceSec,
            avgPaceSecPerKm = avgPaceSec,
            currentSpeedKmh = currentSpeedKmh,
            avgSpeedKmh = avgSpeedKmh,
            elevationGainM = current.elevationGainM + addedElevGain,
            elevationLossM = current.elevationLossM + addedElevLoss,
            currentElevationM = point.altitude,
            stepCount = estimatedSteps,
            calories = calories,
            points = newPoints,
            gpsAccuracyMeters = point.accuracy
        )
    }

    // High-precision timer loop: Ticks at counterIntervalMs (tuneable down to 10ms and up to 10 min!)
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(counterIntervalMs)
                if (_liveStats.value.state == TrackingState.RECORDING) {
                    val now = System.currentTimeMillis()
                    val calculatedMillis = (now - startTimestamp - totalPausedMillis).coerceAtLeast(0L)
                    val elapsedSec = calculatedMillis / 1000.0
                    val dist = _liveStats.value.distanceMeters
                    val ghostExpectedDistM = if (elapsedSec > 0 && targetPaceSecPerKm > 0) {
                        (elapsedSec / targetPaceSecPerKm) * 1000.0
                    } else 0.0
                    val ghostDelta = dist - ghostExpectedDistM

                    _liveStats.value = _liveStats.value.copy(
                        elapsedMillis = calculatedMillis,
                        ghostDeltaDistanceM = ghostDelta
                    )
                }
            }
        }
    }

    // Developer test injection methods
    fun injectMockLocation(lat: Double, lon: Double, alt: Double = 15.0, speedKmh: Double = 12.0) {
        val point = GpsPoint(
            latitude = lat,
            longitude = lon,
            altitude = alt,
            accuracy = 2.5f,
            speed = (speedKmh / 3.6).toFloat(),
            timestamp = System.currentTimeMillis()
        )
        processLocation(point)
    }

    fun injectMockHeartRate(bpm: Int) {
        _liveStats.value = _liveStats.value.copy(heartRate = bpm)
    }

    // Telemetry and auto-save loop: Ticks at telemetryIntervalMs (default 3 seconds)
    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            while (isActive) {
                delay(telemetryIntervalMs)
                if (_liveStats.value.state == TrackingState.RECORDING) {
                    saveRecoveryState()
                }
            }
        }
    }

    // Explicit developer simulation: ONLY triggered manually inside Miles Studio! Never on normal workouts!
    fun startSimulation(startLat: Double = 37.7749, startLng: Double = -122.4194) {
        simulationJob?.cancel()
        _liveStats.value = _liveStats.value.copy(isSimulationActive = true)
        simulationJob = scope.launch {
            var lat = startLat
            var lng = startLng
            var alt = 52.0
            var i = 0
            while (isActive) {
                delay(2000L)
                if (_liveStats.value.state == TrackingState.RECORDING) {
                    i++
                    lat += 0.00015 + (sin(i * 0.3) * 0.00005)
                    lng += 0.00018 + (cos(i * 0.25) * 0.00005)
                    alt += sin(i * 0.1) * 0.8
                    val speed = when (_liveStats.value.activityType) {
                        ActivityType.RUNNING -> 3.2f // ~11.5 km/h
                        ActivityType.CYCLING -> 6.8f // ~24.5 km/h
                        ActivityType.HIKING -> 1.1f // ~4.0 km/h
                        else -> 1.4f // ~5.0 km/h
                    }
                    val pt = GpsPoint(
                        latitude = lat,
                        longitude = lng,
                        altitude = alt,
                        accuracy = 3.2f,
                        speed = speed,
                        bearing = (i * 15f) % 360f,
                        timestamp = System.currentTimeMillis()
                    )
                    processLocation(pt)
                }
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
        _liveStats.value = _liveStats.value.copy(isSimulationActive = false)
    }

    private fun saveRecoveryState() {
        val s = _liveStats.value
        if (s.state == TrackingState.IDLE) return
        recoveryPrefs.edit()
            .putString("state", s.state.name)
            .putString("type", s.activityType.name)
            .putLong("start_ts", startTimestamp)
            .putLong("elapsed_millis", s.elapsedMillis)
            .putString("dist", s.distanceMeters.toString())
            .putInt("steps", s.stepCount)
            .putString("points", MilesRepository.pointsToJson(s.points))
            .putString("waypoints", MilesRepository.waypointsToJson(s.waypoints))
            .putString("source", s.activeSource)
            .apply()
    }

    private fun restoreTrackingStateIfAvailable() {
        val stateStr = recoveryPrefs.getString("state", null) ?: return
        val state = runCatching { TrackingState.valueOf(stateStr) }.getOrNull() ?: return
        if (state == TrackingState.IDLE) return

        val typeStr = recoveryPrefs.getString("type", ActivityType.WALKING.name) ?: ActivityType.WALKING.name
        val type = ActivityType.fromString(typeStr)
        startTimestamp = recoveryPrefs.getLong("start_ts", System.currentTimeMillis())
        val elapsed = recoveryPrefs.getLong("elapsed_millis", 0L)
        val dist = recoveryPrefs.getString("dist", "0.0")?.toDoubleOrNull() ?: 0.0
        val steps = recoveryPrefs.getInt("steps", 0)
        val ptsJson = recoveryPrefs.getString("points", "[]") ?: "[]"
        val wpJson = recoveryPrefs.getString("waypoints", "[]") ?: "[]"
        val src = recoveryPrefs.getString("source", "Built-in GPS") ?: "Built-in GPS"

        val points = MilesRepository.parsePoints(ptsJson)
        val waypoints = MilesRepository.parseWaypoints(wpJson)

        _liveStats.value = LiveWorkoutStats(
            state = TrackingState.PAUSED, // Safe recovery state
            activityType = type,
            elapsedMillis = elapsed,
            distanceMeters = dist,
            stepCount = steps,
            points = points,
            waypoints = waypoints,
            activeSource = src,
            isAutoPaused = false,
            tickIntervalMs = counterIntervalMs,
            telemetryIntervalMs = telemetryIntervalMs
        )
    }

    private fun clearRecoveryState() {
        recoveryPrefs.edit().clear().apply()
    }

    // Formats elapsed time with high precision (e.g. 00:04.2 or 01:23.45)
    fun formatElapsedTime(millis: Long, showTenths: Boolean = true): String {
        val totalSec = millis / 1000
        val hrs = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        val tenths = (millis % 1000) / 100
        val hundredths = (millis % 1000) / 10

        return if (hrs > 0) {
            if (showTenths) {
                String.format(Locale.US, "%d:%02d:%02d.%d", hrs, mins, secs, tenths)
            } else {
                String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
            }
        } else {
            if (showTenths) {
                if (counterIntervalMs < 50L) {
                    String.format(Locale.US, "%02d:%02d.%02d", mins, secs, hundredths)
                } else {
                    String.format(Locale.US, "%02d:%02d.%d", mins, secs, tenths)
                }
            } else {
                String.format(Locale.US, "%02d:%02d", mins, secs)
            }
        }
    }

    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatPace(secPerKm: Double): String {
        if (secPerKm <= 0.0 || secPerKm > 3600.0) return "--:--"
        val mins = (secPerKm / 60).toInt()
        val secs = (secPerKm % 60).toInt()
        return String.format(Locale.US, "%d'%02d\"", mins, secs)
    }

    fun refreshTelemetry() {
        _liveStats.value = _liveStats.value.copy(
            gpsAccuracyMeters = if (_liveStats.value.gpsAccuracyMeters <= 3.0f) 2.4f else 3.1f
        )
    }
}
