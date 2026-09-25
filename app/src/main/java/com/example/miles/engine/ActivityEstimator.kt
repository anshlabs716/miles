package com.example.miles.engine

/**
 * Everyday activity from real pedometer data — distance and active time.
 *
 * Distance uses the standard anthropometric stride estimate derived from the
 * user's real configured height, so a taller person covers more ground per
 * step. Active minutes are counted from real step events (see
 * PedometerManager.activeMinutesToday), never guessed from totals.
 */
object ActivityEstimator {

    /** Average walking stride in metres for a given height (≈0.415 × height). */
    fun strideLengthMeters(heightCm: Float): Double {
        val heightM = heightCm.coerceIn(120f, 220f) / 100.0
        return (heightM * 0.415).coerceIn(0.35, 1.30)
    }

    /** Real walking distance covered by [steps] steps. */
    fun distanceMetersFromSteps(steps: Int, heightCm: Float): Double =
        if (steps <= 0) 0.0 else steps * strideLengthMeters(heightCm)

    /** Steps that weren't part of a recorded workout (no double counting). */
    fun everydaySteps(totalSteps: Int, workoutSteps: Int): Int =
        (totalSteps - workoutSteps).coerceAtLeast(0)

    /**
     * Everyday distance to add on top of finished workouts: the steps taken
     * outside any recorded workout, converted with the real stride length.
     */
    fun everydayDistanceMeters(totalSteps: Int, workoutSteps: Int, heightCm: Float): Double =
        distanceMetersFromSteps(everydaySteps(totalSteps, workoutSteps), heightCm)
}
