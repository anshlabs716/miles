package com.example.miles.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.model.WaypointType
import com.example.miles.data.repository.format
import com.example.miles.engine.DeviceManager
import com.example.miles.engine.SmartTrackingEngine
import com.example.miles.engine.TrackingState
import com.example.miles.ui.map.MapStyleMode
import com.example.miles.ui.map.MilesMapCanvas
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel
import com.example.miles.wear.WearCompanionManager
import com.example.miles.wear.WearConnectionStatus
import kotlinx.coroutines.launch

@Composable
fun WorkoutHudScreen(
    smartEngine: SmartTrackingEngine,
    wearCompanion: WearCompanionManager? = null,
    deviceManager: DeviceManager? = null,
    onFinishWorkout: (ActivityEntity) -> Unit,
    onDiscardWorkout: () -> Unit,
    onBack: () -> Unit = {}
) {
    val liveStats by smartEngine.liveStats.collectAsState()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    // Support edge swipe back gesture to minimize HUD while tracking
    BackHandler(enabled = true) {
        onBack()
    }

    var isLocked by remember { mutableStateOf(false) }
    var isPocketMode by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showAddWaypointDialog by remember { mutableStateOf(false) }
    var mapStyle by remember { mutableStateOf(MapStyleMode.STANDARD) }

    var workoutTitle by remember { mutableStateOf("") }
    var workoutNotes by remember { mutableStateOf("") }

    // Start tracking if idle (WITHOUT fake simulation)
    LaunchedEffect(liveStats.state) {
        if (liveStats.state == TrackingState.IDLE) {
            smartEngine.startTracking(ActivityType.RUNNING)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isPocketMode) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        if (isPocketMode) {
            // Low-power pocket mode display
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isPocketMode = false }
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "POCKET MODE ACTIVE",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = smartEngine.formatElapsedTime(liveStats.elapsedMillis),
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                Text(
                    text = "${(liveStats.distanceMeters / 1000.0).format(2)} KM",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Display dimmed to conserve battery • Tap anywhere to wake",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // HUD Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Activity Type & Live status badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Minimize HUD to background",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (liveStats.state == TrackingState.RECORDING) Color(0xFF00E676) else Color(0xFFFF9100))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = liveStats.activityType.displayName.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // HUD Action tools: Pocket mode, Lock, Waypoint, Map Layer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            mapStyle = when (mapStyle) {
                                MapStyleMode.STANDARD -> MapStyleMode.SATELLITE
                                MapStyleMode.SATELLITE -> MapStyleMode.TERRAIN
                                MapStyleMode.TERRAIN -> MapStyleMode.AMOLED_DARK
                                MapStyleMode.AMOLED_DARK -> MapStyleMode.STANDARD
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Map Style",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { showAddWaypointDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = "Add Waypoint",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { isPocketMode = true }) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Pocket Mode",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { isLocked = !isLocked }) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock",
                            tint = if (isLocked) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- 1. ACTIVE ROUTE NAVIGATION BANNER ---
            if (liveStats.activeNavRoute != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (liveStats.navIsOffRoute) Color(0xFFFF2D55).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (liveStats.navIsOffRoute) Color(0xFFFF2D55) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (liveStats.navIsOffRoute) Icons.Default.Close else Icons.Default.Navigation,
                                    contentDescription = "Navigation",
                                    tint = if (liveStats.navIsOffRoute) Color(0xFFFF2D55) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (liveStats.navIsOffRoute) "⚠️ OFF-ROUTE (${liveStats.navCrossTrackErrorM.toInt()}m)"
                                        else "NAV: ${liveStats.activeNavRoute?.name ?: "Active Route"}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (liveStats.navIsOffRoute) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = if (liveStats.navIsOffRoute) "Turn around towards route path"
                                        else "Next: ${liveStats.navNextWaypointName} (${liveStats.navDistanceToNextM.toInt()}m)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${(liveStats.navRemainingDistanceM / 1000.0).format(2)} km left",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { smartEngine.stopNavigation() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Stop Navigation",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { liveStats.navProgressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (liveStats.navIsOffRoute) Color(0xFFFF2D55) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // --- 2. PROGRESSIVE INTERVAL WORKOUT BANNER ---
            if (liveStats.activeIntervalPlanName != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when (liveStats.currentIntervalType) {
                                "RUN" -> Color(0xFF00E676).copy(alpha = 0.15f)
                                "SPRINT" -> Color(0xFFFF9100).copy(alpha = 0.15f)
                                "WALK", "RECOVERY" -> Color(0xFF2979FF).copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when (liveStats.currentIntervalType) {
                                "RUN" -> Color(0xFF00E676)
                                "SPRINT" -> Color(0xFFFF9100)
                                "WALK", "RECOVERY" -> Color(0xFF2979FF)
                                else -> MaterialTheme.colorScheme.outline
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (liveStats.currentIntervalType) {
                                                "RUN" -> Color(0xFF00E676)
                                                "SPRINT" -> Color(0xFFFF9100)
                                                "WALK", "RECOVERY" -> Color(0xFF2979FF)
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${liveStats.currentIntervalType} (${liveStats.intervalIndex + 1}/${liveStats.totalIntervals})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp),
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = liveStats.currentIntervalLabel,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }

                            // Big remaining countdown seconds
                            val remSec = liveStats.currentIntervalRemainingSeconds
                            val min = remSec / 60
                            val sec = remSec % 60
                            Text(
                                text = String.format("%d:%02d", min, sec),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = if (liveStats.currentIntervalTotalSeconds > 0) {
                            1f - (liveStats.currentIntervalRemainingSeconds.toFloat() / liveStats.currentIntervalTotalSeconds)
                        } else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = when (liveStats.currentIntervalType) {
                                "RUN" -> Color(0xFF00E676)
                                "SPRINT" -> Color(0xFFFF9100)
                                "WALK", "RECOVERY" -> Color(0xFF2979FF)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            // HUD Page Switcher Tabs
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Primary HUD", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Biometrics & GPS", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    text = { Text("Ghost & Splits", fontWeight = FontWeight.Bold) }
                )
            }

            // Horizontal Pager for HUD Pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> PrimaryHudPage(
                        liveStats = liveStats,
                        formatDuration = { smartEngine.formatDuration(it) },
                        formatElapsedTime = { smartEngine.formatElapsedTime(it) },
                        formatPace = { smartEngine.formatPace(it) },
                        mapStyle = mapStyle
                    )
                    1 -> BiometricsPage(liveStats = liveStats)
                    2 -> GhostAndSplitsPage(
                        liveStats = liveStats,
                        smartEngine = smartEngine,
                        formatPace = { smartEngine.formatPace(it) }
                    )
                }
            }

            // Bottom Tracking Controls (Pause/Resume/Stop/Lock overlay)
            if (isLocked) {
                LiquidGlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF2D55)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Screen Locked",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = { isLocked = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Unlock")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pause / Resume Button
                    FilledTonalButton(
                        onClick = {
                            if (liveStats.state == TrackingState.RECORDING) {
                                smartEngine.pauseTracking()
                            } else {
                                smartEngine.resumeTracking()
                            }
                        },
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = if (liveStats.state == TrackingState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Pause/Resume"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (liveStats.state == TrackingState.RECORDING) "Pause" else "Resume",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Stop / Save Button
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Save Workout Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Complete Workout", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Total distance: ${(liveStats.distanceMeters / 1000.0).format(2)} km in ${smartEngine.formatDuration(liveStats.elapsedSeconds)}.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = workoutTitle,
                        onValueChange = { workoutTitle = it },
                        label = { Text("Activity Title (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = workoutNotes,
                        onValueChange = { workoutNotes = it },
                        label = { Text("Journal Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val saved = smartEngine.stopAndSaveWorkout(
                                title = workoutTitle.ifBlank { null },
                                notes = workoutNotes
                            )
                            showSaveDialog = false
                            if (saved != null) onFinishWorkout(saved)
                        }
                    }
                ) {
                    Text("Save to MILES")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        smartEngine.discardWorkout()
                        showSaveDialog = false
                        onDiscardWorkout()
                    }
                ) {
                    Text("Discard", color = Color(0xFFFF2D55))
                }
            }
        )
    }

    // Add Waypoint Dialog
    if (showAddWaypointDialog) {
        var wpName by remember { mutableStateOf("") }
        var wpType by remember { mutableStateOf(WaypointType.WATER) }

        AlertDialog(
            onDismissRequest = { showAddWaypointDialog = false },
            title = { Text("Add Waypoint", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = wpName,
                        onValueChange = { wpName = it },
                        label = { Text("Waypoint Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Type:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        WaypointType.entries.forEach { type ->
                            Text(
                                text = type.name.take(3),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (wpType == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { wpType = type }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                color = if (wpType == type) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (wpName.isNotBlank()) {
                            smartEngine.addWaypoint(wpName, wpType)
                        }
                        showAddWaypointDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWaypointDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PrimaryHudPage(
    liveStats: com.example.miles.engine.LiveWorkoutStats,
    formatDuration: (Long) -> String,
    formatElapsedTime: (Long) -> String,
    formatPace: (Double) -> String,
    mapStyle: MapStyleMode
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Big primary metrics card
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Duration with refresh badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ELAPSED TIME",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "⚡ ${liveStats.tickIntervalMs}ms",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = formatElapsedTime(liveStats.elapsedMillis),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DISTANCE",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = (liveStats.distanceMeters / 1000.0).format(2),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = " km",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "AVG PACE",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatPace(liveStats.avgPaceSecPerKm),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = " /km",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "SPEED",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = liveStats.currentSpeedKmh.format(1),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF00E676)
                            )
                            Text(
                                text = " km/h",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Second Metrics Row: Calories, Steps, Source
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "CALORIES",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${liveStats.calories}",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFF9100)
                            )
                            Text(
                                text = " kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "STEPS",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${liveStats.stepCount}",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF00E5FF)
                            )
                            Text(
                                text = " steps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "TRACKING FEED",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = liveStats.activeSource,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Live Map View Box (Real OpenStreetMap with GPS Location)
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            com.example.miles.ui.map.RealOsmMapView(
                modifier = Modifier.fillMaxSize(),
                points = liveStats.points,
                waypoints = liveStats.waypoints,
                userLocation = liveStats.points.lastOrNull(),
                autoCenter = true,
                showControls = true
            )
        }
    }
}

@Composable
fun BiometricsPage(liveStats: com.example.miles.engine.LiveWorkoutStats) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "HEART RATE & BIOMETRICS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart Rate",
                            tint = if (liveStats.heartRate > 0) Color(0xFFFF2D55) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (liveStats.heartRate > 0) "${liveStats.heartRate} BPM" else "-- BPM",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (liveStats.heartRate > 0) {
                                    when {
                                        liveStats.heartRate < 110 -> "Zone 1 • Recovery"
                                        liveStats.heartRate < 135 -> "Zone 2 • Aerobic Base"
                                        liveStats.heartRate < 155 -> "Zone 3 • Tempo Endurance"
                                        liveStats.heartRate < 175 -> "Zone 4 • Threshold"
                                        else -> "Zone 5 • Maximum Effort"
                                    }
                                } else "No HRM sensor connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (liveStats.heartRate > 0) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${liveStats.calories} kcal",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFF9100)
                        )
                        Text(
                            text = "Active Burn",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "SENSOR & LOCATION TELEMETRY",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                TelemetryRow(label = "Active Location Source", value = liveStats.activeSource)
                TelemetryRow(label = "GPS Accuracy", value = "± ${liveStats.gpsAccuracyMeters.format(1)} m")
                TelemetryRow(label = "Movement Confidence", value = "${liveStats.movementConfidence}%")
                TelemetryRow(label = "Indoor/Outdoor Status", value = if (liveStats.isIndoor) "Indoor (Estimating)" else "Outdoor Sky Lock")
                TelemetryRow(label = "Elevation Gain / Current", value = "+${liveStats.elevationGainM.format(1)} m / ${liveStats.currentElevationM.format(0)} m")
                TelemetryRow(label = "Battery Forecast", value = "~${liveStats.batteryForecastHours} hrs remaining")
            }
        }
    }
}

