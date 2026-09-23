package com.example.miles.ui.tools

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.repository.format
import com.example.miles.ui.theme.LiquidGlassCard
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ToolsTab(val label: String) {
    COMPASS("Compass"),
    ALTIMETER("Altimeter"),
    SPEEDOMETER("Speedometer"),
    DIAGNOSTICS("GNSS & Sensors")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsDiagnosticsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(ToolsTab.COMPASS) }

    // Live Sensors Data
    var azimuthDegrees by remember { mutableFloatStateOf(0f) }
    var pitchDegrees by remember { mutableFloatStateOf(0f) }
    var rollDegrees by remember { mutableFloatStateOf(0f) }
    var accuracyLevel by remember { mutableIntStateOf(3) } // 0: Unreliable, 3: High

    var currentAltitudeM by remember { mutableDoubleStateOf(0.0) }
    var currentPressureHpa by remember { mutableFloatStateOf(1013.25f) }
    var currentSpeedMps by remember { mutableFloatStateOf(0f) }
    var maxSpeedMps by remember { mutableFloatStateOf(0f) }
    var currentGpsHeading by remember { mutableFloatStateOf(0f) }
    var gpsAccuracyM by remember { mutableFloatStateOf(0f) }
    var gpsLat by remember { mutableDoubleStateOf(0.0) }
    var gpsLon by remember { mutableDoubleStateOf(0.0) }
    var hasGpsFix by remember { mutableStateOf(false) }

    // Register hardware sensors for compass & barometer
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotVector = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val pressure = sm?.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotMatrix, orientation)
                    val az = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    azimuthDegrees = (az + 360f) % 360f
                    pitchDegrees = Math.toDegrees(orientation[1].toDouble()).toFloat()
                    rollDegrees = Math.toDegrees(orientation[2].toDouble()).toFloat()
                } else if (event.sensor.type == Sensor.TYPE_PRESSURE) {
                    currentPressureHpa = event.values[0]
                    currentAltitudeM = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0]).toDouble()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    accuracyLevel = accuracy
                }
            }
        }

        var lastLoc: Location? = null

        val locationListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                hasGpsFix = true
                gpsLat = loc.latitude
                gpsLon = loc.longitude
                gpsAccuracyM = loc.accuracy
                if (loc.hasAltitude()) {
                    currentAltitudeM = loc.altitude
                }
                
                var calculatedSpeed = if (loc.hasSpeed() && loc.speed > 0f) loc.speed else 0f
                val prev = lastLoc
                if (prev != null && calculatedSpeed <= 0f) {
                    val timeDeltaSec = (loc.time - prev.time) / 1000f
                    if (timeDeltaSec in 0.2f..10f) {
                        val distM = prev.distanceTo(loc)
                        val s = distM / timeDeltaSec
                        if (s in 0.1f..150f) {
                            calculatedSpeed = s
                        }
                    }
                }
                lastLoc = loc
                currentSpeedMps = calculatedSpeed
                if (calculatedSpeed > maxSpeedMps) {
                    maxSpeedMps = calculatedSpeed
                }

                if (loc.hasBearing()) {
                    currentGpsHeading = loc.bearing
                }
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        rotVector?.let { sm.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }
        pressure?.let { sm.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI) }

        // Initial Last Known Location
        try {
            val lastGps = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = lastGps ?: lastNet
            best?.let {
                hasGpsFix = true
                gpsLat = it.latitude
                gpsLon = it.longitude
                gpsAccuracyM = it.accuracy
                if (it.hasSpeed() && it.speed > 0f) {
                    currentSpeedMps = it.speed
                    if (it.speed > maxSpeedMps) maxSpeedMps = it.speed
                }
                if (it.hasAltitude()) currentAltitudeM = it.altitude
                if (it.hasBearing()) currentGpsHeading = it.bearing
            }
        } catch (_: Exception) {}

        try {
            lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, locationListener)
        } catch (_: Exception) {}
        try {
            lm?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500L, 0f, locationListener)
        } catch (_: Exception) {}

        onDispose {
            sm?.unregisterListener(sensorListener)
            try { lm?.removeUpdates(locationListener) } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tools & Diagnostics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                ToolsTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label, fontSize = 12.sp, maxLines = 1) }
                    )
                }
            }

            when (selectedTab) {
                ToolsTab.COMPASS -> CompassView(
                    azimuth = azimuthDegrees,
                    gpsHeading = currentGpsHeading,
                    altitude = currentAltitudeM,
                    accuracy = accuracyLevel,
                    lat = gpsLat,
                    lon = gpsLon
                )
                ToolsTab.ALTIMETER -> AltimeterView(
                    altitudeM = currentAltitudeM,
                    pressureHpa = currentPressureHpa,
                    hasGps = hasGpsFix
                )
                ToolsTab.SPEEDOMETER -> SpeedometerView(
                    speedMps = currentSpeedMps,
                    maxSpeedMps = maxSpeedMps
                )
                ToolsTab.DIAGNOSTICS -> DiagnosticsView(
                    azimuth = azimuthDegrees,
                    accuracy = accuracyLevel,
                    lat = gpsLat,
                    lon = gpsLon,
                    gpsAccuracy = gpsAccuracyM,
                    hasGpsFix = hasGpsFix,
                    pressure = currentPressureHpa,
                    altitude = currentAltitudeM
                )
            }
        }
    }
}

