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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Train
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.miles.ui.map.RealOsmMapView
import com.example.miles.ui.map.RealOsmTileSource
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

data class SavedPlaceItem(
    val name: String,
    val type: String, // Home, Work, Favorite, Pin
    val lat: Double,
    val lon: Double,
    val icon: ImageVector
)

data class OfflineRegion(
    val name: String,
    val sizeMb: Double,
    val tileCount: Int,
    val lat: Double = 37.7749,
    val lon: Double = -122.4194,
    val isDownloaded: Boolean = true
)

data class OfflineCity(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val sizeMb: Double,
    val tileCount: Int,
    val flag: String
)

@Composable
fun RouteBuilderScreen(
    repository: MilesRepository,
    onStartNavigation: ((SavedRouteEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedRoutes by repository.savedRoutes.collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Route Builder & Navigation, 1: Route Library
    var libraryFavoritesOnly by remember { mutableStateOf(false) }

    // Map & Layer State (Auto-switches directly without menu)
    var selectedTileSource by remember { mutableStateOf(RealOsmTileSource.STANDARD) }
    var showOfflineDialog by remember { mutableStateOf(false) }
    var showPlacesDialog by remember { mutableStateOf(false) }
    var is3dMode by remember { mutableStateOf(false) }
    var citySearchQuery by remember { mutableStateOf("") }

    // Builder Points & Waypoints
    var builderPoints by remember { mutableStateOf<List<GpsPoint>>(emptyList()) }
    var builderWaypoints by remember { mutableStateOf<List<Waypoint>>(emptyList()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var routeName by remember { mutableStateOf("") }
    var routeDescription by remember { mutableStateOf("") }

    // Center Coordinate State
    var centerLat by remember { mutableDoubleStateOf(37.7749) }
    var centerLon by remember { mutableDoubleStateOf(-122.4194) }

    // Search & POI State
    var searchQuery by remember { mutableStateOf("") }
    var selectedPoiCategory by remember { mutableStateOf("All") }

    // Live Turn-by-Turn Navigation State
    var isNavigatingLive by remember { mutableStateOf(false) }
    var activeNavRouteName by remember { mutableStateOf("Custom Planned Route") }
    var navStepIndex by remember { mutableIntStateOf(0) }
    var voiceMuted by remember { mutableStateOf(false) }

    // Saved Places list
    val savedPlaces = remember {
        mutableStateListOf(
            SavedPlaceItem("Home", "HOME", 37.7749, -122.4194, Icons.Default.Home),
            SavedPlaceItem("Work", "WORK", 37.7891, -122.4014, Icons.Default.Work),
            SavedPlaceItem("Golden Gate Park", "FAVORITE", 37.7694, -122.4862, Icons.Default.Park),
            SavedPlaceItem("Twin Peaks Lookout", "FAVORITE", 37.7544, -122.4477, Icons.Default.Explore)
        )
    }

    // Offline Regions
    val offlineRegions = remember {
        mutableStateListOf(
            OfflineRegion("San Francisco Metro", 34.2, 1420),
            OfflineRegion("Mount Tamalpais Trails", 18.5, 680)
        )
    }
    var isDownloadingRegion by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

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
                        text = "OpenStreetMap • Satellite • Offline regions • Turn-by-turn",
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
                    onCenterChanged = { lat, lng ->
                        centerLat = lat
                        centerLon = lng
                    }
                )

                // Top Controls Overlay: Search Bar & POI Filter Chips
                if (!isNavigatingLive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search places, trails, addresses...", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Category POI Quick Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val categories = listOf(
                                Triple("All", Icons.Default.Place, "All"),
                                Triple("Food", Icons.Default.Restaurant, "Food"),
                                Triple("Parks", Icons.Default.Park, "Parks"),
                                Triple("Fuel", Icons.Default.LocalGasStation, "Fuel"),
                                Triple("Transit", Icons.Default.Train, "Transit"),
                                Triple("Health", Icons.Default.LocalHospital, "Health")
                            )
                            items(categories) { (cat, icon, label) ->
                                FilterChip(
                                    selected = selectedPoiCategory == cat,
                                    onClick = {
                                        selectedPoiCategory = cat
                                        Toast.makeText(context, "Filtered POIs for $label", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

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

                        // 2. Offline Maps: Select a City
                        FloatingActionButton(
                            onClick = { showOfflineDialog = true },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Offline City Maps", modifier = Modifier.size(20.dp))
                        }

                        // 3. Saved Places & Pins
                        FloatingActionButton(
                            onClick = { showPlacesDialog = true },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.PinDrop, contentDescription = "Saved Places", modifier = Modifier.size(20.dp))
                        }

                        // 4. Center on GPS Location
                        FloatingActionButton(
                            onClick = {
                                centerLat = 37.7749
                                centerLon = -122.4194
                                Toast.makeText(context, "Centered on GPS location", Toast.LENGTH_SHORT).show()
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // LIVE TURN-BY-TURN NAVIGATION HUD BANNER (When Navigation Active)
                if (isNavigatingLive) {
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
                                            if (navStepIndex % 2 == 0) Icons.Default.TurnRight else Icons.Default.TurnLeft,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (navStepIndex % 2 == 0) "In 150m, Turn Right" else "In 220m, Turn Left",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                                        )
                                        Text(
                                            text = "Onto Pine Ridge Trail • Follow Route",
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
                                        Text("14 min", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                                    }
                                    Column {
                                        Text("SPEED", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                        Text("5.2 km/h", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
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
                                        isNavigatingLive = true
                                        Toast.makeText(context, "Turn-by-turn navigation started", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Navigate", fontWeight = FontWeight.Bold)
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
                                                activeNavRouteName = route.name
                                                selectedTabIndex = 0
                                                isNavigatingLive = true
                                                Toast.makeText(context, "Starting navigation on ${route.name}", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Start Navigation")
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

    // OFFLINE MAPS DOWNLOADER MODAL (Select a City)
    if (showOfflineDialog) {
        val availableCities = remember {
            listOf(
                OfflineCity("New York City", "United States 🇺🇸", 40.7128, -74.0060, 24.5, 1120, "🗽"),
                OfflineCity("Paris", "France 🇫🇷", 48.8566, 2.3522, 22.0, 1050, "🗼"),
                OfflineCity("London", "United Kingdom 🇬🇧", 51.5074, -0.1278, 26.2, 1280, "🎡"),
                OfflineCity("San Francisco", "United States 🇺🇸", 37.7749, -122.4194, 18.5, 890, "🌉"),
                OfflineCity("Tokyo", "Japan 🇯🇵", 35.6762, 139.6503, 31.8, 1540, "⛩️"),
                OfflineCity("Rome", "Italy 🇮🇹", 41.9028, 12.4964, 20.4, 980, "🏛️"),
                OfflineCity("Berlin", "Germany 🇩🇪", 52.5200, 13.4050, 21.0, 1020, "🏙️"),
                OfflineCity("Sydney", "Australia 🇦🇺", -33.8688, 151.2093, 23.1, 1100, "🦘"),
                OfflineCity("Toronto", "Canada 🇨🇦", 43.6532, -79.3832, 19.3, 920, "🍁"),
                OfflineCity("Dubai", "United Arab Emirates 🇦🇪", 25.2048, 55.2708, 17.8, 870, "🕌"),
                OfflineCity("Barcelona", "Spain 🇪🇸", 41.3851, 2.1734, 19.5, 940, "🏖️"),
                OfflineCity("Denver", "United States 🇺🇸", 39.7392, -104.9903, 22.4, 1040, "🏔️"),
                OfflineCity("Mumbai", "India 🇮🇳", 19.0760, 72.8777, 25.0, 1200, "🇮🇳"),
                OfflineCity("Singapore", "Singapore 🇸🇬", 1.3521, 103.8198, 16.2, 780, "🦁"),
                OfflineCity("Los Angeles", "United States 🇺🇸", 34.0522, -118.2437, 27.6, 1350, "☀️"),
                OfflineCity("Zurich", "Switzerland 🇨🇭", 47.3769, 8.5417, 17.0, 820, "⛷️")
            )
        }

        var downloadingCity by remember { mutableStateOf<String?>(null) }

        val filteredCities = remember(citySearchQuery, availableCities) {
            if (citySearchQuery.isBlank()) availableCities
            else availableCities.filter {
                it.name.contains(citySearchQuery, ignoreCase = true) ||
                it.country.contains(citySearchQuery, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select a City to Download", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Download complete offline map tiles by city for zero-data GPS tracking and trail routing.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // City Search Bar
                    OutlinedTextField(
                        value = citySearchQuery,
                        onValueChange = { citySearchQuery = it },
                        placeholder = { Text("Search city (e.g., Paris, Tokyo, London)...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (citySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { citySearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Cities List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCities) { city ->
                            val isDownloaded = offlineRegions.any { it.name.startsWith(city.name) }
                            val isCurrentlyDownloading = downloadingCity == city.name

                            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text(city.flag, fontSize = 22.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(city.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(
                                                    "${city.country} • ~${city.sizeMb} MB • ${city.tileCount} tiles",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (isDownloaded) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        centerLat = city.lat
                                                        centerLon = city.lon
                                                        showOfflineDialog = false
                                                        Toast.makeText(context, "Centered map on ${city.name}", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Go To", fontSize = 11.sp)
                                                }
                                            }
                                        } else if (isCurrentlyDownloading) {
                                            Text("Downloading...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        } else {
                                            Button(
                                                onClick = {
                                                    downloadingCity = city.name
                                                    scope.launch {
                                                        for (p in 1..10) {
                                                            downloadProgress = p / 10f
                                                            delay(200)
                                                        }
                                                        downloadingCity = null
                                                        offlineRegions.add(
                                                            OfflineRegion(
                                                                name = "${city.name} (${city.country})",
                                                                sizeMb = city.sizeMb,
                                                                tileCount = city.tileCount,
                                                                lat = city.lat,
                                                                lon = city.lon
                                                            )
                                                        )
                                                        Toast.makeText(context, "Downloaded offline map for ${city.name}!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Download", fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    if (isCurrentlyDownloading) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { downloadProgress },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            "Downloading tiles: ${(downloadProgress * 100).toInt()}%",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Downloaded Cities Section
                    if (offlineRegions.isNotEmpty()) {
                        Text(
                            "DOWNLOADED OFFLINE CITIES (${offlineRegions.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            offlineRegions.forEach { region ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                centerLat = region.lat
                                                centerLon = region.lon
                                                showOfflineDialog = false
                                                Toast.makeText(context, "Jumped to ${region.name}", Toast.LENGTH_SHORT).show()
                                            }
                                    ) {
                                        Text(region.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("${region.sizeMb} MB • ${region.tileCount} tiles", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    IconButton(
                                        onClick = { offlineRegions.remove(region) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOfflineDialog = false }) { Text("Done") }
            }
        )
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
