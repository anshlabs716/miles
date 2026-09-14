package com.example.miles.ui.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.repository.format
import com.example.miles.ui.theme.LiquidGlassCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    activities: List<ActivityEntity>
) {
    val totalDistanceKm = activities.sumOf { it.distanceMeters } / 1000.0
    val totalSteps = activities.sumOf { it.steps }
    val totalDurationHours = activities.sumOf { it.durationSeconds } / 3600.0
    val totalCalories = activities.sumOf { it.calories }

    // Dynamic Personal records calculations from real user activities
    val fastestActivity = activities.filter { it.avgPaceSecPerKm > 60.0 }.minByOrNull { it.avgPaceSecPerKm }
    val longestActivity = activities.maxByOrNull { it.distanceMeters }
    val maxElevationActivity = activities.filter { it.elevationGainM > 0 }.maxByOrNull { it.elevationGainM }

    // Calculate real dynamic 7-day distance breakdown
    val calendar = Calendar.getInstance()
    val dayLabels = mutableListOf<String>()
    val dayDistances = mutableListOf<Float>()
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    // 7 days ending today
    for (i in 6 downTo 0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        val dayEnd = dayStart + 86400000L

        dayLabels.add(dayFormat.format(Date(dayStart)))
        val distKm = activities.filter { it.startTime in dayStart until dayEnd }.sumOf { it.distanceMeters } / 1000.0
        dayDistances.add(distKm.toFloat())
    }

    // Dynamic current streak calculation
    var currentStreak = 0
    val now = Calendar.getInstance()
    for (i in 0..30) {
        val checkCal = Calendar.getInstance()
        checkCal.add(Calendar.DAY_OF_YEAR, -i)
        checkCal.set(Calendar.HOUR_OF_DAY, 0)
        checkCal.set(Calendar.MINUTE, 0)
        checkCal.set(Calendar.SECOND, 0)
        checkCal.set(Calendar.MILLISECOND, 0)
        val start = checkCal.timeInMillis
        val end = start + 86400000L

        val hasActivity = activities.any { it.startTime in start until end }
        if (hasActivity) {
            currentStreak++
        } else if (i > 0) {
            break // streak broken
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Analytics & Records",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "100% computed on-device • Zero telemetry tracking",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (activities.isEmpty()) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Activity Data Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Record your first walk, run, or ride to unlock weekly trend charts, personal records, and movement consistency analytics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Lifetime Totals Hero Card
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "LIFETIME TOTALS",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TotalStatItem(title = "Distance", value = "${totalDistanceKm.format(1)} km")
                            TotalStatItem(title = "Total Steps", value = totalSteps.toString())
                            TotalStatItem(title = "Active Time", value = "${totalDurationHours.format(1)} hrs")
                            TotalStatItem(title = "Total Burn", value = "$totalCalories kcal")
                        }
                    }
                }
            }

            // Weekly Activity Bar Chart
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "LAST 7 DAYS DISTANCE",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val primaryColor = MaterialTheme.colorScheme.primary
                        val secondaryColor = MaterialTheme.colorScheme.secondary
                        val maxDist = (dayDistances.maxOrNull() ?: 10f).coerceAtLeast(5f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = 26.dp.toPx()
                                val spacing = (size.width - (barWidth * dayLabels.size)) / (dayLabels.size + 1)

                                for (i in dayLabels.indices) {
                                    val x = spacing + i * (barWidth + spacing)
                                    val barHeight = ((dayDistances[i] / maxDist) * (size.height - 30f)).coerceAtLeast(0f)
                                    val y = size.height - 30f - barHeight

                                    // Bar background
                                    drawRoundRect(
                                        color = primaryColor.copy(alpha = 0.12f),
                                        topLeft = Offset(x, 10f),
                                        size = Size(barWidth, size.height - 40f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                                    )

                                    // Active bar
                                    if (dayDistances[i] > 0) {
                                        drawRoundRect(
                                            color = if (i == 6) secondaryColor else primaryColor,
                                            topLeft = Offset(x, y),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Day labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            dayLabels.forEach { day ->
                                Text(text = day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Personal Records Showcase
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
                }
            }

            // Consistency & Streaks
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = Color(0xFFFF2D55),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (currentStreak > 0) "$currentStreak-Day Streak!" else "No Active Streak",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (currentStreak > 0) "Keep moving today to extend your streak." else "Start an activity today to begin your streak.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TotalStatItem(title: String, value: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun RecordItem(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
