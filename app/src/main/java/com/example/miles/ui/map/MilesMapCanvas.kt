package com.example.miles.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.model.GpsPoint
import com.example.miles.data.model.Waypoint
import com.example.miles.data.model.WaypointType
import kotlin.math.cos
import kotlin.math.sin

enum class MapStyleMode {
    STANDARD,
    SATELLITE,
    TERRAIN,
    AMOLED_DARK
}

@Composable
fun MilesMapCanvas(
    modifier: Modifier = Modifier,
    points: List<GpsPoint>,
    waypoints: List<Waypoint> = emptyList(),
    mapStyle: MapStyleMode = MapStyleMode.STANDARD,
    showHeatmap: Boolean = false,
    activeLocationIndex: Int? = null,
    ghostLocationIndex: Int? = null,
    showCenterCrosshair: Boolean = false,
    onCenterChanged: ((Double, Double) -> Unit)? = null
) {
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // Base map theme colors
    val bgColor = when (mapStyle) {
        MapStyleMode.STANDARD -> Color(0xFFE5ECE9)
        MapStyleMode.SATELLITE -> Color(0xFF141F28)
        MapStyleMode.TERRAIN -> Color(0xFFE8E2D5)
        MapStyleMode.AMOLED_DARK -> Color(0xFF000000)
    }

    val gridColor = when (mapStyle) {
        MapStyleMode.STANDARD -> Color(0xFFD6DFD9)
        MapStyleMode.SATELLITE -> Color(0xFF1F2F3D)
        MapStyleMode.TERRAIN -> Color(0xFFD8CFBE)
        MapStyleMode.AMOLED_DARK -> Color(0xFF161616)
    }

    val roadMajorColor = when (mapStyle) {
        MapStyleMode.STANDARD -> Color(0xFFFFD54F)
        MapStyleMode.SATELLITE -> Color(0xFF607D8B)
        MapStyleMode.TERRAIN -> Color(0xFFFFCC80)
        MapStyleMode.AMOLED_DARK -> Color(0xFF2A2A2A)
    }

    val roadMinorColor = when (mapStyle) {
        MapStyleMode.STANDARD -> Color(0xFFFFFFFF)
        MapStyleMode.SATELLITE -> Color(0xFF37474F)
        MapStyleMode.TERRAIN -> Color(0xFFFBF9F5)
        MapStyleMode.AMOLED_DARK -> Color(0xFF1C1C1C)
    }

    val waterColor = when (mapStyle) {
        MapStyleMode.STANDARD -> Color(0xFF90CAF9)
        MapStyleMode.SATELLITE -> Color(0xFF0D47A1).copy(alpha = 0.6f)
        MapStyleMode.TERRAIN -> Color(0xFF81D4FA)
        MapStyleMode.AMOLED_DARK -> Color(0xFF081C2B)
    }

    val parkColor = when (mapStyle) {
        MapStyleMode.STANDARD -> Color(0xFFC8E6C9)
        MapStyleMode.SATELLITE -> Color(0xFF1B3820).copy(alpha = 0.5f)
        MapStyleMode.TERRAIN -> Color(0xFFDCEDC8)
        MapStyleMode.AMOLED_DARK -> Color(0xFF0C2412)
    }

    Box(
        modifier = modifier
            .background(bgColor)
            .pointerInput(Unit) {
                // True transform gestures: Drag pans camera smoothly, Pinch zooms in/out!
                // NEVER creates points or waypoints on drag!
                detectTransformGestures { _, pan, zoom, _ ->
                    panOffsetX += pan.x
                    panOffsetY += pan.y
                    zoomScale = (zoomScale * zoom).coerceIn(0.35f, 6.0f)

                    // Notify center coordinate if listener attached
                    val centerLat = 37.7749 - (panOffsetY / (1200f * zoomScale)) * 0.05
                    val centerLng = -122.4194 + (panOffsetX / (1200f * zoomScale)) * 0.05
                    onCenterChanged?.invoke(centerLat, centerLng)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f + panOffsetX
            val centerY = height / 2f + panOffsetY

            // 1. Draw Map Grid Coordinate Lines
            val gridSize = 64f * zoomScale
            var x = panOffsetX % gridSize
            if (x < 0) x += gridSize
            while (x < width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridSize
            }
            var y = panOffsetY % gridSize
            if (y < 0) y += gridSize
            while (y < height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridSize
            }

            // 2. Draw Realistic Topographic Water Bodies (River / Lake)
            val riverPath = Path().apply {
                moveTo(centerX - 380f * zoomScale, centerY - 280f * zoomScale)
                cubicTo(
                    centerX - 160f * zoomScale, centerY - 320f * zoomScale,
                    centerX - 40f * zoomScale, centerY - 100f * zoomScale,
                    centerX + 120f * zoomScale, centerY - 80f * zoomScale
                )
                cubicTo(
                    centerX + 260f * zoomScale, centerY - 60f * zoomScale,
                    centerX + 320f * zoomScale, centerY + 180f * zoomScale,
                    centerX + 460f * zoomScale, centerY + 300f * zoomScale
                )
            }
            drawPath(
                path = riverPath,
                color = waterColor,
                style = Stroke(width = 34f * zoomScale, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 3. Draw Parks and Nature Reserves
            val parkPath = Path().apply {
                moveTo(centerX - 240f * zoomScale, centerY - 180f * zoomScale)
                cubicTo(
                    centerX - 140f * zoomScale, centerY - 240f * zoomScale,
                    centerX - 40f * zoomScale, centerY - 220f * zoomScale,
                    centerX + 40f * zoomScale, centerY - 160f * zoomScale
                )
                cubicTo(
                    centerX + 90f * zoomScale, centerY - 80f * zoomScale,
                    centerX + 30f * zoomScale, centerY + 60f * zoomScale,
                    centerX - 80f * zoomScale, centerY + 80f * zoomScale
                )
                cubicTo(
                    centerX - 180f * zoomScale, centerY + 70f * zoomScale,
                    centerX - 250f * zoomScale, centerY - 60f * zoomScale,
                    centerX - 240f * zoomScale, centerY - 180f * zoomScale
                )
                close()
            }
            drawPath(path = parkPath, color = parkColor)

            // 4. Draw Road Network (Major Highways and Minor Avenues)
            // Major Avenue 1 (Horizontal)
            val highwayH = Path().apply {
                moveTo(0f, centerY + 20f * zoomScale)
                lineTo(width, centerY + 20f * zoomScale)
            }
            drawPath(
                path = highwayH,
                color = roadMajorColor,
                style = Stroke(width = 12f * zoomScale, cap = StrokeCap.Square)
            )

            // Major Avenue 2 (Vertical)
            val highwayV = Path().apply {
                moveTo(centerX + 60f * zoomScale, 0f)
                lineTo(centerX + 60f * zoomScale, height)
            }
            drawPath(
                path = highwayV,
                color = roadMajorColor,
                style = Stroke(width = 12f * zoomScale, cap = StrokeCap.Square)
            )

            // Minor streets
            val minorStep = 90f * zoomScale
            for (i in -3..3) {
                val streetY = centerY + (i * minorStep)
                if (streetY in 0f..height) {
                    drawLine(
                        color = roadMinorColor,
                        start = Offset(0f, streetY),
                        end = Offset(width, streetY),
                        strokeWidth = 4f * zoomScale
                    )
                }
                val streetX = centerX + (i * minorStep)
                if (streetX in 0f..width) {
                    drawLine(
                        color = roadMinorColor,
                        start = Offset(streetX, 0f),
                        end = Offset(streetX, height),
                        strokeWidth = 4f * zoomScale
                    )
                }
            }

            // 5. GPS Route Points & Polyline
            if (points.isNotEmpty()) {
                var minLat = points.first().latitude
                var maxLat = points.first().latitude
                var minLng = points.first().longitude
                var maxLng = points.first().longitude

                for (p in points) {
                    if (p.latitude < minLat) minLat = p.latitude
                    if (p.latitude > maxLat) maxLat = p.latitude
                    if (p.longitude < minLng) minLng = p.longitude
                    if (p.longitude > maxLng) maxLng = p.longitude
                }

                val latSpan = (maxLat - minLat).coerceAtLeast(0.0005)
                val lngSpan = (maxLng - minLng).coerceAtLeast(0.0005)
                val baseScale = (kotlin.math.min(width * 0.7f, height * 0.7f) / kotlin.math.max(latSpan, lngSpan)).toFloat()
                val drawScale = baseScale * zoomScale

                fun toScreen(lat: Double, lng: Double): Offset {
                    val px = centerX + ((lng - (minLng + lngSpan / 2.0)) * drawScale).toFloat()
                    val py = centerY - ((lat - (minLat + latSpan / 2.0)) * drawScale).toFloat()
                    return Offset(px, py)
                }

                // Heatmap overlay
                if (showHeatmap) {
                    for (p in points) {
                        val ptOffset = toScreen(p.latitude, p.longitude)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFF3D00).copy(alpha = 0.45f), Color.Transparent),
                                center = ptOffset,
                                radius = 35f * zoomScale
                            ),
                            radius = 35f * zoomScale,
                            center = ptOffset
                        )
                    }
                }

                // Polyline
                if (points.size > 1) {
                    val routePath = Path()
                    val firstOffset = toScreen(points[0].latitude, points[0].longitude)
                    routePath.moveTo(firstOffset.x, firstOffset.y)

                    val maxDrawIndex = activeLocationIndex ?: (points.size - 1)
                    for (i in 1..maxDrawIndex.coerceAtMost(points.size - 1)) {
                        val o = toScreen(points[i].latitude, points[i].longitude)
                        routePath.lineTo(o.x, o.y)
                    }

                    // Glow line
                    drawPath(
                        path = routePath,
                        color = primaryColor.copy(alpha = 0.35f),
                        style = Stroke(width = 12f * zoomScale.coerceIn(0.8f, 2.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Core line
                    drawPath(
                        path = routePath,
                        color = primaryColor,
                        style = Stroke(width = 6f * zoomScale.coerceIn(0.8f, 2.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Ghost runner marker
                    ghostLocationIndex?.let { ghostIdx ->
                        if (ghostIdx < points.size) {
                            val ghostOffset = toScreen(points[ghostIdx].latitude, points[ghostIdx].longitude)
                            drawCircle(color = Color.White, radius = 12f, center = ghostOffset)
                            drawCircle(color = Color(0xFFFF9100), radius = 8f, center = ghostOffset)
                        }
                    }

                    // Start marker (Green pin)
                    drawCircle(color = Color.White, radius = 10f, center = firstOffset)
                    drawCircle(color = Color(0xFF00E676), radius = 7f, center = firstOffset)

                    // End / active marker
                    val curPt = points[maxDrawIndex.coerceAtMost(points.size - 1)]
                    val endOffset = toScreen(curPt.latitude, curPt.longitude)

                    drawCircle(
                        color = secondaryColor.copy(alpha = 0.25f),
                        radius = 20f,
                        center = endOffset
                    )
                    drawCircle(color = Color.White, radius = 10f, center = endOffset)
                    drawCircle(color = secondaryColor, radius = 7f, center = endOffset)
                }

                // Draw Waypoints
                for (wp in waypoints) {
                    val wpOffset = toScreen(wp.latitude, wp.longitude)
                    val wpColor = when (wp.type) {
                        WaypointType.WATER -> Color(0xFF00B0FF)
                        WaypointType.REST -> Color(0xFF00E676)
                        WaypointType.TURN -> Color(0xFFFF9100)
                        WaypointType.DESTINATION -> Color(0xFFFF1744)
                        WaypointType.CUSTOM -> Color(0xFFAA00FF)
                    }

                    drawCircle(color = Color.White, radius = 11f, center = wpOffset)
                    drawCircle(color = wpColor, radius = 8f, center = wpOffset)

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 26f
                            isFakeBoldText = true
                        }
                        drawText(wp.name, wpOffset.x + 14f, wpOffset.y + 8f, paint)
                    }
                }
            }
        }

        // Center Crosshair Pin for Route Planning / Pin Dropping
        if (showCenterCrosshair) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Map Center Pin",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }

        // On-screen Navigation Controls: Zoom (+ / -) & Reset North
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale * 1.3f).coerceAtMost(6.0f) },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = { zoomScale = (zoomScale / 1.3f).coerceAtLeast(0.35f) },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = {
                        panOffsetX = 0f
                        panOffsetY = 0f
                        zoomScale = 1f
                    },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = "Reset North", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Distance Scale Bar at Bottom Left
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width((50f * zoomScale).coerceIn(30f, 90f).dp)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val scaleText = when {
                    zoomScale >= 2.5f -> "200 m"
                    zoomScale >= 1.2f -> "500 m"
                    zoomScale >= 0.7f -> "1 km"
                    else -> "2 km"
                }
                Text(scaleText, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
