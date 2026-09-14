package com.example.miles.ui.routes

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.model.GpsPoint
import com.example.miles.data.model.SavedRouteEntity
import com.example.miles.data.model.Waypoint
import com.example.miles.data.model.WaypointType
import com.example.miles.data.repository.MilesRepository
import com.example.miles.data.repository.format
import com.example.miles.ui.map.RealOsmMapView
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun RouteBuilderScreen(
    repository: MilesRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedRoutes by repository.savedRoutes.collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Route Builder, 1: Route Library

    // Builder state
    var builderPoints by remember { mutableStateOf<List<GpsPoint>>(emptyList()) }
    var builderWaypoints by remember { mutableStateOf<List<Waypoint>>(emptyList()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var routeName by remember { mutableStateOf("") }
    var routeDescription by remember { mutableStateOf("") }

    // Calculate total builder distance
    val totalDistanceMeters = remember(builderPoints) {
        var d = 0.0
        for (i in 0 until builderPoints.size - 1) {
            d += MilesRepository.calculateDistanceMeters(
                builderPoints[i].latitude, builderPoints[i].longitude,
                builderPoints[i + 1].latitude, builderPoints[i + 1].longitude
            )
        }
        d
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Routes & Maps",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Plan paths, preview elevation, or analyze your personal heatmap",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Route Builder", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Route Library (${savedRoutes.size})", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTabIndex == 0) {
            // ROUTE BUILDER VIEW
            var centerCoord by remember { mutableStateOf(Pair(37.7749, -122.4194)) }

            // Real OpenStreetMap Canvas
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                RealOsmMapView(
                    modifier = Modifier.fillMaxSize(),
                    points = builderPoints,
                    waypoints = builderWaypoints,
                    showCenterCrosshair = true,
                    showControls = true,
                    onCenterChanged = { lat, lng ->
                        centerCoord = Pair(lat, lng)
                    }
                )
            }

            // Route builder HUD info & action buttons
            LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ESTIMATED DISTANCE",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(totalDistanceMeters / 1000.0).format(2)} km",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "POINTS PLACED",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${builderPoints.size} points • ${builderWaypoints.size} pins",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Explicit Point / Waypoint Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val newPt = GpsPoint(
                                    latitude = centerCoord.first,
                                    longitude = centerCoord.second,
                                    altitude = 45.0 + (builderPoints.size * 2.0),
                                    accuracy = 2.0f,
                                    timestamp = System.currentTimeMillis()
                                )
                                builderPoints = builderPoints + newPt
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Add Point", fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                val wp = Waypoint(
                                    name = "Pin ${builderWaypoints.size + 1}",
                                    latitude = centerCoord.first,
                                    longitude = centerCoord.second,
                                    type = WaypointType.WATER
                                )
                                builderWaypoints = builderWaypoints + wp
                                Toast.makeText(context, "Waypoint added at center pin", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+ Waypoint")
                        }

                        FilledTonalButton(
                            onClick = {
                                if (builderPoints.isNotEmpty()) {
                                    builderPoints = builderPoints.dropLast(1)
                                }
                            },
                            enabled = builderPoints.isNotEmpty(),
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Undo")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reverse Route Button
                        FilledTonalButton(
                            onClick = {
                                builderPoints = builderPoints.reversed()
                                Toast.makeText(context, "Route reversed", Toast.LENGTH_SHORT).show()
                            },
                            enabled = builderPoints.size > 1,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reverse")
                        }

                        // Clear Points Button
                        FilledTonalButton(
                            onClick = {
                                builderPoints = emptyList()
                                builderWaypoints = emptyList()
                            },
                            enabled = builderPoints.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }

                        // Save Route Button
                        Button(
                            onClick = { showSaveDialog = true },
                            enabled = builderPoints.size > 1,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Route")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // ROUTE LIBRARY VIEW
            if (savedRoutes.isEmpty()) {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No saved routes yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Use the Route Builder to plot custom running/cycling paths.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(savedRoutes) { route ->
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = route.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${(route.distanceMeters / 1000.0).format(2)} km • ${route.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = {
                                    scope.launch {
                                        repository.deleteRoute(route.id)
                                        Toast.makeText(context, "Route deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF2D55))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Route Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Route to Library", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = routeName,
                        onValueChange = { routeName = it },
                        label = { Text("Route Name (e.g. 5K Park Loop)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = routeDescription,
                        onValueChange = { routeDescription = it },
                        label = { Text("Description or Waypoint Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (routeName.isNotBlank()) {
                        scope.launch {
                            repository.saveRoute(
                                SavedRouteEntity(
                                    title = routeName,
                                    name = routeName,
                                    description = routeDescription,
                                    distanceMeters = totalDistanceMeters,
                                    routePointsJson = MilesRepository.pointsToJson(builderPoints),
                                    waypointsJson = MilesRepository.waypointsToJson(builderWaypoints)
                                )
                            )
                            Toast.makeText(context, "Saved route \"$routeName\"!", Toast.LENGTH_SHORT).show()
                            showSaveDialog = false
                            selectedTabIndex = 1
                        }
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
