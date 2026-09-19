package com.example.miles.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.miles.data.model.GpsPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationTracker(private val context: Context) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val _currentLocation = MutableStateFlow<GpsPoint?>(null)
    val currentLocation: StateFlow<GpsPoint?> = _currentLocation.asStateFlow()
    private val _isTrackingLocation = MutableStateFlow(false)
    val isTrackingLocation: StateFlow<Boolean> = _isTrackingLocation.asStateFlow()
    private val _gpsAvailable = MutableStateFlow(false)
    val gpsAvailable: StateFlow<Boolean> = _gpsAvailable.asStateFlow()
    private val _status = MutableStateFlow("GPS waiting for permission")
    val status: StateFlow<String> = _status.asStateFlow()
    private var listenerRegistered = false
    private var updateCallback: ((GpsPoint) -> Unit)? = null
    private var intervalMs = 1000L
    private var lastProcessedElapsedMs = 0L

    private val fallbackListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = processLocation(location)
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        override fun onProviderEnabled(provider: String) { refreshAvailability() }
        override fun onProviderDisabled(provider: String) { refreshAvailability() }
    }

    init { refreshAvailability() }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean = runCatching {
        locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }.getOrDefault(false)

    fun refreshAvailability() {
        _gpsAvailable.value = hasLocationPermission() && isLocationEnabled()
        _status.value = when {
            !hasLocationPermission() -> "GPS permission required"
            !isLocationEnabled() -> "Location services are OFF"
            _isTrackingLocation.value -> "GPS tracking active"
            else -> "GPS ready"
        }
    }

    fun startTracking(intervalMs: Long = 1000L, gpsEnabled: Boolean = true, onUpdate: ((GpsPoint) -> Unit)? = null) {
        updateCallback = onUpdate
        this.intervalMs = intervalMs.coerceIn(250L, 10_000L)
        refreshAvailability()
        if (!gpsEnabled) {
            stopTracking(clearCallback = false)
            _status.value = "GPS disabled"
            return
        }
        if (!hasLocationPermission()) {
            stopTracking(clearCallback = false)
            _status.value = "GPS permission required"
            return
        }
        if (!isLocationEnabled()) {
            stopTracking(clearCallback = false)
            _status.value = "Turn on Location services to use GPS"
            return
        }

        stopTracking(clearCallback = false)
        startLocationManagerFallback()
    }

    private fun startLocationManagerFallback() {
        val manager = locationManager ?: return
        try {
            if (!hasLocationPermission()) {
                _status.value = "GPS permission required"
                return
            }
            val hasGps = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val hasNetwork = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!hasGps && !hasNetwork) {
                _status.value = "Location services disabled"
                return
            }
            @Suppress("MissingPermission")
            if (hasGps) {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    intervalMs,
                    0.2f,
                    fallbackListener,
                    Looper.getMainLooper()
                )
            }
            @Suppress("MissingPermission")
            if (hasNetwork) {
                manager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    intervalMs,
                    0.2f,
                    fallbackListener,
                    Looper.getMainLooper()
                )
            }
            listenerRegistered = true
            _isTrackingLocation.value = true
            _gpsAvailable.value = true
            _status.value = "Location active • " + this.intervalMs + " ms"
        } catch (_: Exception) {
            _isTrackingLocation.value = false
            _status.value = "Unable to start GPS"
        }
    }

    fun stopTracking(clearCallback: Boolean = true) {
        runCatching { locationManager?.removeUpdates(fallbackListener) }
        listenerRegistered = false
        _isTrackingLocation.value = false
        if (clearCallback) updateCallback = null
        refreshAvailability()
    }

    private fun processLocation(location: Location) {
        if (!listenerRegistered && !_isTrackingLocation.value) return
        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val minimumGap = (intervalMs * 0.3f).toLong().coerceAtLeast(150L)
        if (nowElapsed - lastProcessedElapsedMs < minimumGap) return
        lastProcessedElapsedMs = nowElapsed
        val point = GpsPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else 0.0,
            accuracy = if (location.hasAccuracy()) location.accuracy else 12.0f,
            speed = if (location.hasSpeed()) location.speed else 0f,
            bearing = if (location.hasBearing()) location.bearing else 0f,
            timestamp = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
        )
        _currentLocation.value = point
        updateCallback?.invoke(point)
    }
}
