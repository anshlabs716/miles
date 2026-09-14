package com.example.miles.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.AppIconOption
import com.example.miles.data.local.MilesPreferences
import com.example.miles.ui.theme.LiquidGlassCard

data class AppIconItem(
    val id: String,
    val alias: String,
    val label: String,
    val description: String,
    val colorPreview: Color
)

@Composable
fun AppIconChangerSection(
    preferences: MilesPreferences,
    selectedIcon: AppIconOption,
    onIconChanged: (AppIconOption) -> Unit
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    val iconOptions = listOf(
        AppIconItem(
            id = "DEFAULT",
            alias = "com.example.MainActivityDefault",
            label = "Default Navy",
            description = "Twilight cyan runner",
            colorPreview = Color(0xFF1F5FFF)
        ),
        AppIconItem(
            id = "AMOLED_RED",
            alias = "com.example.MainActivityAmoledRed",
            label = "AMOLED Red",
            description = "Crimson on black",
            colorPreview = Color(0xFFFF2D55)
        ),
        AppIconItem(
            id = "TWILIGHT",
            alias = "com.example.MainActivityTwilight",
            label = "Twilight Violet",
            description = "Purple nebula",
            colorPreview = Color(0xFFB388FF)
        ),
        AppIconItem(
            id = "EMERALD",
            alias = "com.example.MainActivityEmerald",
            label = "Emerald Sprint",
            description = "Vibrant green",
            colorPreview = Color(0xFF00E676)
        ),
        AppIconItem(
            id = "MONOCHROME",
            alias = "com.example.MainActivityMonochrome",
            label = "Monochrome Pitch",
            description = "White on black",
            colorPreview = Color(0xFFEEEEEE)
        ),
        AppIconItem(
            id = "SOLAR_GOLD",
            alias = "com.example.MainActivitySolar",
            label = "Solar Gold",
            description = "Radiant amber",
            colorPreview = Color(0xFFFFD600)
        ),
        AppIconItem(
            id = "CYBER_CYAN",
            alias = "com.example.MainActivityCyber",
            label = "Cyberpunk Neon",
            description = "Pink & cyan",
            colorPreview = Color(0xFF00F0FF)
        ),
        AppIconItem(
            id = "ARCTIC_FROST",
            alias = "com.example.MainActivityArctic",
            label = "Arctic Frost",
            description = "Ice cyan",
            colorPreview = Color(0xFF00B0FF)
        ),
        AppIconItem(
            id = "SUNSET_BLAZE",
            alias = "com.example.MainActivitySunset",
            label = "Sunset Blaze",
            description = "Coral flame",
            colorPreview = Color(0xFFFF6D00)
        ),
        AppIconItem(
            id = "RETRO_SYNTH",
            alias = "com.example.MainActivityRetro",
            label = "Retro 80s Synth",
            description = "Magenta synthwave",
            colorPreview = Color(0xFFFF007F)
        ),
        AppIconItem(
            id = "ELECTRIC_LIME",
            alias = "com.example.MainActivityLime",
            label = "Electric Lime",
            description = "Acid lime",
            colorPreview = Color(0xFFAEEA00)
        ),
        AppIconItem(
            id = "ROYAL_GOLD",
            alias = "com.example.MainActivityRoyal",
            label = "Royal Obsidian",
            description = "Gold luxury",
            colorPreview = Color(0xFFFFC107)
        )
    )

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dynamic App Launcher Icon",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose from 12 styles. Tap to apply instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        isRefreshing = true
                        refreshAppIconState(context, preferences, selectedIcon) {
                            isRefreshing = false
                            Toast.makeText(context, "Icon state refreshed!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isRefreshing
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh icon state",
                        tint = if (isRefreshing) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Icon grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(iconOptions) { icon ->
                    IconOptionCard(
                        item = icon,
                        isSelected = selectedIcon.label == icon.label,
                        onClick = {
                            applyAppIcon(context, icon.alias, selectedIcon)
                            onIconChanged(getIconOptionFromAlias(icon.alias))
                            Toast.makeText(
                                context,
                                "Icon changed to ${icon.label}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "💡 Icon changes apply instantly. The app will refresh to display your new icon on the home screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IconOptionCard(
    item: AppIconItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isSelected) { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Color preview circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(item.colorPreview)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Label
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            fontSize = 10.sp,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun applyAppIcon(context: Context, targetAlias: String, currentIcon: AppIconOption) {
    try {
        val pm = context.packageManager
        val packageName = context.packageName

        // All available aliases
        val allAliases = listOf(
            "com.example.MainActivityDefault",
            "com.example.MainActivityAmoledRed",
            "com.example.MainActivityTwilight",
            "com.example.MainActivityEmerald",
            "com.example.MainActivityMonochrome",
            "com.example.MainActivitySolar",
            "com.example.MainActivityCyber",
            "com.example.MainActivityArctic",
            "com.example.MainActivitySunset",
            "com.example.MainActivityRetro",
            "com.example.MainActivityLime",
            "com.example.MainActivityRoyal"
        )

        // Disable all except the selected one
        allAliases.forEach { alias ->
            val component = ComponentName(packageName, alias)
            val state = if (alias == targetAlias) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to change icon: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun refreshAppIconState(
    context: Context,
    preferences: MilesPreferences,
    currentIcon: AppIconOption,
    onComplete: () -> Unit
) {
    try {
        val aliasMap = mapOf(
            AppIconOption.DEFAULT to "com.example.MainActivityDefault",
            AppIconOption.AMOLED_RED to "com.example.MainActivityAmoledRed",
            AppIconOption.TWILIGHT to "com.example.MainActivityTwilight",
            AppIconOption.EMERALD to "com.example.MainActivityEmerald",
            AppIconOption.MONOCHROME to "com.example.MainActivityMonochrome",
            AppIconOption.SOLAR_GOLD to "com.example.MainActivitySolar",
            AppIconOption.CYBER_CYAN to "com.example.MainActivityCyber",
            AppIconOption.ARCTIC_FROST to "com.example.MainActivityArctic",
            AppIconOption.SUNSET_BLAZE to "com.example.MainActivitySunset",
            AppIconOption.RETRO_SYNTH to "com.example.MainActivityRetro",
            AppIconOption.ELECTRIC_LIME to "com.example.MainActivityLime",
            AppIconOption.ROYAL_GOLD to "com.example.MainActivityRoyal"
        )

        val targetAlias = aliasMap[currentIcon] ?: "com.example.MainActivityDefault"
        applyAppIcon(context, targetAlias, currentIcon)
        onComplete()
    } catch (e: Exception) {
        Toast.makeText(context, "Refresh failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun getIconOptionFromAlias(alias: String): AppIconOption {
    return when (alias) {
        "com.example.MainActivityDefault" -> AppIconOption.DEFAULT
        "com.example.MainActivityAmoledRed" -> AppIconOption.AMOLED_RED
        "com.example.MainActivityTwilight" -> AppIconOption.TWILIGHT
        "com.example.MainActivityEmerald" -> AppIconOption.EMERALD
        "com.example.MainActivityMonochrome" -> AppIconOption.MONOCHROME
        "com.example.MainActivitySolar" -> AppIconOption.SOLAR_GOLD
        "com.example.MainActivityCyber" -> AppIconOption.CYBER_CYAN
        "com.example.MainActivityArctic" -> AppIconOption.ARCTIC_FROST
        "com.example.MainActivitySunset" -> AppIconOption.SUNSET_BLAZE
        "com.example.MainActivityRetro" -> AppIconOption.RETRO_SYNTH
        "com.example.MainActivityLime" -> AppIconOption.ELECTRIC_LIME
        "com.example.MainActivityRoyal" -> AppIconOption.ROYAL_GOLD
        else -> AppIconOption.DEFAULT
    }
}
