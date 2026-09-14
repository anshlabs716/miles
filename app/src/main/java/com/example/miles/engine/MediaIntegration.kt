package com.example.miles.engine

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveMediaTrack(
    val title: String,
    val artist: String,
    val appName: String,
    val isPlaying: Boolean = false,
    val positionSeconds: Int = 0,
    val durationSeconds: Int = 0
)

class MediaIntegration(private val context: Context) {
    // Real default: Null until actual system media session is detected
    private val _currentTrack = MutableStateFlow<ActiveMediaTrack?>(null)
    val currentTrack: StateFlow<ActiveMediaTrack?> = _currentTrack.asStateFlow()

    fun togglePlayPause() {
        val cur = _currentTrack.value ?: return
        _currentTrack.value = cur.copy(isPlaying = !cur.isPlaying)
    }

    fun dismiss() {
        _currentTrack.value = null
    }

    fun updateMediaState(track: ActiveMediaTrack?) {
        _currentTrack.value = track
    }
}
