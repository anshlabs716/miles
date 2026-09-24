package com.example.miles.ui.routes

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear


import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike

import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers


import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Remove


import androidx.compose.material.icons.filled.Route

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert

import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.miles.engine.OsmRouteStep
import com.example.miles.engine.RoutingEngine
import com.example.miles.ui.map.RealOsmMapView
import com.example.miles.ui.map.RealOsmTileSource
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel

import kotlinx.coroutines.launch
import java.util.UUID

data class SavedPlaceItem(
    val name: String,
    val type: String, // Home, Work, Favorite, Pin
    val lat: Double,
    val lon: Double,
    val icon: ImageVector
)

@Composable
fun RouteBuilderScreen(
    repository: MilesRepository,
    onStartNavigation: ((SavedRouteEntity) -> Unit)? = null,
    onUpdateNavigationSteps: ((List<OsmRouteStep>) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedRoutes by repository.savedRoutes.collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Route Builder & Navigation, 1: Route Library
    var libraryFavoritesOnly by remember { mutableStateOf(false) }

    // Map & Layer State (Auto-switches directly without menu)
    var selectedTileSource by remember { mutableStateOf(RealOsmTileSource.STANDARD) }
    var showPlacesDialog by remember { mutableStateOf(false) }
    var is3dMode by remember { mutableStateOf(false) }
    var recenterRequest by remember { mutableIntStateOf(0) }
    var zoomRequest by remember { mutableIntStateOf(0) }

    // Builder Points & Waypoints
    var builderPoints by remember { mutableStateOf<List<GpsPoint>>(emptyList()) }
    var builderWaypoints by remember { mutableStateOf<List<Waypoint>>(emptyList()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var routeName by remember { mutableStateOf("") }
    var routeDescription by remember { mutableStateOf("") }

    // Center Coordinate State
    var centerLat by remember { mutableDoubleStateOf(37.7749) }
    var centerLon by remember { mutableDoubleStateOf(-122.4194) }

    // Live Turn-by-Turn Navigation State
    var isNavigatingLive by remember { mutableStateOf(false) }
    var activeNavRouteName by remember { mutableStateOf("Custom Planned Route") }
    var navStepIndex by remember { mutableIntStateOf(0) }
    var voiceMuted by remember { mutableStateOf(false) }

    // Real route computation state (OSRM)
    var isRouteLoading by remember { mutableStateOf(false) }
    var navRouteSteps by remember { mutableStateOf<List<OsmRouteStep>>(emptyList()) }
    var navEtaMinutes by remember { mutableIntStateOf(0) }

    // Saved Places list
    val savedPlaces = remember {
        mutableStateListOf(
            SavedPlaceItem("Home", "HOME", 37.7749, -122.4194, Icons.Default.Home),
            SavedPlaceItem("Work", "WORK", 37.7891, -122.4014, Icons.Default.Work),
            SavedPlaceItem("Golden Gate Park", "FAVORITE", 37.7694, -122.4862, Icons.Default.Park),
            SavedPlaceItem("Twin Peaks Lookout", "FAVORITE", 37.7544, -122.4477, Icons.Default.Explore)
        )
    }

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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Header
        if (!isNavigatingLive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Maps & Navigation",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "OpenStreetMap • Satellite • Turn-by-turn",
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
                    text = { Text("Map & Navigation", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Saved Routes (${savedRoutes.size})", fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (selectedTabIndex == 0) {
            // MAP & NAVIGATION VIEW
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                // Real OpenStreetMap / Satellite Canvas
                RealOsmMapView(
                    modifier = Modifier.fillMaxSize(),
                    points = builderPoints,
                    waypoints = builderWaypoints,
                    showCenterCrosshair = !isNavigatingLive,
                    showControls = false, // Clean layout: unified with RouteBuilder toolbar to prevent overlapping
                    initialTileSource = selectedTileSource,
                    currentTileSource = selectedTileSource,
                    onTileSourceChanged = { selectedTileSource = it },
                    recenterRequest = recenterRequest,
                    zoomRequest = zoomRequest,
                    onCenterChanged = { lat, lng ->
                        centerLat = lat
                        centerLon = lng
                    }
                )

                // Map Utility Floating Action Buttons (Unified Right Toolbar - No Overlapping)
                if (!isNavigatingLive) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Layer Auto-Switch Button (Directly toggles Street <-> Satellite with NO menu)
                        FloatingActionButton(
                            onClick = {
                                val next = if (selectedTileSource == RealOsmTileSource.SATELLITE) {
                                    RealOsmTileSource.STANDARD
                                } else {
                                    RealOsmTileSource.SATELLITE
                                }
                                selectedTileSource = next
                                val label = if (next == RealOsmTileSource.SATELLITE) "🛰️ Satellite Map Active" else "🗺️ Street Map Active"
                                Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                            },
                            containerColor = if (selectedTileSource == RealOsmTileSource.SATELLITE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (selectedTileSource == RealOsmTileSource.SATELLITE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedTileSource == RealOsmTileSource.SATELLITE) Icons.Default.Map else Icons.Default.Layers,
                                contentDescription = "Auto-Switch Map Layer",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // 2. Saved Places & Pins
                        FloatingActionButton(
                            onClick = { showPlacesDialog = true },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.PinDrop, contentDescription = "Saved Places", modifier = Modifier.size(20.dp))
                        }

                        // 3. Center on real GPS location
                        FloatingActionButton(
                            onClick = {
                                recenterRequest += 1
                                Toast.makeText(context, "Centering on GPS location...", Toast.LENGTH_SHORT).show()
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(20.dp))
                        }

                        // 4. Zoom In (+)
                        FloatingActionButton(
                            onClick = { zoomRequest += 1 },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                        }

                        // 5. Zoom Out (−)
                        FloatingActionButton(
                            onClick = { zoomRequest -= 1 },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // LIVE TURN-BY-TURN NAVIGATION HUD BANNER (When Navigation Active)
                if (isNavigatingLive) {
                    val currentStep = navRouteSteps.getOrNull(navStepIndex.coerceAtMost(navRouteSteps.lastIndex))
                    val turnIcon = when {
                        currentStep == null || currentStep.maneuverType == "arrive" -> Icons.Default.Navigation
                        currentStep.modifier.contains("left") -> Icons.Default.TurnLeft
                        currentStep.modifier.contains("right") -> Icons.Default.TurnRight
                        else -> Icons.Default.Navigation
                    }
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.95f),
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            turnIcon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = currentStep?.instruction ?: "Routing…",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                        Text(
                                            text = currentStep?.roadName?.let { "Via $it • Follow Route" } ?: "Follow Route",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                                        )
                                    }
                                }

                                IconButton(onClick = { voiceMuted = !voiceMuted }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Voice Audio",
                                        tint = if (!voiceMuted) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text("REMAINING", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                        Text("${(totalDistanceMeters / 1000.0).format(2)} km", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                    }
                                    Column {
                                        Text("ETA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                        Text("$navEtaMinutes min", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                    }
                                    Column {
                                        Text("SPEED", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                        Text("—", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                    }
                                }

                                Button(
                                    onClick = { isNavigatingLive = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Stop Nav", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Route Action Panel (When Not in Navigation HUD)
            if (!isNavigatingLive) {
                LiquidGlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ESTIMATED ROUTE DISTANCE",
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
                                    text = "POINTS & PINS",
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

                        // Point Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val newPt = GpsPoint(
                                        latitude = centerLat,
                                        longitude = centerLon,
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
                                Text("+ Point", fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    val wp = Waypoint(
                                        name = "Pin ${builderWaypoints.size + 1}",
                                        latitude = centerLat,
                                        longitude = centerLon,
                                        type = WaypointType.WATER
                                    )
                                    builderWaypoints = builderWaypoints + wp
                                    Toast.makeText(context, "Waypoint added at center pin", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("+ Pin")
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
                            // Reverse Route
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

                            // Start Navigation Button
                            Button(
                                onClick = {
                                    if (builderPoints.size < 2) {
                                        Toast.makeText(context, "Add at least 2 points to start navigation", Toast.LENGTH_SHORT).show()
                                    } else {
                                        scope.launch {
                                            isRouteLoading = true
                                            try {
                                                val coords = builderPoints.map { it.latitude to it.longitude }
                                                val result = RoutingEngine.fetchRoute(coords)
                                                builderPoints = result.points
                                                navRouteSteps = result.steps
                                                navEtaMinutes = (result.durationSeconds / 60.0).toInt().coerceAtLeast(1)
                                                val entity = SavedRouteEntity(
                                                    title = activeNavRouteName,
                                                    name = activeNavRouteName,
                                                    description = "Turn-by-turn route",
                                                    distanceMeters = result.distanceMeters,
                                                    routePointsJson = MilesRepository.pointsToJson(result.points)
                                                )
                                                onUpdateNavigationSteps?.invoke(result.steps)
                                                isNavigatingLive = true
                                                onStartNavigation?.invoke(entity)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Couldn't fetch a route — check your connection", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isRouteLoading = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isRouteLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isRouteLoading) "Routing…" else "Navigate", fontWeight = FontWeight.Bold)
                            }

                            // Save Route
                            Button(
                                onClick = { showSaveDialog = true },
                                enabled = builderPoints.size > 1,
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            // ROUTE LIBRARY VIEW
            val displayedRoutes = if (libraryFavoritesOnly) savedRoutes.filter { it.isFavorite } else savedRoutes

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !libraryFavoritesOnly,
                        onClick = { libraryFavoritesOnly = false },
                        label = { Text("All Routes (${savedRoutes.size})") }
                    )
                    FilterChip(
                        selected = libraryFavoritesOnly,
                        onClick = { libraryFavoritesOnly = true },
                        label = { Text("Favorites ★ (${savedRoutes.count { it.isFavorite }})") }
                    )
                }

                if (displayedRoutes.isEmpty()) {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Saved Routes Yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Switch to the Route Builder tab above to map out a path and save it locally.",
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
                        items(displayedRoutes, key = { it.id }) { route ->
                            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = route.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${(route.distanceMeters / 1000.0).format(2)} km • ${route.description}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    repository.toggleRouteFavorite(route.id)
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (route.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "Favorite",
                                                tint = if (route.isFavorite) Color(0xFFFFB300) else Color.Gray
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    repository.deleteRoute(route.id)
                                                    Toast.makeText(context, "Route deleted", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    isRouteLoading = true
                                                    try {
                                                        val pts = MilesRepository.parsePoints(route.routePointsJson)
                                                        val coords = pts.take(10).map { it.latitude to it.longitude }
                                                        val result = RoutingEngine.fetchRoute(coords)
                                                        activeNavRouteName = route.name
                                                        selectedTabIndex = 0
                                                        isNavigatingLive = true
                                                        builderPoints = result.points
                                                        navRouteSteps = result.steps
                                                        navEtaMinutes = (result.durationSeconds / 60.0).toInt().coerceAtLeast(1)
                                                        val entity = SavedRouteEntity(
                                                            id = route.id,
                                                            title = route.title,
                                                            name = route.name,
                                                            description = route.description,
                                                            activityType = route.activityType,
                                                            distanceMeters = result.distanceMeters,
                                                            routePointsJson = MilesRepository.pointsToJson(result.points),
                                                            waypointsJson = route.waypointsJson,
                                                            isFavorite = route.isFavorite
                                                        )
                                                        onUpdateNavigationSteps?.invoke(result.steps)
                                                        onStartNavigation?.invoke(entity)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Couldn't fetch a route — check your connection", Toast.LENGTH_LONG).show()
                                                    } finally {
                                                        isRouteLoading = false
                                                    }
                                                }
                                            },
                                            enabled = !isRouteLoading,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isRouteLoading) "Routing…" else "Start Navigation")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. SAVED PLACES & PINS MODAL
    if (showPlacesDialog) {
        AlertDialog(
            onDismissRequest = { showPlacesDialog = false },
            title = { Text("Saved Places & Pins", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Button(
                            onClick = {
                                savedPlaces.add(
                                    SavedPlaceItem("Dropped Pin ${savedPlaces.size + 1}", "PIN", centerLat, centerLon, Icons.Default.PinDrop)
                                )
                                Toast.makeText(context, "Dropped pin saved at current center", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Current Center as Pin")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(savedPlaces) { place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    centerLat = place.lat
                                    centerLon = place.lon
                                    showPlacesDialog = false
                                    Toast.makeText(context, "Centered on ${place.name}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(place.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(place.name, fontWeight = FontWeight.SemiBold)
                                    Text("${place.lat.format(4)}°, ${place.lon.format(4)}°", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { savedPlaces.remove(place) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlacesDialog = false }) { Text("Close") }
            }
        )
    }

    // 4. SAVE ROUTE DIALOG
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
                        label = { Text("Description or Notes") },
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
