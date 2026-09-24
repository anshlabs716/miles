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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FitnessCenter
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
import androidx.compose.runtime.rememberCoroutineScope
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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import com.example.miles.engine.DeviceManager
import com.example.miles.engine.MediaIntegration
import com.example.miles.engine.MoveReminderManager
import com.example.miles.engine.PedometerManager
import com.example.miles.engine.SmartTrackingEngine
import com.example.miles.engine.TrackingForegroundService
import com.example.miles.health.HealthConnectConnectionState
import com.example.miles.health.HealthConnectAvailability
import com.example.miles.health.HealthConnectManager
import com.example.miles.engine.TrackingState
import com.example.miles.ui.dashboard.DashboardScreen
import com.example.miles.ui.devices.DevicesScreen
import com.example.miles.ui.history.ActivityDetailScreen
import com.example.miles.ui.journal.JournalScreen
import com.example.miles.ui.routes.RouteBuilderScreen
import com.example.miles.ui.settings.SettingsScreen
import com.example.miles.ui.setup.OnboardingSetupScreen
import com.example.miles.ui.setup.PermissionPromptScreen
import com.example.miles.ui.studio.DistanceCalculatorScreen
import com.example.miles.ui.studio.MilesStudioScreen
import com.example.miles.ui.theme.MilesTheme
import com.example.miles.ui.tools.ToolsDiagnosticsScreen
import com.example.miles.ui.training.ProgressiveTrainingScreen
import com.example.miles.ui.workout.WorkoutHudScreen
import com.example.miles.wear.WearCompanionManager
import kotlinx.coroutines.launch

enum class MilesNavigationTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home), TRAINING("Training", Icons.Default.FitnessCenter), JOURNAL("Journal", Icons.AutoMirrored.Filled.List), ROUTES("Routes", Icons.Default.Map), PROFILE("Profile", Icons.Default.Person)
}

enum class MilesSubScreen { NONE, SETUP, ACTIVITY_DETAIL, WORKOUT_HUD, DEVICES, STUDIO, DISTANCE_CALCULATOR, PERMISSIONS_PROMPT, TOOLS }

class MainActivity : ComponentActivity() {
    private lateinit var database: MilesDatabase
    private lateinit var repository: MilesRepository
    private lateinit var preferences: MilesPreferences
    private lateinit var smartEngine: SmartTrackingEngine
    private lateinit var wearCompanion: WearCompanionManager
    private lateinit var deviceManager: DeviceManager
    private lateinit var mediaIntegration: MediaIntegration
    private lateinit var locationTracker: com.example.miles.engine.LocationTracker
    private lateinit var pedometerManager: PedometerManager
    private lateinit var moveReminderManager: MoveReminderManager
    private lateinit var healthConnectManager: HealthConnectManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        database = MilesDatabase.getInstance(this)
        repository = MilesRepository(database.activityDao(), database.savedRouteDao(), database.privacyZoneDao(), database.goalDao())
        preferences = MilesPreferences(this)
        smartEngine = SmartTrackingEngine.getInstance(this, repository)
        wearCompanion = WearCompanionManager(this)
        deviceManager = DeviceManager(this)
        mediaIntegration = MediaIntegration(this)
        locationTracker = com.example.miles.engine.LocationTracker(this)
        pedometerManager = PedometerManager(this, preferences)
        moveReminderManager = MoveReminderManager(this, preferences)
        healthConnectManager = HealthConnectManager(this)
        moveReminderManager.scheduleNextReminder()
        com.example.miles.widget.MilesWidgetSyncReceiver.schedulePeriodicSync(this)
        com.example.miles.widget.MilesWidgetSyncReceiver.syncWidgetsNow(this)

