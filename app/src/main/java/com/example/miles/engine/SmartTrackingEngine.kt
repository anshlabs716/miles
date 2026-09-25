package com.example.miles.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.miles.data.local.MilesDatabase
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.model.GpsPoint
import com.example.miles.data.model.SavedRouteEntity
import com.example.miles.data.model.Waypoint
import com.example.miles.data.model.WorkoutInterval
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
import kotlin.math.atan2
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
    val telemetryIntervalMs: Long = 3000L,

    // Route Navigation
    val activeNavRoute: SavedRouteEntity? = null,
    val navTargetPoints: List<GpsPoint> = emptyList(),
    val navNextPointIndex: Int = 0,
    val navDistanceToNextM: Double = 0.0,
    val navNextWaypointName: String = "",
    val navBearingDeg: Float = 0f,
    val navCrossTrackErrorM: Double = 0.0,
    val navIsOffRoute: Boolean = false,
    val navRemainingDistanceM: Double = 0.0,
    val navProgressPercent: Float = 0f,

    // Progressive Interval Workout
    val activeIntervalPlanName: String? = null,
    val intervalIndex: Int = 0,
    val totalIntervals: Int = 0,
    val currentIntervalLabel: String = "",
    val currentIntervalType: String = "",
    val currentIntervalRemainingSeconds: Int = 0,
    val currentIntervalTotalSeconds: Int = 0
) {
    val elapsedSeconds: Long get() = elapsedMillis / 1000L
}

