package com.example.miles.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.miles.data.model.GpsPoint
import com.example.miles.data.model.Waypoint
import com.example.miles.data.repository.format
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

enum class RealOsmTileSource(val title: String, val attribution: String) {
    STANDARD("OpenStreetMap", "© OpenStreetMap contributors"),
    CYCLOSM("CyclOSM Outdoor", "© CyclOSM / OpenStreetMap"),
    HUMANITARIAN("Humanitarian OSM", "© Humanitarian OSM / HOT"),
    CARTO_DARK("Carto Dark", "© CARTO / OpenStreetMap"),
    CARTO_LIGHT("Carto Voyager", "© CARTO / OpenStreetMap"),
    OPEN_TOPO("OpenTopoMap", "© OpenTopoMap / SRTM")
}

@Composable
fun RealOsmMapView(
    modifier: Modifier = Modifier,
    points: List<GpsPoint> = emptyList(),
    waypoints: List<Waypoint> = emptyList(),
    userLocation: GpsPoint? = null,
    isInteractive: Boolean = true,
    showControls: Boolean = true,
    showCenterCrosshair: Boolean = false,
    autoCenter: Boolean = true,
    onAutoCenterChanged: ((Boolean) -> Unit)? = null,
    initialTileSource: RealOsmTileSource = RealOsmTileSource.STANDARD,
    onCenterChanged: ((Double, Double) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Device location detected directly from system providers if userLocation not passed
    var detectedDeviceLocation by remember { mutableStateOf<GpsPoint?>(null) }
    val effectiveUserLocation = userLocation ?: detectedDeviceLocation

    // Center coordinates
    var centerLat by remember {
        mutableDoubleStateOf(
            effectiveUserLocation?.latitude ?: points.lastOrNull()?.latitude ?: 37.7749
        )
    }
    var centerLon by remember {
        mutableDoubleStateOf(
            effectiveUserLocation?.longitude ?: points.lastOrNull()?.longitude ?: -122.4194
        )
    }
    var zoomLevel by remember { mutableFloatStateOf(16f) }
    var tileSource by remember { mutableStateOf(initialTileSource) }
    var isFollowingUser by remember(autoCenter) { mutableStateOf(autoCenter) }

    // Pulsing animation for active GPS fix
    val infiniteTransition = rememberInfiniteTransition(label = "gpsPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Helper to query system GPS for immediate fix
    fun querySystemLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        try {
            val lastGps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val lastPassive = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            val best = listOfNotNull(lastGps, lastNet, lastPassive)
                .maxByOrNull { it.time }

            if (best != null) {
                val pt = GpsPoint(
                    latitude = best.latitude,
                    longitude = best.longitude,
                    altitude = best.altitude,
                    accuracy = best.accuracy,
                    speed = best.speed,
                    bearing = best.bearing,
                    timestamp = best.time
                )
                detectedDeviceLocation = pt
                if (isFollowingUser) {
                    centerLat = pt.latitude
                    centerLon = pt.longitude
                    onCenterChanged?.invoke(centerLat, centerLon)
                }
            }

            // Also register a fast one-time update
            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    val pt = GpsPoint(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitude = loc.altitude,
                        accuracy = loc.accuracy,
                        speed = loc.speed,
                        bearing = loc.bearing,
                        timestamp = loc.time
                    )
                    detectedDeviceLocation = pt
                    if (isFollowingUser) {
                        centerLat = pt.latitude
                        centerLon = pt.longitude
                        onCenterChanged?.invoke(centerLat, centerLon)
                    }
                    lm.removeUpdates(this)
                }
                @Deprecated("Deprecated") override fun onStatusChanged(p: String?, s: Int, b: Bundle?) {}
                override fun onProviderEnabled(p: String) {}
                override fun onProviderDisabled(p: String) {}
            }
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
            }
        } catch (_: Exception) {}
    }

    // Query on mount
    LaunchedEffect(Unit) {
        querySystemLocation()
    }

    // When new userLocation arrives from props, update center if following
    LaunchedEffect(effectiveUserLocation) {
        if (isFollowingUser && effectiveUserLocation != null) {
            centerLat = effectiveUserLocation.latitude
            centerLon = effectiveUserLocation.longitude
            onCenterChanged?.invoke(centerLat, centerLon)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Gesture handling for drag pan and pinch zoom
        val gestureModifier = if (isInteractive) {
            Modifier.pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    isFollowingUser = false
                    val oldZoom = zoomLevel
                    val newZoom = (oldZoom + (zoom - 1f) * 2f).coerceIn(3f, 19f)
                    zoomLevel = newZoom

                    val tileSize = 256.0 * 2.0.pow(newZoom.toDouble())
                    val lonDelta = -(pan.x / tileSize) * 360.0
                    val latRad = Math.toRadians(centerLat)
                    val latDelta = (pan.y / tileSize) * (360.0 * cos(latRad))

                    centerLon = (centerLon + lonDelta).coerceIn(-180.0, 180.0)
                    centerLat = (centerLat + latDelta).coerceIn(-85.0, 85.0)
                    onCenterChanged?.invoke(centerLat, centerLon)
                }
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier)
        ) {
            // 1. RENDER REAL OPENSTREETMAP TILE SLICES
            val zoomInt = zoomLevel.roundToInt().coerceIn(3, 19)
            val numTiles = 2.0.pow(zoomInt.toDouble())

            val centerTileX = ((centerLon + 180.0) / 360.0 * numTiles)
            val latRad = Math.toRadians(centerLat)
            val centerTileY = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * numTiles)

            val tileSizePx = 256f * (2.0.pow((zoomLevel - zoomInt).toDouble())).toFloat()

            val tilesXNeeded = (widthPx / tileSizePx).toInt() + 3
            val tilesYNeeded = (heightPx / tileSizePx).toInt() + 3

            val startTileX = (floor(centerTileX) - tilesXNeeded / 2).toInt()
            val endTileX = (floor(centerTileX) + tilesXNeeded / 2).toInt()
            val startTileY = (floor(centerTileY) - tilesYNeeded / 2).toInt()
            val endTileY = (floor(centerTileY) + tilesYNeeded / 2).toInt()

            for (tileX in startTileX..endTileX) {
                for (tileY in startTileY..endTileY) {
                    val clampedTileX = ((tileX % numTiles.toInt()) + numTiles.toInt()) % numTiles.toInt()
                    if (tileY < 0 || tileY >= numTiles.toInt()) continue

                    val offsetX = ((tileX - centerTileX) * tileSizePx + (widthPx / 2f)).roundToInt()
                    val offsetY = ((tileY - centerTileY) * tileSizePx + (heightPx / 2f)).roundToInt()

                    val tileUrl = when (tileSource) {
                        RealOsmTileSource.STANDARD ->
                            "https://tile.openstreetmap.org/$zoomInt/$clampedTileX/$tileY.png"
                        RealOsmTileSource.CYCLOSM ->
                            "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/$zoomInt/$clampedTileX/$tileY.png"
                        RealOsmTileSource.HUMANITARIAN ->
                            "https://a.tile.openstreetmap.fr/hot/$zoomInt/$clampedTileX/$tileY.png"
                        RealOsmTileSource.CARTO_LIGHT ->
                            "https://a.basemaps.cartocdn.com/rastertiles/voyager/$zoomInt/$clampedTileX/$tileY.png"
                        RealOsmTileSource.CARTO_DARK ->
                            "https://a.basemaps.cartocdn.com/dark_all/$zoomInt/$clampedTileX/$tileY.png"
                        RealOsmTileSource.OPEN_TOPO ->
                            "https://tile.opentopomap.org/$zoomInt/$clampedTileX/$tileY.png"
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(tileUrl)
                            .addHeader("User-Agent", "MILES-Android-App/2.4 (contact: bhatiaansh716@gmail.com)")
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .size(with(density) { tileSizePx.toDp() })
                            .offset { IntOffset(offsetX, offsetY) }
                    )
                }
            }

            // 2. VECTOR OVERLAY CANVAS (GPS BREADCRUMBS, WAYPOINTS, REAL LOCATION AURA)
            Canvas(modifier = Modifier.fillMaxSize()) {
                fun geoToPixel(lat: Double, lon: Double): Offset {
                    val xTile = ((lon + 180.0) / 360.0 * numTiles)
                    val latR = Math.toRadians(lat.coerceIn(-85.0, 85.0))
                    val yTile = ((1.0 - ln(tan(latR) + 1.0 / cos(latR)) / PI) / 2.0 * numTiles)

                    val px = ((xTile - centerTileX) * tileSizePx + (widthPx / 2f)).toFloat()
                    val py = ((yTile - centerTileY) * tileSizePx + (heightPx / 2f)).toFloat()
                    return Offset(px, py)
                }

                // Draw Breadcrumb GPS Route
                if (points.size >= 2) {
                    val path = Path()
                    val firstPx = geoToPixel(points.first().latitude, points.first().longitude)
                    path.moveTo(firstPx.x, firstPx.y)

                    for (i in 1 until points.size) {
                        val pt = points[i]
                        val px = geoToPixel(pt.latitude, pt.longitude)
                        path.lineTo(px.x, px.y)
                    }

                    // Outer halo for high contrast visibility
                    drawPath(
                        path = path,
                        color = Color.Black.copy(alpha = 0.5f),
                        style = Stroke(width = 11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Core route line
                    drawPath(
                        path = path,
                        color = Color(0xFF00E5FF),
                        style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Start pin
                    drawCircle(color = Color(0xFF00E676), radius = 10f, center = firstPx)
                    drawCircle(color = Color.White, radius = 5f, center = firstPx)
                }

                // Draw Waypoints
                waypoints.forEach { wp ->
                    val wpPx = geoToPixel(wp.latitude, wp.longitude)
                    drawCircle(color = Color(0xFFFF9100), radius = 12f, center = wpPx)
                    drawCircle(color = Color.White, radius = 6f, center = wpPx)
                    drawContext.canvas.nativeCanvas.drawText(
                        wp.name,
                        wpPx.x + 16f,
                        wpPx.y + 5f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 32f
                            isFakeBoldText = true
                            setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
                        }
                    )
                }

                // Draw REAL GPS User Location Marker
                effectiveUserLocation?.let { uLoc ->
                    val userPx = geoToPixel(uLoc.latitude, uLoc.longitude)

                    // Accuracy Aura Ring
                    val accRadiusPx = (uLoc.accuracy / (156543.03392 * cos(Math.toRadians(uLoc.latitude)) / 2.0.pow(zoomLevel.toDouble()))).toFloat().coerceIn(14f, 120f)
                    drawCircle(
                        color = Color(0xFF0088FF).copy(alpha = 0.18f),
                        radius = accRadiusPx,
                        center = userPx
                    )

                    // Outer pulsing radar ring
                    drawCircle(
                        color = Color(0xFF00B0FF).copy(alpha = (1.7f - pulseScale).coerceIn(0.1f, 0.7f)),
                        radius = 18f * pulseScale,
                        center = userPx
                    )

                    // Core GPS marker dot
                    drawCircle(color = Color(0xFF0088FF), radius = 12f, center = userPx)
                    drawCircle(color = Color.White, radius = 5f, center = userPx)

                    // Bearing Heading Arrow (if speed > 1 km/h)
                    if (uLoc.speed > 0.3f && uLoc.bearing != 0f) {
                        val bearingRad = Math.toRadians((uLoc.bearing - 90.0))
                        val arrowX = userPx.x + (cos(bearingRad) * 22f).toFloat()
                        val arrowY = userPx.y + (sin(bearingRad) * 22f).toFloat()
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = userPx,
                            end = Offset(arrowX, arrowY),
                            strokeWidth = 5f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Center Reticle / Crosshair (for Route Builder)
                if (showCenterCrosshair) {
                    val centerX = widthPx / 2f
                    val centerY = heightPx / 2f

                    // Shadow cross
                    drawLine(
                        color = Color.Black.copy(alpha = 0.6f),
                        start = Offset(centerX - 24f, centerY),
                        end = Offset(centerX + 24f, centerY),
                        strokeWidth = 6f
                    )
                    drawLine(
                        color = Color.Black.copy(alpha = 0.6f),
                        start = Offset(centerX, centerY - 24f),
                        end = Offset(centerX, centerY + 24f),
                        strokeWidth = 6f
                    )

                    // Reticle lines
                    drawLine(
                        color = Color(0xFFFF2D55),
                        start = Offset(centerX - 22f, centerY),
                        end = Offset(centerX + 22f, centerY),
                        strokeWidth = 3.5f
                    )
                    drawLine(
                        color = Color(0xFFFF2D55),
                        start = Offset(centerX, centerY - 22f),
                        end = Offset(centerX, centerY + 22f),
                        strokeWidth = 3.5f
                    )
                    drawCircle(
                        color = Color(0xFFFF2D55),
                        radius = 8f,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3f)
                    )
                }
            }

            // 3. FLOATING MAP CONTROLS
            if (showControls) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Tile Layer Switcher
                    SmallFloatingActionButton(
                        onClick = {
                            tileSource = when (tileSource) {
                                RealOsmTileSource.STANDARD -> RealOsmTileSource.CYCLOSM
                                RealOsmTileSource.CYCLOSM -> RealOsmTileSource.HUMANITARIAN
                                RealOsmTileSource.HUMANITARIAN -> RealOsmTileSource.CARTO_DARK
                                RealOsmTileSource.CARTO_DARK -> RealOsmTileSource.CARTO_LIGHT
                                RealOsmTileSource.CARTO_LIGHT -> RealOsmTileSource.OPEN_TOPO
                                RealOsmTileSource.OPEN_TOPO -> RealOsmTileSource.STANDARD
                            }
                            Toast.makeText(context, "Map layer: ${tileSource.title}", Toast.LENGTH_SHORT).show()
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Map Style")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Zoom Controls (+ / -)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                        tonalElevation = 4.dp
                    ) {
                        Column {
                            IconButton(
                                onClick = { zoomLevel = (zoomLevel + 1f).coerceAtMost(19f) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom In")
                            }
                            IconButton(
                                onClick = { zoomLevel = (zoomLevel - 1f).coerceAtLeast(3f) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Centering Toggle FAB
                    SmallFloatingActionButton(
                        onClick = {
                            val next = !isFollowingUser
                            isFollowingUser = next
                            onAutoCenterChanged?.invoke(next)
                            if (next) {
                                querySystemLocation()
                                effectiveUserLocation?.let { loc ->
                                    centerLat = loc.latitude
                                    centerLon = loc.longitude
                                    onCenterChanged?.invoke(centerLat, centerLon)
                                }
                                Toast.makeText(context, "Auto-Centering: ON (Locks to GPS)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Auto-Centering: OFF (Free Pan)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        containerColor = if (isFollowingUser) Color(0xFF00E676) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isFollowingUser) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Toggle Auto Centering",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // "MY LOCATION" ACTION FAB
                    FloatingActionButton(
                        onClick = {
                            isFollowingUser = true
                            onAutoCenterChanged?.invoke(true)
                            querySystemLocation()
                            effectiveUserLocation?.let { loc ->
                                centerLat = loc.latitude
                                centerLon = loc.longitude
                                zoomLevel = 16.5f
                                onCenterChanged?.invoke(centerLat, centerLon)
                                Toast.makeText(context, "Centered on real GPS: ${loc.latitude.format(4)}, ${loc.longitude.format(4)}", Toast.LENGTH_SHORT).show()
                            } ?: run {
                                Toast.makeText(context, "Acquiring GPS fix...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        containerColor = if (isFollowingUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isFollowingUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(6.dp),
                        shape = CircleShape,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Center On My Location",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Attribution & Live Coordinates Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.72f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            text = "${tileSource.title} • Z${zoomLevel.roundToInt()}",
                            color = Color(0xFF00E5FF),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        )
                        Text(
                            text = "Lat: ${centerLat.format(4)} • Lon: ${centerLon.format(4)}",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        )
                        Text(
                            text = tileSource.attribution,
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                        )
                    }
                }
            }
        }
    }
}
