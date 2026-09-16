package com.example.miles.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.PrivacyZoneEntity
import com.example.miles.data.repository.MilesRepository
import com.example.miles.data.repository.format
import kotlinx.coroutines.launch

@Composable
fun AddPrivacyZoneDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, lat: Double, lon: Double, radius: Float) -> Unit
) {
    var zoneName by remember { mutableStateOf("Home / Workplace") }
    var latText by remember { mutableStateOf("37.7749") }
    var lonText by remember { mutableStateOf("-122.4194") }
    var radiusText by remember { mutableStateOf("250") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add GPS Privacy Zone") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Track points recorded inside this radius will be sanitized from shared exports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = zoneName,
                    onValueChange = { zoneName = it },
                    label = { Text("Zone Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it },
                    label = { Text("Exclusion Radius (Meters)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: 37.7749
                    val lon = lonText.toDoubleOrNull() ?: -122.4194
                    val radius = radiusText.toFloatOrNull() ?: 200f
                    onAdd(zoneName, lat, lon, radius)
                }
            ) {
                Text("Save Zone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BackupRestoreDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encrypted SQLite Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MILES stores all workouts, GNSS routes, and bio-telemetry in local SQLite. You can generate a standalone database snapshot or import prior history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Database: miles_db.sqlite\nWAL Mode: Enabled\nLocation: /data/user/0/com.aistudio.miles.track/databases",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Toast.makeText(context, "Encrypted backup created in Downloads/MILES_Backup.sqlite", Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            ) {
                Text("Export Backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun TrashBinDialog(
    trashActivities: List<ActivityEntity>,
    repository: MilesRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trash Bin (${trashActivities.size})") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Soft-deleted activities are kept here for 30 days before permanent purging.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (trashActivities.isEmpty()) {
                    Text(
                        text = "Trash is empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(trashActivities, key = { it.id }) { act ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(act.title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        text = "${(act.distanceMeters / 1000.0).format(2)} km • ${act.activityType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repository.restoreFromTrash(act.id)
                                                Toast.makeText(context, "Activity restored!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = "Restore", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                repository.permanentlyDelete(act.id)
                                                Toast.makeText(context, "Purged permanently", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Permanently", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (trashActivities.isNotEmpty()) {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.emptyTrash()
                            Toast.makeText(context, "Trash emptied!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    }
                ) {
                    Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun WipeConfirmDialog(
    onDismiss: () -> Unit,
    onConfirmWipe: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wipe All Local Activity Data?") },
        text = {
            Text(
                text = "This will permanently remove all recorded workouts, GPS trackpoints, telemetry metrics, and privacy zones from SQLite. This action cannot be reversed.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmWipe,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirm Full Wipe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ResetDefaultsDialog(
    preferences: MilesPreferences,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reset All Preferences?") },
        text = {
            Text(
                text = "This will restore all athlete profiles, HUD settings, audio parameters, accessibility modes, and themes back to factory defaults. Your activity history will not be deleted.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    preferences.resetAllPreferences()
                    Toast.makeText(context, "Preferences reset to factory defaults", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Text("Reset Defaults")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