sealed class TrackingEvent {
    data class MilestoneReached(val title: String, val message: String) : TrackingEvent()
    data class AutoPauseTriggered(val isPaused: Boolean) : TrackingEvent()
    data class AnomalyDetected(val message: String) : TrackingEvent()
    data class IntervalChanged(val title: String, val instruction: String) : TrackingEvent()
    data class NavigationAlert(val message: String, val isWarning: Boolean = false) : TrackingEvent()
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
            targetAlertManager.config = targetAlertManager.config.copy(targetPaceSecPerKm = field)
        }

    val navigationTts = NavigationTtsManager(context)
    val targetAlertManager = WorkoutTargetAlertManager(context, navigationTts)

    // Kalman / smoothing state
    private var lastFilteredPoint: GpsPoint? = null

    init {
        restoreTrackingStateIfAvailable()
    }

    companion object {
        @Volatile
        private var instance: SmartTrackingEngine? = null

        /**
         * Returns the process-wide tracking engine, creating it lazily if needed.
         */
        fun getInstance(context: Context, repository: MilesRepository? = null): SmartTrackingEngine {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: run {
                    val appContext = context.applicationContext
                    val db = MilesDatabase.getInstance(appContext)
                    val repo = repository ?: MilesRepository(
                        db.activityDao(),
                        db.savedRouteDao(),
                        db.privacyZoneDao(),
                        db.goalDao()
                    )
                    SmartTrackingEngine(appContext, repo).also { instance = it }
                }
            }
        }
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

        val finalCalories = calculateCalories(current.activityType, current.elapsedSeconds, current.distanceMeters, current.stepCount).coerceAtLeast(current.calories)
        val finalSteps = if (current.stepCount > 0) current.stepCount else (current.distanceMeters / 0.76).toInt()

        val entity = ActivityEntity(
            id = UUID.randomUUID().toString(),
            title = finalTitle,
            activityType = current.activityType.name,
            startTime = startTimestamp,
            endTime = endTs,
            durationSeconds = current.elapsedSeconds,
            distanceMeters = current.distanceMeters,
            steps = finalSteps,
            avgPaceSecPerKm = current.avgPaceSecPerKm,
            bestPaceSecPerKm = (current.avgPaceSecPerKm * 0.9).coerceAtLeast(180.0),
            avgSpeedKmh = current.avgSpeedKmh,
            maxSpeedKmh = (current.avgSpeedKmh * 1.35).coerceAtLeast(current.avgSpeedKmh),
            elevationGainM = current.elevationGainM,
            elevationLossM = current.elevationLossM,
            calories = finalCalories,
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

        val settingsPrefs = context.getSharedPreferences("miles_settings", Context.MODE_PRIVATE)
        val curCal = settingsPrefs.getInt("today_workout_calories", 0)
        val curMin = settingsPrefs.getInt("today_workout_duration_min", 0)
        val curDist = settingsPrefs.getFloat("today_workout_distance_m", 0f)
        val todayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        settingsPrefs.edit()
            .putString("today_workout_date", todayKey)
            .putInt("today_workout_calories", curCal + finalCalories)
            .putInt("today_workout_duration_min", curMin + (current.elapsedSeconds / 60).toInt())
            .putFloat("today_workout_distance_m", curDist + current.distanceMeters.toFloat())
            .apply()
        runCatching {
            com.example.miles.widget.MilesWidgetUpdater.updateAllWidgets(
                context = context,
                calories = 0,
                calGoal = 0,
                steps = 0,
                stepGoal = 0,
                activeMin = 0,
                activeGoal = 0
            )
        }

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

    // Active Route Navigation state
    private var activeNavRouteEntity: SavedRouteEntity? = null
    private var targetNavPoints: List<GpsPoint> = emptyList()
    private var targetNavWaypoints: List<Waypoint> = emptyList()

    // Active Interval Workout state
    private var activeIntervals: List<WorkoutInterval> = emptyList()
    private var currentIntervalIdx: Int = 0
    private var currentIntervalRemainingSec: Int = 0
    private var lastIntervalSecondTick: Long = 0L

    fun setNavigationRoute(route: SavedRouteEntity?) {
        activeNavRouteEntity = route
        if (route == null) {
            targetNavPoints = emptyList()
            targetNavWaypoints = emptyList()
            _liveStats.value = _liveStats.value.copy(
                activeNavRoute = null,
                navTargetPoints = emptyList(),
                navDistanceToNextM = 0.0,
                navNextWaypointName = "",
                navBearingDeg = 0f,
                navCrossTrackErrorM = 0.0,
                navIsOffRoute = false,
                navRemainingDistanceM = 0.0,
                navProgressPercent = 0f
            )
            return
        }

        val points = MilesRepository.parsePoints(route.routePointsJson)
        val waypoints = MilesRepository.parseWaypoints(route.waypointsJson)
        targetNavPoints = points
        targetNavWaypoints = waypoints

        _liveStats.value = _liveStats.value.copy(
            activeNavRoute = route,
            navTargetPoints = points,
            navRemainingDistanceM = route.distanceMeters,
            navNextWaypointName = waypoints.firstOrNull()?.name ?: "Waypoint 1",
            navProgressPercent = 0f
        )
        _events.tryEmit(TrackingEvent.NavigationAlert("Started navigation on ${route.name}"))
    }

    fun stopNavigation() {
        setNavigationRoute(null)
    }

    fun startIntervalWorkout(
        planTitle: String,
        intervals: List<WorkoutInterval>,
        activityType: ActivityType = ActivityType.RUNNING
    ) {
        if (intervals.isEmpty()) return
        activeIntervals = intervals
        currentIntervalIdx = 0
        val first = intervals.first()
        currentIntervalRemainingSec = first.durationSeconds
        lastIntervalSecondTick = System.currentTimeMillis()

        if (_liveStats.value.state != TrackingState.RECORDING) {
            startTracking(activityType)
        }

        _liveStats.value = _liveStats.value.copy(
            activeIntervalPlanName = planTitle,
            intervalIndex = 0,
            totalIntervals = intervals.size,
            currentIntervalLabel = first.instruction.ifBlank { "${first.type.label} (${first.durationSeconds / 60}m)" },
            currentIntervalType = first.type.name,
            currentIntervalRemainingSeconds = first.durationSeconds,
            currentIntervalTotalSeconds = first.durationSeconds
        )

        _events.tryEmit(TrackingEvent.IntervalChanged(first.type.label, first.instruction))
    }

    private fun appendValidatedPoint(point: GpsPoint) {
        val current = _liveStats.value
        val newPoints = current.points + point

        var totalDist = current.distanceMeters
        var addedElevGain = 0.0
        var addedElevLoss = 0.0

        var effectiveCurrentSpeedKmh = (point.speed * 3.6).toDouble()
        var effectiveCurrentPaceSec = if (point.speed > 0.2f) (1000.0 / point.speed) else 0.0

        if (current.points.isNotEmpty()) {
            val last = current.points.last()
            val legDist = MilesRepository.calculateDistanceMeters(last.latitude, last.longitude, point.latitude, point.longitude)

            // Stationary noise & GPS drift deadband filter:
            // Tolerant of realistic mobile GPS accuracies (up to 65m) and indoor/urban canyon signals
            // Only reject extreme outliers (> 70m with negligible leg distance) or true sub-decimeter jitter (< 0.35m)
            val isStationaryNoise = (point.accuracy > 70.0f && legDist < 5.0) || (legDist < 0.35)

            if (isStationaryNoise) {
                // Device is stationary: update current altitude and GPS accuracy without accumulating artificial drift
                _liveStats.value = current.copy(
                    gpsAccuracyMeters = point.accuracy,
                    currentElevationM = point.altitude,
                    currentSpeedKmh = if (point.speed > 0.3f) (point.speed * 3.6).toDouble() else 0.0,
                    currentPaceSecPerKm = if (point.speed > 0.3f) (1000.0 / point.speed) else 0.0
                )
                return
            }

            totalDist = current.distanceMeters + legDist

            // Calculate derived speed when device/emulator does not populate location.speed
            val timeDeltaSec = ((point.timestamp - last.timestamp) / 1000.0).coerceIn(0.1, 15.0)
            val derivedSpeedMps = if (timeDeltaSec > 0.1) legDist / timeDeltaSec else 0.0
            val effectiveSpeedMps = if (point.speed > 0.2f) point.speed.toDouble() else derivedSpeedMps
            effectiveCurrentSpeedKmh = effectiveSpeedMps * 3.6
            effectiveCurrentPaceSec = if (effectiveSpeedMps > 0.25) (1000.0 / effectiveSpeedMps) else 0.0

            val elevDiff = point.altitude - last.altitude
            if (elevDiff > 0.6) addedElevGain = elevDiff
            else if (elevDiff < -0.6) addedElevLoss = abs(elevDiff)
        }

        val elapsed = current.elapsedSeconds
        val avgSpeedKmh = if (elapsed > 0) (totalDist / elapsed) * 3.6 else 0.0
        val avgPaceSec = if (totalDist > 20.0 && elapsed > 0) (elapsed / (totalDist / 1000.0)) else 0.0
        val currentSpeedKmh = effectiveCurrentSpeedKmh
        val currentPaceSec = effectiveCurrentPaceSec

        val estimatedSteps = (totalDist / 0.76).toInt()
        val stepCount = maxOf(current.stepCount, estimatedSteps)
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
            navigationTts.speak("Milestone reached: $currentKm kilometers. Pace: $paceStr per kilometer.")
        }

        // Live Target Zone alerts (pace, speed, heart rate)
        targetAlertManager.evaluateMetrics(
            paceSecPerKm = if (currentPaceSec > 0) currentPaceSec else avgPaceSec,
            speedKmh = currentSpeedKmh,
            heartRateBpm = if (current.heartRate > 0) current.heartRate else null
        )

        // Route Navigation Calculations
        var navCrossTrackError = current.navCrossTrackErrorM
        var navIsOffRoute = current.navIsOffRoute
        var navDistToNext = current.navDistanceToNextM
        var navBearing = current.navBearingDeg
        var navRemainingDist = current.navRemainingDistanceM
        var navProgress = current.navProgressPercent
        var navNextWpName = current.navNextWaypointName

        if (targetNavPoints.isNotEmpty()) {
            // Find closest point on planned route
            var closestIdx = 0
            var minDist = Double.MAX_VALUE
            for (i in targetNavPoints.indices) {
                val d = MilesRepository.calculateDistanceMeters(
                    point.latitude, point.longitude,
                    targetNavPoints[i].latitude, targetNavPoints[i].longitude
                )
                if (d < minDist) {
                    minDist = d
                    closestIdx = i
                }
            }

            navCrossTrackError = minDist
            val wasOffRoute = navIsOffRoute
            navIsOffRoute = minDist > 35.0 // More than 35m off path

            if (navIsOffRoute && !wasOffRoute) {
                _events.tryEmit(TrackingEvent.NavigationAlert("⚠️ Off-Route (${minDist.toInt()}m from path) - Return to route!", true))
                navigationTts.announceRerouting()
            } else if (!navIsOffRoute && wasOffRoute) {
                _events.tryEmit(TrackingEvent.NavigationAlert("Back on track! ✓"))
                navigationTts.speak("Back on track.")
            }

            val nextIdx = (closestIdx + 1).coerceAtMost(targetNavPoints.size - 1)
            val nextTargetPoint = targetNavPoints[nextIdx]
            navDistToNext = MilesRepository.calculateDistanceMeters(
                point.latitude, point.longitude,
                nextTargetPoint.latitude, nextTargetPoint.longitude
            )

            // Bearing from current position to next route point
            val lat1 = Math.toRadians(point.latitude)
            val lon1 = Math.toRadians(point.longitude)
            val lat2 = Math.toRadians(nextTargetPoint.latitude)
            val lon2 = Math.toRadians(nextTargetPoint.longitude)
            val dLon = lon2 - lon1
            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
            val brng = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
            navBearing = brng.toFloat()

            // Calculate remaining distance along route from closest point to end
            var rem = 0.0
            for (i in closestIdx until targetNavPoints.size - 1) {
                rem += MilesRepository.calculateDistanceMeters(
                    targetNavPoints[i].latitude, targetNavPoints[i].longitude,
                    targetNavPoints[i + 1].latitude, targetNavPoints[i + 1].longitude
                )
            }
            navRemainingDist = rem
            val totalRouteDist = activeNavRouteEntity?.distanceMeters ?: (rem + totalDist).coerceAtLeast(1.0)
            navProgress = ((totalRouteDist - rem) / totalRouteDist).toFloat().coerceIn(0f, 1f)

            // Named waypoints if present
            val nextWp = targetNavWaypoints.firstOrNull { wp ->
                MilesRepository.calculateDistanceMeters(point.latitude, point.longitude, wp.latitude, wp.longitude) > 20.0
            }
            if (nextWp != null) navNextWpName = nextWp.name
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
            stepCount = stepCount,
            calories = calories,
            points = newPoints,
            gpsAccuracyMeters = point.accuracy,
            navCrossTrackErrorM = navCrossTrackError,
            navIsOffRoute = navIsOffRoute,
            navDistanceToNextM = navDistToNext,
            navBearingDeg = navBearing,
            navRemainingDistanceM = navRemainingDist,
            navProgressPercent = navProgress,
            navNextWaypointName = navNextWpName
        )
    }

    fun calculateCalories(activityType: ActivityType, elapsedSeconds: Long, distanceMeters: Double, steps: Int): Int {
        val perMetreRate = when (activityType) {
            ActivityType.RUNNING -> 0.065
            ActivityType.CYCLING -> 0.035
            ActivityType.HIKING -> 0.055
            else -> 0.045
        }
        return CalorieEstimator.activityCalories(
            met = activityType.metScore,
            weightKg = bodyWeightKg(),
            elapsedSeconds = elapsedSeconds,
            distanceMeters = distanceMeters,
            steps = steps,
            perMetreRate = perMetreRate
        )
    }

    /** The user's real body weight (kg) from settings, so estimates are personal. */
    private fun bodyWeightKg(): Int =
        context.getSharedPreferences("miles_settings", Context.MODE_PRIVATE)
            .getInt("body_weight_kg", 70)

    fun processStepDelta(delta: Int) {
        if (_liveStats.value.state != TrackingState.RECORDING || delta <= 0) return
        val current = _liveStats.value
        val newSteps = current.stepCount + delta
        val addedDist = if (current.points.isEmpty()) delta * 0.76 else 0.0
        val newDist = current.distanceMeters + addedDist
        val currentCals = calculateCalories(current.activityType, current.elapsedSeconds, newDist, newSteps)
        _liveStats.value = current.copy(
            stepCount = newSteps,
            distanceMeters = newDist,
            calories = currentCals
        )
    }

    fun setActiveSource(source: String) {
        _liveStats.value = _liveStats.value.copy(activeSource = source)
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
                    val liveCals = calculateCalories(_liveStats.value.activityType, (calculatedMillis / 1000L), dist, _liveStats.value.stepCount)

                    // Interval workout progression tick (once per second)
                    if (activeIntervals.isNotEmpty() && now - lastIntervalSecondTick >= 1000L) {
                        lastIntervalSecondTick = now
                        if (currentIntervalRemainingSec > 1) {
                            currentIntervalRemainingSec--
                            _liveStats.value = _liveStats.value.copy(
                                currentIntervalRemainingSeconds = currentIntervalRemainingSec
                            )
                        } else {
                            // Advance to next interval
                            if (currentIntervalIdx < activeIntervals.size - 1) {
                                currentIntervalIdx++
                                val next = activeIntervals[currentIntervalIdx]
                                currentIntervalRemainingSec = next.durationSeconds
                                _liveStats.value = _liveStats.value.copy(
                                    intervalIndex = currentIntervalIdx,
                                    currentIntervalLabel = next.instruction.ifBlank { "${next.type.label} (${next.durationSeconds / 60}m)" },
                                    currentIntervalType = next.type.name,
                                    currentIntervalRemainingSeconds = next.durationSeconds,
                                    currentIntervalTotalSeconds = next.durationSeconds
                                )
                                _events.tryEmit(TrackingEvent.IntervalChanged(next.type.label, next.instruction))
                                navigationTts.announceIntervalChange(next.type.label, next.durationSeconds, next.instruction)
                            } else {
                                // Workout complete
                                _events.tryEmit(TrackingEvent.MilestoneReached("Interval Workout Complete! 🎉", "Great job finishing your structured session!"))
                                navigationTts.speak("Interval workout complete! Great job finishing your structured session.")
                            }
                        }
                    }

                    _liveStats.value = _liveStats.value.copy(
                        elapsedMillis = calculatedMillis,
                        ghostDeltaDistanceM = ghostDelta,
                        calories = liveCals
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
