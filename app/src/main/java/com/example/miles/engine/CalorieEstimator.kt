package com.example.miles.engine

/**
 * Calorie maths — always derived from real measured data (elapsed time from the
 * real timer, real distance, real step counts, the user's real body weight).
 * Nothing here invents values: every input is something the device measured or
 * the user entered.
 *
 * Formulae:
 *  - Activity: `kcal/min = MET × 3.5 × kg / 200`, cross-checked against
 *    per-metre and per-step costs (both scaled by body weight vs a 70 kg
 *    reference), taking the larger of the two like standard trackers do.
 *  - Everyday activity: per-step walking energy at ~100 steps/min.
 */
object CalorieEstimator {

    /** Keeps body weight in a sane, safe range (kg). */
    fun clampWeight(weightKg: Int): Double = weightKg.coerceIn(30, 250).toDouble()

    /**
     * Calories for a tracked activity.
     * [met] comes from the real activity type (ActivityType.metScore).
     */
    fun activityCalories(
        met: Double,
        weightKg: Int,
        elapsedSeconds: Long,
        distanceMeters: Double,
        steps: Int,
        perMetreRate: Double,
        perStepRate: Double = 0.04
    ): Int {
        val kg = clampWeight(weightKg)
        val timeCals = (met * kg * 3.5 / 200.0) * (elapsedSeconds / 60.0)
        // Distance/step costs are calibrated for 70 kg — scale to the real user.
        val weightFactor = kg / 70.0
        val distCals = perMetreRate * distanceMeters * weightFactor
        val stepCals = steps * perStepRate * weightFactor
        val result = maxOf(timeCals, distCals + stepCals)
        return result.toInt().coerceAtLeast(if (elapsedSeconds > 10) 1 else 0)
    }

    /**
     * Everyday active calories from the real step count that wasn't part of a
     * recorded workout. Walking MET (3.5) at ~100 steps/min gives roughly
     * 400–480 kcal per 10k steps at 70 kg.
     */
    fun dailyActiveFromSteps(steps: Int, weightKg: Int): Int {
        if (steps <= 0) return 0
        val kcalPerStep = 3.5 * 3.5 * clampWeight(weightKg) / 200.0 / 100.0
        return (steps * kcalPerStep).toInt().coerceAtLeast(0)
    }
}
