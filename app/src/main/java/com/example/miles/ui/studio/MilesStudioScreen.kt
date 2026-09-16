package com.example.miles.ui.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.repository.MilesRepository
import com.example.miles.data.repository.format
import com.example.miles.engine.SmartTrackingEngine
import com.example.miles.ui.theme.LiquidGlassCard
import com.example.miles.wear.WearCompanionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MilesStudioScreen(
    repository: MilesRepository,
    smartEngine: SmartTrackingEngine,
    wearCompanion: WearCompanionManager,
    preferences: MilesPreferences,
    onBack: () -> Unit,
    onOpenDistanceCalculator: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liveStats by smartEngine.liveStats.collectAsState()
    val wearLogs by wearCompanion.communicationLogs.collectAsState()
    val userPrefs by preferences.userPreferences.collectAsState()
    val activities by repository.activities.collectAsState(initial = emptyList())
    val routes by repository.savedRoutes.collectAsState(initial = emptyList())
    val privacyZones by repository.privacyZones.collectAsState(initial = emptyList())

    var selectedStudioTab by remember { mutableIntStateOf(0) }
    val studioTabs = listOf(
        "🛰️ GNSS & Kalman",
        "⚡ Power & Physics",
        "🧪 Mock & Sensors",
        "⏱️ Clock & Haptics",
        "📐 Geodesy Math",
        "💾 SQLite & Telemetry"
    )

    // Clock Interval
    var customCounterMsText by remember { mutableStateOf(userPrefs.counterIntervalMs.toString()) }

    // Geodesy Quick Calculator State
    var lat1Text by remember { mutableStateOf("37.7749") }
    var lon1Text by remember { mutableStateOf("-122.4194") }
    var lat2Text by remember { mutableStateOf("37.8024") }
    var lon2Text by remember { mutableStateOf("-122.4058") }
    var calcVincentyMeters by remember { mutableStateOf<Double?>(null) }
    var calcHaversineMeters by remember { mutableStateOf<Double?>(null) }
    var calcInitialBearingDeg by remember { mutableStateOf<Double?>(null) }

    // Mock injection states
    var mockLatText by remember { mutableStateOf("37.7749") }
    var mockLonText by remember { mutableStateOf("-122.4194") }
    var mockAltText by remember { mutableStateOf("15.0") }
    var mockSpeedText by remember { mutableStateOf("12.0") }

    // VDOT Calculator States
    var vdotDistanceMeters by remember { mutableStateOf(5000.0) }
    var vdotTimeMinutesText by remember { mutableStateOf("22.5") } // 22:30 5k

    // Barometer Hypsometric State
    var baroStationPressureHpa by remember { mutableStateOf("1010.5") }

    // NMEA 0183 Serial Terminal Stream
    val nmeaStream = remember { mutableStateListOf<String>() }
    var isNmeaStreaming by remember { mutableStateOf(true) }

    LaunchedEffect(isNmeaStreaming) {
        if (!isNmeaStreaming) return@LaunchedEffect
        var counter = 0
        while (isActive && isNmeaStreaming) {
            delay(1200)
            val lat = 37.7749 + (counter * 0.00012)
            val lon = -122.4194 + (counter * 0.00015)
            val speedKnots = 6.2 + (sin(counter.toDouble()) * 0.4)
            val sentence = when (counter % 4) {
                0 -> String.format(Locale.US, "\$GPGGA,%06d.00,%09.4f,N,%010.4f,W,1,08,1.2,18.4,M,-29.2,M,,*47", 123000 + counter, lat * 100, -lon * 100)
                1 -> String.format(Locale.US, "\$GPRMC,%06d.00,A,%09.4f,N,%010.4f,W,%05.1f,045.2,140926,,,A*76", 123000 + counter, lat * 100, -lon * 100, speedKnots)
                2 -> String.format(Locale.US, "\$GPVTG,045.2,T,,M,%05.2f,N,%05.2f,K,A*22", speedKnots, speedKnots * 1.852)
                else -> "\$GPGSA,A,3,04,05,09,12,14,24,25,31,,,,,2.1,1.2,1.8*3B"
            }
            nmeaStream.add(sentence)
            if (nmeaStream.size > 20) {
                nmeaStream.removeAt(0)
            }
            counter++
        }
    }

    // Vibrator helper
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

    // System gesture navigation & swipe back: return to primary tab or exit studio
    BackHandler(enabled = true) {
        if (selectedStudioTab != 0) {
            selectedStudioTab = 0
        } else {
            onBack()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MILES Studio",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "NERD LAB",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Text(
                    text = "Kalman Q/R filters, GNSS constellations, Running Watts & Geodesy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    smartEngine.refreshTelemetry()
                    vibrate(15)
                    Toast.makeText(context, "Telemetry & Kalman Sensors Refreshed", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh Telemetry",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Sub-tabs for comprehensive developer tools
        ScrollableTabRow(
            selectedTabIndex = selectedStudioTab,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            studioTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedStudioTab == index,
                    onClick = { selectedStudioTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedStudioTab) {
                // ==========================================
                // TAB 0: GNSS & KALMAN ENGINE
                // ==========================================
                0 -> {
                    // Live GNSS Telemetry Card
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "LIVE GNSS HARDWARE TELEMETRY",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val lastPoint = liveStats.points.lastOrNull()
                                DiagnosticRow("Receiver Provider", userPrefs.gpsProviderMode)
                                DiagnosticRow("CEP 95% Accuracy", "± ${liveStats.gpsAccuracyMeters.format(2)} m")
                                DiagnosticRow("WGS-84 Coordinates", "${lastPoint?.latitude?.format(6) ?: "37.774900"}, ${lastPoint?.longitude?.format(6) ?: "-122.419400"}")
                                DiagnosticRow("Ellipsoidal Altitude", "${lastPoint?.altitude?.format(1) ?: "14.2"} m")
                                DiagnosticRow("Instantaneous Speed", "${liveStats.currentSpeedKmh.format(2)} km/h (${(liveStats.currentSpeedKmh / 3.6).format(2)} m/s)")
                                DiagnosticRow("Bearing Track", "${lastPoint?.bearing?.format(1) ?: "44.8"}° True North")
                                DiagnosticRow("HDOP Mask Threshold", "< ${userPrefs.maxHdopThreshold.format(1)}")
                                DiagnosticRow("Active Constellations", "GPS + GLO + GAL + BEI")
                            }
                        }
                    }

                    // Kalman Filter Matrix Tuning Card
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "DYNAMIC KALMAN FILTER MATRIX TUNING",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "x_k = F·x_{k-1} + B·u_k + w_k  •  P_k = F·P_{k-1}·F^T + Q\nTuning Process Noise (Q) vs Measurement Noise (R) controls latency vs smoothness.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Process Noise Q
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Process Noise Covariance (Q)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        String.format(Locale.US, "%.4f m²/s²", userPrefs.kalmanProcessNoiseQ),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Slider(
                                    value = userPrefs.kalmanProcessNoiseQ,
                                    onValueChange = { preferences.updatePreferences { p -> p.copy(kalmanProcessNoiseQ = it) } },
                                    valueRange = 0.0005f..0.0300f,
                                    steps = 29
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Measurement Noise R
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Measurement Variance (R)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        String.format(Locale.US, "%.1f m²", userPrefs.kalmanMeasurementNoiseR),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Slider(
                                    value = userPrefs.kalmanMeasurementNoiseR,
                                    onValueChange = { preferences.updatePreferences { p -> p.copy(kalmanMeasurementNoiseR = it) } },
                                    valueRange = 1.0f..20.0f,
                                    steps = 19
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Heading threshold
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Heading Velocity Cutoff", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "${userPrefs.minHeadingSpeedMps.format(2)} m/s (${(userPrefs.minHeadingSpeedMps * 3.6f).format(1)} km/h)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Slider(
                                    value = userPrefs.minHeadingSpeedMps,
                                    onValueChange = { preferences.updatePreferences { p -> p.copy(minHeadingSpeedMps = it) } },
                                    valueRange = 0.2f..2.0f,
                                    steps = 9
                                )
                            }
                        }
                    }

                    // Multi-Constellation Satellite Receiver Selection
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "MULTI-CONSTELLATION GNSS SATELLITES",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Select active satellite orbital constellations and augmentation systems for multi-band trilateration.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                StudioToggleRow(
                                    title = "🇺🇸 NAVSTAR GPS (L1 / L5 Dual-Band)",
                                    subtitle = "US Department of Defense standard positioning",
                                    checked = userPrefs.gnssGpsEnabled,
                                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(gnssGpsEnabled = it) } }
                                )
                                StudioToggleRow(
                                    title = "🇷🇺 GLONASS (L1OF / L2OF FDMA)",
                                    subtitle = "Russian high-latitude aerospace constellation",
                                    checked = userPrefs.gnssGlonassEnabled,
                                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(gnssGlonassEnabled = it) } }
                                )
                                StudioToggleRow(
                                    title = "🇪🇺 GALILEO (E1 / E5a AltBOC High-Precision)",
                                    subtitle = "European civilian constellation (1m civilian precision)",
                                    checked = userPrefs.gnssGalileoEnabled,
                                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(gnssGalileoEnabled = it) } }
                                )
                                StudioToggleRow(
                                    title = "🇨🇳 BEIDOU (B1I / B2a Phase Modulation)",
                                    subtitle = "Chinese regional geostationary & MEO satellites",
                                    checked = userPrefs.gnssBeidouEnabled,
                                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(gnssBeidouEnabled = it) } }
                                )
                                StudioToggleRow(
                                    title = "🇯🇵 QZSS (Quasi-Zenith L1C/A)",
                                    subtitle = "Japanese high-elevation urban canyon augment",
                                    checked = userPrefs.gnssQzssEnabled,
                                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(gnssQzssEnabled = it) } }
                                )
                                StudioToggleRow(
                                    title = "🛰️ SBAS (WAAS / EGNOS / MSAS Differential)",
                                    subtitle = "Ground station ionospheric differential correction",
                                    checked = userPrefs.gnssSbasEnabled,
                                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(gnssSbasEnabled = it) } }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                StudioToggleRow(
                                    title = "Multipath Acceleration Outlier Filter",
                                    subtitle = "Reject urban skyscraper reflections (> 4.0 m/s² spike)",
                                    checked = userPrefs.multipathFilterEnabled,
                                    onCheckedChange = { preferences.updatePreferences { p -> p.copy(multipathFilterEnabled = it) } }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Dead reckoning duration
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Tunnel Dead Reckoning Coasting", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "${userPrefs.deadReckoningDurationSec}s",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Slider(
                                    value = userPrefs.deadReckoningDurationSec.toFloat(),
                                    onValueChange = { preferences.updatePreferences { p -> p.copy(deadReckoningDurationSec = it.toInt()) } },
                                    valueRange = 0f..30f,
                                    steps = 6
                                )
                            }
                        }
                    }

                    // Live NMEA 0183 Serial Terminal Card
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "NMEA 0183 SERIAL TERMINAL",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = isNmeaStreaming,
                                            onClick = { isNmeaStreaming = !isNmeaStreaming },
                                            label = { Text(if (isNmeaStreaming) "Live" else "Paused", fontSize = 10.sp) }
                                        )
                                        IconButton(
                                            onClick = {
                                                val all = nmeaStream.joinToString("\n")
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                clipboard?.setPrimaryClip(ClipData.newPlainText("NMEA Stream", all))
                                                Toast.makeText(context, "NMEA stream copied to clipboard", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy NMEA", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0D1117))
                                        .padding(10.dp)
                                ) {
                                    LazyColumn {
                                        items(nmeaStream) { line ->
                                            Text(
                                                text = line,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                color = Color(0xFF00E5FF)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 1: BIOMECHANICS & RUNNING POWER
                // ==========================================
                1 -> {
                    // Running Power Physics Lab Card
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "REAL-TIME RUNNING POWER ENGINE (WATTS)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "P_total = P_kinetic + P_potential + P_aerodynamic\nP_aero = 0.5 · ρ · CdA · (v - v_wind)² · v",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Live Calculated Power Card
                                val speedMps = liveStats.currentSpeedKmh / 3.6
                                val massKg = userPrefs.userWeightKg
                                val effectiveWindMps = userPrefs.windSpeedKmh / 3.6
                                val relativeAirSpeed = (speedMps + effectiveWindMps).coerceAtLeast(0.0)
                                val pAero = 0.5 * userPrefs.airDensityRho * userPrefs.dragCoefficientCdA * (relativeAirSpeed.pow(2)) * speedMps
                                val pKinetic = massKg * 3.98 * speedMps // ~4.0 J/kg/m running energy cost
                                val grade = (liveStats.elevationGainM / 1000.0).coerceIn(-0.15, 0.15)
                                val pPotential = massKg * 9.81 * speedMps * grade
                                val totalWatts = (pKinetic + pPotential + pAero).coerceAtLeast(0.0)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("TOTAL POWER", style = MaterialTheme.typography.labelSmall)
                                        Text("${totalWatts.toInt()} W", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary))
                                        Text("${(totalWatts / massKg).format(2)} W/kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("KINETIC", style = MaterialTheme.typography.labelSmall)
                                        Text("${pKinetic.toInt()} W", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("AERODYNAMIC", style = MaterialTheme.typography.labelSmall)
                                        Text("${pAero.format(1)} W", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // CdA Drag Slider
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Aerodynamic Drag (Cd·A)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "${userPrefs.dragCoefficientCdA.format(2)} m²",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Slider(
                                    value = userPrefs.dragCoefficientCdA,
                                    onValueChange = { preferences.updatePreferences { p -> p.copy(dragCoefficientCdA = it) } },
                                    valueRange = 0.18f..0.38f,
                                    steps = 10
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Air Density Rho
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Air Density (ρ at sea level)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "${userPrefs.airDensityRho.format(3)} kg/m³",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Slider(
                                    value = userPrefs.airDensityRho,
                                    onValueChange = { preferences.updatePreferences { p -> p.copy(airDensityRho = it) } },
                                    valueRange = 1.15f..1.30f,
                                    steps = 15
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Headwind / Tailwind Vector
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Wind Vector (Headwind / Tailwind)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                    Text(
                                        "${if (userPrefs.windSpeedKmh > 0) "+${userPrefs.windSpeedKmh.toInt()} km/h Headwind" else "${userPrefs.windSpeedKmh.toInt()} km/h Tailwind"}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Slider(
                                    value = userPrefs.windSpeedKmh,
                                    onValueChange = { preferences.updatePreferences { p -> p.copy(windSpeedKmh = it) } },
                                    valueRange = -25f..25f,
                                    steps = 20
                                )
                            }
                        }
                    }

                    // Grade Adjusted Pace (GAP) Model
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "GRADE ADJUSTED PACE (GAP) ALGORITHM",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Normalized flat-ground equivalent speed model on hills.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val gapModels = listOf(
                                    "MINETTI_2002" to "Minetti et al. 2002 (5th-Order Poly)",
                                    "STRAVA_EMPIRICAL" to "Strava Empirical Hill Curve",
                                    "DAVIES_1980" to "Davies 1980 Treadmill Ergometer"
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    gapModels.forEach { (id, label) ->
                                        FilterChip(
                                            selected = userPrefs.gapAlgorithm == id,
                                            onClick = { preferences.updatePreferences { it.copy(gapAlgorithm = id) } },
                                            label = { Text(label, fontSize = 11.sp) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Ground Contact Time (GCT)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${userPrefs.groundContactTimeMs} ms", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary))
                                        Slider(
                                            value = userPrefs.groundContactTimeMs.toFloat(),
                                            onValueChange = { preferences.updatePreferences { p -> p.copy(groundContactTimeMs = it.toInt()) } },
                                            valueRange = 180f..320f,
                                            steps = 14
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Vertical Oscillation", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        Text("${userPrefs.verticalOscillationCm.format(1)} cm", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary))
                                        Slider(
                                            value = userPrefs.verticalOscillationCm,
                                            onValueChange = { preferences.updatePreferences { p -> p.copy(verticalOscillationCm = it) } },
                                            valueRange = 5.0f..14.0f,
                                            steps = 9
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Jack Daniels VDOT & VO2 Max Physiology Calculator
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "JACK DANIELS VDOT & VO2 MAX PHYSIOLOGY",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Calculate exact aerobic running economy (VDOT) and Jack Daniels training paces.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(1500.0 to "1500m", 5000.0 to "5K", 10000.0 to "10K", 21097.5 to "Half", 42195.0 to "Marathon").forEach { (dist, label) ->
                                        FilterChip(
                                            selected = vdotDistanceMeters == dist,
                                            onClick = { vdotDistanceMeters = dist },
                                            label = { Text(label, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = vdotTimeMinutesText,
                                    onValueChange = { vdotTimeMinutesText = it },
                                    label = { Text("Race Finish Time (minutes, e.g. 21.5)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                val timeMin = vdotTimeMinutesText.toDoubleOrNull() ?: 22.0
                                val velocityMpm = vdotDistanceMeters / timeMin
                                val vo2Cost = -4.60 + 0.182258 * velocityMpm + 0.000104 * velocityMpm.pow(2)
                                val percentVo2 = 0.8 + 0.1894393 * exp(-0.012778 * timeMin) + 0.2989558 * exp(-0.1932605 * timeMin)
                                val vdot = (vo2Cost / percentVo2).coerceIn(30.0, 85.0)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("VDOT SCORE", style = MaterialTheme.typography.labelSmall)
                                        Text(vdot.format(1), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("EST. VO2 MAX", style = MaterialTheme.typography.labelSmall)
                                        Text("${(vdot * 1.05).format(1)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                        Text("ml/kg/min", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Prescribed Training Paces (min/km)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(4.dp))
                                val easyPaceSec = (1000.0 / (velocityMpm * 0.70)) * 60
                                val marathonPaceSec = (1000.0 / (velocityMpm * 0.84)) * 60
                                val thresholdPaceSec = (1000.0 / (velocityMpm * 0.90)) * 60
                                val intervalPaceSec = (1000.0 / (velocityMpm * 0.98)) * 60

                                fun formatPace(sec: Double): String {
                                    val m = (sec / 60).toInt()
                                    val s = (sec % 60).toInt()
                                    return String.format(Locale.US, "%d:%02d", m, s)
                                }

                                DiagnosticRow("Easy / Recovery (E-Pace)", "${formatPace(easyPaceSec)} /km")
                                DiagnosticRow("Marathon (M-Pace)", "${formatPace(marathonPaceSec)} /km")
                                DiagnosticRow("Threshold (T-Pace)", "${formatPace(thresholdPaceSec)} /km")
                                DiagnosticRow("Interval VO2 (I-Pace)", "${formatPace(intervalPaceSec)} /km")
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 2: HARDWARE & INJECTION BENCH
                // ==========================================
                2 -> {
                    // Mock GPS Injector Card
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "MOCK GPS COORDINATE INJECTOR",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Synthetically broadcast coordinates directly into SmartTrackingEngine to evaluate maps and lap splitting without leaving the desk.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = mockLatText,
                                        onValueChange = { mockLatText = it },
                                        label = { Text("Latitude") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = mockLonText,
                                        onValueChange = { mockLonText = it },
                                        label = { Text("Longitude") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = mockAltText,
                                        onValueChange = { mockAltText = it },
                                        label = { Text("Altitude (m)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = mockSpeedText,
                                        onValueChange = { mockSpeedText = it },
                                        label = { Text("Speed (km/h)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick presets
                                Text("Preset Geographic Test Tracks", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        Triple("SF Embarcadero", 37.7955, -122.3937),
                                        Triple("NYC Central Park", 40.7829, -73.9654),
                                        Triple("Tokyo Imperial", 35.6852, 139.7528)
                                    ).forEach { (city, lat, lon) ->
                                        FilterChip(
                                            selected = mockLatText == lat.toString(),
                                            onClick = {
                                                mockLatText = lat.toString()
                                                mockLonText = lon.toString()
                                            },
                                            label = { Text(city, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        val lat = mockLatText.toDoubleOrNull() ?: 37.7749
                                        val lon = mockLonText.toDoubleOrNull() ?: -122.4194
                                        val alt = mockAltText.toDoubleOrNull() ?: 15.0
                                        val speed = mockSpeedText.toDoubleOrNull() ?: 12.0
                                        smartEngine.injectMockLocation(lat, lon, alt, speed)
                                        Toast.makeText(context, "Injected fix: $lat, $lon (${speed}km/h)", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Inject Single Location Fix")
                                }
                            }
                        }
                    }

                    // Biometric Heart Rate Injector
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "BIOMETRIC HEART RATE INJECTOR",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Inject synthetic pulse waves into the workout HUD to test Karvonen HR alerts without wearing a chest strap.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(0 to "Disconnect", 55 to "55 (Rest)", 132 to "132 (Z2)", 164 to "164 (Z4)", 188 to "188 (Z5 Max)").forEach { (bpm, label) ->
                                        FilterChip(
                                            selected = liveStats.heartRate == bpm,
                                            onClick = {
                                                smartEngine.injectMockHeartRate(bpm)
                                                Toast.makeText(context, "Heart rate set to $bpm BPM", Toast.LENGTH_SHORT).show()
                                            },
                                            label = { Text(label, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Barometric Altimeter Hypsometric Calculator
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "BAROMETRIC HYPSOMETRIC ALTIMETER",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "h = 44330 · (1 - (P / P0)^0.190284)  •  P0 = 1013.25 hPa ISA standard",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = baroStationPressureHpa,
                                    onValueChange = { baroStationPressureHpa = it },
                                    label = { Text("Station Atmospheric Pressure (hPa)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                val pStation = baroStationPressureHpa.toDoubleOrNull() ?: 1013.25
                                val p0 = userPrefs.barometerQnhHpa.toDouble()
                                val altitudeMeters = 44330.0 * (1.0 - (pStation / p0).pow(0.190284))

                                Spacer(modifier = Modifier.height(8.dp))
                                DiagnosticRow("ISA Pressure Altitude (m)", "${altitudeMeters.format(1)} m")
                                DiagnosticRow("ISA Pressure Altitude (ft)", "${(altitudeMeters * 3.28084).format(1)} ft")
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 3: CLOCK, LOOP & HAPTICS
                // ==========================================
                3 -> {
                    // Counter Clock Interval Card
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "LIVE COUNTER TICK RATE",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Current: ${userPrefs.counterIntervalMs}ms",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tune the internal stopwatch timer ticker interval from 10 milliseconds to 10 minutes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                val counterPresets = listOf(
                                    10L to "10ms (100Hz)",
                                    50L to "50ms",
                                    100L to "100ms",
                                    500L to "500ms",
                                    1000L to "1s (Standard)",
                                    3000L to "3s",
                                    60000L to "1m (Eco)",
                                    600000L to "10m"
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    counterPresets.chunked(4).forEach { row ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            row.forEach { (ms, label) ->
                                                FilterChip(
                                                    selected = userPrefs.counterIntervalMs == ms,
                                                    onClick = {
                                                        preferences.setCounterInterval(ms)
                                                        smartEngine.counterIntervalMs = ms
                                                        customCounterMsText = ms.toString()
                                                        Toast.makeText(context, "Timer set to $label", Toast.LENGTH_SHORT).show()
                                                    },
                                                    label = { Text(label, fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customCounterMsText,
                                        onValueChange = { customCounterMsText = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("Custom Tick (ms)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            val parsed = customCounterMsText.toLongOrNull() ?: 100L
                                            val clamped = parsed.coerceIn(10L, 600000L)
                                            preferences.setCounterInterval(clamped)
                                            smartEngine.counterIntervalMs = clamped
                                            customCounterMsText = clamped.toString()
                                            Toast.makeText(context, "Tick set to ${clamped}ms", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("Apply")
                                    }
                                }
                            }
                        }
                    }

                    // Hardware Haptics PWM Actuator Card
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "HARDWARE HAPTIC PWM ACTUATOR",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Actuate device linear resonant actuators (LRA) and eccentric rotating mass (ERM) motors.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            vibrate(25)
                                            Toast.makeText(context, "Subtle Click (25ms)", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Click")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            vibrate(75)
                                            Toast.makeText(context, "Metronome Pulse (75ms)", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Pulse")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 60, 100, 60, 150), -1))
                                            } else {
                                                @Suppress("DEPRECATION")
                                                vibrator?.vibrate(longArrayOf(0, 100, 60, 100, 60, 150), -1)
                                            }
                                            Toast.makeText(context, "Milestone Split Buzz", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Milestone")
                                    }
                                }
                            }
                        }
                    }

                    // Wear OS Protocol Link Card
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "WEAR OS COMPANION PROTOCOL",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            wearCompanion.pingWatch()
                                            Toast.makeText(context, "Dispatched PING packet", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Send PING")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            wearCompanion.sendLiveWorkoutUpdate(120L, 450.0, 310.0, 142)
                                            Toast.makeText(context, "Dispatched METRICS packet", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Send METRICS")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Recent Packets (${wearLogs.size}):", style = MaterialTheme.typography.labelSmall)
                                if (wearLogs.isEmpty()) {
                                    Text("No watch communication logged.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    wearLogs.takeLast(4).reversed().forEach { log ->
                                        Text(
                                            text = "[${log.direction}] ${log.topic}: ${log.payload}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 4: GEODESY & SPHERICAL MATH
                // ==========================================
                4 -> {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "WGS-84 ELLIPSOID & GEODESY LAB",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    FilledTonalButton(onClick = onOpenDistanceCalculator) {
                                        Text("Full Calculator")
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Compares sub-millimeter Vincenty inverse geodesic calculation on the WGS-84 oblate spheroid (a=6378137m, f=1/298.257) against Haversine spherical model.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                Text("Coordinate Point 1 (Origin)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = lat1Text, onValueChange = { lat1Text = it }, label = { Text("Lat 1") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(value = lon1Text, onValueChange = { lon1Text = it }, label = { Text("Lon 1") }, modifier = Modifier.weight(1f), singleLine = true)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Coordinate Point 2 (Destination)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(value = lat2Text, onValueChange = { lat2Text = it }, label = { Text("Lat 2") }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(value = lon2Text, onValueChange = { lon2Text = it }, label = { Text("Lon 2") }, modifier = Modifier.weight(1f), singleLine = true)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        val pLat1 = lat1Text.toDoubleOrNull() ?: 37.7749
                                        val pLon1 = lon1Text.toDoubleOrNull() ?: -122.4194
                                        val pLat2 = lat2Text.toDoubleOrNull() ?: 37.8024
                                        val pLon2 = lon2Text.toDoubleOrNull() ?: -122.4058

                                        // Haversine
                                        val r = 6371000.0
                                        val dLat = Math.toRadians(pLat2 - pLat1)
                                        val dLon = Math.toRadians(pLon2 - pLon1)
                                        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(pLat1)) * cos(Math.toRadians(pLat2)) * sin(dLon / 2).pow(2)
                                        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
                                        val hav = r * c

                                        // Vincenty (WGS-84 oblate ellipsoid)
                                        val aWgs = 6378137.0
                                        val bWgs = 6356752.314245
                                        val fWgs = 1 / 298.257223563
                                        val L = Math.toRadians(pLon2 - pLon1)
                                        val U1 = atan2((1 - fWgs) * sin(Math.toRadians(pLat1)), cos(Math.toRadians(pLat1)))
                                        val U2 = atan2((1 - fWgs) * sin(Math.toRadians(pLat2)), cos(Math.toRadians(pLat2)))
                                        val sinU1 = sin(U1)
                                        val cosU1 = cos(U1)
                                        val sinU2 = sin(U2)
                                        val cosU2 = cos(U2)

                                        var lambda = L
                                        var lambdaP: Double
                                        var iterLimit = 100
                                        var cosSqAlpha: Double
                                        var sinSigma: Double
                                        var cos2SigmaM: Double
                                        var cosSigma: Double
                                        var sigma: Double

                                        do {
                                            val sinLambda = sin(lambda)
                                            val cosLambda = cos(lambda)
                                            sinSigma = sqrt((cosU2 * sinLambda).pow(2) + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda).pow(2))
                                            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
                                            sigma = atan2(sinSigma, cosSigma)
                                            val sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma
                                            cosSqAlpha = 1 - sinAlpha * sinAlpha
                                            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha
                                            val C = fWgs / 16 * cosSqAlpha * (4 + fWgs * (4 - 3 * cosSqAlpha))
                                            lambdaP = lambda
                                            lambda = L + (1 - C) * fWgs * sinAlpha * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)))
                                        } while (kotlin.math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0)

                                        val uSq = cosSqAlpha * (aWgs * aWgs - bWgs * bWgs) / (bWgs * bWgs)
                                        val A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)))
                                        val B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)))
                                        val deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) * (-3 + 4 * cos2SigmaM * cos2SigmaM)))
                                        val vinc = bWgs * A * (sigma - deltaSigma)

                                        // Bearing
                                        val y = sin(dLon) * cos(Math.toRadians(pLat2))
                                        val x = cos(Math.toRadians(pLat1)) * sin(Math.toRadians(pLat2)) - sin(Math.toRadians(pLat1)) * cos(Math.toRadians(pLat2)) * cos(dLon)
                                        val bearing = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0

                                        calcHaversineMeters = hav
                                        calcVincentyMeters = vinc
                                        calcInitialBearingDeg = bearing
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Compute Geodesic Inverses")
                                }

                                if (calcVincentyMeters != null && calcHaversineMeters != null) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    val vinc = calcVincentyMeters!!
                                    val hav = calcHaversineMeters!!
                                    DiagnosticRow("Vincenty Ellipsoid Distance", "${vinc.format(3)} m (${(vinc / 1000.0).format(4)} km)")
                                    DiagnosticRow("Haversine Great Circle Distance", "${hav.format(3)} m (${(hav / 1000.0).format(4)} km)")
                                    DiagnosticRow("Ellipsoidal Discrepancy (Δ)", "${(vinc - hav).format(3)} m (${((vinc - hav) / vinc * 100).format(4)}%)")
                                    DiagnosticRow("Initial Azimuth Bearing", "${calcInitialBearingDeg?.format(2)}° True North")
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // TAB 5: SQLITE, SCHEMAS & RAW TELEMETRY
                // ==========================================
                5 -> {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ROOM DATABASE & SQLITE ENGINE",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                DiagnosticRow("Stored Activities", "${activities.size} entities")
                                DiagnosticRow("Saved Routes", "${routes.size} entities")
                                DiagnosticRow("Active Privacy Zones", "${privacyZones.size} zones")
                                DiagnosticRow("Journal Mode", "PRAGMA journal_mode=WAL")
                                DiagnosticRow("Synchronous Mode", "PRAGMA synchronous=NORMAL")
                                DiagnosticRow("SQLite Page Size", "4096 bytes")

                                Spacer(modifier = Modifier.height(14.dp))

                                // Generate Raw GPX 1.1 Preview
                                Text("Telemetry Export Engine", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        val sampleGpx = buildString {
                                            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                                            appendLine("<gpx version=\"1.1\" creator=\"MILES Engine\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
                                            appendLine("  <trk><name>MILES Studio Telemetry Track</name><trkseg>")
                                            liveStats.points.takeLast(5).forEach { pt ->
                                                appendLine("    <trkpt lat=\"${pt.latitude}\" lon=\"${pt.longitude}\"><ele>${pt.altitude}</ele><time>${pt.timestamp}</time></trkpt>")
                                            }
                                            appendLine("  </trkseg></trk>")
                                            appendLine("</gpx>")
                                        }
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("GPX 1.1", sampleGpx))
                                        Toast.makeText(context, "Generated and copied valid GPX 1.1 XML", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Live Track as GPX 1.1 XML")
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // JVM Memory Heap
                                val runtime = Runtime.getRuntime()
                                val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                                val maxMb = runtime.maxMemory() / (1024 * 1024)
                                DiagnosticRow("JVM Heap Usage", "$usedMb MB / $maxMb MB")

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            System.gc()
                                            Toast.makeText(context, "Garbage collection triggered", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Run JVM GC")
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repository.purgePreloadedSeedData()
                                                Toast.makeText(context, "Sample data purged clean!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Purge Sample Data")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StudioToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
