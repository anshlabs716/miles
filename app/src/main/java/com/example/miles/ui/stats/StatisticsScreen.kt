package com.example.miles.ui.stats

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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.repository.format
import com.example.miles.engine.StreakCalculator
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class StatsTimeFrame(val label: String, val days: Int) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    YEAR("Year", 365),
    ALL_TIME("All Time", 3650)
}

data class FitnessBadge(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val isUnlocked: Boolean,
    val progressPercent: Float,
    val progressDetail: String
)

@Composable
fun StatisticsScreen(
    activities: List<ActivityEntity>,
    stepGoal: Int = 8000
) {
    var selectedTimeFrame by remember { mutableStateOf(StatsTimeFrame.WEEK) }
    var selectedBadgeDetail by remember { mutableStateOf<FitnessBadge?>(null) }

    // Filter activities according to the selected time horizon
    val now = System.currentTimeMillis()
    val frameStart = now - (selectedTimeFrame.days * 86400000L)
    val previousFrameStart = frameStart - (selectedTimeFrame.days * 86400000L)

    val currentPeriodActivities = activities.filter { it.startTime >= frameStart }
    val previousPeriodActivities = activities.filter { it.startTime in previousFrameStart until frameStart }

    // Current period aggregations
    val periodDistanceKm = currentPeriodActivities.sumOf { it.distanceMeters } / 1000.0
    val prevPeriodDistanceKm = previousPeriodActivities.sumOf { it.distanceMeters } / 1000.0
    val distanceDeltaPct = if (prevPeriodDistanceKm > 0) ((periodDistanceKm - prevPeriodDistanceKm) / prevPeriodDistanceKm) * 100 else 0.0

    val periodWorkoutsCount = currentPeriodActivities.size
    val periodDurationHours = currentPeriodActivities.sumOf { it.durationSeconds } / 3600.0
    val periodCalories = currentPeriodActivities.sumOf { it.calories }
    val periodSteps = currentPeriodActivities.sumOf { it.steps }

    // Lifetime totals
    val totalDistanceKm = activities.sumOf { it.distanceMeters } / 1000.0
    val totalCalories = activities.sumOf { it.calories }
    val totalWorkouts = activities.size

    // Sport breakdown
    val runKm = currentPeriodActivities.filter { it.activityType == ActivityType.RUNNING.name }.sumOf { it.distanceMeters } / 1000.0
    val cycleKm = currentPeriodActivities.filter { it.activityType == ActivityType.CYCLING.name }.sumOf { it.distanceMeters } / 1000.0
    val walkKm = currentPeriodActivities.filter { it.activityType == ActivityType.WALKING.name }.sumOf { it.distanceMeters } / 1000.0
    val hikeKm = currentPeriodActivities.filter { it.activityType == ActivityType.HIKING.name }.sumOf { it.distanceMeters } / 1000.0
    val totalSportKm = (runKm + cycleKm + walkKm + hikeKm).coerceAtLeast(0.1)

    // Dynamic Personal Records
    val fastestActivity = activities.filter { it.avgPaceSecPerKm > 60.0 }.minByOrNull { it.avgPaceSecPerKm }
    val longestActivity = activities.maxByOrNull { it.distanceMeters }
    val maxElevationActivity = activities.filter { it.elevationGainM > 0 }.maxByOrNull { it.elevationGainM }
    val highestCalorieActivity = activities.maxByOrNull { it.calories }

    // 7-day or 30-day dynamic chart data
    val numBars = if (selectedTimeFrame == StatsTimeFrame.WEEK) 7 else 14
    val barLabels = mutableListOf<String>()
    val barDistances = mutableListOf<Float>()
    val dayFormat = SimpleDateFormat(if (selectedTimeFrame == StatsTimeFrame.WEEK) "EEE" else "d", Locale.getDefault())

    for (i in (numBars - 1) downTo 0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dStart = cal.timeInMillis
        val dEnd = dStart + 86400000L

        barLabels.add(dayFormat.format(Date(dStart)))
        val distKm = activities.filter { it.startTime in dStart until dEnd }.sumOf { it.distanceMeters } / 1000.0
        barDistances.add(distKm.toFloat())
    }

    // Dynamic current streak (shared real logic: goal-meeting days, not just activity days)
    val currentStreak = remember(activities, stepGoal) {
        val todayKey = StreakCalculator.todayKey()
        val metDays = StreakCalculator.metDaysFromActivities(
            activitySteps = activities.map { it.startTime to it.steps },
            stepGoal = stepGoal
        )
        StreakCalculator.currentStreak(metDays, todayKey)
    }

    // Milestones and Badges List
    val maxDistanceM = activities.maxOfOrNull { it.distanceMeters } ?: 0.0
    val maxElevationM = activities.maxOfOrNull { it.elevationGainM } ?: 0.0

    val badges = listOf(
        FitnessBadge(
            id = "first_5k",
            title = "5K Pioneer",
            description = "Complete any single outdoor run, walk, or hike of at least 5.0 km.",
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            color = Color(0xFF2979FF),
            isUnlocked = maxDistanceM >= 5000.0,
            progressPercent = (maxDistanceM / 5000.0).toFloat().coerceIn(0f, 1f),
            progressDetail = "${(maxDistanceM / 1000.0).format(1)} / 5.0 km"
        ),
        FitnessBadge(
            id = "first_10k",
            title = "10K Conqueror",
            description = "Complete any workout of at least 10.0 km.",
            icon = Icons.Default.EmojiEvents,
            color = Color(0xFFFF9100),
            isUnlocked = maxDistanceM >= 10000.0,
            progressPercent = (maxDistanceM / 1000.0 / 10.0).toFloat().coerceIn(0f, 1f),
            progressDetail = "${(maxDistanceM / 1000.0).format(1)} / 10.0 km"
        ),
        FitnessBadge(
            id = "half_marathon",
            title = "Half Marathoner",
            description = "Complete 21.1 km in a single session.",
            icon = Icons.Default.EmojiEvents,
            color = Color(0xFFFF3D00),
            isUnlocked = maxDistanceM >= 21100.0,
            progressPercent = (maxDistanceM / 21100.0).toFloat().coerceIn(0f, 1f),
            progressDetail = "${(maxDistanceM / 1000.0).format(1)} / 21.1 km"
        ),
        FitnessBadge(
            id = "century_cyclist",
            title = "Century Cyclist",
            description = "Ride 50+ km on a bike in a single workout.",
            icon = Icons.Default.DirectionsBike,
            color = Color(0xFF00E676),
            isUnlocked = activities.filter { it.activityType == ActivityType.CYCLING.name }.any { it.distanceMeters >= 50000.0 },
            progressPercent = (activities.filter { it.activityType == ActivityType.CYCLING.name }.maxOfOrNull { it.distanceMeters }?.div(50000.0) ?: 0.0).toFloat().coerceIn(0f, 1f),
            progressDetail = "Cycling endurance"
        ),
        FitnessBadge(
            id = "peak_climber",
            title = "Mountain Climber",
            description = "Ascend 200+ meters of elevation in a single activity.",
            icon = Icons.Default.Terrain,
            color = Color(0xFFAB47BC),
            isUnlocked = maxElevationM >= 200.0,
            progressPercent = (maxElevationM / 200.0).toFloat().coerceIn(0f, 1f),
            progressDetail = "${maxElevationM.toInt()} / 200 m"
        ),
        FitnessBadge(
            id = "week_streak",
            title = "Consistency King",
            description = "Maintain an active workout streak of 7 consecutive days.",
            icon = Icons.Default.LocalFireDepartment,
            color = Color(0xFFFF5252),
            isUnlocked = currentStreak >= 7,
            progressPercent = (currentStreak / 7f).coerceIn(0f, 1f),
            progressDetail = "$currentStreak / 7 days"
        ),
        FitnessBadge(
            id = "century_club",
            title = "100km Total Club",
            description = "Accumulate 100 kilometers across all combined activities.",
            icon = Icons.Default.TrendingUp,
            color = Color(0xFF00B0FF),
            isUnlocked = totalDistanceKm >= 100.0,
            progressPercent = (totalDistanceKm / 100.0).toFloat().coerceIn(0f, 1f),
            progressDetail = "${totalDistanceKm.format(1)} / 100 km"
        ),
        FitnessBadge(
            id = "night_owl",
            title = "Night Owl",
            description = "Record a GPS activity after 9:00 PM.",
            icon = Icons.Default.NightsStay,
            color = Color(0xFF7C4DFF),
            isUnlocked = activities.any {
                val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
                cal.get(Calendar.HOUR_OF_DAY) >= 21
            },
            progressPercent = 1f,
            progressDetail = "After dark session"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP HEADER & TIMEFRAME SELECTOR
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Statistics & Trends",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "100% computed on-device • Zero telemetry tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizon Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsTimeFrame.entries.forEach { frame ->
                    FilterChip(
                        selected = selectedTimeFrame == frame,
                        onClick = { selectedTimeFrame = frame },
                        label = { Text(frame.label, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }

        // 1. PERIOD SUMMARY & DELTA COMPARISON CARD
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL DISTANCE (${selectedTimeFrame.label.uppercase()})",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${periodDistanceKm.format(2)} km",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                            )
                        }

                        if (prevPeriodDistanceKm > 0) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (distanceDeltaPct >= 0) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = if (distanceDeltaPct >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${if (distanceDeltaPct >= 0) "+" else ""}${distanceDeltaPct.format(1)}% vs prev",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (distanceDeltaPct >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TotalStatItem("WORKOUTS", "$periodWorkoutsCount")
                        TotalStatItem("ACTIVE TIME", "${periodDurationHours.format(1)} hrs")
                        TotalStatItem("CALORIES", "$periodCalories kcal")
                        TotalStatItem("STEPS", String.format(Locale.getDefault(), "%,d", periodSteps))
                    }
                }
            }
        }

        // 2. PERIOD ACTIVITY BAR CHART
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "DISTANCE ACTIVITY PROFILE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val maxDist = (barDistances.maxOrNull() ?: 1f).coerceAtLeast(1f)
                            val spacing = 12.dp.toPx()
                            val barWidth = ((size.width - (spacing * (barDistances.size + 1))) / barDistances.size).coerceAtLeast(6f)

                            for (i in barLabels.indices) {
                                val x = spacing + i * (barWidth + spacing)
                                val barHeight = ((barDistances[i] / maxDist) * (size.height - 24f)).coerceAtLeast(0f)
                                val y = size.height - 24f - barHeight

                                // Track background
                                drawRoundRect(
                                    color = primaryColor.copy(alpha = 0.12f),
                                    topLeft = Offset(x, 8f),
                                    size = Size(barWidth, size.height - 30f),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )

                                // Active bar
                                if (barDistances[i] > 0) {
                                    drawRoundRect(
                                        color = if (i == barLabels.size - 1) secondaryColor else primaryColor,
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )
                                }
                            }
                        }
                    }

                    // Bar labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        barLabels.forEach { label ->
                            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // 3. SPORT TYPE BREAKDOWN
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SPORTS BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Proportional Multi-color Segmented Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        if (runKm > 0) Box(modifier = Modifier.weight((runKm / totalSportKm).toFloat()).fillMaxSize().background(Color(0xFF2979FF)))
                        if (cycleKm > 0) Box(modifier = Modifier.weight((cycleKm / totalSportKm).toFloat()).fillMaxSize().background(Color(0xFF00E676)))
                        if (walkKm > 0) Box(modifier = Modifier.weight((walkKm / totalSportKm).toFloat()).fillMaxSize().background(Color(0xFFFF9100)))
                        if (hikeKm > 0) Box(modifier = Modifier.weight((hikeKm / totalSportKm).toFloat()).fillMaxSize().background(Color(0xFFAB47BC)))
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SportBadge(Icons.AutoMirrored.Filled.DirectionsRun, "Run", "${runKm.format(1)} km", Color(0xFF2979FF))
                        SportBadge(Icons.Default.DirectionsBike, "Ride", "${cycleKm.format(1)} km", Color(0xFF00E676))
                        SportBadge(Icons.AutoMirrored.Filled.DirectionsWalk, "Walk", "${walkKm.format(1)} km", Color(0xFFFF9100))
                        SportBadge(Icons.Default.NordicWalking, "Hike", "${hikeKm.format(1)} km", Color(0xFFAB47BC))
                    }
                }
            }
        }

        // 4. ACTIVITY CONSISTENCY HEATMAP (Annual / Recent 84 Days Matrix)
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVITY CONSISTENCY HEATMAP",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Last 12 Weeks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 7 rows x 12 columns grid
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        val cols = 12
                        val rows = 7
                        val cellSize = 10.dp.toPx()
                        val cellSpacing = 3.dp.toPx()

                        val cal = Calendar.getInstance()
                        for (c in 0 until cols) {
                            for (r in 0 until rows) {
                                val dayOffset = (cols - 1 - c) * 7 + (rows - 1 - r)
                                val cellCal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -dayOffset)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val dStart = cellCal.timeInMillis
                                val dEnd = dStart + 86400000L
                                val actCount = activities.count { it.startTime in dStart until dEnd }

                                val fillAlpha = when {
                                    actCount >= 3 -> 0.95f
                                    actCount == 2 -> 0.65f
                                    actCount == 1 -> 0.35f
                                    else -> 0.08f
                                }

                                drawRoundRect(
                                    color = if (actCount > 0) primaryColor.copy(alpha = fillAlpha) else Color.Gray.copy(alpha = 0.12f),
                                    topLeft = Offset(c * (cellSize + cellSpacing), r * (cellSize + cellSpacing)),
                                    size = Size(cellSize, cellSize),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Less", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(Modifier.size(8.dp).background(Color.Gray.copy(alpha = 0.12f), RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(Modifier.size(8.dp).background(primaryColor.copy(alpha = 0.35f), RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(Modifier.size(8.dp).background(primaryColor.copy(alpha = 0.65f), RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(Modifier.size(8.dp).background(primaryColor.copy(alpha = 0.95f), RoundedCornerShape(2.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 5. HEART RATE ZONE DISTRIBUTION
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "CARDIO & HEART RATE ZONES",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val zones = listOf(
                        Triple("Zone 5: Peak (>180 bpm)", 0.08f, Color(0xFFFF1744)),
                        Triple("Zone 4: Anaerobic (160-180 bpm)", 0.22f, Color(0xFFFF9100)),
                        Triple("Zone 3: Aerobic (140-160 bpm)", 0.45f, Color(0xFF00E676)),
                        Triple("Zone 2: Fat Burn (120-140 bpm)", 0.18f, Color(0xFF2979FF)),
                        Triple("Zone 1: Warm-up (<120 bpm)", 0.07f, Color(0xFF7C4DFF))
                    )

                    zones.forEach { (zoneTitle, pct, color) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(zoneTitle, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                Text("${(pct * 100).toInt()}%", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = color)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = color
                            )
                        }
                    }
                }
            }
        }

        // 6. PERSONAL RECORDS SHOWCASE
        item {
            Text(
                text = "Personal Records",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                fastestActivity?.let { act ->
                    val pace = act.avgPaceSecPerKm
                    RecordItem(
                        title = "Fastest Pace",
                        value = "${(pace / 60).toInt()}'${(pace % 60).toInt()}\" /km",
                        subtitle = act.title,
                        icon = Icons.Default.Speed,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                longestActivity?.let { act ->
                    RecordItem(
                        title = "Longest Distance",
                        value = "${(act.distanceMeters / 1000.0).format(2)} km",
                        subtitle = act.title,
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFFFFB300)
                    )
                }

                maxElevationActivity?.let { act ->
                    RecordItem(
                        title = "Max Elevation Gain",
                        value = "+${act.elevationGainM.toInt()} m",
                        subtitle = act.title,
                        icon = Icons.Default.Timeline,
                        color = Color(0xFF00E676)
                    )
                }

                highestCalorieActivity?.let { act ->
                    RecordItem(
                        title = "Most Calories Burned",
                        value = "${act.calories} kcal",
                        subtitle = act.title,
                        icon = Icons.Default.LocalFireDepartment,
                        color = Color(0xFFFF5252)
                    )
                }
            }
        }

        // 7. MILESTONE ACHIEVEMENTS & BADGES WALL
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Milestones & Badges (${badges.count { it.isUnlocked }}/${badges.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Grid of 2 columns for badges
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                badges.chunked(2).forEach { rowBadges ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowBadges.forEach { badge ->
                            BadgeCard(
                                badge = badge,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedBadgeDetail = badge }
                            )
                        }
                        if (rowBadges.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // BADGE DETAIL DIALOG
    selectedBadgeDetail?.let { badge ->
        AlertDialog(
            onDismissRequest = { selectedBadgeDetail = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (badge.isUnlocked) badge.color else Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        badge.icon,
                        contentDescription = null,
                        tint = if (badge.isUnlocked) Color.White else Color.Gray,
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(badge.title, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (badge.isUnlocked) "UNLOCKED ★" else "IN PROGRESS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (badge.isUnlocked) Color(0xFF10B981) else Color.Gray
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(badge.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { badge.progressPercent },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = badge.color
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(badge.progressDetail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBadgeDetail = null }) {
                    Text("Awesome")
                }
            }
        )
    }
}

@Composable
private fun TotalStatItem(title: String, value: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SportBadge(icon: ImageVector, label: String, dist: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(dist, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun BadgeCard(
    badge: FitnessBadge,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (badge.isUnlocked) badge.color.copy(alpha = 0.18f) else Color.Gray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    badge.icon,
                    contentDescription = null,
                    tint = if (badge.isUnlocked) badge.color else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = badge.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
            )
            Text(
                text = if (badge.isUnlocked) "Earned ★" else "${(badge.progressPercent * 100).toInt()}%",
                fontSize = 10.sp,
                color = if (badge.isUnlocked) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecordItem(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
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
                        .size(40.dp)
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = color
            )
        }
    }
}
