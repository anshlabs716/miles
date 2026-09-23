package com.example.miles.ui.history

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.repository.MilesRepository
import com.example.miles.data.repository.format
import com.example.miles.ui.map.MapStyleMode
import com.example.miles.ui.map.MilesMapCanvas
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityDetailScreen(
    activity: ActivityEntity,
    repository: MilesRepository,
    onBack: () -> Unit,
    onSetAsGhostReference: (ActivityEntity) -> Unit,
    onDeleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val points = remember(activity.routePointsJson) {
        MilesRepository.parsePoints(activity.routePointsJson)
    }
    val waypoints = remember(activity.waypointsJson) {
        MilesRepository.parseWaypoints(activity.waypointsJson)
    }

    // Replay / Time machine state
    var replayProgress by remember { mutableFloatStateOf(1f) } // 0f to 1f
    var isReplaying by remember { mutableStateOf(false) }
    var replaySpeed by remember { mutableFloatStateOf(1f) }

    // Dialog states
    var showExportDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showEditNotesDialog by remember { mutableStateOf(false) }
    var showCompareDialog by remember { mutableStateOf(false) }
    val allActivities by repository.activities.collectAsState(initial = emptyList())
    var currentNotes by remember { mutableStateOf(activity.notes) }
    var isFavorite by remember { mutableStateOf(activity.isFavorite) }
    var mapStyle by remember { mutableStateOf(MapStyleMode.STANDARD) }

    // Active point index corresponding to scrubber
    val activeIndex = if (points.isNotEmpty()) {
        (replayProgress * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
    } else 0
    val activePoint = points.getOrNull(activeIndex)

    // Automated replay loop
    LaunchedEffect(isReplaying, replaySpeed) {
        if (isReplaying) {
            while (isActive && isReplaying) {
                delay((100L / replaySpeed).toLong())
                val next = replayProgress + 0.015f
                if (next >= 1f) {
                    replayProgress = 1f
                    isReplaying = false
                } else {
                    replayProgress = next
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            // Top Bar with Back, Favorite, Share, Export, and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        isFavorite = !isFavorite
                        scope.launch {
                            repository.updateActivity(activity.copy(isFavorite = isFavorite))
                        }
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Designer", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export GPX/TCX", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = {
                        scope.launch {
                            repository.moveToTrash(activity.id)
                            Toast.makeText(context, "Moved to Trash", Toast.LENGTH_SHORT).show()
                            onDeleted()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Trash", tint = Color(0xFFFF2D55))
                    }
                }
            }
        }

        // Title and Date
        item {
            Column {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${activity.activityType} • ${SimpleDateFormat("EEEE, MMMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(activity.startTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Interactive Map with Replay Marker
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                val activePoint = if (activeIndex in points.indices) points[activeIndex] else points.lastOrNull()
                com.example.miles.ui.map.RealOsmMapView(
                    modifier = Modifier.fillMaxSize(),
                    points = points,
                    waypoints = waypoints,
                    userLocation = activePoint,
                    showControls = true
                )
            }
        }

        // Replay / Time Machine Controls
        item {
            LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Replay",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Activity Replay & Time Machine",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Play/Pause scrubber button
                        IconButton(onClick = {
                            if (replayProgress >= 1f) replayProgress = 0f
                            isReplaying = !isReplaying
                        }) {
                            Icon(
                                imageVector = if (isReplaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause Replay",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Scrubber Slider
                    Slider(
                        value = replayProgress,
                        onValueChange = {
                            replayProgress = it
                            isReplaying = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Point-in-time stats at scrubber position
                    activePoint?.let { pt ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Point: ${activeIndex + 1}/${points.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Alt: ${pt.altitude.toInt()} m • Speed: ${(pt.speed * 3.6).format(1)} km/h",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Core Activity Metrics Grid
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "WORKOUT METRICS",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricDetailItem(label = "DISTANCE", value = "${(activity.distanceMeters / 1000.0).format(2)} km")
                        MetricDetailItem(label = "DURATION", value = "${activity.durationSeconds / 60}m ${activity.durationSeconds % 60}s")
                        MetricDetailItem(label = "AVG PACE", value = "${(activity.avgPaceSecPerKm / 60).toInt()}'${(activity.avgPaceSecPerKm % 60).toInt()}\"/km")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricDetailItem(label = "BEST PACE", value = "${(activity.bestPaceSecPerKm / 60).toInt()}'${(activity.bestPaceSecPerKm % 60).toInt()}\"/km")
                        MetricDetailItem(label = "AVG SPEED", value = "${activity.avgSpeedKmh.format(1)} km/h")
                        MetricDetailItem(label = "MAX SPEED", value = "${activity.maxSpeedKmh.format(1)} km/h")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricDetailItem(label = "ELEVATION GAIN", value = "+${activity.elevationGainM.toInt()} m")
                        MetricDetailItem(label = "HEART RATE", value = "${activity.avgHeartRate} bpm (max ${activity.maxHeartRate})")
                        MetricDetailItem(label = "CALORIES", value = "${activity.calories} kcal")
                    }
                }
            }
        }

        // Journal & Notes Card
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JOURNAL & NOTES",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showEditNotesDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Notes", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (currentNotes.isNotBlank()) currentNotes else "No notes added yet for this workout.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Action Buttons: Ghost Reference, Compare & Data Repair
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        onSetAsGhostReference(activity)
                        Toast.makeText(context, "Set as Ghost Mode reference for next workout!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ghost Pacer", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = { showCompareDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compare", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        scope.launch {
                            val result = repository.runDataRepair(activity.id)
                            Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Repair", fontSize = 12.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // Export Dialog (GPX, TCX, KML, GeoJSON, CSV, Native JSON)
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Activity", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose an open, privacy-preserving standard format for external tools, Strava, Garmin, or backup:")
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val gpx = repository.exportActivityAsGpx(activity.id)
                                Toast.makeText(context, "Exported GPX (${gpx.length} chars)", Toast.LENGTH_SHORT).show()
                                showExportDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("GPX (GPS Exchange Format)")
                    }

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val tcx = repository.exportActivityAsTcx(activity.id)
                                Toast.makeText(context, "Exported TCX (${tcx.length} chars)", Toast.LENGTH_SHORT).show()
                                showExportDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TCX (Garmin Training Center)")
                    }

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val kml = repository.exportActivityAsKml(activity.id)
                                Toast.makeText(context, "Exported KML (${kml.length} chars)", Toast.LENGTH_SHORT).show()
                                showExportDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("KML (Google Earth & Maps.me)")
                    }

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val geojson = repository.exportActivityAsGeoJson(activity.id)
                                Toast.makeText(context, "Exported GeoJSON (${geojson.length} chars)", Toast.LENGTH_SHORT).show()
                                showExportDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("GeoJSON (Open GIS Standard)")
                    }

                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val json = repository.exportActivityAsJson(activity.id)
                                Toast.makeText(context, "Exported Native JSON", Toast.LENGTH_SHORT).show()
                                showExportDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Native MILES JSON (Full Telemetry)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Workout Comparison Dialog
    if (showCompareDialog) {
        val otherActivities = remember(allActivities, activity.id) {
            allActivities.filter { it.id != activity.id }
        }
        var selectedOtherActivity by remember { mutableStateOf(otherActivities.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { showCompareDialog = false },
            title = { Text("Side-by-Side Workout Comparison", fontWeight = FontWeight.Bold) },
            text = {
                if (otherActivities.isEmpty()) {
                    Text("Record at least one other activity to compare performance side-by-side.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Select comparison baseline:", style = MaterialTheme.typography.labelMedium)

                        // Selector chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(otherActivities) { other ->
                                androidx.compose.material3.FilterChip(
                                    selected = selectedOtherActivity?.id == other.id,
                                    onClick = { selectedOtherActivity = other },
                                    label = { Text(other.title, fontSize = 11.sp, maxLines = 1) }
                                )
                            }
                        }

                        selectedOtherActivity?.let { baseline ->
                            Spacer(modifier = Modifier.height(6.dp))
                            LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ComparisonMetricRow(
                                        label = "Distance",
                                        current = "${(activity.distanceMeters / 1000.0).format(2)} km",
                                        other = "${(baseline.distanceMeters / 1000.0).format(2)} km",
                                        diff = "${((activity.distanceMeters - baseline.distanceMeters) / 1000.0).format(2)} km"
                                    )
                                    ComparisonMetricRow(
                                        label = "Duration",
                                        current = "${activity.durationSeconds / 60}m ${activity.durationSeconds % 60}s",
                                        other = "${baseline.durationSeconds / 60}m ${baseline.durationSeconds % 60}s",
                                        diff = "${(activity.durationSeconds - baseline.durationSeconds) / 60}m"
                                    )
                                    ComparisonMetricRow(
                                        label = "Pace",
                                        current = "${(activity.avgPaceSecPerKm / 60).toInt()}'${(activity.avgPaceSecPerKm % 60).toInt()}\"/km",
                                        other = "${(baseline.avgPaceSecPerKm / 60).toInt()}'${(baseline.avgPaceSecPerKm % 60).toInt()}\"/km",
                                        diff = "${(activity.avgPaceSecPerKm - baseline.avgPaceSecPerKm).toInt()}s/km"
                                    )
                                    ComparisonMetricRow(
                                        label = "Calories",
                                        current = "${activity.calories} kcal",
                                        other = "${baseline.calories} kcal",
                                        diff = "${activity.calories - baseline.calories} kcal"
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCompareDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Share Designer Dialog
    if (showShareDialog) {
        ShareDesignerDialog(
            activity = activity,
            points = points,
            onDismiss = { showShareDialog = false }
        )
    }

    // Edit Notes Dialog
    if (showEditNotesDialog) {
        var tempNotes by remember { mutableStateOf(currentNotes) }
        AlertDialog(
            onDismissRequest = { showEditNotesDialog = false },
            title = { Text("Edit Journal Notes") },
            text = {
                OutlinedTextField(
                    value = tempNotes,
                    onValueChange = { tempNotes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes & Thoughts") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    currentNotes = tempNotes
                    scope.launch {
                        repository.updateActivity(activity.copy(notes = tempNotes))
                    }
                    showEditNotesDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNotesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MetricDetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ComparisonMetricRow(
    label: String,
    current: String,
    other: String,
    diff: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
        Text(current, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(other, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(diff, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
    }
}
