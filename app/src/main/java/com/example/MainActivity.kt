package com.example

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.miles.data.local.MilesDatabase
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.repository.MilesRepository
import com.example.miles.engine.DeviceManager
import com.example.miles.engine.MediaIntegration
import com.example.miles.engine.SmartTrackingEngine
import com.example.miles.engine.TrackingState
import com.example.miles.ui.dashboard.DashboardScreen
import com.example.miles.ui.devices.DevicesScreen
import com.example.miles.ui.history.ActivityDetailScreen
import com.example.miles.ui.journal.JournalScreen
import com.example.miles.ui.routes.RouteBuilderScreen
import com.example.miles.ui.settings.SettingsScreen
import com.example.miles.ui.setup.OnboardingSetupScreen
import com.example.miles.ui.studio.DistanceCalculatorScreen
import com.example.miles.ui.studio.MilesStudioScreen
import com.example.miles.ui.theme.MilesTheme
import com.example.miles.ui.workout.WorkoutHudScreen
import com.example.miles.wear.WearCompanionManager

enum class MilesNavigationTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    JOURNAL("Journal", Icons.AutoMirrored.Filled.List),
    ROUTES("Routes", Icons.Default.Map),
    PROFILE("Profile", Icons.Default.Person)
}

enum class MilesSubScreen {
    NONE,
    SETUP,
    ACTIVITY_DETAIL,
    WORKOUT_HUD,
    DEVICES,
    STUDIO,
    DISTANCE_CALCULATOR
}

class MainActivity : ComponentActivity() {
    private lateinit var database: MilesDatabase
    private lateinit var repository: MilesRepository
    private lateinit var preferences: MilesPreferences
    private lateinit var smartEngine: SmartTrackingEngine
    private lateinit var wearCompanion: WearCompanionManager
    private lateinit var deviceManager: DeviceManager
    private lateinit var mediaIntegration: MediaIntegration
    private lateinit var locationTracker: com.example.miles.engine.LocationTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = MilesDatabase.getInstance(this)
        repository = MilesRepository(
            database.activityDao(),
            database.savedRouteDao(),
            database.privacyZoneDao(),
            database.goalDao()
        )
        preferences = MilesPreferences(this)
        smartEngine = SmartTrackingEngine(this, repository)
        wearCompanion = WearCompanionManager(this)
        deviceManager = DeviceManager(this)
        mediaIntegration = MediaIntegration(this)
        locationTracker = com.example.miles.engine.LocationTracker(this)

        setContent {
            val userPrefs by preferences.userPreferences.collectAsState()
            val activities by repository.activities.collectAsState(initial = emptyList())
            val liveStats by smartEngine.liveStats.collectAsState()

            // Location permission launcher for real GPS tracking
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                if (granted) {
                    locationTracker.startTracking { point ->
                        if (smartEngine.liveStats.value.state == TrackingState.RECORDING) {
                            smartEngine.processLocation(point)
                        }
                    }
                }
            }

            // Ensure no unwanted preloaded seed data exists & request location
            LaunchedEffect(Unit) {
                repository.purgePreloadedSeedData()
                if (!locationTracker.hasLocationPermission()) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else {
                    locationTracker.startTracking { point ->
                        if (smartEngine.liveStats.value.state == TrackingState.RECORDING) {
                            smartEngine.processLocation(point)
                        }
                    }
                }
            }

