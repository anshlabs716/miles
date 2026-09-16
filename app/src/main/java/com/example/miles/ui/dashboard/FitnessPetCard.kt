package com.example.miles.ui.dashboard

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.local.PetType
import com.example.miles.data.local.UserPreferences
import com.example.miles.ui.theme.LiquidGlassCard
import kotlinx.coroutines.delay

@Composable
fun FitnessPetCard(
    preferences: MilesPreferences,
    userPreferences: UserPreferences,
    todaySteps: Int,
    stepGoal: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPetDialog by remember { mutableStateOf(false) }
    var petReactionText by remember { mutableStateOf<String?>(null) }
    var showHeartAnimation by remember { mutableStateOf(false) }

    val petType = userPreferences.petType
    val isLazyDay = userPreferences.isTodayLazyDay
    val isFed = isLazyDay || (todaySteps >= stepGoal)
    val feedingProgress = if (isLazyDay) 1f else (todaySteps.toFloat() / stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "petAnimation")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Clear reaction bubble after a short time
    LaunchedEffect(petReactionText) {
        if (petReactionText != null) {
            showHeartAnimation = true
            delay(2800L)
            petReactionText = null
            showHeartAnimation = false
        }
    }

    if (petType == PetType.OFF) {
        // Minimal collapsed card allowing user to enable a pet if desired
        LiquidGlassCard(
            modifier = modifier
                .fillMaxWidth()
                .clickable { showPetDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Pets,
                            contentDescription = "Fitness Pet",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Virtual Step Companion",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adopt a dog, cat, parrot, or bunny to feed with your steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = { showPetDialog = true }) {
                    Text("Choose Pet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        // Active Pet Card
        LiquidGlassCard(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .animateContentSize()
            ) {
                // Header: Pet Name & Edit Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${petType.emoji} ${userPreferences.petName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isFed) Color(0xFF00E676).copy(alpha = 0.15f)
                                    else Color(0xFFFF9100).copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isLazyDay) "RESTING 😴" else if (isFed) "FED & HAPPY 🍖" else "HUNGRY 🥣",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isFed) Color(0xFF00E676) else Color(0xFFFF9100),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showPetDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Change Pet",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Pet Avatar + Speech Bubble
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tap-able Pet Avatar with gentle breathing/bounce animation
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .offset(y = bounceOffset.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                            .clickable {
                                val reaction = when (petType) {
                                    PetType.DOG -> "Woof! Let's go for a walk! 🐕🐾"
                                    PetType.CAT -> "Purr... Pet me and get those steps! 🐱✨"
                                    PetType.PARROT -> "Squawk! Step up! 250 steps! 🦜🪶"
                                    PetType.BUNNY -> "Hop hop! Jump to the step goal! 🐰🥕"
                                    PetType.OFF -> ""
                                }
                                petReactionText = reaction
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = petType.emoji,
                            fontSize = 32.sp
                        )
                        if (showHeartAnimation) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF2D55),
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.TopEnd)
                                    .scale(1.2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Speech bubble / status text
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = petReactionText ?: if (isLazyDay) {
                                "Zzz... Snoozing on a Lazy Day! 😴 Cozy & well fed."
                            } else if (isFed) {
                                "Yum! Daily goal reached! ${userPreferences.petName} is full & energized! ✨"
                            } else {
                                "Take ${(stepGoal - todaySteps).coerceAtLeast(0)} more steps to feed me ${petType.favoriteTreat}!"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isLazyDay) "Rest days protect your pet feeding streak." else "Tap ${userPreferences.petName} to play!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Feeding Progress bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Feeding Progress",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (isLazyDay) "100% (Lazy Day)" else "$todaySteps / $stepGoal steps",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isFed) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { feedingProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isFed) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lazy Day / Rest Day Quick Action Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isLazyDay) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isLazyDay) Icons.Default.Bedtime else Icons.Default.Spa,
                                contentDescription = null,
                                tint = if (isLazyDay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isLazyDay) "Today is a Lazy Day 🛋️" else "Take a Lazy Day Today?",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${userPreferences.lazyDaysThisWeek}/${userPreferences.maxLazyDaysPerWeek} rest days used this week",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val success = preferences.toggleLazyDay()
                            if (!success) {
                                Toast.makeText(
                                    context,
                                    "Weekly limit reached (${userPreferences.maxLazyDaysPerWeek} lazy days max per week).",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                val msg = if (!isLazyDay) "Lazy day activated! Enjoy your rest day 🛋️" else "Back to active mode! 💪"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text(
                            text = if (isLazyDay) "Resume Active" else "Declare Lazy Day",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    // Dialog for Choosing Pet or Turning Off
    if (showPetDialog) {
        PetSelectionDialog(
            currentType = petType,
            currentName = userPreferences.petName,
            onDismiss = { showPetDialog = false },
            onSave = { selectedType, newName ->
                preferences.setPet(selectedType, newName)
                showPetDialog = false
                Toast.makeText(context, "Pet settings updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun PetSelectionDialog(
    currentType: PetType,
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (PetType, String) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var petName by remember { mutableStateOf(if (currentName.isBlank() && currentType != PetType.OFF) currentType.defaultName else currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Fitness Companion",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pick your pet companion. Hit your step goal each day to feed it, or turn off for a minimal dashboard.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PetType.entries.forEach { option ->
                    val isSelected = selectedType == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedType = option
                                if (option != PetType.OFF && (petName.isBlank() || petName == currentType.defaultName)) {
                                    petName = option.defaultName
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = option.emoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = option.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (option != PetType.OFF) {
                                    Text(
                                        text = "Treat: ${option.favoriteTreat}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (selectedType != PetType.OFF) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = petName,
                        onValueChange = { petName = it },
                        label = { Text("Pet Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedType, petName) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
