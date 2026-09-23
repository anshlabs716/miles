package com.example.miles.auto

import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.example.R
import com.example.miles.data.local.MilesDatabase
import com.example.miles.data.model.SavedRouteEntity
import com.example.miles.engine.NavigationTtsManager
import com.example.miles.engine.SmartTrackingEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MilesCarNavigationScreen(carContext: CarContext) : Screen(carContext) {

    private var isNavigating: Boolean = false
    private var currentStreet: String = "Market Street"
    private var nextTurnManeuver: String = "Turn Right in 250m"
    private var distanceRemainingMeters: Double = 4200.0
    private var etaMinutes: Int = 14
    private var currentSpeedKmh: Double = 28.5
    private var sampleRoutes: List<SavedRouteEntity> = emptyList()

    init {
        // Query stored routes from Room asynchronously for in-dash display
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val db = MilesDatabase.getInstance(carContext)
                sampleRoutes = db.savedRouteDao().getAllRoutesOnce()
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        return if (isNavigating) {
            buildActiveNavigationTemplate()
        } else {
            buildIdleDashboardTemplate()
        }
    }

    private fun buildActiveNavigationTemplate(): Template {
        val distanceKm = distanceRemainingMeters / 1000.0
        val distText = String.format(Locale.US, "%.1f km", distanceKm)
        val speedText = String.format(Locale.US, "%.1f km/h", currentSpeedKmh)

        val paneBuilder = Pane.Builder()

        // Maneuver row (Large prominent header)
        paneBuilder.addRow(
            Row.Builder()
                .setTitle(nextTurnManeuver)
                .addText("Current Road: $currentStreet")
                .build()
        )

        // ETA & Remaining Distance
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("ETA: $etaMinutes min  •  $distText remaining")
                .addText("Speed: $speedText  •  GPS Lock: Strong")
                .build()
        )

        // Maneuver status row
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Route Progress")
                .addText("Following offline OpenStreetMap track. Recalculating active.")
                .build()
        )

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Stop")
                    .setOnClickListener {
                        isNavigating = false
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Reroute")
                    .setOnClickListener {
                        nextTurnManeuver = "Continue straight for 400m"
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

    private fun buildIdleDashboardTemplate(): Template {
        val paneBuilder = Pane.Builder()

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("MILES Car Dashboard")
                .addText("Offline-first OpenStreetMap Navigation & Fitness")
                .build()
        )

        paneBuilder.addRow(
            Row.Builder()
                .setTitle("1-Tap Quick Start Route")
                .addText("Start in-dash turn-by-turn navigation")
                .build()
        )

        if (sampleRoutes.isNotEmpty()) {
            sampleRoutes.take(3).forEach { route ->
                paneBuilder.addRow(
                    Row.Builder()
                        .setTitle(route.name)
                        .addText("${String.format(Locale.US, "%.1f", route.distanceMeters / 1000.0)} km  •  ${route.activityType}")
                        .build()
                )
            }
        }

        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Start Nav")
                    .setOnClickListener {
                        isNavigating = true
                        currentStreet = "Pine Street"
                        nextTurnManeuver = "Turn Left onto Montgomery St in 350m"
                        invalidate()
                    }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Explore")
                    .setOnClickListener {
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
}
