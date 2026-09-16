package com.example.miles.ui.history

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.repository.format
import com.example.miles.ui.theme.LiquidGlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SmartCollectionFilter(val label: String) {
    ALL("All Activities"),
    FAVORITES("Favorites ★"),
    LONGEST_RUNS("Longest Runs"),
    LONGEST_WALKS("Longest Walks"),
    NIGHT_WORKOUTS("Night Activities"),
    PERSONAL_BESTS("Personal Records")
}

@Composable
fun HistoryScreen(
    activities: List<ActivityEntity>,
    onSelectActivity: (ActivityEntity) -> Unit,
    onToggleFavorite: ((ActivityEntity) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCollection by remember { mutableStateOf(SmartCollectionFilter.ALL) }
    var selectedTypeFilter by remember { mutableStateOf<ActivityType?>(null) }

    // Filter logic
    val filteredActivities = activities.filter { act ->
        val matchesSearch = searchQuery.isBlank() ||
                act.title.contains(searchQuery, ignoreCase = true) ||
                act.notes.contains(searchQuery, ignoreCase = true) ||
                act.activityType.contains(searchQuery, ignoreCase = true)

        val matchesType = selectedTypeFilter == null || act.activityType.equals(selectedTypeFilter?.name, ignoreCase = true)

        val matchesCollection = when (selectedCollection) {
            SmartCollectionFilter.ALL -> true
            SmartCollectionFilter.FAVORITES -> act.isFavorite
            SmartCollectionFilter.LONGEST_RUNS -> act.activityType.equals(ActivityType.RUNNING.name, ignoreCase = true) && act.distanceMeters > 5000.0
            SmartCollectionFilter.LONGEST_WALKS -> act.activityType.equals(ActivityType.WALKING.name, ignoreCase = true) && act.distanceMeters > 3000.0
            SmartCollectionFilter.NIGHT_WORKOUTS -> {
                val hour = SimpleDateFormat("HH", Locale.US).format(Date(act.startTime)).toIntOrNull() ?: 12
                hour >= 20 || hour <= 5
            }
            SmartCollectionFilter.PERSONAL_BESTS -> act.avgPaceSecPerKm > 0 && act.avgPaceSecPerKm < 320.0
        }

        matchesSearch && matchesType && matchesCollection
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Activity History",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${activities.size} workouts saved locally • No cloud accounts required",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Search Input Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by title, location, notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Smart Collections Filter Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SmartCollectionFilter.entries) { collection ->
                    FilterChip(
                        selected = selectedCollection == collection,
                        onClick = { selectedCollection = collection },
                        label = { Text(collection.label) }
                    )
                }
            }
        }

        // Activity Type Filters
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All Types") }
                    )
                }
                items(ActivityType.entries) { type ->
                    FilterChip(
                        selected = selectedTypeFilter == type,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == type) null else type },
                        label = { Text(type.displayName) }
                    )
                }
            }
        }

        if (filteredActivities.isEmpty()) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching activities found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Try clearing search filters or collection selection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredActivities) { act ->
                HistoryActivityItem(
                    activity = act,
                    onClick = { onSelectActivity(act) },
                    onToggleFavorite = { onToggleFavorite?.invoke(act) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HistoryActivityItem(
    activity: ActivityEntity,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (ActivityType.fromString(activity.activityType)) {
                        ActivityType.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
                        ActivityType.CYCLING -> Icons.Default.DirectionsBike
                        ActivityType.HIKING -> Icons.Default.NordicWalking
                        else -> Icons.AutoMirrored.Filled.DirectionsWalk
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = activity.activityType,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = activity.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = SimpleDateFormat("EEEE, MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(activity.startTime)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { onToggleFavorite?.invoke() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (activity.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (activity.isFavorite) "Favorite" else "Not favorite",
                        tint = if (activity.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(label = "DISTANCE", value = "${(activity.distanceMeters / 1000.0).format(2)} km")
                StatColumn(label = "TIME", value = "${activity.durationSeconds / 60}m ${activity.durationSeconds % 60}s")
                StatColumn(label = "AVG PACE", value = "${(activity.avgPaceSecPerKm / 60).toInt()}'${(activity.avgPaceSecPerKm % 60).toInt()}\"/km")
                StatColumn(label = "ELEVATION", value = "+${activity.elevationGainM.toInt()} m")
            }

            if (activity.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "\"${activity.notes}\"",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
