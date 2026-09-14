package com.example.miles.ui.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NordicWalking
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.BaseThemeOption
import com.example.miles.data.local.DistanceUnit
import com.example.miles.data.local.MilesPreferences
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.ui.theme.LiquidGlassPanel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingSetupScreen(
    preferences: MilesPreferences,
    onSetupComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) } // 0: Welcome, 1: Profile & Sport, 2: Goals, 3: Style & Units, 4: Ready

    // Setup state initialized from current preferences
    val currentPrefs = preferences.userPreferences.value
    var userName by remember { mutableStateOf(currentPrefs.userName) }
    var selectedSport by remember { mutableStateOf(currentPrefs.primarySport) }
    var selectedStepGoal by remember { mutableIntStateOf(currentPrefs.dailyStepGoal) }
    var selectedActiveMinGoal by remember { mutableIntStateOf(currentPrefs.dailyActiveMinutesGoal) }
    var selectedWeeklyDistKm by remember { mutableFloatStateOf(currentPrefs.weeklyDistanceGoalKm) }
    var selectedUnit by remember { mutableStateOf(currentPrefs.unit) }
    var selectedTheme by remember { mutableStateOf(currentPrefs.theme) }
    var liquidGlassEnabled by remember { mutableStateOf(currentPrefs.liquidGlassEnabled) }

    val totalSteps = 5

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top Bar with progress indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    IconButton(onClick = { step-- }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Step progress dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until totalSteps) {
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (i == step) 24.dp else 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i == step) MaterialTheme.colorScheme.primary
                                    else if (i < step) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                )
                        )
                    }
                }

                if (step in 1..3) {
                    TextButton(onClick = { step = 4 }) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Animated step pages
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).with(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).with(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "SetupSteps"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> WelcomeStep(onStart = { step = 1 })
                        1 -> ProfileStep(
                            userName = userName,
                            onNameChange = { userName = it },
                            selectedSport = selectedSport,
                            onSportSelect = { selectedSport = it },
                            onNext = { step = 2 }
                        )
                        2 -> GoalsStep(
                            selectedStepGoal = selectedStepGoal,
                            onStepGoalSelect = { selectedStepGoal = it },
                            selectedActiveMinGoal = selectedActiveMinGoal,
                            onActiveMinSelect = { selectedActiveMinGoal = it },
                            unit = selectedUnit,
                            weeklyDistKm = selectedWeeklyDistKm,
                            onWeeklyDistChange = { selectedWeeklyDistKm = it },
                            onNext = { step = 3 }
                        )
                        3 -> StyleStep(
                            selectedUnit = selectedUnit,
                            onUnitSelect = {
                                selectedUnit = it
                                preferences.setUnit(it)
                            },
                            selectedTheme = selectedTheme,
                            onThemeSelect = {
                                selectedTheme = it
                                preferences.setTheme(it)
                            },
                            liquidGlassEnabled = liquidGlassEnabled,
                            onLiquidGlassToggle = {
                                liquidGlassEnabled = it
                                preferences.setLiquidGlass(it)
                            },
                            onNext = { step = 4 }
                        )
                        4 -> CompletionStep(
                            userName = userName,
                            primarySport = selectedSport,
                            stepGoal = selectedStepGoal,
                            unit = selectedUnit,
                            onFinish = {
                                preferences.completeSetup(
                                    userName = userName.trim(),
                                    primarySport = selectedSport,
                                    dailyStepGoal = selectedStepGoal,
                                    dailyActiveMinutesGoal = selectedActiveMinGoal,
                                    weeklyDistanceGoalKm = selectedWeeklyDistKm,
                                    unit = selectedUnit,
                                    theme = selectedTheme,
                                    liquidGlassEnabled = liquidGlassEnabled
                                )
                                onSetupComplete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App emblem
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "MILES",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Fluid, Private Fitness Tracking",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Key Value Props
        FeatureHighlight(
            icon = Icons.Default.Lock,
            title = "Zero-Cloud Privacy",
            description = "100% of your workouts, routes, and health stats stay on your device. No accounts, no servers."
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureHighlight(
            icon = Icons.Default.Speed,
            title = "Precision Telemetry",
            description = "Smart GPS smoothing, auto-pause detection, split paces, and live elevation profiles."
        )

        Spacer(modifier = Modifier.height(14.dp))

        FeatureHighlight(
            icon = Icons.Default.Palette,
            title = "Fluid & Tactile Interface",
            description = "Google Fit inspired activity rings, clean typography, pure AMOLED black, and liquid glass."
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun ProfileStep(
    userName: String,
    onNameChange: (String) -> Unit,
    selectedSport: String,
    onSportSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "About You",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Personalize your activity HUD and daily goals.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = userName,
            onValueChange = onNameChange,
            placeholder = { Text("e.g. Alex (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Primary Activity Focus",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "We will place your favorite activity at the center of your quick start bar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        val sports = listOf(
            Triple("RUNNING", "Running", Icons.AutoMirrored.Filled.DirectionsRun),
            Triple("WALKING", "Walking", Icons.AutoMirrored.Filled.DirectionsWalk),
            Triple("CYCLING", "Cycling", Icons.Default.DirectionsBike),
            Triple("HIKING", "Hiking", Icons.Default.NordicWalking)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sports.forEach { (typeKey, title, icon) ->
                val isSelected = selectedSport == typeKey
                SportOptionCard(
                    title = title,
                    icon = icon,
                    isSelected = isSelected,
                    onClick = { onSportSelect(typeKey) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Next: Set Goals", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun GoalsStep(
    selectedStepGoal: Int,
    onStepGoalSelect: (Int) -> Unit,
    selectedActiveMinGoal: Int,
    onActiveMinSelect: (Int) -> Unit,
    unit: DistanceUnit,
    weeklyDistKm: Float,
    onWeeklyDistChange: (Float) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Your Daily Targets",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Google Fit style concentric activity rings will calibrate to these goals.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Steps Goal
        Text(
            text = "Daily Step Target",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        val stepOptions = listOf(5000, 8000, 10000, 12000)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stepOptions.forEach { steps ->
                GoalPill(
                    label = "${steps / 1000}k",
                    sublabel = "steps",
                    isSelected = selectedStepGoal == steps,
                    onClick = { onStepGoalSelect(steps) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Daily Active Move Minutes
        Text(
            text = "Daily Active Move Minutes",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        val minOptions = listOf(30, 45, 60, 90)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            minOptions.forEach { mins ->
                GoalPill(
                    label = "$mins",
                    sublabel = "mins",
                    isSelected = selectedActiveMinGoal == mins,
                    onClick = { onActiveMinSelect(mins) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Weekly Distance
        Text(
            text = "Weekly Distance Focus",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        val distOptions = listOf(15f, 25f, 40f, 60f)
        val distUnitLabel = if (unit == DistanceUnit.METRIC) "km" else "mi"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            distOptions.forEach { dist ->
                val displayDist = if (unit == DistanceUnit.METRIC) dist.toInt() else (dist * 0.621371f).toInt()
                GoalPill(
                    label = "$displayDist",
                    sublabel = distUnitLabel,
                    isSelected = weeklyDistKm == dist,
                    onClick = { onWeeklyDistChange(dist) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Next: Style & Units", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun StyleStep(
    selectedUnit: DistanceUnit,
    onUnitSelect: (DistanceUnit) -> Unit,
    selectedTheme: BaseThemeOption,
    onThemeSelect: (BaseThemeOption) -> Unit,
    liquidGlassEnabled: Boolean,
    onLiquidGlassToggle: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Appearance & Units",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Choose how metrics and visual themes are displayed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Distance Units
        Text(
            text = "Measurement System",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onUnitSelect(DistanceUnit.METRIC) }
                    .border(
                        width = if (selectedUnit == DistanceUnit.METRIC) 2.dp else 1.dp,
                        color = if (selectedUnit == DistanceUnit.METRIC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedUnit == DistanceUnit.METRIC) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Metric", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("km, m, km/h", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onUnitSelect(DistanceUnit.IMPERIAL) }
                    .border(
                        width = if (selectedUnit == DistanceUnit.IMPERIAL) 2.dp else 1.dp,
                        color = if (selectedUnit == DistanceUnit.IMPERIAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedUnit == DistanceUnit.IMPERIAL) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Imperial", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("miles, ft, mph", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Themes
        Text(
            text = "Visual Aesthetic",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        val themeOptions = listOf(
            Triple(BaseThemeOption.TWILIGHT, "Cosmic Twilight", "Deep navy and glowing violet"),
            Triple(BaseThemeOption.AMOLED, "Pure AMOLED", "Infinite pitch black (#000000)"),
            Triple(BaseThemeOption.AMOLED_RED, "AMOLED Crimson", "Sport carbon and track red"),
            Triple(BaseThemeOption.LIGHT, "Clean Light", "Bright, high-contrast daytime palette"),
            Triple(BaseThemeOption.STOCK, "Material You", "Dynamic Android system theme")
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            themeOptions.forEach { (opt, title, desc) ->
                val isSelected = selectedTheme == opt
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeSelect(opt) }
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Liquid Glass Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Liquid Glass Overlay", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Specular gradient outlines and frosted card surfaces", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = liquidGlassEnabled,
                    onCheckedChange = onLiquidGlassToggle
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Review & Finish", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun CompletionStep(
    userName: String,
    primarySport: String,
    stepGoal: Int,
    unit: DistanceUnit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E676).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (userName.isNotBlank()) "You're all set, $userName!" else "You're all set!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your personal fitness tracker is initialized and ready. Your workout database is 100% clean and private.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryRow("Primary Sport", primarySport.lowercase().replaceFirstChar { it.uppercase() })
                SummaryRow("Daily Step Target", "$stepGoal steps")
                SummaryRow("Measurement System", if (unit == DistanceUnit.METRIC) "Metric (km, km/h)" else "Imperial (miles, mph)")
                SummaryRow("Privacy Storage", "100% Local Room Database")
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Enter MILES",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FeatureHighlight(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SportOptionCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalPill(
    label: String,
    sublabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
