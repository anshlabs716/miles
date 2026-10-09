package com.example.miles.health

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController

/**
 * Android 14+ (API 34) requires every app that declares Health Connect permissions to also
 * declare an activity that can answer `VIEW_PERMISSION_USAGE` and
 * `ACTION_SHOW_PERMISSIONS_RATIONALE`. Without it the system has nowhere to send the user, so
 * the Health Connect permission sheet can silently fail to appear.
 *
 * This activity explains exactly what MILES reads and why, then hands control back to the
 * Health Connect permission contract. MILES only ever reads steps and exercise sessions.
 */
class HealthPermissionsRationaleActivity : ComponentActivity() {

    private val permissionLauncher: ActivityResultLauncher<Set<String>> =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) {
            // The system sheet owns the result; this screen only had to explain why.
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = HealthConnectManager(this)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Why MILES asks for Health Connect",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Health Connect is optional. MILES runs fully without it, and " +
                                "nothing is uploaded anywhere.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "MILES only reads two record types:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "• Steps, to import step counts you already recorded on this " +
                                "phone instead of walking twice.\n\n" +
                                "• Exercise sessions, to attach existing workouts to your history.\n\n" +
                                "MILES never writes to Health Connect, never deletes records, and " +
                                "never reads heart rate, sleep, location or anything else.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "The data stays on this device. You can revoke access at any " +
                                "time from Health Connect settings.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Button(
                            onClick = { permissionLauncher.launch(manager.permissions) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue to Health Connect")
                        }

                        OutlinedButton(
                            onClick = { manager.manageDataIntent()?.let(::startActivity) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Health Connect settings")
                        }

                        OutlinedButton(
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Not now")
                        }
                    }
                }
            }
        }
    }
}
