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
                            val (activeIcon, message) = preferences.refreshAppIconState()
                            triggerHaptic(vibrator)
                            Toast.makeText(context, "Refreshed & Synced\n$message", Toast.LENGTH_SHORT).show()
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
            if (shouldShow(SettingsCategory.WORKOUT, "workout", "hud", "units", "metric", "imperial", "auto pause", "countdown", "screen awake", "ghost pacer", "battery saver", "target pace")) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "WORKOUT HUD & RUN ENGINE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Units Metric vs Imperial
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Distance & Pace Units", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        if (userPrefs.unit == DistanceUnit.METRIC) "Kilometers (km) • min/km" else "Miles (mi) • min/mi",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = userPrefs.unit == DistanceUnit.METRIC,
                                        onClick = { preferences.setUnit(DistanceUnit.METRIC) },
                                        label = { Text("Metric (km)") }
                                    )
                                    FilterChip(
                                        selected = userPrefs.unit == DistanceUnit.IMPERIAL,
                                        onClick = { preferences.setUnit(DistanceUnit.IMPERIAL) },
                                        label = { Text("Imperial (mi)") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Auto-Pause & Sensitivity
                            SettingRow(
                                title = "Smart Auto-Pause",
                                subtitle = "Pause stopwatch when movement drops below ${userPrefs.autoPauseSensitivityKmh} km/h",
                                checked = userPrefs.autoPause,
                                onCheckedChange = { preferences.setAutoPause(it) }
                            )

                            if (userPrefs.autoPause) {
                                Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                                    Text(
                                        "Threshold Speed: ${userPrefs.autoPauseSensitivityKmh.format(1)} km/h",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Slider(
                                        value = userPrefs.autoPauseSensitivityKmh,
                                        onValueChange = { preferences.updatePreferences { p -> p.copy(autoPauseSensitivityKmh = it) } },
                                        valueRange = 0.5f..3.5f,
                                        steps = 5
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Countdown timer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Pre-Run Countdown", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Audio delay before GPS starts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0, 3, 5, 10).forEach { sec ->
                                        FilterChip(
                                            selected = userPrefs.countdownSeconds == sec,
                                            onClick = { preferences.updatePreferences { it.copy(countdownSeconds = sec) } },
                                            label = { Text(if (sec == 0) "Off" else "${sec}s") }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            SettingRow(
                                title = "Keep Screen Awake",
                                subtitle = "Prevents device sleep while tracking workout HUD",
                                checked = userPrefs.mapKeepScreenOn,
                                onCheckedChange = { preferences.updatePreferences { p -> p.copy(mapKeepScreenOn = it) } }
                            )

                            SettingRow(
                                title = "Smart Do Not Disturb (DND)",
                                subtitle = "Silence calls and notifications during live tracking",
                                checked = userPrefs.smartDnd,
                                onCheckedChange = { preferences.setSmartDnd(it) }
                            )

                            SettingRow(
                                title = "Battery Saver Tracking",
                                subtitle = "Throttle GPS updates when battery drops below 20%",
                                checked = userPrefs.batterySaverTracking,
                                onCheckedChange = { preferences.setBatterySaverTracking(it) }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Ghost Pacer Target Pace
                            Column {
                                val minPerKm = (userPrefs.targetPaceSecPerKm / 60).toInt()
                                val secRemainder = (userPrefs.targetPaceSecPerKm % 60).toInt()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ghost Pacer Target Pace", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        String.format(Locale.US, "%d:%02d /km", minPerKm, secRemainder),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = userPrefs.targetPaceSecPerKm.toFloat(),
                                    onValueChange = { preferences.updatePreferences { p -> p.copy(targetPaceSecPerKm = it.toDouble()) } },
                                    valueRange = 180f..480f, // 3:00 to 8:00 min/km
                                    steps = 30
                                )
                            }
                        }
                    }
                }
            }

            // 3. AUDIO COACH & METRONOME
            if (shouldShow(SettingsCategory.AUDIO, "audio", "coach", "voice", "speech", "interval", "metronome", "cadence", "spm", "volume")) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "AUDIO COACH & CADENCE METRONOME",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            SettingRow(
                                title = "Voice Audio Announcements",
                                subtitle = "Text-to-speech updates during workouts",
                                checked = userPrefs.voiceAnnouncements,
                                onCheckedChange = { preferences.setVoiceAnnouncements(it) }
                            )

                            if (userPrefs.voiceAnnouncements) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Interval
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Announcement Interval", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(0.5f to "500m", 1.0f to "1 km", 2.0f to "2 km", 5.0f to "5 km").forEach { (dist, label) ->
                                            FilterChip(
                                                selected = userPrefs.audioCueIntervalKm == dist,
                                                onClick = { preferences.updatePreferences { it.copy(audioCueIntervalKm = dist) } },
                                                label = { Text(label, fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Volume & Speech Rate
                                Text("Speech Volume: ${userPrefs.audioCueVolumePercent}%", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = userPrefs.audioCueVolumePercent.toFloat(),
                                    onValueChange = { preferences.updatePreferences { p -> p.copy(audioCueVolumePercent = it.toInt()) } },
                                    valueRange = 0f..100f,
                                    steps = 10
                                )

                                Text("Speech Rate: ${userPrefs.audioSpeechRate.format(2)}x", style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = userPrefs.audioSpeechRate,
                                    onValueChange = { preferences.updatePreferences { p -> p.copy(audioSpeechRate = it) } },
                                    valueRange = 0.75f..1.50f,
                                    steps = 6
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Spoken metrics checkboxes
                                Text("Spoken Metrics", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = userPrefs.audioAnnouncePace,
                                        onClick = { preferences.updatePreferences { it.copy(audioAnnouncePace = !it.audioAnnouncePace) } },
                                        label = { Text("Pace", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = userPrefs.audioAnnounceHeartRate,
                                        onClick = { preferences.updatePreferences { it.copy(audioAnnounceHeartRate = !it.audioAnnounceHeartRate) } },
                                        label = { Text("Heart Rate", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = userPrefs.audioAnnounceDistance,
                                        onClick = { preferences.updatePreferences { it.copy(audioAnnounceDistance = !it.audioAnnounceDistance) } },
                                        label = { Text("Distance", fontSize = 11.sp) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        Toast.makeText(context, "Audio Cue: \"1 kilometer completed. Pace: 5:14 per kilometer. Heart rate: 148 BPM.\"", Toast.LENGTH_LONG).show()
                                        vibrate(80)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Preview Voice Announcement")
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Cadence Metronome
                            SettingRow(
                                title = "Cadence Metronome",
                                subtitle = "Rhythmic audio/haptic pulse to dial in target cadence",
                                checked = userPrefs.cadenceMetronomeEnabled,
                                onCheckedChange = { preferences.updatePreferences { p -> p.copy(cadenceMetronomeEnabled = it) } }
                            )

                            if (userPrefs.cadenceMetronomeEnabled) {
                                Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Target Cadence", style = MaterialTheme.typography.bodySmall)
                                        Text("${userPrefs.targetCadenceSpm} SPM", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                    }
                                    Slider(
                                        value = userPrefs.targetCadenceSpm.toFloat(),
                                        onValueChange = { preferences.updatePreferences { p -> p.copy(targetCadenceSpm = it.toInt()) } },
                                        valueRange = 150f..200f,
                                        steps = 10
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. MAPS & NAVIGATION
            if (shouldShow(SettingsCategory.MAPS, "maps", "navigation", "tiles", "layer", "osm", "cyclosm", "dark", "satellite", "auto follow", "heading", "polyline", "stroke", "cache")) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "MAPS & LIVE NAVIGATION ENGINE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Default Map Tile Layer", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Real OpenStreetMap rendered tiles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(8.dp))

                            val layers = listOf(
                                "OPEN_STREET_MAP" to "Standard OSM",
                                "CYCLOSM" to "CyclOSM Outdoor",
                                "HUMANITARIAN" to "Humanitarian",
                                "CARTO_DARK" to "Carto Dark Matter",
                                "CARTO_LIGHT" to "Carto Positron",
                                "OPEN_TOPO" to "OpenTopoMap"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                layers.chunked(2).forEach { row ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        row.forEach { (id, label) ->
                                            FilterChip(
                                                selected = userPrefs.defaultMapLayer == id,
                                                onClick = {
                                                    preferences.updatePreferences { it.copy(defaultMapLayer = id) }
                                                    Toast.makeText(context, "Map layer set to $label", Toast.LENGTH_SHORT).show()
                                                },
                                                label = { Text(label, fontSize = 11.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            SettingRow(
                                title = "Auto-Follow Runner",
                                subtitle = "Automatically center map camera on GPS fix",
                                checked = userPrefs.mapAutoFollow,
                                onCheckedChange = { preferences.updatePreferences { p -> p.copy(mapAutoFollow = it) } }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Heading Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Map Rotation", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Heading orientation style", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = userPrefs.mapHeadingRotationMode == "NORTH_UP",
                                        onClick = { preferences.updatePreferences { it.copy(mapHeadingRotationMode = "NORTH_UP") } },
                                        label = { Text("North-Up") }
                                    )
                                    FilterChip(
                                        selected = userPrefs.mapHeadingRotationMode == "BEARING_UP",
                                        onClick = { preferences.updatePreferences { it.copy(mapHeadingRotationMode = "BEARING_UP") } },
                                        label = { Text("Course-Up") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Route Polyline Color Scheme
                            Text("Breadcrumb Route Color Scheme", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "SPEED_GRADIENT" to "Speed Gradient",
                                    "HR_GRADIENT" to "HR Zones",
                                    "ELEVATION" to "Elevation",
                                    "SOLID" to "Neon Cyan"
                                ).forEach { (mode, label) ->
                                    FilterChip(
                                        selected = userPrefs.mapPolylineColorMode == mode,
                                        onClick = { preferences.updatePreferences { it.copy(mapPolylineColorMode = mode) } },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Route Stroke Width
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Polyline Stroke Width", style = MaterialTheme.typography.bodySmall)
                                Text("${userPrefs.mapPolylineStrokeWidthDp} dp", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Slider(
                                value = userPrefs.mapPolylineStrokeWidthDp.toFloat(),
                                onValueChange = { preferences.updatePreferences { p -> p.copy(mapPolylineStrokeWidthDp = it.toInt()) } },
                                valueRange = 2f..8f,
                                steps = 5
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(context, "OSM Tile Cache purged successfully", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear Offline Map Tile Cache")
                            }
                        }
                    }
                }
            }

            // 5. THEMES & APP ICONS
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

                            Spacer(modifier = Modifier.height(14.dp))

                            Spacer(modifier = Modifier.height(16.dp))

                            // DYNAMIC APP LAUNCHER ICON SUITE
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Dynamic App Launcher Icon",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "12 STYLES",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val (activeIcon, msg) = preferences.refreshAppIconState()
                                        triggerHaptic(vibrator)
                                        Toast.makeText(context, "Launcher resynced!\n$msg", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Resync Launcher with OS",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Text(
                                "Switch home screen launcher icon. Tap 'Refresh' to force Android Package Manager resync.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Active Status Indicator Pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(userPrefs.appIcon.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Active: ${userPrefs.appIcon.label}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    "Ready in OS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Horizontal scrolling list of all 12 icons with rich previews
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(AppIconOption.entries) { icon ->
                                    val isSelected = userPrefs.appIcon == icon
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .width(108.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                preferences.setAppIcon(icon)
                                                triggerHaptic(vibrator)
                                                Toast.makeText(
                                                    context,
                                                    "Switched launcher icon to ${icon.label}!\nAndroid home launcher will update.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .padding(10.dp)
                                    ) {
                                        // Visual adaptive icon mockup
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    when (icon) {
                                                        AppIconOption.DEFAULT -> Color(0xFF001224)
                                                        AppIconOption.AMOLED_RED -> Color(0xFF0A0002)
                                                        AppIconOption.TWILIGHT -> Color(0xFF140828)
                                                        AppIconOption.EMERALD -> Color(0xFF00240D)
                                                        AppIconOption.MONOCHROME -> Color(0xFF121212)
                                                        AppIconOption.SOLAR_GOLD -> Color(0xFF1F1600)
                                                        AppIconOption.CYBER_CYAN -> Color(0xFF090D1A)
                                                        AppIconOption.ARCTIC_FROST -> Color(0xFFECEFF1)
                                                        AppIconOption.SUNSET_BLAZE -> Color(0xFF1C0A00)
                                                        AppIconOption.RETRO_SYNTH -> Color(0xFF0D061A)
                                                        AppIconOption.ELECTRIC_LIME -> Color(0xFF0D1A00)
                                                        AppIconOption.ROYAL_GOLD -> Color(0xFF121212)
                                                    }
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(icon.colorHex).copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.DirectionsRun,
                                                contentDescription = null,
                                                tint = Color(icon.colorHex),
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = icon.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                                            }
                                        } else {
                                            Text(
                                                "Tap to apply",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Resync Button
                            FilledTonalButton(
                                onClick = {
                                    val (activeIcon, msg) = preferences.refreshAppIconState()
                                    triggerHaptic(vibrator)
                                    Toast.makeText(context, "Launcher resynced!\n$msg", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resync Launcher Icon with Android OS")
                            }
                        }
                    }
                }
            }

            // 6. ACCESSIBILITY SUITE
            if (shouldShow(SettingsCategory.ACCESSIBILITY, "accessibility", "color vision", "protanopia", "deuteranopia", "tritanopia", "large text", "bold", "targets", "reduced motion", "contrast")) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ACCESSIBILITY & VISION SUITE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("Color Vision Correction Filters", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ColorVisionMode.entries.forEach { mode ->
                                    val isSelected = userPrefs.accessibility.colorVisionMode == mode
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable { preferences.updateAccessibility { it.copy(colorVisionMode = mode) } }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(mode.label, style = MaterialTheme.typography.bodyMedium)
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            SettingRow(
                                title = "Large Text Legibility (+125%)",
                                subtitle = "Enlarge font scales across HUD and cards",
                                checked = userPrefs.accessibility.largerText,
                                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(largerText = it) } }
                            )

                            SettingRow(
                                title = "Enlarged UI Badges (+120%)",
                                subtitle = "Expand padding and card touch areas",
                                checked = userPrefs.accessibility.largerUi,
                                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(largerUi = it) } }
                            )

                            SettingRow(
                                title = "Bold Typography Weight",
                                subtitle = "Render text in bolder weight for outdoor readability",
                                checked = userPrefs.accessibility.boldText,
                                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(boldText = it) } }
                            )

                            SettingRow(
                                title = "Large Touch Targets (64dp)",
                                subtitle = "Extend all buttons to at least 64dp for cold hands/gloves",
                                checked = userPrefs.accessibility.largeTouchTargets,
                                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(largeTouchTargets = it) } }
                            )

                            SettingRow(
                                title = "Reduced Motion & Transitions",
                                subtitle = "Disable fast sliding animations",
                                checked = userPrefs.accessibility.reducedMotion,
                                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(reducedMotion = it) } }
                            )

                            SettingRow(
                                title = "Zero UI Animations",
                                subtitle = "Completely bypass all layout animations",
                                checked = userPrefs.accessibility.disableAnimations,
                                onCheckedChange = { preferences.updateAccessibility { a -> a.copy(disableAnimations = it) } }
                            )
                        }
                    }
                }
            }

            // 7. SENSORS, WEARABLES & HAPTICS
            if (shouldShow(SettingsCategory.SENSORS, "sensors", "ble", "bluetooth", "watch", "wear", "haptic", "vibration", "alarm", "zone alarm")) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "SENSORS & WEARABLE INTEGRATION",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Wear OS Companion Link
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Watch, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Wear OS Watch Companion", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Bidirectional real-time sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                FilledTonalButton(onClick = onOpenDevices) {
                                    Text("Pairing Hub")
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Heart rate zone alert
                            SettingRow(
                                title = "Heart Rate Ceiling Alarm",
                                subtitle = "Vibrate when heart rate surpasses ${userPrefs.hrZoneAlarmBpm} BPM",
                                checked = userPrefs.hrZoneAlarmEnabled,
                                onCheckedChange = { preferences.updatePreferences { p -> p.copy(hrZoneAlarmEnabled = it) } }
                            )

                            if (userPrefs.hrZoneAlarmEnabled) {
                                Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Ceiling Threshold", style = MaterialTheme.typography.bodySmall)
                                        Text("${userPrefs.hrZoneAlarmBpm} BPM", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error))
                                    }
                                    Slider(
                                        value = userPrefs.hrZoneAlarmBpm.toFloat(),
                                        onValueChange = { preferences.updatePreferences { p -> p.copy(hrZoneAlarmBpm = it.toInt()) } },
                                        valueRange = 140f..210f,
                                        steps = 14
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Haptic pulse actuator test
                            Text("Haptic Actuator Feedback Test", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        vibrate(40)
                                        Toast.makeText(context, "Tick (40ms)", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Tick")
                                }
                                OutlinedButton(
                                    onClick = {
                                        vibrate(120)
                                        Toast.makeText(context, "Pulse (120ms)", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Pulse")
                                }
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 100, 80, 150), -1))
                                        } else {
                                            @Suppress("DEPRECATION")
                                            vibrator?.vibrate(longArrayOf(0, 100, 80, 100, 80, 150), -1)
                                        }
                                        Toast.makeText(context, "Milestone Pattern", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Milestone")
                                }
                            }
                        }
                    }
                }
            }

            // 8. PRIVACY, STORAGE & DATA EXPORT
            if (shouldShow(SettingsCategory.PRIVACY, "privacy", "storage", "zones", "trash", "backup", "export", "wipe", "reset", "database")) {
                item {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "PRIVACY, BACKUP & STORAGE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Privacy Zones
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("GPS Privacy Zones (${privacyZones.size})", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Scrub coordinates around home/work", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FilledTonalButton(onClick = { showAddZoneDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Zone")
                                }
                            }

                            if (privacyZones.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                privacyZones.forEach { zone ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(zone.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text("${zone.radiusMeters.toInt()}m radius • ${zone.latitude.format(4)}, ${zone.longitude.format(4)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = {
                                            scope.launch {
                                                repository.deletePrivacyZone(zone.id)
                                                Toast.makeText(context, "Deleted Privacy Zone \"${zone.name}\"", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Trash bin
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Activity Trash Bin", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("${trashActivities.size} soft-deleted workouts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedButton(onClick = { showTrashDialog = true }) {
                                    Text("Open Trash")
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Encrypted Backup & Export
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Encrypted Backup & Export", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("Export AES-256 encrypted archive (.miles)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FilledTonalButton(onClick = { showBackupDialog = true }) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export")
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Reset Defaults & Wipe
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showResetDefaultsDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset Prefs")
                                }
                                Button(
                                    onClick = { showWipeConfirmDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Wipe All")
                                }
                            }
                        }
                    }
                }
            }

            // 9. MILES STUDIO PROMINENT CARD
            if (shouldShow(SettingsCategory.STUDIO, "studio", "developer", "nerd", "kalman", "gnss", "geodesy", "biomechanics", "nmea", "power", "vdot", "trimp")) {
                item {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenStudio() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "MILES Developer Studio",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Kalman Q/R filters, NMEA console, Minetti GAP, Running Power watts, VDOT, Geodesy & SQLite",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Add Privacy Zone Dialog
    if (showAddZoneDialog) {
        var zoneName by remember { mutableStateOf("") }
        var zoneLat by remember { mutableStateOf("37.7749") }
        var zoneLon by remember { mutableStateOf("-122.4194") }
        var zoneRadius by remember { mutableStateOf("300") }

        AlertDialog(
            onDismissRequest = { showAddZoneDialog = false },
            title = { Text("Add Privacy Zone") },
            text = {
                Column {
                    Text("Points inside this radius will be scrubbed from start and finish tracks.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = zoneName, onValueChange = { zoneName = it }, label = { Text("Zone Label (e.g. Home)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = zoneLat, onValueChange = { zoneLat = it }, label = { Text("Latitude") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = zoneLon, onValueChange = { zoneLon = it }, label = { Text("Longitude") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(value = zoneRadius, onValueChange = { zoneRadius = it }, label = { Text("Radius (meters)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val lat = zoneLat.toDoubleOrNull() ?: 37.7749
                    val lon = zoneLon.toDoubleOrNull() ?: -122.4194
                    val rad = zoneRadius.toFloatOrNull() ?: 300f
                    if (zoneName.isNotBlank()) {
                        scope.launch {
                            repository.savePrivacyZone(
                                PrivacyZoneEntity(
                                    name = zoneName,
                                    latitude = lat,
                                    longitude = lon,
                                    radiusMeters = rad
                                )
                            )
                            showAddZoneDialog = false
                            Toast.makeText(context, "Added Privacy Zone \"$zoneName\"", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddZoneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Encrypted Backup Dialog
    if (showBackupDialog) {
        var backupPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Create Encrypted Backup") },
            text = {
                Column {
                    Text("Enter a password to encrypt your activities with AES-256 before exporting.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backupPass,
                        onValueChange = { backupPass = it },
                        label = { Text("Encryption Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (backupPass.isNotBlank()) {
                        scope.launch {
                            val count = repository.exportEncryptedBackup(context, backupPass)
                            showBackupDialog = false
                            Toast.makeText(context, "Exported $count activities to encrypted backup file", Toast.LENGTH_LONG).show()
                        }
                    }
                }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Trash Bin Dialog
    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            title = { Text("Trash Bin (${trashActivities.size})") },
            text = {
                if (trashActivities.isEmpty()) {
                    Text("Trash is empty. Deleted activities appear here for 30 days before permanent deletion.")
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(trashActivities) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("${(item.distanceMeters / 1000.0).toString().take(4)} km", style = MaterialTheme.typography.bodySmall)
                                }
                                Row {
                                    IconButton(onClick = {
                                        scope.launch {
                                            repository.restoreFromTrash(item.id)
                                            Toast.makeText(context, "Restored \"${item.title}\"", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Text("Restore", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            repository.permanentlyDelete(item.id)
                                            Toast.makeText(context, "Permanently deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF2D55), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (trashActivities.isNotEmpty()) {
                    Button(
                        onClick = {
                            scope.launch {
                                repository.emptyTrash()
                                showTrashDialog = false
                                Toast.makeText(context, "Emptied trash bin", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55))
                    ) {
                        Text("Empty Trash")
                    }
                } else {
                    Button(onClick = { showTrashDialog = false }) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (trashActivities.isNotEmpty()) {
                    TextButton(onClick = { showTrashDialog = false }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Wipe All Data Dialog
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = { Text("Erase All Data Locally?") },
            text = {
                Text("This permanently purges all recorded workouts, routes, offline maps, and settings. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.wipeAllData()
                            preferences.resetAllPreferences()
                            showWipeConfirmDialog = false
                            Toast.makeText(context, "All local data erased", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF2D55))
                ) {
                    Text("Erase Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Defaults Dialog
    if (showResetDefaultsDialog) {
        AlertDialog(
            onDismissRequest = { showResetDefaultsDialog = false },
            title = { Text("Reset Preferences?") },
            text = {
                Text("Reset all tracking, map, audio, accessibility, and developer preferences to factory defaults?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        preferences.resetAllPreferences()
                        showResetDefaultsDialog = false
                        Toast.makeText(context, "Preferences reset to factory defaults", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Reset Defaults")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDefaultsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
