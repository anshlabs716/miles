package com.example.miles.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.miles.data.model.GpsPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationTracker(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _currentLocation = MutableStateFlow<GpsPoint?>(null)
    val currentLocation: StateFlow<GpsPoint?> = _currentLocation.asStateFlow()

    private val _isTrackingLocation = MutableStateFlow(false)
    val isTrackingLocation: StateFlow<Boolean> = _isTrackingLocation.asStateFlow()

    private var onLocationUpdate: ((GpsPoint) -> Unit)? = null
    private var locationCallback: LocationCallback? = null

    private val systemLocationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            handleNewLocation(loc)
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private var lastProcessedTime: Long = 0L
    private var currentIntervalMs: Long = 1000L

    fun startTracking(
        intervalMs: Long = 1000L,
        gpsEnabled: Boolean = true,
        onUpdate: ((GpsPoint) -> Unit)? = null
    ) {
        this.onLocationUpdate = onUpdate
        this.currentIntervalMs = intervalMs.coerceIn(100L, 10000L)

        if (!gpsEnabled) {
            Log.i("LocationTracker", "GPS tracking turned OFF by user setting.")
            stopTracking()
            return
        }

        if (!hasLocationPermission()) {
            Log.w("LocationTracker", "Cannot start location tracking: permission not granted")
            return
        }
        if (_isTrackingLocation.value && locationCallback != null) {
            // Already tracking with an active callback; avoid duplicate listener registration
            return
        }

        try {
            // Remove any previous listener before registering a new one
            locationCallback?.let { fusedClient.removeLocationUpdates(it) }
            // First fetch immediate last known location
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    handleNewLocation(loc)
                } else {
                    val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    val bestLast = lastGps ?: lastNet
                    if (bestLast != null) handleNewLocation(bestLast)
                }
            }

            // High-precision fused location updates with user-configured refresh speed
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, currentIntervalMs)
                .setMinUpdateIntervalMillis(currentIntervalMs / 2)
                .setMinUpdateDistanceMeters(0.8f)
                .build()

            val cb = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { handleNewLocation(it) }
                }
            }
            locationCallback = cb

            fusedClient.requestLocationUpdates(request, cb, Looper.getMainLooper())
            _isTrackingLocation.value = true
        } catch (e: SecurityException) {
            Log.e("LocationTracker", "SecurityException requesting location: ${e.message}")
        } catch (e: Exception) {
            Log.e("LocationTracker", "Error requesting location from fused provider, falling back to LocationManager: ${e.message}")
            try {
                // Fallback to system LocationManager only if FusedClient fails
                locationManager?.let { lm ->
                    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        lm.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            currentIntervalMs,
                            0.8f,
                            systemLocationListener,
                            Looper.getMainLooper()
                        )
                        _isTrackingLocation.value = true
                    }
                }
            } catch (fallbackEx: Exception) {
                Log.e("LocationTracker", "Fallback LocationManager failed: ${fallbackEx.message}")
            }
        }
    }

    fun stopTracking() {
        try {
            locationCallback?.let { fusedClient.removeLocationUpdates(it) }
            locationCallback = null
            locationManager?.removeUpdates(systemLocationListener)
        } catch (e: Exception) {
            Log.w("LocationTracker", "Error stopping location updates: ${e.message}")
        }
        _isTrackingLocation.value = false
        onLocationUpdate = null
    }

    private fun handleNewLocation(loc: Location) {
        val now = System.currentTimeMillis()
        // Deduplicate rapid bursts based on user configured refresh interval
        val minThreshold = (currentIntervalMs * 0.85).toLong().coerceAtLeast(100L)
        if (now - lastProcessedTime < minThreshold) {
            return
        }
        lastProcessedTime = now

        val point = GpsPoint(
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitude = loc.altitude,
            accuracy = loc.accuracy,
            speed = loc.speed,
            bearing = loc.bearing,
            timestamp = if (loc.time > 0) loc.time else now
        )
        _currentLocation.value = point
        onLocationUpdate?.invoke(point)
    }
}