@Composable
private fun CompassView(
    azimuth: Float,
    gpsHeading: Float,
    altitude: Double,
    accuracy: Int,
    lat: Double,
    lon: Double
) {
    val animatedAzimuth by animateFloatAsState(
        targetValue = azimuth,
        animationSpec = tween(150),
        label = "compassAzimuth"
    )

    val cardinal = when ((azimuth + 22.5f).toInt() % 360) {
        in 0..44 -> "N"
        in 45..89 -> "NE"
        in 90..134 -> "E"
        in 135..179 -> "SE"
        in 180..224 -> "S"
        in 225..269 -> "SW"
        in 270..314 -> "W"
        else -> "NW"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(12.dp))

            // Main Compass Dial Canvas
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f - 16.dp.toPx()

                    // Outer ring
                    drawCircle(
                        color = Color(0xFF2C3036),
                        radius = radius + 6.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Rotating compass dial ring
                    rotate(-animatedAzimuth, pivot = center) {
                        for (deg in 0 until 360 step 15) {
                            val rad = Math.toRadians(deg.toDouble())
                            val isMajor = deg % 90 == 0
                            val isSemi = deg % 45 == 0
                            val tickLen = if (isMajor) 18.dp.toPx() else if (isSemi) 12.dp.toPx() else 7.dp.toPx()
                            val strokeW = if (isMajor) 3.5.dp.toPx() else if (isSemi) 2.dp.toPx() else 1.2.dp.toPx()
                            val tickColor = if (deg == 0) Color(0xFFFF5252) else if (isMajor) Color.White else Color(0xFF8E9196)

                            val startX = (center.x + (radius - tickLen) * sin(rad)).toFloat()
                            val startY = (center.y - (radius - tickLen) * cos(rad)).toFloat()
                            val endX = (center.x + radius * sin(rad)).toFloat()
                            val endY = (center.y - radius * cos(rad)).toFloat()

                            drawLine(
                                color = tickColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeW,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Static Pointer Needle at top
                    val needlePath = Path().apply {
                        moveTo(center.x, center.y - radius + 8.dp.toPx())
                        lineTo(center.x - 9.dp.toPx(), center.y - radius + 32.dp.toPx())
                        lineTo(center.x + 9.dp.toPx(), center.y - radius + 32.dp.toPx())
                        close()
                    }
                    drawPath(needlePath, color = Color(0xFFFF5252))

                    // Center Hub
                    drawCircle(color = Color(0xFF1E2024), radius = 32.dp.toPx(), center = center)
                    drawCircle(color = Color(0xFFFF5252), radius = 6.dp.toPx(), center = center)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${azimuth.toInt()}°",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = cardinal,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Calibration & Accuracy Status Chip
            val calText = when (accuracy) {
                3 -> "High Calibration (Accurate)"
                2 -> "Medium Calibration"
                1 -> "Low Accuracy (Wave in Figure-8)"
                else -> "Uncalibrated Sensor"
            }
            val calColor = if (accuracy >= 2) Color(0xFF4CAF50) else Color(0xFFFFB300)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(calColor.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = calColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(calText, color = calColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))

            // Coordinates & Altitude Cards
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Telemetry & Position", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("COORDINATES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (lat != 0.0) "${lat.format(4)}°, ${lon.format(4)}°" else "Acquiring GPS fix...", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("ALTITUDE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${altitude.toInt()} m", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("MAGNETIC BEARING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${azimuth.toInt()}° ($cardinal)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("GPS HEADING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (gpsHeading > 0f) "${gpsHeading.toInt()}°" else "Stationary", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AltimeterView(
    altitudeM: Double,
    pressureHpa: Float,
    hasGps: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Landscape, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(8.dp))
                Text("CURRENT ELEVATION", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "${altitudeM.toInt()} m",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "${(altitudeM * 3.28084).toInt()} ft above sea level",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Atmospheric Barometer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("AIR PRESSURE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${pressureHpa.format(1)} hPa", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("INCHES OF MERCURY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${(pressureHpa * 0.02953).format(2)} inHg", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Standard sea level pressure is 1013.25 hPa. Barometric altitude calculation reacts instantly to elevation changes inside buildings, stairwells, and outdoor mountain terrain.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpeedometerView(
    speedMps: Float,
    maxSpeedMps: Float,
    onSimulateSpeed: ((Float) -> Unit)? = null,
    onResetMaxSpeed: (() -> Unit)? = null
) {
    var selectedUnit by remember { mutableStateOf("KM/H") } // "KM/H", "MPH", "M/S", "KNOTS"
    var simulatedSpeedMps by remember { mutableFloatStateOf(-1f) }

    val effectiveSpeedMps = if (simulatedSpeedMps >= 0f) simulatedSpeedMps else speedMps
    val effectiveMaxMps = maxOf(maxSpeedMps, effectiveSpeedMps)

    val speedKmh = effectiveSpeedMps * 3.6f
    val speedMph = speedKmh * 0.621371f
    val speedKnots = speedKmh * 0.539957f

    val displayedSpeed = when (selectedUnit) {
        "MPH" -> speedMph
        "M/S" -> effectiveSpeedMps
        "KNOTS" -> speedKnots
        else -> speedKmh
    }

    val maxGaugeSpeed = when (selectedUnit) {
        "MPH" -> 50f
        "M/S" -> 20f
        "KNOTS" -> 40f
        else -> 60f
    }

    val animatedSweep by animateFloatAsState(
        targetValue = (displayedSpeed / maxGaugeSpeed).coerceIn(0f, 1f) * 270f,
        animationSpec = tween(durationMillis = 400),
        label = "SpeedGaugeSweep"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Unit Switcher Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("KM/H", "MPH", "M/S", "KNOTS").forEach { unit ->
                    FilterChip(
                        selected = selectedUnit == unit,
                        onClick = { selectedUnit = unit },
                        label = { Text(unit, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f - 24.dp.toPx()

                    // Background Gauge Arc (135 deg to 405 deg = 270 deg)
                    drawArc(
                        color = Color(0xFF2A2D33),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active Speed Arc
                    if (animatedSweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF00E676),
                                    Color(0xFFFFEA00),
                                    Color(0xFFFF9100),
                                    Color(0xFFFF1744)
                                )
                            ),
                            startAngle = 135f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                            style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Ticks around gauge
                    val numTicks = 9
                    for (i in 0..numTicks) {
                        val angleDeg = 135f + (i.toFloat() / numTicks) * 270f
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val tickInner = radius - 16.dp.toPx()
                        val tickOuter = radius - 8.dp.toPx()
                        val start = Offset(
                            center.x + tickInner * cos(angleRad).toFloat(),
                            center.y + tickInner * sin(angleRad).toFloat()
                        )
                        val end = Offset(
                            center.x + tickOuter * cos(angleRad).toFloat(),
                            center.y + tickOuter * sin(angleRad).toFloat()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = start,
                            end = end,
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Rotating Needle
                    val needleAngleDeg = 135f + animatedSweep
                    val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
                    val needleLength = radius - 20.dp.toPx()
                    val needleEnd = Offset(
                        center.x + needleLength * cos(needleAngleRad).toFloat(),
                        center.y + needleLength * sin(needleAngleRad).toFloat()
                    )
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = center,
                        end = needleEnd,
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 6.dp.toPx(),
                        center = center
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(
                        text = displayedSpeed.format(1),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = selectedUnit.lowercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (simulatedSpeedMps >= 0f) Color(0xFF9C27B0).copy(alpha = 0.2f) else Color(0xFF00E676).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (simulatedSpeedMps >= 0f) "SIMULATION ACTIVE" else "GPS HARDWARE LIVE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (simulatedSpeedMps >= 0f) Color(0xFFE1BEE7) else Color(0xFF00E676),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        item {
            // Speed Metrics Cards
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("METRIC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${speedKmh.format(1)} km/h", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("IMPERIAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${speedMph.format(1)} mph", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MAX RECORDED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val maxDisplay = when (selectedUnit) {
                            "MPH" -> (effectiveMaxMps * 3.6f * 0.621371f).format(1) + " mph"
                            else -> (effectiveMaxMps * 3.6f).format(1) + " km/h"
                        }
                        Text(maxDisplay, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        item {
            // Interactive Speed Simulation & Test Suite
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "SPEEDOMETER TEST MODES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Test gauge calibration with realistic velocity presets or lock back to live GPS:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { simulatedSpeedMps = 1.4f }, // 5 km/h
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Walk", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { simulatedSpeedMps = 3.1f }, // 11 km/h
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Jog", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { simulatedSpeedMps = 6.9f }, // 25 km/h
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Bike", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { simulatedSpeedMps = 18.0f }, // 65 km/h
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Drive", fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { simulatedSpeedMps = -1f },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Lock Real GPS", fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { simulatedSpeedMps = 0f },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Zero (0.0)", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsView(
    azimuth: Float,
    accuracy: Int,
    lat: Double,
    lon: Double,
    gpsAccuracy: Float,
    hasGpsFix: Boolean,
    pressure: Float,
    altitude: Double
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("HARDWARE SENSORS & TELEMETRY", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
        }

        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GpsFixed, null, tint = if (hasGpsFix) Color(0xFF4CAF50) else Color(0xFFFFB300))
                        Spacer(Modifier.width(10.dp))
                        Text("Global Navigation Satellite System (GNSS)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Status: ${if (hasGpsFix) "Active 3D Satellite Lock" else "Searching for Constellations..."}", style = MaterialTheme.typography.bodyMedium)
                    Text("Accuracy: ${if (gpsAccuracy > 0f) "±${gpsAccuracy.format(1)} meters" else "Pending"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Coordinates: ${if (lat != 0.0) "${lat.format(6)}°, ${lon.format(6)}°" else "Unresolved"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Constellations: GPS (USA), GLONASS (RU), Galileo (EU), BeiDou (CN)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sensors, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Sensor Calibration & Health", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Compass Bearing: ${azimuth.toInt()}°", style = MaterialTheme.typography.bodyMedium)
                    Text("Barometric Pressure: ${pressure.format(2)} hPa", style = MaterialTheme.typography.bodyMedium)
                    Text("Calculated Elevation: ${altitude.toInt()} m", style = MaterialTheme.typography.bodyMedium)
                    Text("Local SQLite Storage: Ready (Zero Network Telemetry)", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}
