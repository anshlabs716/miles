package com.example.miles.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.DistanceUnit
import com.example.miles.data.local.UserPreferences
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.repository.format
import com.example.miles.engine.DeviceManager
import com.example.miles.engine.MediaIntegration
import com.example.miles.engine.SmartTrackingEngine
import com.example.miles.engine.TrackingState
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel
import java.text.SimpleDateFormat
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilledTonalButton
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.miles.data.local.MilesPreferences
import com.example.miles.engine.PedometerManager

@Composable
fun DashboardScreen(
    smartEngine: SmartTrackingEngine,
    mediaIntegration: MediaIntegration,
    deviceManager: DeviceManager,
    activities: List<ActivityEntity>,
    userPreferences: UserPreferences,
    preferences: MilesPreferences? = null,
    pedometerManager: PedometerManager? = null,
    onStartActivity: (ActivityType) -> Unit,
    onNavigateToHud: () -> Unit,
    onSelectActivity: (ActivityEntity) -> Unit,
    onOpenStudio: () -> Unit
) {
    val liveStats by smartEngine.liveStats.collectAsState()
    val mediaTrack by mediaIntegration.currentTrack.collectAsState()

    // Real hardware steps combined with recorded workout steps
    val pedometerSteps by (pedometerManager?.todaySteps?.collectAsState() ?: remember { mutableIntStateOf(0) })

    // Calculate real daily summaries strictly from user's activities
    val todayStart = rememberTodayStartTimestamp()
    val todayActivities = activities.filter { it.startTime >= todayStart }
    val workoutSteps = todayActivities.sumOf { it.steps }
    val rawSteps = maxOf(pedometerSteps, workoutSteps)

    // Fluid stopwatch-like counter animation every time the user opens the app
    val animatedStepCounter = remember { Animatable(0f) }
    LaunchedEffect(rawSteps) {
        animatedStepCounter.animateTo(
            targetValue = rawSteps.toFloat(),
            animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
        )
    }
    val todaySteps = animatedStepCounter.value.toInt()
    val todayDistanceM = todayActivities.sumOf { it.distanceMeters }
    val isMetric = userPreferences.unit == DistanceUnit.METRIC
    val todayDistanceDisplay = if (isMetric) todayDistanceM / 1000.0 else todayDistanceM * 0.000621371
    val unitLabel = if (isMetric) "km" else "mi"
    val todayDurationMin = todayActivities.sumOf { it.durationSeconds } / 60
    val todayCalories = todayActivities.sumOf { it.calories }

    val stepGoal = userPreferences.dailyStepGoal.coerceAtLeast(1000)
    val activeMinGoal = userPreferences.dailyActiveMinutesGoal.coerceAtLeast(10)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            // Neat, spacious greeting header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(userPreferences.appIcon.colorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = "App Icon",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val greeting = when {
                            hour < 12 -> "Good morning"
                            hour < 17 -> "Good afternoon"
                            else -> "Good evening"
                        }
                        val namePart = if (userPreferences.userName.isNotBlank()) ", ${userPreferences.userName}" else ""
                        Text(
                            text = "$greeting$namePart",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenStudio,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Miles Studio", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Studio", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    // Privacy Indicator Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Local-Only",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Active recording banner (if workout is currently running in background)
        if (liveStats.state != TrackingState.IDLE) {
            item {
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHud() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (liveStats.state == TrackingState.RECORDING) Color(0xFF00E676) else Color(0xFFFF9100))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (liveStats.state == TrackingState.RECORDING) "WORKOUT IN PROGRESS" else "WORKOUT PAUSED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${liveStats.activityType.displayName} • ${(liveStats.distanceMeters / 1000.0).format(2)} km",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Duration: ${smartEngine.formatDuration(liveStats.elapsedSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onNavigateToHud() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open HUD", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Google Fit-Inspired Concentric Activity Ring Hero Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TODAY'S ACTIVITY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Concentric Google Fit Style Activity Rings
                    GoogleFitActivityRings(
                        steps = todaySteps,
                        stepGoal = stepGoal,
                        activeMinutes = todayDurationMin.toInt(),
                        activeMinGoal = activeMinGoal
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Trio of clean, neat metric chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricPill(
                            icon = Icons.Default.Timer,
                            value = "$todayDurationMin",
                            unit = "min",
                            color = Color(0xFF00E676)
                        )
                        MetricPill(
                            icon = Icons.Default.Route,
                            value = todayDistanceDisplay.format(2),
                            unit = unitLabel,
                            color = Color(0xFF00B0FF)
                        )
                        MetricPill(
                            icon = Icons.Default.LocalFireDepartment,
                            value = "$todayCalories",
                            unit = "kcal",
                            color = Color(0xFFFF9100)
                        )
                    }
                }
            }
        }

        // Virtual Fitness Pet Companion (Parrot, Bunny, Dog, Cat, or Keep Off) & Lazy Day Manager
        if (preferences != null) {
            item {
                FitnessPetCard(
                    preferences = preferences,
                    userPreferences = userPreferences,
                    todaySteps = todaySteps,
                    stepGoal = stepGoal
                )
            }
        }

        // Quick Start Activities (Fluid sport selector)
        item {
            Column {
                Text(
                    text = "Quick Start Workout",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Place preferred sport first
                    val sportsList = mutableListOf(
                        Triple(ActivityType.RUNNING, "Run", Icons.AutoMirrored.Filled.DirectionsRun),
                        Triple(ActivityType.WALKING, "Walk", Icons.AutoMirrored.Filled.DirectionsWalk),
                        Triple(ActivityType.CYCLING, "Ride", Icons.Default.DirectionsBike),
                        Triple(ActivityType.HIKING, "Hike", Icons.Default.NordicWalking)
                    )
                    val primaryType = ActivityType.fromString(userPreferences.primarySport)
                    val sortedSports = sportsList.sortedByDescending { it.first == primaryType }

                    items(sortedSports) { (type, name, icon) ->
                        val isPreferred = type == primaryType
                        FluidSportCard(
                            name = name,
                            icon = icon,
                            isPreferred = isPreferred,
                            onClick = { onStartActivity(type) }
                        )
                    }
                }
            }
        }

        // Music integration mini card (only if real media playback is active)
        mediaTrack?.let { track ->
            item {
                LiquidGlassPanel(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Music",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${track.artist} • ${track.appName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { mediaIntegration.togglePlayPause() }) {
                                Icon(
                                    imageVector = if (track.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Workouts Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Workouts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (activities.isNotEmpty()) {
                    Text(
                        text = "${activities.size} saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (activities.isEmpty()) {
            item {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Ready for your first workout?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap any activity above to start tracking. Your route, pace, and health metrics will be saved 100% locally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(activities.take(3)) { activity ->
                CleanActivityCard(
                    activity = activity,
                    isMetric = isMetric,
                    onClick = { onSelectActivity(activity) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Google Fit style dual concentric activity rings:
 * Outer Ring: Active Minutes (Emerald Green)
 * Inner Ring: Step Count (Electric Cyan)
 */
@Composable
fun GoogleFitActivityRings(
    steps: Int,
    stepGoal: Int,
    activeMinutes: Int,
    activeMinGoal: Int
) {
    val stepProgress = (steps.toFloat() / stepGoal).coerceIn(0f, 1f)
    val activeMinProgress = (activeMinutes.toFloat() / activeMinGoal).coerceIn(0f, 1f)

    val animatedStepProgress by animateFloatAsState(
        targetValue = stepProgress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "stepProgress"
    )

    val animatedMinProgress by animateFloatAsState(
        targetValue = activeMinProgress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "minProgress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(190.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val outerRadius = (size.minDimension - strokeWidth) / 2f
            val innerRadius = outerRadius - strokeWidth - 6.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer Ring Background (Active Minutes)
            drawCircle(
                color = Color(0xFF00E676).copy(alpha = 0.15f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Outer Ring Active Sweep (Active Minutes)
            if (animatedMinProgress > 0f) {
                drawArc(
                    color = Color(0xFF00E676),
                    startAngle = -90f,
                    sweepAngle = animatedMinProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    size = Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Inner Ring Background (Steps)
            drawCircle(
                color = Color(0xFF00B0FF).copy(alpha = 0.15f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Inner Ring Active Sweep (Steps)
            if (animatedStepProgress > 0f) {
                drawArc(
                    color = Color(0xFF00B0FF),
                    startAngle = -90f,
                    sweepAngle = animatedStepProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%,d", steps),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "of ${String.format(Locale.getDefault(), "%,d", stepGoal)} steps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${activeMinutes}m active",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF00E676)
                )
            }
        }
    }
}

@Composable
fun MetricPill(
    icon: ImageVector,
    value: String,
    unit: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FluidSportCard(
    name: String,
    icon: ImageVector,
    isPreferred: Boolean,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .width(108.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPreferred) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = if (isPreferred) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isPreferred) "Primary" else "Start GPS",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPreferred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CleanActivityCard(
    activity: ActivityEntity,
    isMetric: Boolean,
    onClick: () -> Unit
) {
    val distanceDisplay = if (isMetric) activity.distanceMeters / 1000.0 else activity.distanceMeters * 0.000621371
    val unitLabel = if (isMetric) "km" else "mi"

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (ActivityType.fromString(activity.activityType)) {
                        ActivityType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
                        ActivityType.CYCLING -> Icons.Default.DirectionsBike
                        ActivityType.HIKING -> Icons.Default.NordicWalking
                        else -> Icons.AutoMirrored.Filled.DirectionsWalk
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = activity.activityType,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${distanceDisplay.format(2)} $unitLabel • ${activity.durationSeconds / 60}m • ${activity.calories} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()).format(Date(activity.startTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun rememberTodayStartTimestamp(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
