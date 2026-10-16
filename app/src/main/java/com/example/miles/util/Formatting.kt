package com.example.miles.util

import java.util.Locale

/**
 * Formats a pace (seconds per kilometre) the way every screen in MILES shows it:
 * `243.0 -> 4'03"`, with the seconds always two digits so a pace never renders
 * as `4'3"/km`.
 *
 * Paces that do not exist — nothing recorded, zero, or over an hour — render as
 * `--:--` instead of a fake `0'00"`, because 0 s/km would mean infinite speed.
 */
fun formatPaceSecPerKm(secPerKm: Double): String {
    if (!secPerKm.isFinite() || secPerKm <= 0.0 || secPerKm > 3600.0) return "--:--"
    val mins = (secPerKm / 60).toInt()
    val secs = (secPerKm % 60).toInt()
    return String.format(Locale.US, "%d'%02d\"", mins, secs)
}