            MilesTheme(
                themeOption = userPrefs.theme,
                liquidGlassEnabled = userPrefs.liquidGlassEnabled,
                accessibility = userPrefs.accessibility
            ) {
                var currentTab by remember { mutableStateOf(MilesNavigationTab.HOME) }
                var subScreen by remember { mutableStateOf(MilesSubScreen.NONE) }
                var selectedActivity by remember { mutableStateOf<ActivityEntity?>(null) }
                var lastBackPressTime by remember { mutableStateOf(0L) }

                // System gesture navigation & swipe-to-back interception
                BackHandler(enabled = true) {
                    when {
                        subScreen == MilesSubScreen.DISTANCE_CALCULATOR -> {
                            subScreen = MilesSubScreen.STUDIO
                        }
                        subScreen == MilesSubScreen.WORKOUT_HUD -> {
                            // Minimize workout HUD to background and navigate safely to home without stopping workout
                            subScreen = MilesSubScreen.NONE
                            Toast.makeText(
                                this@MainActivity,
                                "Workout recording in background. Tap banner to return.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        subScreen != MilesSubScreen.NONE -> {
                            subScreen = MilesSubScreen.NONE
                        }
                        currentTab != MilesNavigationTab.HOME -> {
                            currentTab = MilesNavigationTab.HOME
                        }
                        else -> {
                            // On Home tab: double back within 2s to exit prevents accidental closure
                            val now = System.currentTimeMillis()
                            if (now - lastBackPressTime < 2000L) {
                                finish()
                            } else {
                                lastBackPressTime = now
                                Toast.makeText(
                                    this@MainActivity,
                                    "Swipe back again to exit",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                // Check if user needs onboarding/setup
                if (!userPrefs.hasCompletedSetup || subScreen == MilesSubScreen.SETUP) {
                    OnboardingSetupScreen(
                        preferences = preferences,
                        onSetupComplete = {
                            subScreen = MilesSubScreen.NONE
                        }
                    )
                } else {
                    val configuration = LocalConfiguration.current
                    val isExpanded = configuration.screenWidthDp >= 600
                    val isWorkoutActive = liveStats.state != TrackingState.IDLE

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (!isExpanded && subScreen == MilesSubScreen.NONE) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 6.dp
                                ) {
                                    MilesNavigationTab.entries.forEach { tab ->
                                        val isSelected = currentTab == tab
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                currentTab = tab
                                                subScreen = MilesSubScreen.NONE
                                            },
                                            icon = {
                                                if (tab == MilesNavigationTab.HOME && isWorkoutActive) {
                                                    BadgedBox(badge = { Badge() }) {
                                                        Icon(tab.icon, contentDescription = tab.label)
                                                    }
                                                } else {
                                                    Icon(tab.icon, contentDescription = tab.label)
                                                }
                                            },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Navigation Rail for Tablets / Wide foldables
                            if (isExpanded && subScreen == MilesSubScreen.NONE) {
                                NavigationRail(
                                    modifier = Modifier.fillMaxHeight(),
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    MilesNavigationTab.entries.forEach { tab ->
                                        val isSelected = currentTab == tab
                                        NavigationRailItem(
                                            selected = isSelected,
                                            onClick = {
                                                currentTab = tab
                                                subScreen = MilesSubScreen.NONE
                                            },
                                            icon = {
                                                if (tab == MilesNavigationTab.HOME && isWorkoutActive) {
                                                    BadgedBox(badge = { Badge() }) {
                                                        Icon(tab.icon, contentDescription = tab.label)
                                                    }
                                                } else {
                                                    Icon(tab.icon, contentDescription = tab.label)
                                                }
                                            },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }

                            // Content Container
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                when (subScreen) {
                                    MilesSubScreen.WORKOUT_HUD -> {
                                        WorkoutHudScreen(
                                            smartEngine = smartEngine,
                                            onFinishWorkout = { saved ->
                                                selectedActivity = saved
                                                subScreen = MilesSubScreen.ACTIVITY_DETAIL
                                            },
                                            onDiscardWorkout = {
                                                subScreen = MilesSubScreen.NONE
                                            }
                                        )
                                    }
                                    MilesSubScreen.ACTIVITY_DETAIL -> {
                                        selectedActivity?.let { act ->
                                            ActivityDetailScreen(
                                                activity = act,
                                                repository = repository,
                                                onBack = { subScreen = MilesSubScreen.NONE },
                                                onSetAsGhostReference = { ghost ->
                                                    smartEngine.setGhostModeActivity(ghost)
                                                },
                                                onDeleted = { subScreen = MilesSubScreen.NONE }
                                            )
                                        } ?: run { subScreen = MilesSubScreen.NONE }
                                    }
                                    MilesSubScreen.DEVICES -> {
                                        DevicesScreen(
                                            deviceManager = deviceManager,
                                            wearCompanion = wearCompanion
                                        )
                                    }
                                    MilesSubScreen.STUDIO -> {
                                        MilesStudioScreen(
                                            repository = repository,
                                            smartEngine = smartEngine,
                                            wearCompanion = wearCompanion,
                                            preferences = preferences,
                                            onBack = { subScreen = MilesSubScreen.NONE },
                                            onOpenDistanceCalculator = { subScreen = MilesSubScreen.DISTANCE_CALCULATOR }
                                        )
                                    }
                                    MilesSubScreen.DISTANCE_CALCULATOR -> {
                                        DistanceCalculatorScreen(
                                            onBack = { subScreen = MilesSubScreen.STUDIO }
                                        )
                                    }
                                    MilesSubScreen.SETUP -> {
                                        // Handled in parent check
                                    }
                                    MilesSubScreen.NONE -> {
                                        when (currentTab) {
                                            MilesNavigationTab.HOME -> {
                                                DashboardScreen(
                                                    smartEngine = smartEngine,
                                                    mediaIntegration = mediaIntegration,
                                                    deviceManager = deviceManager,
                                                    activities = activities,
                                                    userPreferences = userPrefs,
                                                    onStartActivity = { type ->
                                                        smartEngine.counterIntervalMs = userPrefs.counterIntervalMs
                                                        smartEngine.telemetryIntervalMs = userPrefs.telemetryIntervalMs
                                                        smartEngine.startTracking(type)
                                                        subScreen = MilesSubScreen.WORKOUT_HUD
                                                    },
                                                    onNavigateToHud = {
                                                        subScreen = MilesSubScreen.WORKOUT_HUD
                                                    },
                                                    onSelectActivity = { act ->
                                                        selectedActivity = act
                                                        subScreen = MilesSubScreen.ACTIVITY_DETAIL
                                                    },
                                                    onOpenStudio = {
                                                        subScreen = MilesSubScreen.STUDIO
                                                    }
                                                )
                                            }
                                            MilesNavigationTab.JOURNAL -> {
                                                JournalScreen(
                                                    activities = activities,
                                                    onSelectActivity = { act ->
                                                        selectedActivity = act
                                                        subScreen = MilesSubScreen.ACTIVITY_DETAIL
                                                    }
                                                )
                                            }
                                            MilesNavigationTab.ROUTES -> {
                                                RouteBuilderScreen(
                                                    repository = repository
                                                )
                                            }
                                            MilesNavigationTab.PROFILE -> {
                                                SettingsScreen(
                                                    preferences = preferences,
                                                    repository = repository,
                                                    onOpenStudio = { subScreen = MilesSubScreen.STUDIO },
                                                    onOpenDevices = { subScreen = MilesSubScreen.DEVICES },
                                                    onRerunSetup = { subScreen = MilesSubScreen.SETUP }
                                                )
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
    }
}
