package com.example.miles.ui.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Android Health Connect System Permissions Sheet.
 * Matches pixel-for-pixel the standard Health Connect permission flow shown in
 * Screenshot_20260922_174955.png ("Allow MILES Wear OS to access your fitness and wellness data?").
 */
@Composable
fun PermissionPromptScreen(
    appName: String = "MILES Wear OS",
    onAllow: () -> Unit,
    onDeny: () -> Unit
) {
    val darkBackground = Color(0xFF131314)
    val textWhite = Color(0xFFE3E2E6)
    val textMuted = Color(0xFFC4C7C5)
    val linkBlue = Color(0xFF8AB4F8)
    val cardBackground = Color(0xFF233152)

    var showLearnMoreDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // Health Connect permissions toggles
    val permissions = remember {
        listOf(
            "Heart rate",
            "Steps",
            "Distance",
            "Total calories burned",
            "Exercise",
            "Sleep",
            "Speed"
        )
    }

    val itemChecked = remember {
        mutableStateMapOf<String, Boolean>().apply {
            permissions.forEach { put(it, false) }
        }
    }

    val allChecked = permissions.all { itemChecked[it] == true }
    val anyChecked = permissions.any { itemChecked[it] == true }

    fun toggleAll(enabled: Boolean) {
        permissions.forEach { itemChecked[it] = enabled }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = darkBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                // Health Connect Heart Logo
                HealthConnectHeartLogo(
                    modifier = Modifier.size(48.dp),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title: "Allow MILES Wear OS to access your fitness and wellness data?"
                val titleAnnotated = buildAnnotatedString {
                    append("Allow ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textWhite)) {
                        append(appName)
                    }
                    append(" to access your fitness and wellness data?")
                }

                Text(
                    text = titleAnnotated,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 23.sp,
                        lineHeight = 31.sp
                    ),
                    color = textWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Information bullet 1: Choose data + learn more link
                InfoBulletRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                ) {
                    val annotatedString = buildAnnotatedString {
                        append("Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, ")
                        pushStringAnnotation(tag = "LEARN_MORE", annotation = "learn_more")
                        withStyle(SpanStyle(color = linkBlue, textDecoration = TextDecoration.Underline)) {
                            append("learn more")
                        }
                        pop()
                    }
                    ClickableText(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textMuted,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        ),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(tag = "LEARN_MORE", start = offset, end = offset)
                                .firstOrNull()?.let {
                                    showLearnMoreDialog = true
                                }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Information bullet 2: 30 days history
                InfoBulletRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                ) {
                    Text(
                        text = "If you give read access, the app can read new data and data from the past 30 days",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textMuted,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Information bullet 3: Privacy policy link
                InfoBulletRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                ) {
                    val privacyAnnotated = buildAnnotatedString {
                        append("You can learn how $appName handles your data in their ")
                        pushStringAnnotation(tag = "PRIVACY", annotation = "privacy")
                        withStyle(SpanStyle(color = linkBlue, textDecoration = TextDecoration.Underline)) {
                            append("privacy policy")
                        }
                        pop()
                    }
                    ClickableText(
                        text = privacyAnnotated,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textMuted,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp
                        ),
                        onClick = { offset ->
                            privacyAnnotated.getStringAnnotations(tag = "PRIVACY", start = offset, end = offset)
                                .firstOrNull()?.let {
                                    showPrivacyPolicyDialog = true
                                }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // "Allow all" Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(cardBackground)
                        .clickable { toggleAll(!allChecked) }
                        .padding(horizontal = 22.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Allow all",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 17.sp
                            ),
                            color = textWhite,
                            modifier = Modifier.weight(1f)
                        )

                        HealthConnectSwitch(
                            checked = allChecked,
                            onCheckedChange = { toggleAll(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Subheader: Allow "MILES Wear OS" to read
                Text(
                    text = "Allow \"$appName\" to read",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = linkBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 6.dp)
                )

                // Individual Permission Rows
                permissions.forEach { perm ->
                    val isChecked = itemChecked[perm] == true
                    PermissionItemRow(
                        title = perm,
                        checked = isChecked,
                        onCheckedChange = { next ->
                            itemChecked[perm] = next
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Sticky Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Don't allow" Button
                OutlinedButton(
                    onClick = onDeny,
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, Color(0xFF5E6266)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textWhite
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .padding(end = 6.dp)
                ) {
                    Text(
                        text = "Don't allow",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)
                    )
                }

                // "Allow" Button
                Button(
                    onClick = onAllow,
                    enabled = anyChecked,
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (anyChecked) linkBlue else Color(0xFF222428),
                        contentColor = if (anyChecked) Color(0xFF040C19) else Color(0xFF8E918F),
                        disabledContainerColor = Color(0xFF222428),
                        disabledContentColor = Color(0xFF8E918F)
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .padding(start = 6.dp)
                ) {
                    Text(
                        text = "Allow",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }

    // Dialog: Learn More
    if (showLearnMoreDialog) {
        AlertDialog(
            onDismissRequest = { showLearnMoreDialog = false },
            title = {
                Text(
                    text = "About Health & Fitness Data",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Health Connect provides an on-device repository for your health, fitness, and wellness records. MILES reads and writes data entirely on this device without transmitting any metrics to remote servers.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showLearnMoreDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }

    // Dialog: Privacy Policy
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = {
                Text(
                    text = "MILES Privacy Policy",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "MILES is a 100% Free and Open-Source Software (FOSS) project. It has zero third-party tracking SDKs, zero ads, and zero server-side telemetry. All GPS traces, steps, and biometrics stay strictly in local SQLite storage under your sole control.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Health Connect Heart Logo Icon.
 * Replicates the official Health Connect heart with inner loop ring.
 */
@Composable
private fun HealthConnectHeartLogo(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.6.dp.toPx()
        val w = size.width
        val h = size.height

        // Outer Heart Outline
        val heartPath = Path().apply {
            moveTo(w * 0.50f, h * 0.86f)
            // Left curve
            cubicTo(
                w * 0.16f, h * 0.62f,
                w * 0.05f, h * 0.36f,
                w * 0.28f, h * 0.18f
            )
            cubicTo(
                w * 0.40f, h * 0.08f,
                w * 0.50f, h * 0.24f,
                w * 0.50f, h * 0.24f
            )
            // Right curve
            cubicTo(
                w * 0.50f, h * 0.24f,
                w * 0.60f, h * 0.08f,
                w * 0.72f, h * 0.18f
            )
            cubicTo(
                w * 0.95f, h * 0.36f,
                w * 0.84f, h * 0.62f,
                w * 0.50f, h * 0.86f
            )
            close()
        }

        drawPath(
            path = heartPath,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Inner Circle in Right Lobe
        drawCircle(
            color = color,
            radius = w * 0.09f,
            center = Offset(w * 0.67f, h * 0.36f),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
private fun InfoBulletRow(
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.padding(top = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun PermissionItemRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val textWhite = Color(0xFFE3E2E6)
    val textMuted = Color(0xFFC4C7C5)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon for permission
        PermissionIcon(title = title, tint = textMuted)

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = textWhite,
            modifier = Modifier.weight(1f)
        )

        HealthConnectSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PermissionIcon(
    title: String,
    tint: Color
) {
    when (title) {
        "Heart rate" -> {
            // Heartbeat ECG Pulse wave icon matching the screenshot
            Canvas(modifier = Modifier.size(24.dp)) {
                val w = size.width
                val h = size.height
                val stroke = 2.dp.toPx()
                val path = Path().apply {
                    moveTo(0f, h * 0.52f)
                    lineTo(w * 0.28f, h * 0.52f)
                    lineTo(w * 0.40f, h * 0.16f)
                    lineTo(w * 0.54f, h * 0.88f)
                    lineTo(w * 0.68f, h * 0.36f)
                    lineTo(w * 0.78f, h * 0.52f)
                    lineTo(w, h * 0.52f)
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        "Steps" -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        "Distance" -> {
            Icon(
                imageVector = Icons.Default.NearMe,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        "Total calories burned" -> {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        "Exercise" -> {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        "Sleep" -> {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        "Speed" -> {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        else -> {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun HealthConnectSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color(0xFF040C19),
            checkedTrackColor = Color(0xFF8AB4F8),
            checkedBorderColor = Color(0xFF8AB4F8),
            uncheckedThumbColor = Color(0xFF909398),
            uncheckedTrackColor = Color(0xFF3B3D42),
            uncheckedBorderColor = Color(0xFF3B3D42)
        )
    )
}
