package com.example.miles.engine

/**
 * Accumulates the heart-rate samples a workout actually received.
 *
 * The saved average and peak come only from these samples. With no sensor
 * reading there is no average and no peak — both stay 0 rather than being
 * guessed from the current value.
 */
class HeartRateTracker {
    private var count = 0
    private var total = 0L
    private var peak = 0

    /** Records one real sensor reading. Values outside 20-240 bpm are ignored. */
    fun record(bpm: Int) {
        if (bpm !in 20..240) return
        count++
        total += bpm
        if (bpm > peak) peak = bpm
    }

    /** Average of recorded samples, or 0 when nothing was recorded. */
    fun average(): Int = if (count > 0) (total / count).toInt() else 0

    /** Highest recorded sample, or 0 when nothing was recorded. */
    fun peak(): Int = peak

    /** Samples recorded so far. */
    fun sampleCount(): Int = count

    /** Called when a new workout starts so stats never leak between workouts. */
    fun reset() {
        count = 0
        total = 0L
        peak = 0
    }
}
