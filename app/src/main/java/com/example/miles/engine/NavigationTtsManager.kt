package com.example.miles.engine

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.miles.data.local.MilesPreferences
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale

class NavigationTtsManager(
    private val context: Context,
    private val preferences: MilesPreferences? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady: Boolean = false
    private var lastAnnouncementTimeMs: Long = 0L
    private var lastSpokenMessage: String = ""

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext, this)
        }.onFailure {
            Log.w("NavigationTtsManager", "TTS initialization failed: ${it.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setSpeechRate(1.02f)
            tts?.setPitch(1.0f)
            isTtsReady = true
        } else {
            isTtsReady = false
            Log.w("NavigationTtsManager", "TextToSpeech init status: $status")
        }
    }

    fun speak(text: String, isHighPriority: Boolean = false) {
        if (!isTtsReady || tts == null || text.isBlank()) return

        val now = System.currentTimeMillis()
        if (!isHighPriority && text == lastSpokenMessage && (now - lastAnnouncementTimeMs) < 6000L) {
            return
        }

        lastSpokenMessage = text
        lastAnnouncementTimeMs = now

        val queueMode = if (isHighPriority) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val utteranceId = "miles_nav_${System.currentTimeMillis()}"

        runCatching {
            tts?.speak(text, queueMode, params, utteranceId)
        }
    }

    fun announceTurnManeuver(instruction: String, distanceMeters: Double) {
        val distanceText = when {
            distanceMeters < 30.0 -> "Turn now"
            distanceMeters < 1000.0 -> "In ${distanceMeters.toInt()} meters"
            else -> "In ${String.format(Locale.US, "%.1f", distanceMeters / 1000.0)} kilometers"
        }
        val speech = if (distanceMeters < 30.0) {
            "$instruction now"
        } else {
            "$distanceText, $instruction"
        }
        speak(speech)
    }

    fun announceRerouting() {
        speak("Off route. Recalculating route.", isHighPriority = true)
    }

    fun announceArrival(destinationName: String = "your destination") {
        speak("You have arrived at $destinationName.", isHighPriority = true)
    }

    fun announceWorkoutAlert(alertMessage: String) {
        speak(alertMessage, isHighPriority = true)
    }

    fun announceIntervalChange(intervalName: String, durationSec: Int, instruction: String) {
        val durationMins = durationSec / 60
        val durationDesc = if (durationMins > 0) "$durationMins minutes" else "$durationSec seconds"
        val message = if (instruction.isNotBlank()) {
            "$intervalName for $durationDesc. $instruction"
        } else {
            "$intervalName phase for $durationDesc"
        }
        speak(message, isHighPriority = true)
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsReady = false
        }
    }
}
