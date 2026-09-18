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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.platform.LocalContext
import com.example.miles.engine.DeviceSourceType
import com.example.miles.wear.WearCompanionManager
import com.example.miles.wear.WearConnectionStatus
import com.example.miles.widget.MilesWidgetUpdater
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.DistanceUnit
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.local.UserPreferences
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.repository.format
import com.example.miles.engine.DeviceManager
import com.example.miles.engine.MediaIntegration
import com.example.miles.engine.PedometerManager
import com.example.miles.engine.SmartTrackingEngine
import com.example.miles.engine.TrackingState
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    smartEngine: SmartTrackingEngine,
    mediaIntegration: MediaIntegration,
    deviceManager: DeviceManager,
    activities: List<ActivityEntity>,
    userPreferences: UserPreferences,
    preferences: MilesPreferences? = null,
    pedometerManager: PedometerManager? = null,
    wearCompanion: WearCompanionManager? = null,
    onStartActivity: (ActivityType) -> Unit,
    onNavigateToHud: () -> Unit,
    onSelectActivity: (ActivityEntity) -> Unit,
    onOpenStudio: () -> Unit
) {
    val context = LocalContext.current
    val liveStats by smartEngine.liveStats.collectAsState()
    val mediaTrack by mediaIntegration.currentTrack.collectAsState()
    val pedometerSteps by (pedometerManager?.todaySteps?.collectAsState() ?: remember { mutableIntStateOf(0) })
    val currentBpm by deviceManager.heartRateBpm.collectAsState()
    val hasHrCap by deviceManager.hasHeartRateCapability.collectAsState()
    val deviceSources by deviceManager.sources.collectAsState()
    val wearStatus by (wearCompanion?.connectionStatus?.collectAsState() ?: remember { mutableStateOf(WearConnectionStatus.DISCONNECTED) })
    val isWatchConnected = wearStatus == WearConnectionStatus.CONNECTED || deviceSources.any { it.type == DeviceSourceType.WEAR_OS_SENSOR && it.isConnected }
    val hasHeartRateDevice = hasHrCap || (currentBpm != null && currentBpm!! > 0) || isWatchConnected

    val todayStart = rememberTodayStartTimestamp()
    val todayActivities = activities.filter { it.startTime >= todayStart }
    val workoutSteps = todayActivities.sumOf { it.steps }
    val rawSteps = maxOf(pedometerSteps, workoutSteps)
    val animatedStepCounter = remember { Animatable(0f) }
    LaunchedEffect(rawSteps) {
        animatedStepCounter.animateTo(rawSteps.toFloat(), tween(1400, easing = FastOutSlowInEasing))
    }
    val todaySteps = animatedStepCounter.value.toInt()
    val todayDistanceM = todayActivities.sumOf { it.distanceMeters }
    val isMetric = userPreferences.unit == DistanceUnit.METRIC
    val todayDistanceDisplay = if (isMetric) todayDistanceM / 1000.0 else todayDistanceM * 0.000621371
    val unitLabel = if (isMetric) "km" else "mi"
    val todayDurationMin = todayActivities.sumOf { it.durationSeconds } / 60
    val workoutCalories = todayActivities.sumOf { it.calories }

    // Enhanced calorie estimation: base step burn + workout calories.
    // When watch or heart-rate sensor is paired, calorie burn integrates elevated physiological exertion!
    val hrFactor = if (isWatchConnected && currentBpm != null && currentBpm!! > 90) 1.25f else 1.0f
    val stepCalories = (todaySteps * 0.045f * hrFactor).toInt()
    val totalCalories = maxOf(workoutCalories, stepCalories + workoutCalories)
    val calorieGoal = 500.coerceAtLeast(100)
    val stepGoal = userPreferences.dailyStepGoal.coerceAtLeast(1000)
    val activeMinGoal = userPreferences.dailyActiveMinutesGoal.coerceAtLeast(10)

    // Synchronize data with home screen widgets
    LaunchedEffect(totalCalories, calorieGoal, todaySteps, stepGoal, todayDurationMin, activeMinGoal, currentBpm, isWatchConnected, liveStats.state) {
        MilesWidgetUpdater.updateAllWidgets(
            context = context,
            calories = totalCalories,
            calGoal = calorieGoal,
            steps = todaySteps,
            stepGoal = stepGoal,
            activeMin = todayDurationMin.toInt(),
            activeGoal = activeMinGoal,
            hrBpm = currentBpm,
            isWatchConnected = isWatchConnected,
            isRecording = liveStats.state == TrackingState.RECORDING,
            sportName = liveStats.activityType.displayName
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Color(userPreferences.appIcon.colorHex)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, "App Icon", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val greeting = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
                        val namePart = if (userPreferences.userName.isNotBlank()) ", ${userPreferences.userName}" else ""
                        Text("$greeting$namePart", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
                        Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onOpenStudio,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Studio",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Studio", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (liveStats.state != TrackingState.IDLE) {
            item {
                LiquidGlassCard(Modifier.fillMaxWidth().clickable { onNavigateToHud() }) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column {
                            Text(if (liveStats.state == TrackingState.RECORDING) "WORKOUT IN PROGRESS" else "WORKOUT PAUSED", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text("${liveStats.activityType.displayName} • ${(liveStats.distanceMeters / 1000.0).format(2)} km", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Duration: ${smartEngine.formatDuration(liveStats.elapsedSeconds)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = onNavigateToHud, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Open HUD") }
                    }
                }
            }
        }

        item {
            LiquidGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TODAY'S ACTIVITY", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(20.dp))
                    GoogleFitActivityRings(
                        calories = totalCalories,
                        calorieGoal = calorieGoal,
                        activeMinutes = todayDurationMin.toInt(),
                        activeMinGoal = activeMinGoal,
                        steps = todaySteps,
                        stepGoal = stepGoal,
                        heartRateBpm = currentBpm,
                        hasHeartRateDevice = hasHeartRateDevice,
                        isWatchConnected = isWatchConnected
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                        MetricPill(Icons.Default.LocalFireDepartment, "$totalCalories", "kcal")
                        MetricPill(Icons.Default.Timer, "$todayDurationMin", "min")
                        MetricPill(Icons.Default.Route, todayDistanceDisplay.format(2), unitLabel)
                        if (hasHeartRateDevice) {
                            val hrText = if (currentBpm != null && currentBpm!! > 0) "$currentBpm" else "--"
                            MetricPill(Icons.Default.Favorite, hrText, "bpm")
                        }
                    }
                }
            }
        }

        if (preferences != null) {
            item { FitnessPetCard(preferences, userPreferences, todaySteps, stepGoal) }
        }

        item {
            Column {
                Text("Quick Start Workout", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val sports = listOf(
                        Triple(ActivityType.RUNNING, "Run", Icons.AutoMirrored.Filled.DirectionsRun),
                        Triple(ActivityType.WALKING, "Walk", Icons.AutoMirrored.Filled.DirectionsWalk),
                        Triple(ActivityType.CYCLING, "Ride", Icons.Default.DirectionsBike),
                        Triple(ActivityType.HIKING, "Hike", Icons.Default.NordicWalking)
                    )
                    val primary = ActivityType.fromString(userPreferences.primarySport)
                    items(sports.sortedByDescending { it.first == primary }) { (type, name, icon) ->
                        FluidSportCard(name, icon, type == primary) { onStartActivity(type) }
                    }
                }
            }
        }

        mediaTrack?.let { track ->
            item {
                LiquidGlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, "Music", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${track.artist} • ${track.appName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                        IconButton(onClick = { mediaIntegration.togglePlayPause() }) { Icon(if (track.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause") }
                    }
                }
            }
        }

        item { Text("Recent Workouts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
        if (activities.isEmpty()) {
            item {
                LiquidGlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(14.dp))
                        Text("Ready for your first workout?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(6.dp))
                        Text("Tap an activity above to start tracking. Your route and activity data are saved locally.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(activities.take(3)) { activity -> CleanActivityCard(activity, isMetric) { onSelectActivity(activity) } }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun GoogleFitActivityRings(
    calories: Int,
    calorieGoal: Int,
    activeMinutes: Int,
    activeMinGoal: Int,
    steps: Int,
    stepGoal: Int,
    heartRateBpm: Int?,
    hasHeartRateDevice: Boolean,
    isWatchConnected: Boolean = false
) {
    val calProgress = (calories.toFloat() / calorieGoal).coerceIn(0f, 1f)
    val activeProgress = (activeMinutes.toFloat() / activeMinGoal).coerceIn(0f, 1f)
    val stepProgress = (steps.toFloat() / stepGoal).coerceIn(0f, 1f)
    val hrProgress = if (heartRateBpm != null && heartRateBpm > 40) {
        ((heartRateBpm - 40).toFloat() / 140f).coerceIn(0.1f, 1f)
    } else {
        0.35f
    }

    val animatedCal by animateFloatAsState(calProgress, tween(1000, easing = FastOutSlowInEasing), label = "calProgress")
    val animatedMinutes by animateFloatAsState(activeProgress, tween(1000, easing = FastOutSlowInEasing), label = "minProgress")
    val animatedSteps by animateFloatAsState(stepProgress, tween(1000, easing = FastOutSlowInEasing), label = "stepProgress")
    val animatedHr by animateFloatAsState(if (hasHeartRateDevice) hrProgress else 0f, tween(800, easing = FastOutSlowInEasing), label = "hrProgress")

    val ringSize = if (hasHeartRateDevice) 210.dp else 195.dp
    val strokeWidth = if (hasHeartRateDevice) 10.dp else 13.dp
    val strokeGap = 4.dp

    Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val gap = strokeGap.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            // Ring 1 (Outermost): Calories Burned - Coral Flame (#FF5722)
            val r1 = (size.minDimension - stroke) / 2f
            drawCircle(Color(0xFFFF5722).copy(alpha = 0.15f), r1, center, style = Stroke(stroke))
            if (animatedCal > 0) {
                drawArc(
                    color = Color(0xFFFF5722),
                    startAngle = -90f,
                    sweepAngle = animatedCal * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - r1, center.y - r1),
                    size = Size(r1 * 2, r1 * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }

            // Ring 2: Active Minutes - Vivid Green (#00E676)
            val r2 = r1 - stroke - gap
            drawCircle(Color(0xFF00E676).copy(alpha = 0.15f), r2, center, style = Stroke(stroke))
            if (animatedMinutes > 0) {
                drawArc(
                    color = Color(0xFF00E676),
                    startAngle = -90f,
                    sweepAngle = animatedMinutes * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - r2, center.y - r2),
                    size = Size(r2 * 2, r2 * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }

            // Ring 3: Steps - Neon Cyan (#00B0FF)
            val r3 = r2 - stroke - gap
            drawCircle(Color(0xFF00B0FF).copy(alpha = 0.15f), r3, center, style = Stroke(stroke))
            if (animatedSteps > 0) {
                drawArc(
                    color = Color(0xFF00B0FF),
                    startAngle = -90f,
                    sweepAngle = animatedSteps * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - r3, center.y - r3),
                    size = Size(r3 * 2, r3 * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }

            // Ring 4 (Innermost): Heart Rate BPM - Crimson Flame (#FF1744)
            // USER DIRECTIVE: "and add bpm but if no watch paired or device that can check bpm no ring for bpm"
            if (hasHeartRateDevice) {
                val r4 = r3 - stroke - gap
                drawCircle(Color(0xFFFF1744).copy(alpha = 0.15f), r4, center, style = Stroke(stroke))
                if (animatedHr > 0) {
                    drawArc(
                        color = Color(0xFFFF1744),
                        startAngle = -90f,
                        sweepAngle = animatedHr * 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - r4, center.y - r4),
                        size = Size(r4 * 2, r4 * 2),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Main hero number: STEPS COUNT (large primary display)
            Text(
                text = String.format(Locale.getDefault(), "%,d", steps),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = Color(0xFF00B0FF)
            )
            Text(
                text = "of ${String.format(Locale.getDefault(), "%,d", stepGoal)} steps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(3.dp))
            // Calories burned clearly displayed alongside active minutes
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔥 ${String.format(Locale.getDefault(), "%,d", calories)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                )
                Text(
                    text = " / ${String.format(Locale.getDefault(), "%,d", calorieGoal)} kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " • ${activeMinutes}m",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF00E676)
                )
            }
            if (hasHeartRateDevice) {
                Spacer(Modifier.height(3.dp))
                val hrDisplay = if (heartRateBpm != null && heartRateBpm > 0) "$heartRateBpm bpm" else if (isWatchConnected) "Watch Linked" else "HR Ready"
                Text(
                    text = "❤️ $hrDisplay",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFF1744)
                )
            }
        }
    }
}

@Composable
fun MetricPill(icon: ImageVector, value: String, unit: String) {
    Row(Modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = .10f)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp)); Text(value, fontWeight = FontWeight.Bold); Spacer(Modifier.width(3.dp)); Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FluidSportCard(name: String, icon: ImageVector, isPreferred: Boolean, onClick: () -> Unit) {
    LiquidGlassCard(Modifier.width(108.dp).clickable { onClick() }, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(if (isPreferred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(icon, name, tint = if (isPreferred) Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp)); Text(name, fontWeight = FontWeight.Bold); Text(if (isPreferred) "Primary" else "Start GPS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CleanActivityCard(activity: ActivityEntity, isMetric: Boolean, onClick: () -> Unit) {
    val distance = if (isMetric) activity.distanceMeters / 1000.0 else activity.distanceMeters * .000621371
    val unit = if (isMetric) "km" else "mi"
    LiquidGlassCard(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(activity.title, fontWeight = FontWeight.Bold)
                    Text("${distance.format(2)} $unit • ${activity.durationSeconds / 60}m • ${activity.calories} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()).format(Date(activity.startTime)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "View Details", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun rememberTodayStartTimestamp(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
