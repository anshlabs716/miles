package com.example.miles.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.R
import com.example.miles.data.local.MilesPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Keeps real hardware sensors alive while an active/background tracking session needs them. */
class TrackingForegroundService : Service() {
    private lateinit var preferences: MilesPreferences
    private lateinit var pedometer: PedometerManager
    private lateinit var locationTracker: LocationTracker
    private val scope = CoroutineScope(Dispatchers.Main.immediate + Job())

    override fun onCreate() {
        super.onCreate()
        preferences = MilesPreferences(this)
        pedometer = PedometerManager(this, preferences)
        locationTracker = LocationTracker(this)
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Hardware tracking active"), foregroundType())
        if (hasActivityRecognition()) pedometer.startTracking()

        scope.launch {
            preferences.userPreferences.collect { prefs ->
                if (prefs.stepSensorHardwareEnabled && hasActivityRecognition()) pedometer.startTracking()
                else pedometer.stopTracking()
                if (prefs.gpsSensorEnabled && locationTracker.hasLocationPermission()) {
                    locationTracker.startTracking(prefs.sensorRefreshRateMs, true)
                } else {
                    locationTracker.stopTracking()
                }
                updateNotification(prefs.gpsSensorEnabled && locationTracker.hasLocationPermission())
            }
        }
    }

    private fun hasActivityRecognition(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    private fun foregroundType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
    } else 0

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "MILES tracking", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Shows when MILES is keeping hardware tracking alive"
                }
            )
        }
    }

    private fun buildNotification(detail: String): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MILES tracking")
            .setContentText(detail)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

    private fun updateNotification(gps: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(if (gps) "Steps + GPS tracking active" else "Step tracking active"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        pedometer.stopTracking()
        locationTracker.stopTracking()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "miles_tracking"
        private const val NOTIFICATION_ID = 1001
        fun start(context: Context) = ContextCompat.startForegroundService(context, Intent(context, TrackingForegroundService::class.java))
        fun stop(context: Context) = context.stopService(Intent(context, TrackingForegroundService::class.java))
    }
}
