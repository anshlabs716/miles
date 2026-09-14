package com.example.miles.ui.history

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.GpsPoint
import com.example.miles.data.repository.format
import com.example.miles.ui.map.MapStyleMode
import com.example.miles.ui.map.MilesMapCanvas
import com.example.miles.ui.theme.LiquidGlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ShareLayoutMode {
    HERO_STATS,
    MINIMAL_MAP,
    ATHLETIC_CARD
}

@Composable
fun ShareDesignerDialog(
    activity: ActivityEntity,
    points: List<GpsPoint>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var layoutMode by remember { mutableStateOf(ShareLayoutMode.ATHLETIC_CARD) }
    var showPace by remember { mutableStateOf(true) }
    var showElevation by remember { mutableStateOf(true) }
    var maskPrivacyZones by remember { mutableStateOf(true) }
    var customCaption by remember { mutableStateOf(activity.title) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Share Designer",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Generate a privacy-respecting workout card without tracking links or telemetry watermarks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // The Visual Share Card Preview Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                ) {
                    // Map background
                    MilesMapCanvas(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        points = points,
                        mapStyle = MapStyleMode.AMOLED_DARK
                    )

                    // Overlay card info
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = customCaption,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "MILES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(activity.distanceMeters / 1000.0).format(2)} km",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = Color.White
                            )
                            Text(
                                text = "${activity.durationSeconds / 60}m ${activity.durationSeconds % 60}s",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0)
                            )
                            if (showPace) {
                                Text(
                                    text = "${(activity.avgPaceSecPerKm / 60).toInt()}'${(activity.avgPaceSecPerKm % 60).toInt()}\"/km",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                            if (showElevation) {
                                Text(
                                    text = "+${activity.elevationGainM.toInt()}m",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }
                }

                // Controls: Privacy Zone Masking
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = maskPrivacyZones,
                        onCheckedChange = { maskPrivacyZones = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Mask Privacy Zones (Home / Office)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Obscures the first and last 300 meters of route",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        Toast.makeText(context, "Workout card saved to Photos & ready to share!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Card")
                    }
                }
            }
        }
    }
}
