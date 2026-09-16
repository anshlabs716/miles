package com.example.miles.ui.settings

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.AppIconOption
import com.example.miles.data.local.BaseThemeOption
import com.example.miles.data.local.ColorVisionMode
import com.example.miles.data.local.DistanceUnit
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.model.PrivacyZoneEntity
import com.example.miles.data.repository.MilesRepository
import com.example.miles.data.repository.format
import com.example.miles.ui.theme.LiquidGlassCard
import kotlinx.coroutines.launch
import java.util.Locale

enum class SettingsCategory(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Tune),
    ATHLETE("Athlete & Profile", Icons.Default.FitnessCenter),
    WORKOUT("Workout HUD", Icons.Default.Speed),
    AUDIO("Audio Coach", Icons.Default.VolumeUp),
    MAPS("Maps & GPS", Icons.Default.Map),
    APPEARANCE("Themes & Icons", Icons.Default.Palette),
    ACCESSIBILITY("Accessibility", Icons.Default.AccessibilityNew),
    SENSORS("Sensors & Watch", Icons.Default.Sensors),
    PRIVACY("Privacy & Storage", Icons.Default.Lock),
    STUDIO("Miles Studio", Icons.Default.Code)
}

@Composable
fun SettingsScreen(
    preferences: MilesPreferences,
    repository: MilesRepository,
    onOpenStudio: () -> Unit,
    onOpenDevices: () -> Unit = {},
    onRerunSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPrefs by preferences.userPreferences.collectAsState()
    val privacyZones by repository.privacyZones.collectAsState(initial = emptyList())
    val trashActivities by repository.trashActivities.collectAsState(initial = emptyList())

    // Category filter and search query
    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(userPrefs.appIcon) }

    // Dialog states
    var showAddZoneDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var showResetDefaultsDialog by remember { mutableStateOf(false) }

    // Biometrics edit states
    var nameInput by remember(userPrefs.userName) { mutableStateOf(userPrefs.userName) }
    var ageInput by remember(userPrefs.userAge) { mutableStateOf(userPrefs.userAge.toString()) }
    var weightInput by remember(userPrefs.userWeightKg) { mutableStateOf(userPrefs.userWeightKg.toString()) }
    var heightInput by remember(userPrefs.userHeightCm) { mutableStateOf(userPrefs.userHeightCm.toString()) }
    var maxHrInput by remember(userPrefs.maxHeartRateBpm) { mutableStateOf(userPrefs.maxHeartRateBpm.toString()) }
    var restingHrInput by remember(userPrefs.restingHeartRateBpm) { mutableStateOf(userPrefs.restingHeartRateBpm.toString()) }
    var lthrInput by remember(userPrefs.lactateThresholdHrBpm) { mutableStateOf(userPrefs.lactateThresholdHrBpm.toString()) }

    // Haptics helper
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrate(durationMs: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(durationMs)
        }
    }

    // Calculated BMI
    val bmi = remember(userPrefs.userWeightKg, userPrefs.userHeightCm) {
        val hM = userPrefs.userHeightCm / 100f
        if (hM > 0.5f) userPrefs.userWeightKg / (hM * hM) else 22.5f
    }
    val bmiLabel = remember(bmi) {
        when {
            bmi < 18.5f -> "Underweight"
            bmi < 25.0f -> "Normal"
            bmi < 30.0f -> "Overweight"
            else -> "Obese"
        }
    }

    // Heart Rate Reserve
    val hrr = (userPrefs.maxHeartRateBpm - userPrefs.restingHeartRateBpm).coerceAtLeast(40)

    fun matchesSearch(vararg terms: String): Boolean {
        if (searchQuery.isBlank()) return true
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        return terms.any { it.lowercase(Locale.ROOT).contains(q) }
    }

    fun shouldShow(category: SettingsCategory, vararg searchTerms: String): Boolean {
        if (searchQuery.isNotBlank()) {
            return matchesSearch(*searchTerms, category.label)
        }
        return selectedCategory == SettingsCategory.ALL || selectedCategory == category
    }

    // Swipe-to-back gesture support: clears search or category filter before popping screen
    BackHandler(enabled = searchQuery.isNotBlank() || selectedCategory != SettingsCategory.ALL) {
        if (searchQuery.isNotBlank()) {
            searchQuery = ""
        } else {
            selectedCategory = SettingsCategory.ALL
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP HEADER & SEARCH BAR
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Settings & Config",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Customize biometrics, HUD, real maps, audio & engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            triggerHaptic(vibrator)
                            Toast.makeText(context, "Settings refreshed!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Settings & Launcher State",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    FilledTonalButton(
                        onClick = onOpenStudio,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Studio")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search all settings (e.g. pace, maps, HR, haptics...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // CATEGORY PILLS
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SettingsCategory.entries) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat && searchQuery.isBlank(),
                        onClick = {
                            selectedCategory = cat
                            searchQuery = ""
                        },
                        label = { Text(cat.label, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // SETTINGS CONTENT LIST
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. ATHLETE PROFILE & BIOMETRICS
            if (shouldShow(SettingsCategory.ATHLETE, "profile", "weight", "height", "bmi", "heart rate", "max hr", "resting hr", "lthr", "age", "gender", "goals", "steps")) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "ATHLETE PROFILE & BIOMETRICS",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (userPrefs.userName.isNotBlank()) userPrefs.userName else "Runner Profile",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "BMI ${bmi.format(1)} ($bmiLabel)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Name & Age
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = {
                                        nameInput = it
                                        preferences.updatePreferences { p -> p.copy(userName = it) }
                                    },
                                    label = { Text("Full Name / Callout") },
                                    modifier = Modifier.weight(1.5f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = ageInput,
                                    onValueChange = {
                                        ageInput = it
                                        it.toIntOrNull()?.let { a -> preferences.updatePreferences { p -> p.copy(userAge = a) } }
                                    },
                                    label = { Text("Age") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Weight & Height
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = weightInput,
                                    onValueChange = {
                                        weightInput = it
                                        it.toFloatOrNull()?.let { w -> preferences.updatePreferences { p -> p.copy(userWeightKg = w) } }
                                    },
                                    label = { Text("Weight (kg)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = heightInput,
                                    onValueChange = {
                                        heightInput = it
                                        it.toFloatOrNull()?.let { h -> preferences.updatePreferences { p -> p.copy(userHeightCm = h) } }
                                    },
                                    label = { Text("Height (cm)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Heart Rate Parameters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = restingHrInput,
                                    onValueChange = {
                                        restingHrInput = it
                                        it.toIntOrNull()?.let { rhr -> preferences.updatePreferences { p -> p.copy(restingHeartRateBpm = rhr) } }
                                    },
                                    label = { Text("Rest HR") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = maxHrInput,
                                    onValueChange = {
                                        maxHrInput = it
                                        it.toIntOrNull()?.let { mhr -> preferences.updatePreferences { p -> p.copy(maxHeartRateBpm = mhr) } }
                                    },
                                    label = { Text("Max HR") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = lthrInput,
                                    onValueChange = {
                                        lthrInput = it
                                        it.toIntOrNull()?.let { lthr -> preferences.updatePreferences { p -> p.copy(lactateThresholdHrBpm = lthr) } }
                                    },
                                    label = { Text("LTHR") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Heart Rate Reserve (HRR): $hrr BPM. Karvonen Zone 2: ${(userPrefs.restingHeartRateBpm + hrr * 0.60f).toInt()} - ${(userPrefs.restingHeartRateBpm + hrr * 0.70f).toInt()} BPM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Primary Sport & Onboarding rerun
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Primary Discipline", style = MaterialTheme.typography.labelSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("RUNNING", "CYCLING", "WALKING").forEach { sport ->
                                            FilterChip(
                                                selected = userPrefs.primarySport == sport,
                                                onClick = { preferences.updatePreferences { it.copy(primarySport = sport) } },
                                                label = { Text(sport.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }
                                OutlinedButton(onClick = onRerunSetup) {
                                    Text("Rerun Setup", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. WORKOUT HUD & RUNTIME ENGINE
            if (shouldShow(SettingsCategory.WORKOUT, "hud", "workout", "unit", "pause", "countdown", "metronome", "heart rate alarm", "cadence", "dnd", "tick", "battery")) {
                item {
                    WorkoutSettingsCard(
                        userPrefs = userPrefs,
                        preferences = preferences
                    )
                }
            }

            // 3. AUDIO COACH & METRONOME
            if (shouldShow(SettingsCategory.AUDIO, "audio", "coach", "voice", "speech", "split", "volume", "cue", "announce", "tts")) {
                item {
                    AudioSettingsCard(
                        userPrefs = userPrefs,
                        preferences = preferences
                    )
                }
            }

            // 4. MAPS & GPS NAVIGATION
            if (shouldShow(SettingsCategory.MAPS, "maps", "gps", "satellite", "osm", "rotation", "follow", "filter", "accuracy", "breadcrumb", "heading")) {
                item {
                    MapsSettingsCard(
                        userPrefs = userPrefs,
                        preferences = preferences
                    )
                }
            }

            // 5. THEMES & APP ICONS - WITH ICON CHANGER
            if (shouldShow(SettingsCategory.APPEARANCE, "themes", "appearance", "color", "dark", "light", "amoled", "icon", "liquid glass", "glass")) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "THEMES & APP ICON ALIASES",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Visual Palette", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                BaseThemeOption.entries.forEach { theme ->
                                    val isSelected = userPrefs.theme == theme
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable { preferences.setTheme(theme) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (theme) {
                                                             BaseThemeOption.TWILIGHT -> Color(0xFF00E5FF)
                                                             BaseThemeOption.AMOLED -> Color(0xFF101010)
                                                             BaseThemeOption.AMOLED_RED -> Color(0xFFFF2D55)
                                                             BaseThemeOption.LIGHT -> Color(0xFFF0F4F8)
                                                             BaseThemeOption.STOCK -> Color(0xFF7C4DFF)
                                                        }
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(theme.label, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            SettingRow(
                                title = "Liquid Glass Frost & Specular Glow",
                                subtitle = "Frosted specular blur and translucent panel styling",
                                checked = userPrefs.liquidGlassEnabled,
                                onCheckedChange = { preferences.setLiquidGlass(it) }
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // INTEGRATED APP ICON CHANGER
                            AppIconChangerSection(
                                preferences = preferences,
                                selectedIcon = selectedIcon,
                                onIconChanged = { newIcon ->
                                    selectedIcon = newIcon
                                }
                            )
                        }
                    }
                }
            }

            // 6. ACCESSIBILITY SUITE
            if (shouldShow(SettingsCategory.ACCESSIBILITY, "accessibility", "larger text", "bold", "contrast", "motion", "color vision", "protanopia", "targets")) {
                item {
                    AccessibilitySettingsCard(
                        userPrefs = userPrefs,
                        preferences = preferences
                    )
                }
            }

            // 7. SENSORS & WEARABLES
            if (shouldShow(SettingsCategory.SENSORS, "sensors", "wear", "watch", "bluetooth", "ble", "heart rate strap", "haptics", "vibration")) {
                item {
                    SensorsSettingsCard(
                        onOpenDevices = onOpenDevices,
                        vibrator = vibrator
                    )
                }
            }

            // 8. PRIVACY, DATA & EXPORTS
            if (shouldShow(SettingsCategory.PRIVACY, "privacy", "zone", "backup", "trash", "restore", "wipe", "reset", "export", "sqlite")) {
                item {
                    PrivacySettingsCard(
                        privacyZones = privacyZones,
                        trashCount = trashActivities.size,
                        onAddZone = { showAddZoneDialog = true },
                        onDeleteZone = { zoneId ->
                            scope.launch {
                                repository.deletePrivacyZone(zoneId)
                                Toast.makeText(context, "Privacy zone removed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenBackup = { showBackupDialog = true },
                        onOpenTrash = { showTrashDialog = true },
                        onWipeData = { showWipeConfirmDialog = true },
                        onResetDefaults = { showResetDefaultsDialog = true }
                    )
                }
            }

            // 9. MILES STUDIO PROMINENT CARD
            if (shouldShow(SettingsCategory.STUDIO, "studio", "kalman", "gnss", "developer", "math", "vdot", "nmea", "telemetry")) {
                item {
                    StudioShortcutCard(onOpenStudio = onOpenStudio)
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // ==========================================================
        // DIALOGS
        // ==========================================================
        if (showAddZoneDialog) {
            AddPrivacyZoneDialog(
                onDismiss = { showAddZoneDialog = false },
                onAdd = { name, lat, lon, radius ->
                    scope.launch {
                        repository.savePrivacyZone(
                            PrivacyZoneEntity(
                                name = name,
                                latitude = lat,
                                longitude = lon,
                                radiusMeters = radius
                            )
                        )
                        Toast.makeText(context, "Privacy zone '$name' saved", Toast.LENGTH_SHORT).show()
                        showAddZoneDialog = false
                    }
                }
            )
        }

        if (showBackupDialog) {
            BackupRestoreDialog(onDismiss = { showBackupDialog = false })
        }

        if (showTrashDialog) {
            TrashBinDialog(
                trashActivities = trashActivities,
                repository = repository,
                onDismiss = { showTrashDialog = false }
            )
        }

        if (showWipeConfirmDialog) {
            WipeConfirmDialog(
                onDismiss = { showWipeConfirmDialog = false },
                onConfirmWipe = {
                    scope.launch {
                        repository.clearAllActivities()
                        Toast.makeText(context, "All activity data has been wiped", Toast.LENGTH_LONG).show()
                        showWipeConfirmDialog = false
                    }
                }
            )
        }

        if (showResetDefaultsDialog) {
            ResetDefaultsDialog(
                preferences = preferences,
                onDismiss = { showResetDefaultsDialog = false }
            )
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

fun triggerHaptic(vibrator: Vibrator?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator != null) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else if (vibrator != null) {
        @Suppress("DEPRECATION")
        vibrator.vibrate(50)
    }
}
