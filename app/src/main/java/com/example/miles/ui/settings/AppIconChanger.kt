package com.example.miles.ui.settings

import android.content.Context
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
import com.example.miles.engine.AppIconManager
import com.example.miles.ui.theme.LiquidGlassCard

data class AppIconItem(
    val option: AppIconOption,
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

    val iconOptions = AppIconOption.entries.map { option ->
        AppIconItem(option, Color(option.colorHex))
    }

    fun refresh() {
        isRefreshing = true
        runCatching {
            val manager = AppIconManager(context)
            val active = manager.refreshLauncherStatus()
            preferences.setAppIcon(active)
            onIconChanged(active)
            Toast.makeText(context, "Launcher icon state refreshed: ${active.label}", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Icon refresh failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
        isRefreshing = false
    }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        text = "Choose from every launcher icon declared by MILES. Refresh verifies the real PackageManager state.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = ::refresh, enabled = !isRefreshing) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh launcher icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(iconOptions, key = { it.option.name }) { item ->
                    IconOptionCard(
                        item = item,
                        isSelected = selectedIcon == item.option,
                        onClick = {
                            runCatching {
                                val manager = AppIconManager(context)
                                check(manager.setAppIcon(item.option)) { "Launcher alias is not installed" }
                                preferences.setAppIcon(item.option)
                                onIconChanged(item.option)
                                Toast.makeText(context, "Icon changed to ${item.option.label}", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, "Failed to change icon: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Changes use Android launcher aliases. Some launchers cache icons; use Refresh after returning to the home screen if the launcher preview has not updated yet.",
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
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isSelected, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(item.colorPreview)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.option.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            fontSize = 10.sp,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = item.option.subtitle,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AnimatedVisibility(visible = isSelected, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
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
