package com.example.miles.ui.studio

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.miles.data.repository.MilesRepository
import com.example.miles.data.repository.format
import com.example.miles.ui.theme.LiquidGlassCard
import kotlin.math.sqrt

@Composable
fun DistanceCalculatorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Point A
    var latA by remember { mutableStateOf("37.7749") }
    var lngA by remember { mutableStateOf("-122.4194") }
    var altA by remember { mutableStateOf("15.0") }

    // Point B
    var latB by remember { mutableStateOf("37.8080") }
    var lngB by remember { mutableStateOf("-122.4177") }
    var altB by remember { mutableStateOf("85.0") }

    // Calculation results
    var dist2dMeters by remember { mutableStateOf(0.0) }
    var dist3dMeters by remember { mutableStateOf(0.0) }
    var hasCalculated by remember { mutableStateOf(false) }

    fun calculate() {
        val lA = latA.toDoubleOrNull() ?: 0.0
        val lnA = lngA.toDoubleOrNull() ?: 0.0
        val alA = altA.toDoubleOrNull() ?: 0.0

        val lB = latB.toDoubleOrNull() ?: 0.0
        val lnB = lngB.toDoubleOrNull() ?: 0.0
        val alB = altB.toDoubleOrNull() ?: 0.0

        val d2d = MilesRepository.calculateDistanceMeters(lA, lnA, lB, lnB)
        val dAlt = alB - alA
        val d3d = sqrt(d2d * d2d + dAlt * dAlt)

        dist2dMeters = d2d
        dist3dMeters = d3d
        hasCalculated = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Distance Calculator",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Technical 2D/3D Great-Circle Geodesic Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Point A Inputs
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "POINT A (ORIGIN)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latA,
                            onValueChange = { latA = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = lngA,
                            onValueChange = { lngA = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = altA,
                        onValueChange = { altA = it },
                        label = { Text("Altitude (meters)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        }

        // Point B Inputs
        item {
            LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "POINT B (DESTINATION)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latB,
                            onValueChange = { latB = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = lngB,
                            onValueChange = { lngB = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = altB,
                        onValueChange = { altB = it },
                        label = { Text("Altitude (meters)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        }

        // Calculate Button
        item {
            Button(
                onClick = { calculate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compute Haversine Geodesic Distance", fontWeight = FontWeight.Bold)
            }
        }

        // Results Display
        if (hasCalculated) {
            item {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CALCULATION OUTPUT",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = {
                                val text = "2D: ${(dist2dMeters / 1000.0).format(3)} km (${(dist2dMeters * 0.000621371).format(3)} mi) | 3D: ${(dist3dMeters / 1000.0).format(3)} km"
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Copied results to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "2D Horizontal Distance:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(dist2dMeters / 1000.0).format(3)} km (${dist2dMeters.format(1)} m)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(dist2dMeters * 0.000621371).format(3)} miles • ${(dist2dMeters * 3.28084).format(1)} feet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "3D Terrain Distance (with elevation delta):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(dist3dMeters / 1000.0).format(3)} km (${dist3dMeters.format(1)} m)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = Color(0xFF00E676)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