@Composable
fun GhostAndSplitsPage(
    liveStats: com.example.miles.engine.LiveWorkoutStats,
    smartEngine: com.example.miles.engine.SmartTrackingEngine,
    formatPace: (Double) -> String
) {
    var selectedTargetPaceSec by remember { mutableStateOf(smartEngine.targetPaceSecPerKm) }

    val quickPaces = listOf(
        270.0 to "4'30\"",
        300.0 to "5'00\"",
        330.0 to "5'30\"",
        360.0 to "6'00\"",
        390.0 to "6'30\""
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE GHOST PACER",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Target: ${formatPace(selectedTargetPaceSec)}/km",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Quick target pace selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPaces.forEach { (sec, label) ->
                        FilterChip(
                            selected = selectedTargetPaceSec == sec,
                            onClick = {
                                selectedTargetPaceSec = sec
                                smartEngine.targetPaceSecPerKm = sec
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val delta = liveStats.ghostDeltaDistanceM ?: 0.0
                val isAhead = delta >= 0.0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isAhead) "+${delta.format(1)} m" else "${delta.format(1)} m",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                            color = if (isAhead) Color(0xFF00E676) else Color(0xFFFF2D55)
                        )
                        Text(
                            text = if (isAhead) "AHEAD OF GHOST PACER" else "BEHIND GHOST PACER",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isAhead) Color(0xFF00E676) else Color(0xFFFF2D55)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Avg: ${formatPace(liveStats.avgPaceSecPerKm)}/km",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Instant: ${formatPace(liveStats.currentPaceSecPerKm)}/km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Visual Relative Delta Gauge Bar (-100m to +100m)
                val clampedFraction = ((delta / 100.0).coerceIn(-1.0, 1.0).toFloat() + 1f) / 2f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = clampedFraction)
                            .background(if (isAhead) Color(0xFF00E676) else Color(0xFFFF2D55))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-100m Behind", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF2D55), fontSize = 10.sp)
                    Text("Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text("+100m Ahead", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00E676), fontSize = 10.sp)
                }
            }
        }

        // Kilometer Splits
        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "KILOMETER SPLITS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                val totalCompletedKm = (liveStats.distanceMeters / 1000.0).toInt()
                if (totalCompletedKm == 0) {
                    Text(
                        text = "No kilometer splits recorded yet.\nYour first split will calculate automatically at 1.00 km.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    for (km in 1..totalCompletedKm) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Kilometer $km",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatPace(liveStats.avgPaceSecPerKm),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}
