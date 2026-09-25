package com.example.miles.ui.dashboard

import android.content.Context
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.example.miles.engine.DeviceSourceType
import com.example.miles.engine.CalorieEstimator
import com.example.miles.engine.MediaIntegration
import com.example.miles.engine.PedometerManager
import com.example.miles.engine.SmartTrackingEngine
import com.example.miles.engine.TrackingState
import com.example.miles.engine.WeatherFetcher
import com.example.miles.engine.WeatherInfo
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel
import com.example.miles.wear.WearCompanionManager
import com.example.miles.wear.WearConnectionStatus
import com.example.miles.widget.MilesWidgetUpdater
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
    onOpenStudio: () -> Unit,
    onOpenTools: () -> Unit = {},
    onOpenPermissionsPrompt: () -> Unit = {},
    onOpenRoutes: () -> Unit = {},
    onOpenStats: () -> Unit = {}
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

    // Battery & Hardware queries
    val batteryManager = remember { context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager }
    val batteryPct = remember {
        batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(1, 100) ?: 82
    }
    val isCharging = remember {
        batteryManager?.isCharging == true
    }

    // Real weather for your location (Open-Meteo — free, no API key)
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }
    LaunchedEffect(Unit) {
        weather = WeatherFetcher.fetch(context)
    }
    val weatherCond = weather?.condition.orEmpty()
    val weatherIcon = when {
        weatherCond.contains("Thunder") -> Icons.Default.Thunderstorm
        weatherCond.contains("Snow") -> Icons.Default.AcUnit
        weatherCond.contains("Fog") -> Icons.Default.Cloud
        weatherCond.contains("Rain") || weatherCond.contains("Drizzle") || weatherCond.contains("Shower") -> Icons.Default.Grain
        weatherCond.contains("Clear") -> Icons.Default.WbSunny
        else -> Icons.Default.Cloud
    }

    // Dashboard Customization Local State
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var cardActivityRings by remember { mutableStateOf(true) }
    var cardQuickSports by remember { mutableStateOf(true) }
    var cardWeather by remember { mutableStateOf(true) }
    var cardTelemetry by remember { mutableStateOf(true) }
    var cardStreaks by remember { mutableStateOf(true) }
    var cardPersonalRecords by remember { mutableStateOf(true) }
    var cardPet by remember { mutableStateOf(true) }
    var cardRecentWorkouts by remember { mutableStateOf(true) }
    var hideCalories by remember { mutableStateOf(false) }
    var hideSteps by remember { mutableStateOf(false) }
    var hideActiveTime by remember { mutableStateOf(false) }
    var layoutMode by remember { mutableStateOf("STANDARD") } // STANDARD, COMPACT, EXPANDED

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

    val todayDurationSec = todayActivities.sumOf { it.durationSeconds }
    val todayDurationMin = todayDurationSec / 60
    // Real calories: finished activities + the in-progress workout + everyday
    // step activity that wasn't part of a recorded workout.
    val liveWorkoutCalories = if (liveStats.state == TrackingState.RECORDING) liveStats.calories else 0
    val workoutStepsToday = todayActivities.sumOf { it.steps } + liveStats.stepCount
    val everydaySteps = (pedometerSteps - workoutStepsToday).coerceAtLeast(0)
    val everydayCalories = CalorieEstimator.dailyActiveFromSteps(everydaySteps, userPreferences.userWeightKg.toDouble())
    val totalCalories = todayActivities.sumOf { it.calories } + liveWorkoutCalories + everydayCalories

    val calorieGoal = userPreferences.dailyCaloriesGoal.coerceAtLeast(100)
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
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(if (layoutMode == "COMPACT") 10.dp else 16.dp)
    ) {
        // 1. TOP HEADER & STUDIO SHORTCUT
        item {
            Spacer(modifier = Modifier.height(8.dp))
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
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showCustomizeDialog = true }) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Customize Dashboard", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(
                        onClick = onOpenStudio,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Studio", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. LIVE HARDWARE STATUS STRIP (Battery, GPS, Health Connect, Wear OS, Weather)
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Weather Chip
                item {
                    StatusChip(
                        icon = weatherIcon,
                        label = weather
                            ?.let { wt ->
                                listOfNotNull(
                                    wt.temperatureC?.let { "${it.roundToInt()}°C" },
                                    wt.condition
                                ).joinToString(" ")
                            }
                            ?: "Weather --",
                        tint = Color(0xFFFFB300),
                        onClick = { cardWeather = !cardWeather }
                    )
                }
                // Battery Chip
                item {
                    StatusChip(
                        icon = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                        label = "$batteryPct% ${if (isCharging) "Charging" else "Battery"}",
                        tint = if (batteryPct > 20) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )
                }
                // GPS Status Chip
                item {
                    StatusChip(
                        icon = Icons.Default.GpsFixed,
                        label = if (liveStats.state != TrackingState.IDLE) "GPS Active" else "GPS Ready",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onOpenTools
                    )
                }
                // Health Connect Status Chip
                item {
                    StatusChip(
                        icon = Icons.Default.Favorite,
                        label = "Health Connect",
                        tint = Color(0xFFFF4081),
                        onClick = onOpenPermissionsPrompt
                    )
                }
                // Wear OS Status Chip
                item {
                    StatusChip(
                        icon = Icons.Default.Watch,
                        label = if (isWatchConnected) "Watch Linked" else "Watch Sync",
                        tint = if (isWatchConnected) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. WORKOUT IN PROGRESS BANNER
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

        // 4. QUICK ACTIONS HUB (Start Workout, Open Maps, Tools, Stats, Health Connect)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionPill(
                    icon = Icons.Default.Map,
                    label = "Maps",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenRoutes
                )
                QuickActionPill(
                    icon = Icons.Default.Explore,
                    label = "Tools",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenTools
                )
                QuickActionPill(
                    icon = Icons.Default.TrendingUp,
                    label = "Statistics",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStats
                )
                QuickActionPill(
                    icon = Icons.Default.Favorite,
                    label = "Health",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenPermissionsPrompt
                )
            }
        }

        // 5. TODAY'S ACTIVITY RINGS CARD
        if (cardActivityRings) {
            item {
                LiquidGlassCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TODAY'S ACTIVITY", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        GoogleFitActivityRings(
                            calories = if (hideCalories) 0 else totalCalories,
                            calorieGoal = calorieGoal,
                            activeMinutes = if (hideActiveTime) 0 else todayDurationMin.toInt(),
                            activeMinGoal = activeMinGoal,
                            steps = if (hideSteps) 0 else todaySteps,
                            stepGoal = stepGoal,
                            heartRateBpm = currentBpm,
                            hasHeartRateDevice = hasHeartRateDevice,
                            isWatchConnected = isWatchConnected
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                            if (!hideCalories) MetricPill(Icons.Default.LocalFireDepartment, "$totalCalories", "kcal")
                            if (!hideActiveTime) MetricPill(Icons.Default.Timer, "$todayDurationMin", "min")
                            MetricPill(Icons.Default.Route, todayDistanceDisplay.format(2), unitLabel)
                            if (hasHeartRateDevice) {
                                val hrText = if (currentBpm != null && currentBpm!! > 0) "$currentBpm" else "--"
                                MetricPill(Icons.Default.Favorite, hrText, "bpm")
                            }
                        }
                    }
                }
            }
        }

        // 6. QUICK START SPORTS
        if (cardQuickSports) {
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
        }

        // 7. WEATHER & ENVIRONMENTAL CONDITIONS CARD
        if (cardWeather) {
            item {
                LiquidGlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(weatherIcon, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("LOCAL WEATHER & CONDITIONS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        weather
                                            ?.let { wt ->
                                                listOfNotNull(
                                                    wt.temperatureC?.let { "${it.roundToInt()}°C" },
                                                    wt.condition
                                                ).joinToString(" • ")
                                            }
                                            ?: "Weather unavailable — enable location & check connection",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Column {
                                Text("WIND", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    weather?.let { wt ->
                                        listOfNotNull(
                                            wt.windSpeedKmh?.let { "${it.roundToInt()} km/h" },
                                            wt.windDirection
                                        ).joinToString(" ")
                                    } ?: "--",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Column {
                                Text("HUMIDITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(weather?.humidityPct?.let { "$it%" } ?: "--", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Column {
                                Text("UV INDEX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    weather?.let { wt ->
                                        listOfNotNull(
                                            wt.uvIndex?.let { "${it.roundToInt()}" },
                                            wt.uvCategory
                                        ).joinToString(" ")
                                    } ?: "--",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                            Column {
                                Text("SUNSET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(weather?.sunset ?: "--", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }
            }
        }

        // 8. DAILY GOALS & STREAKS CARD
        if (cardStreaks) {
            item {
                LiquidGlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("DAILY GOALS & STREAKS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                            Text("🔥 5 Day Streak", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFF5722))
                        }
                        Spacer(Modifier.height(12.dp))

                        // Step Goal Progress
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Steps: $todaySteps / $stepGoal", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            Text("${((todaySteps.toFloat() / stepGoal) * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (todaySteps.toFloat() / stepGoal).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(10.dp))

                        // Calorie Goal Progress
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Calories: $totalCalories / $calorieGoal kcal", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                            Text("${((totalCalories.toFloat() / calorieGoal) * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF5252))
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (totalCalories.toFloat() / calorieGoal).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFFF5252)
                        )
                    }
                }
            }
        }

        // 9. PERSONAL RECORDS HIGHLIGHT CARD
        if (cardPersonalRecords && activities.isNotEmpty()) {
            val fastest = activities.filter { it.avgPaceSecPerKm > 60.0 }.minByOrNull { it.avgPaceSecPerKm }
            val longest = activities.maxByOrNull { it.distanceMeters }
            item {
                LiquidGlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("PERSONAL RECORDS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Column {
                                Text("LONGEST ACTIVITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(longest?.let { "${(it.distanceMeters / 1000.0).format(2)} km" } ?: "--", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("FASTEST PACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(fastest?.let { "${(it.avgPaceSecPerKm / 60.0).format(2)} min/km" } ?: "--", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // 10. REAL-TIME HARDWARE TELEMETRY CARD
        if (cardTelemetry) {
            item {
                LiquidGlassCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("DEVICE & SENSOR TELEMETRY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Column {
                                Text("BATTERY LIFE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$batteryPct% ${if (isCharging) "(Charging)" else "(Good)"}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("LOCAL PRIVACY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("SQLite (100% Offline)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF4CAF50))
                            }
                        }
                    }
                }
            }
        }

        // 11. FITNESS PET COMPANION
        if (cardPet && preferences != null) {
            item { FitnessPetCard(preferences, userPreferences, todaySteps, stepGoal) }
        }

        // 12. MEDIA / MUSIC CONTROLLER
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

        // 13. RECENT WORKOUTS
        if (cardRecentWorkouts) {
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
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    // DASHBOARD CUSTOMIZATION MODAL
    if (showCustomizeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomizeDialog = false },
            title = { Text("Dashboard Customization", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        Text("LAYOUT DENSITY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = layoutMode == "STANDARD", onClick = { layoutMode = "STANDARD" }, label = { Text("Standard") })
                            FilterChip(selected = layoutMode == "COMPACT", onClick = { layoutMode = "COMPACT" }, label = { Text("Compact") })
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("VISIBLE CARDS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                    }
                    item { CustomSwitchRow("Today's Activity Rings", cardActivityRings) { cardActivityRings = it } }
                    item { CustomSwitchRow("Quick Sports Bar", cardQuickSports) { cardQuickSports = it } }
                    item { CustomSwitchRow("Weather & Environmental Card", cardWeather) { cardWeather = it } }
                    item { CustomSwitchRow("Daily Goals & Streaks", cardStreaks) { cardStreaks = it } }
                    item { CustomSwitchRow("Personal Records", cardPersonalRecords) { cardPersonalRecords = it } }
                    item { CustomSwitchRow("Hardware & Battery Telemetry", cardTelemetry) { cardTelemetry = it } }
                    item { CustomSwitchRow("Fitness Pet Companion", cardPet) { cardPet = it } }
                    item { CustomSwitchRow("Recent Workouts", cardRecentWorkouts) { cardRecentWorkouts = it } }

                    item {
                        Spacer(Modifier.height(14.dp))
                        Text("METRIC PRIVACY & VISIBILITY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                    }
                    item { CustomSwitchRow("Hide Calories", hideCalories) { hideCalories = it } }
                    item { CustomSwitchRow("Hide Steps", hideSteps) { hideSteps = it } }
                    item { CustomSwitchRow("Hide Active Minutes", hideActiveTime) { hideActiveTime = it } }
                }
            },
            confirmButton = {
                Button(onClick = { showCustomizeDialog = false }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = {
                    cardActivityRings = true
                    cardQuickSports = true
                    cardWeather = true
                    cardTelemetry = true
                    cardStreaks = true
                    cardPersonalRecords = true
                    cardPet = true
                    cardRecentWorkouts = true
                    hideCalories = false
                    hideSteps = false
                    hideActiveTime = false
                    layoutMode = "STANDARD"
                }) { Text("Reset to Defaults") }
            }
        )
    }
}

@Composable
private fun StatusChip(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

@Composable
private fun QuickActionPill(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun CustomSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
    heartRateBpm: Int? = null,
    hasHeartRateDevice: Boolean = false,
    isWatchConnected: Boolean = false
) {
    val calRatio = (calories.toFloat() / calorieGoal).coerceIn(0f, 1.5f)
    val actRatio = (activeMinutes.toFloat() / activeMinGoal).coerceIn(0f, 1.5f)
    val stepRatio = (steps.toFloat() / stepGoal).coerceIn(0f, 1.5f)

    Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeW = 13.dp.toPx()
            val spacing = 5.dp.toPx()

            val rOuter = size.width / 2f - strokeW / 2f
            val rMid = rOuter - strokeW - spacing
            val rInner = rMid - strokeW - spacing

            drawCircle(Color(0xFFE53935).copy(alpha = 0.20f), radius = rOuter, center = center, style = Stroke(strokeW))
            drawCircle(Color(0xFF00E676).copy(alpha = 0.20f), radius = rMid, center = center, style = Stroke(strokeW))
            drawCircle(Color(0xFF2979FF).copy(alpha = 0.20f), radius = rInner, center = center, style = Stroke(strokeW))

            drawArc(Color(0xFFE53935), startAngle = -90f, sweepAngle = calRatio * 360f, useCenter = false, topLeft = Offset(center.x - rOuter, center.y - rOuter), size = Size(rOuter * 2, rOuter * 2), style = Stroke(strokeW, cap = StrokeCap.Round))
            drawArc(Color(0xFF00E676), startAngle = -90f, sweepAngle = actRatio * 360f, useCenter = false, topLeft = Offset(center.x - rMid, center.y - rMid), size = Size(rMid * 2, rMid * 2), style = Stroke(strokeW, cap = StrokeCap.Round))
            drawArc(Color(0xFF2979FF), startAngle = -90f, sweepAngle = stepRatio * 360f, useCenter = false, topLeft = Offset(center.x - rInner, center.y - rInner), size = Size(rInner * 2, rInner * 2), style = Stroke(strokeW, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.getDefault(), "%,d", steps),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 24.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "STEPS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                color = Color(0xFF2979FF)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%,d", calories)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFE53935)
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
