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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.DistanceUnit
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.local.UserPreferences
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.engine.DeviceManager
import com.example.miles.engine.PedometerManager
import com.example.miles.engine.SmartTrackingEngine
import com.example.miles.engine.MediaIntegration
import kotlin.math.max

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
    val rawSteps = max(pedometerSteps, workoutSteps)

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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Existing dashboard content remains below this real-data header.
        item {
            Text(
                text = todaySteps.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun rememberTodayStartTimestamp(): Long {
    val now = java.time.LocalDate.now()
    return now.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}
