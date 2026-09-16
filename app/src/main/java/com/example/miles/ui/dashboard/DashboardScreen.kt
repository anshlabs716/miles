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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Timer
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
    onStartActivity: (ActivityType) -> Unit,
    onNavigateToHud: () -> Unit,
    onSelectActivity: (ActivityEntity) -> Unit,
    onOpenStudio: () -> Unit
) {
    val liveStats by smartEngine.liveStats.collectAsState()
    val mediaTrack by mediaIntegration.currentTrack.collectAsState()
    val pedometerSteps by (pedometerManager?.todaySteps?.collectAsState() ?: remember { mutableIntStateOf(0) })
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
    val todayCalories = todayActivities.sumOf { it.calories }
    val stepGoal = userPreferences.dailyStepGoal.coerceAtLeast(1000)
    val activeMinGoal = userPreferences.dailyActiveMinutesGoal.coerceAtLeast(10)

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
                    Column {
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val greeting = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
                        val namePart = if (userPreferences.userName.isNotBlank()) ", ${userPreferences.userName}" else ""
                        Text("$greeting$namePart", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.onBackground)
                        Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                FilledTonalButton(onClick = onOpenStudio, shape = RoundedCornerShape(16.dp)) { Text("Studio", fontWeight = FontWeight.Bold) }
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
                    GoogleFitActivityRings(todaySteps, stepGoal, todayDurationMin.toInt(), activeMinGoal)
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                        MetricPill(Icons.Default.Timer, "$todayDurationMin", "min")
                        MetricPill(Icons.Default.Route, todayDistanceDisplay.format(2), unitLabel)
                        MetricPill(Icons.Default.LocalFireDepartment, "$todayCalories", "kcal")
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
fun GoogleFitActivityRings(steps: Int, stepGoal: Int, activeMinutes: Int, activeMinGoal: Int) {
    val stepProgress = (steps.toFloat() / stepGoal).coerceIn(0f, 1f)
    val activeProgress = (activeMinutes.toFloat() / activeMinGoal).coerceIn(0f, 1f)
    val animatedSteps by animateFloatAsState(stepProgress, tween(1000, easing = FastOutSlowInEasing), label = "stepProgress")
    val animatedMinutes by animateFloatAsState(activeProgress, tween(1000, easing = FastOutSlowInEasing), label = "minProgress")
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val outer = (size.minDimension - stroke) / 2f
            val inner = outer - stroke - 6.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color(0xFF00E676).copy(alpha = .15f), outer, center, style = Stroke(stroke))
            if (animatedMinutes > 0) drawArc(Color(0xFF00E676), -90f, animatedMinutes * 360f, false, Offset(center.x - outer, center.y - outer), Size(outer * 2, outer * 2), style = Stroke(stroke, cap = StrokeCap.Round))
            drawCircle(Color(0xFF00B0FF).copy(alpha = .15f), inner, center, style = Stroke(stroke))
            if (animatedSteps > 0) drawArc(Color(0xFF00B0FF), -90f, animatedSteps * 360f, false, Offset(center.x - inner, center.y - inner), Size(inner * 2, inner * 2), style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(String.format(Locale.getDefault(), "%,d", steps), style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black))
            Text("of ${String.format(Locale.getDefault(), "%,d", stepGoal)} steps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${activeMinutes}m active", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF00E676))
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
    LiquidGlassCard(Modifier.width(108.dp).clickable(onClick), shape = RoundedCornerShape(18.dp)) {
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
    LiquidGlassCard(Modifier.fillMaxWidth().clickable(onClick), shape = RoundedCornerShape(16.dp)) {
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