        setContent {
            val userPrefs by preferences.userPreferences.collectAsState()
            val activities by repository.activities.collectAsState(initial = emptyList())
            val liveStats by smartEngine.liveStats.collectAsState()
            val powerManager = remember { getSystemService(Context.POWER_SERVICE) as? PowerManager }
            var showBatteryOptDialog by remember { mutableStateOf(powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) }

            var healthConnectState by remember { mutableStateOf(HealthConnectConnectionState.UNAVAILABLE) }
            val healthConnectPermissionsLauncher = rememberLauncherForActivityResult(
                androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
            ) { grantedPermissions ->
                healthConnectState = if (grantedPermissions.containsAll(healthConnectManager.permissions)) {
                    HealthConnectConnectionState.CONNECTED
                } else {
                    HealthConnectConnectionState.AVAILABLE_NOT_PERMITTED
                }
            }
            val requestHealthConnectPermissions = {
                if (healthConnectManager.availability == HealthConnectAvailability.AVAILABLE) {
                    healthConnectPermissionsLauncher.launch(healthConnectManager.permissions)
                }
            }
            val openHealthConnectSettings: () -> Unit = {
                healthConnectManager.manageDataIntent()?.let { settingsIntent ->
                    runCatching { startActivity(settingsIntent) }
                }
                Unit
            }
            LaunchedEffect(Unit) {
                healthConnectState = healthConnectManager.connectionState()
            }
            val allPermissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                val activityGranted = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q || permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true
                if (activityGranted) TrackingForegroundService.start(this@MainActivity)
                if (locationGranted) {
                    locationTracker.startTracking(intervalMs = userPrefs.sensorRefreshRateMs, gpsEnabled = userPrefs.gpsSensorEnabled) { point ->
                        if (smartEngine.liveStats.value.state == TrackingState.RECORDING) smartEngine.processLocation(point)
                    }
                }
                // Health Connect is optional. On first boot, ask only when its provider is available.
                if (!userPrefs.healthConnectFirstBootHandled) {
                    when (healthConnectManager.availability) {
                        HealthConnectAvailability.AVAILABLE -> requestHealthConnectPermissions()
                        HealthConnectAvailability.UNAVAILABLE -> Toast.makeText(
                            this@MainActivity,
                            "Health Connect is unavailable. MILES works fully without it.",
                            Toast.LENGTH_LONG
                        ).show()
                        HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> Toast.makeText(
                            this@MainActivity,
                            "Health Connect needs an update. MILES works fully without it.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    preferences.markHealthConnectFirstBootHandled()
                }
            }
            val requiredPermissions = remember {
                buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION); add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
                    add(Manifest.permission.BODY_SENSORS)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) { add(Manifest.permission.BLUETOOTH_SCAN); add(Manifest.permission.BLUETOOTH_CONNECT) }
                }.toTypedArray()
            }
            val permissionPromptVisible = !userPrefs.permissionPromptShown

            LaunchedEffect(Unit) {
                repository.purgePreloadedSeedData()
            }

            LaunchedEffect(userPrefs.hasCompletedSetup, userPrefs.stepSensorHardwareEnabled) {
                val activityGranted = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q || checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (activityGranted && userPrefs.hasCompletedSetup) TrackingForegroundService.start(this@MainActivity)
                if (userPrefs.stepSensorHardwareEnabled) pedometerManager.startTracking() else pedometerManager.stopTracking()
            }

            LaunchedEffect(userPrefs.sensorRefreshRateMs, userPrefs.gpsSensorEnabled) {
                if (locationTracker.hasLocationPermission()) locationTracker.startTracking(userPrefs.sensorRefreshRateMs, userPrefs.gpsSensorEnabled) { point ->
                    if (smartEngine.liveStats.value.state == TrackingState.RECORDING) smartEngine.processLocation(point)
                }
            }

            MilesTheme(themeOption = userPrefs.theme, liquidGlassEnabled = userPrefs.liquidGlassEnabled, accessibility = userPrefs.accessibility) {
                var currentTab by remember { mutableStateOf(MilesNavigationTab.HOME) }
                var tabBackStack by remember { mutableStateOf(listOf(MilesNavigationTab.HOME)) }
                var subScreen by remember { mutableStateOf(MilesSubScreen.NONE) }
                var selectedActivity by remember { mutableStateOf<ActivityEntity?>(null) }
                var lastBackPressTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
                val mainScope = rememberCoroutineScope()

                LaunchedEffect(intent) {
                    val quickSport = intent?.getStringExtra(com.example.miles.widget.MilesQuickWorkoutWidgetProvider.EXTRA_START_SPORT)
                    if (!quickSport.isNullOrBlank()) {
                        val sportType = ActivityType.fromString(quickSport)
                        smartEngine.counterIntervalMs = userPrefs.counterIntervalMs
                        smartEngine.telemetryIntervalMs = userPrefs.telemetryIntervalMs
                        smartEngine.startTracking(sportType)
                        subScreen = MilesSubScreen.WORKOUT_HUD
                        intent.removeExtra(com.example.miles.widget.MilesQuickWorkoutWidgetProvider.EXTRA_START_SPORT)
                    }
                }

                BackHandler(enabled = true) {
                    when {
                        subScreen == MilesSubScreen.PERMISSIONS_PROMPT -> subScreen = MilesSubScreen.NONE
                        subScreen == MilesSubScreen.DISTANCE_CALCULATOR -> subScreen = MilesSubScreen.STUDIO
                        subScreen == MilesSubScreen.ACTIVITY_DETAIL -> subScreen = MilesSubScreen.NONE
                        subScreen == MilesSubScreen.STUDIO -> subScreen = MilesSubScreen.NONE
                        subScreen == MilesSubScreen.DEVICES -> subScreen = MilesSubScreen.NONE
                        subScreen == MilesSubScreen.WORKOUT_HUD -> { subScreen = MilesSubScreen.NONE; Toast.makeText(this@MainActivity, "Workout running in background. Tap card on Home to resume.", Toast.LENGTH_SHORT).show() }
                        subScreen == MilesSubScreen.SETUP -> if (userPrefs.hasCompletedSetup) subScreen = MilesSubScreen.NONE
                        tabBackStack.size > 1 -> { val popped = tabBackStack.dropLast(1); tabBackStack = popped; currentTab = popped.last() }
                        currentTab != MilesNavigationTab.HOME -> { currentTab = MilesNavigationTab.HOME; tabBackStack = listOf(MilesNavigationTab.HOME) }
                        else -> { val now = System.currentTimeMillis(); if (now - lastBackPressTime < 2000L) moveTaskToBack(true) else { lastBackPressTime = now; Toast.makeText(this@MainActivity, "Swipe back again to exit MILES", Toast.LENGTH_SHORT).show() } }
                    }
                }

                if (showBatteryOptDialog && !permissionPromptVisible) {
                    AlertDialog(
                        onDismissRequest = { showBatteryOptDialog = false },
                        icon = { Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        title = { Text("Unrestricted Background Battery", style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) },
                        text = { Text("MILES uses background tracking for real steps and active GPS routes. Android may stop tracking if battery restrictions remain enabled.", style = MaterialTheme.typography.bodyMedium) },
                        confirmButton = { Button(onClick = { showBatteryOptDialog = false; runCatching { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }) }.onFailure { runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } } }) { Text("Allow Exemption") } },
                        dismissButton = { TextButton(onClick = { showBatteryOptDialog = false }) { Text("Not Now") } }
                    )
                }

                if (permissionPromptVisible) {
                    PermissionPromptScreen(
                        onAllow = {
                            preferences.markPermissionPromptShown()
                            allPermissionsLauncher.launch(requiredPermissions)
                            if (healthConnectManager.availability == HealthConnectAvailability.AVAILABLE) {
                                requestHealthConnectPermissions()
                            }
                        },
                        onDeny = {
                            preferences.markPermissionPromptShown()
                            preferences.markHealthConnectFirstBootHandled()
                        }
                    )
                } else if (!userPrefs.hasCompletedSetup || subScreen == MilesSubScreen.SETUP) {
                    OnboardingSetupScreen(preferences = preferences, onSetupComplete = { subScreen = MilesSubScreen.NONE })
                } else {
                    val configuration = LocalConfiguration.current
                    val isExpanded = configuration.screenWidthDp >= 600
                    val isWorkoutActive = liveStats.state != TrackingState.IDLE
                    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
                        if (!isExpanded && subScreen == MilesSubScreen.NONE) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                            MilesNavigationTab.entries.forEach { tab -> NavigationBarItem(selected = currentTab == tab, onClick = { if (currentTab != tab) { tabBackStack = tabBackStack + tab; currentTab = tab }; subScreen = MilesSubScreen.NONE }, icon = { if (tab == MilesNavigationTab.HOME && isWorkoutActive) BadgedBox(badge = { Badge() }) { Icon(tab.icon, contentDescription = tab.label) } else Icon(tab.icon, contentDescription = tab.label) }, label = { Text(tab.label) }) }
                        }
                    }) { innerPadding ->
                        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            if (isExpanded && subScreen == MilesSubScreen.NONE) NavigationRail(modifier = Modifier.fillMaxHeight(), containerColor = MaterialTheme.colorScheme.surface) {
                                MilesNavigationTab.entries.forEach { tab -> NavigationRailItem(selected = currentTab == tab, onClick = { if (currentTab != tab) { tabBackStack = tabBackStack + tab; currentTab = tab }; subScreen = MilesSubScreen.NONE }, icon = { Icon(tab.icon, contentDescription = tab.label) }, label = { Text(tab.label) }) }
                            }
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                when (subScreen) {
                                    MilesSubScreen.WORKOUT_HUD -> WorkoutHudScreen(smartEngine = smartEngine, onFinishWorkout = { saved -> selectedActivity = saved; subScreen = MilesSubScreen.ACTIVITY_DETAIL }, onDiscardWorkout = { subScreen = MilesSubScreen.NONE }, onBack = { subScreen = MilesSubScreen.NONE })
                                    MilesSubScreen.ACTIVITY_DETAIL -> selectedActivity?.let { act -> ActivityDetailScreen(activity = act, repository = repository, onBack = { subScreen = MilesSubScreen.NONE }, onSetAsGhostReference = { ghost -> smartEngine.setGhostModeActivity(ghost) }, onDeleted = { subScreen = MilesSubScreen.NONE }) } ?: run { subScreen = MilesSubScreen.NONE }
                                    MilesSubScreen.DEVICES -> DevicesScreen(deviceManager = deviceManager, wearCompanion = wearCompanion, preferences = preferences, moveReminderManager = moveReminderManager)
                                    MilesSubScreen.STUDIO -> MilesStudioScreen(repository = repository, smartEngine = smartEngine, wearCompanion = wearCompanion, preferences = preferences, onBack = { subScreen = MilesSubScreen.NONE }, onOpenDistanceCalculator = { subScreen = MilesSubScreen.DISTANCE_CALCULATOR })
                                    MilesSubScreen.DISTANCE_CALCULATOR -> DistanceCalculatorScreen(onBack = { subScreen = MilesSubScreen.STUDIO })
                                    MilesSubScreen.PERMISSIONS_PROMPT -> PermissionPromptScreen(
                                        onAllow = {
                                            preferences.markPermissionPromptShown()
                                            allPermissionsLauncher.launch(requiredPermissions)
                                            if (healthConnectManager.availability == HealthConnectAvailability.AVAILABLE) {
                                                requestHealthConnectPermissions()
                                            }
                                            subScreen = MilesSubScreen.NONE
                                        },
                                        onDeny = {
                                            subScreen = MilesSubScreen.NONE
                                        }
                                    )
                                    MilesSubScreen.TOOLS -> ToolsDiagnosticsScreen(onBack = { subScreen = MilesSubScreen.NONE })
                                    MilesSubScreen.SETUP -> Unit
                                    MilesSubScreen.NONE -> when (currentTab) {
                                        MilesNavigationTab.HOME -> DashboardScreen(
                                            smartEngine = smartEngine,
                                            mediaIntegration = mediaIntegration,
                                            deviceManager = deviceManager,
                                            activities = activities,
                                            userPreferences = userPrefs,
                                            preferences = preferences,
                                            pedometerManager = pedometerManager,
                                            wearCompanion = wearCompanion,
                                            onStartActivity = { type ->
                                                smartEngine.counterIntervalMs = userPrefs.counterIntervalMs
                                                smartEngine.telemetryIntervalMs = userPrefs.telemetryIntervalMs
                                                smartEngine.startTracking(type)
                                                subScreen = MilesSubScreen.WORKOUT_HUD
                                            },
                                            onNavigateToHud = { subScreen = MilesSubScreen.WORKOUT_HUD },
                                            onSelectActivity = { act ->
                                                selectedActivity = act
                                                subScreen = MilesSubScreen.ACTIVITY_DETAIL
                                            },
                                            onOpenStudio = { subScreen = MilesSubScreen.STUDIO },
                                            onOpenTools = { subScreen = MilesSubScreen.TOOLS },
                                            onOpenPermissionsPrompt = { subScreen = MilesSubScreen.PERMISSIONS_PROMPT },
                                            onOpenRoutes = { currentTab = MilesNavigationTab.ROUTES },
                                            onOpenStats = { currentTab = MilesNavigationTab.JOURNAL }
                                        )
                                        MilesNavigationTab.JOURNAL -> JournalScreen(
                                            activities = activities,
                                            repository = repository,
                                            preferences = preferences,
                                            onSelectActivity = { act -> selectedActivity = act; subScreen = MilesSubScreen.ACTIVITY_DETAIL },
                                            onToggleFavorite = { act -> mainScope.launch { repository.toggleActivityFavorite(act.id) } }
                                        )
                                        MilesNavigationTab.TRAINING -> ProgressiveTrainingScreen(preferences = preferences, onStartWorkout = { title, intervals, type -> smartEngine.counterIntervalMs = userPrefs.counterIntervalMs; smartEngine.telemetryIntervalMs = userPrefs.telemetryIntervalMs; smartEngine.startIntervalWorkout(title, intervals, type); subScreen = MilesSubScreen.WORKOUT_HUD })
                                        MilesNavigationTab.ROUTES -> RouteBuilderScreen(repository = repository, onStartNavigation = { route -> smartEngine.setNavigationRoute(route); if (smartEngine.liveStats.value.state != TrackingState.RECORDING) smartEngine.startTracking(ActivityType.RUNNING); subScreen = MilesSubScreen.WORKOUT_HUD })
                                        MilesNavigationTab.PROFILE -> SettingsScreen(
                                            preferences = preferences,
                                            repository = repository,
                                            onOpenStudio = { subScreen = MilesSubScreen.STUDIO },
                                            onOpenDevices = { subScreen = MilesSubScreen.DEVICES },
                                            onRerunSetup = { subScreen = MilesSubScreen.SETUP },
                                            onOpenPermissionsPrompt = { subScreen = MilesSubScreen.PERMISSIONS_PROMPT },
                                            onOpenTools = { subScreen = MilesSubScreen.TOOLS },
                                            healthConnectState = healthConnectState,
                                            onRequestHealthConnectPermissions = requestHealthConnectPermissions,
                                            onOpenHealthConnectSettings = openHealthConnectSettings,
                                            canOpenHealthConnectSettings = healthConnectManager.manageDataIntent() != null
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::pedometerManager.isInitialized) pedometerManager.stopTracking()
        if (::locationTracker.isInitialized) locationTracker.stopTracking()
    }
}
