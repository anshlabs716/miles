package com.example.miles.auto

import android.os.Handler
import android.os.Looper
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.example.miles.data.local.MilesDatabase
import com.example.miles.data.model.ActivityType
import com.example.miles.data.model.SavedRouteEntity
import com.example.miles.engine.LiveWorkoutStats
import com.example.miles.engine.SmartTrackingEngine
import com.example.miles.engine.TrackingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Android Auto screen. This is NOT a mock — every value it shows comes from the
 * live [SmartTrackingEngine] (real GPS speed, real distance, real route progress)
 * and from saved routes stored on-device in Room.
 */
class MilesCarNavigationScreen(carContext: CarContext) : Screen(carContext) {

    // Process-wide engine: if the phone Activity already created it we share that
    // instance, otherwise we lazily create one (same DB, same state).
    private val engine: SmartTrackingEngine = SmartTrackingEngine.getInstance(carContext)

    private var isNavigating: Boolean = false
    private var selectedRoute: SavedRouteEntity? = null
    private var sampleRoutes: List<SavedRouteEntity> = emptyList()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            if (isNavigating) {
                invalidate()
                mainHandler.postDelayed(this, 2000L)
            }
        }
    }

    init {
        // Query stored routes from Room asynchronously for in-dash display.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val db = MilesDatabase.getInstance(carContext)
                sampleRoutes = db.savedRouteDao().getAllRoutesOnce()
            }
            mainHandler.post { invalidate() }
        }
    }

    override fun onGetTemplate(): Template {
        val stats = engine.liveStats.value
        return if (isNavigating || stats.state != TrackingState.IDLE) {
            buildActiveNavigationTemplate(stats)
        } else {
            buildIdleDashboardTemplate(stats)
        }
    }

    // ------------------------------------------------------------------
    // Active navigation (live data from the tracking engine)
    // ------------------------------------------------------------------
    private fun buildActiveNavigationTemplate(stats: LiveWorkoutStats): Template {
        val paneBuilder = Pane.Builder()

        val speedKmh = stats.currentSpeedKmh
        val speedText = if (speedKmh > 0.05) {
            String.format(Locale.US, "%.1f km/h", speedKmh)
        } else {
            "0.0 km/h"
        }

        val remainingKm = stats.navRemainingDistanceM / 1000.0
        val remainingText = if (stats.navRemainingDistanceM >= 1000) {
            String.format(Locale.US, "%.1f km", remainingKm)
        } else {
            "${stats.navRemainingDistanceM.roundToInt()} m"
        }

        val etaMin = if (stats.navRemainingDistanceM <= 0) {
            null
        } else {
            val estSpeed = when {
                speedKmh > 2.0 -> speedKmh
                stats.avgSpeedKmh > 1.0 -> stats.avgSpeedKmh
                else -> 5.0 // walking fallback while GPS warms up
            }
            (stats.navRemainingDistanceM / 1000.0 / estSpeed * 60.0).roundToInt()
        }
        val etaText = etaMin?.let { "ETA: ${it} min" } ?: "ETA: — min"

        // Maneuver row (prominent header) — real, from the engine's navigation state.
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(maneuverFor(stats))
                .addText("Speed: $speedText  •  GPS ±${stats.gpsAccuracyMeters.toInt()}m")
                .build()
        )

        // Progress row — real remaining distance / progress / ETA.
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("$etaText  •  $remainingText remaining")
                .addText("Progress: ${stats.navProgressPercent.roundToInt()}%  •  ${distanceTraveledText(stats)}")
                .build()
        )

        // Route row — the actual route being followed.
        val routeName = stats.activeNavRoute?.name
            ?: selectedRoute?.name
            ?: "Free Tracking"
        val routeDetail = listOfNotNull(
            stats.navTargetPoints.size.takeIf { it > 0 }?.let { "$it points" },
            stats.activeSource.takeIf { it.isNotBlank() }
        ).joinToString("  •  ")
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(routeName)
                .addText(if (routeDetail.isBlank()) routeName else routeDetail)
                .build()
        )

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Stop")
                    .setOnClickListener {
                        engine.stopNavigation()
                        isNavigating = false
                        mainHandler.removeCallbacks(refreshRunnable)
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Reroute")
                    .setOnClickListener {
                        val route = stats.activeNavRoute ?: selectedRoute
                        if (route != null) engine.setNavigationRoute(route)
                        invalidate()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("MILES In-Dash Navigation")
            .setActionStrip(actionStrip)
            .setHeaderAction(Action.BACK)
            .build()
    }

    // ------------------------------------------------------------------
    // Idle dashboard (real saved routes + live GPS status)
    // ------------------------------------------------------------------
    private fun buildIdleDashboardTemplate(stats: LiveWorkoutStats): Template {
        val paneBuilder = Pane.Builder()

        // Live status row — reflects what the phone app is doing right now.
        if (stats.state != TrackingState.IDLE) {
            val statusText = if (stats.state == TrackingState.PAUSED) "Paused" else "Tracking in progress"
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle(statusText)
                    .addText("${distanceTraveledText(stats)}  •  ${String.format(Locale.US, "%.1f km/h", stats.currentSpeedKmh)}")
                    .setOnClickListener {
                        isNavigating = true
                        mainHandler.post(refreshRunnable)
                        invalidate()
                    }
                    .build()
            )
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("Ready — no active workout")
                    .addText("MILES live on this car screen via real GPS tracking")
                    .build()
            )
        }

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("1-Tap Start Route")
                .addText("Start in-dash turn-by-turn navigation from a saved route")
                .build()
        )

        if (sampleRoutes.isNotEmpty()) {
            sampleRoutes.take(4).forEach { route ->
                val kmText = String.format(Locale.US, "%.1f km", route.distanceMeters / 1000.0)
                val activityLabel = runCatching { ActivityType.fromString(route.activityType).displayName }
                    .getOrDefault(route.activityType)
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle(route.name)
                        .addText("$kmText  •  $activityLabel")
                        .setOnClickListener {
                            startNavigation(route)
                        }
                        .build()
                )
            }
        } else {
            paneBuilder.addRow(
                Row.Builder()
                    .setTitle("No saved routes yet")
                    .addText("Save a route in the MILES app on your phone first")
                    .build()
            )
        }

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Refresh")
                    .setOnClickListener {
                        reloadRoutes()
                        invalidate()
                    }
                    .build()
            )
            .build()

        return PaneTemplate.Builder(paneBuilder.build())
            .setTitle("MILES — Android Auto")
            .setActionStrip(actionStrip)
            .build()
    }

    // ------------------------------------------------------------------
    // Navigation helpers
    // ------------------------------------------------------------------
    private fun startNavigation(route: SavedRouteEntity) {
        selectedRoute = route
        engine.setNavigationRoute(route)
        engine.startTracking(ActivityType.fromString(route.activityType), "Android Auto")
        isNavigating = true
        mainHandler.removeCallbacks(refreshRunnable)
        mainHandler.post(refreshRunnable)
        invalidate()
    }

    private fun reloadRoutes() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val db = MilesDatabase.getInstance(carContext)
                sampleRoutes = db.savedRouteDao().getAllRoutesOnce()
            }
            mainHandler.post { invalidate() }
        }
    }

    /** "Waypoint / Turn Left in 250 m" — real nav state, with geometry fallback. */
    private fun maneuverFor(stats: LiveWorkoutStats): String {
        val distM = stats.navDistanceToNextM
        val distText = if (distM >= 1000) {
            String.format(Locale.US, "%.1f km", distM / 1000.0)
        } else {
            "${distM.roundToInt()} m"
        }
        val actionText = if (stats.navNextWaypointName.isNotBlank()) {
            stats.navNextWaypointName
        } else {
            deriveManeuver(stats)
        }
        return "$actionText in $distText"
    }

    /** Derive a turn instruction from the route geometry when no named waypoint exists. */
    private fun deriveManeuver(stats: LiveWorkoutStats): String {
        val pts = stats.navTargetPoints
        val idx = stats.navNextPointIndex
        if (pts.size < idx + 2 || idx < 1) return "Continue on route"
        val p0 = pts[idx - 1]
        val p1 = pts[idx]
        val p2 = pts[idx + 1]
        val b1 = bearingLatLng(p0.latitude, p0.longitude, p1.latitude, p1.longitude)
        val b2 = bearingLatLng(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
        val diff = (b2 - b1 + 540.0) % 360.0 - 180.0
        return when {
            diff < -40 -> "Turn left"
            diff > 40 -> "Turn right"
            diff < -12 -> "Bear left"
            diff > 12 -> "Bear right"
            else -> "Continue straight"
        }
    }

    private fun bearingLatLng(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val la1 = Math.toRadians(lat1)
        val la2 = Math.toRadians(lat2)
        val y = Math.sin(dLon) * Math.cos(la2)
        val x = Math.cos(la1) * Math.sin(la2) - Math.sin(la1) * Math.cos(la2) * Math.cos(dLon)
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0
    }

    private fun distanceTraveledText(stats: LiveWorkoutStats): String {
        val m = stats.distanceMeters
        return if (m >= 1000) {
            String.format(Locale.US, "%.2f km traveled", m / 1000.0)
        } else {
            "${m.roundToInt()} m traveled"
        }
    }
}