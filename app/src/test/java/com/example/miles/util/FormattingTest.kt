package com.example.miles.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pinned because a tester caught the activity screens rendering a 4:03 pace as
 * `4'3"/km` — the seconds were never padded. Every pace in MILES goes through
 * [formatPaceSecPerKm], so these hold the format steady.
 */
class FormattingTest {

    @Test
    fun `seconds are always two digits`() {
        assertEquals("4'03\"", formatPaceSecPerKm(243.0))   // the reported 4:03 case
        assertEquals("9'05\"", formatPaceSecPerKm(545.0))
        assertEquals("10'00\"", formatPaceSecPerKm(600.0))
        assertEquals("5'09\"", formatPaceSecPerKm(309.0))
    }

    @Test
    fun `whole minutes end in 00`() {
        assertEquals("4'00\"", formatPaceSecPerKm(240.0))
        assertEquals("1'00\"", formatPaceSecPerKm(60.0))
        assertEquals("6'00\"", formatPaceSecPerKm(360.0))
    }

    @Test
    fun `a pace under a minute keeps its zero minutes`() {
        assertEquals("0'45\"", formatPaceSecPerKm(45.0))
    }

    @Test
    fun `a fast pace does not round the seconds up into the next minute`() {
        assertEquals("3'59\"", formatPaceSecPerKm(239.9))
    }

    @Test
    fun `paces that do not exist show as unknown instead of 0'00`() {
        // 0 s/km would mean infinite speed, so it must never be displayed as a pace.
        assertEquals("--:--", formatPaceSecPerKm(0.0))
        assertEquals("--:--", formatPaceSecPerKm(-1.0))
        assertEquals("--:--", formatPaceSecPerKm(Double.NaN))
        assertEquals("--:--", formatPaceSecPerKm(Double.POSITIVE_INFINITY))
        // Over an hour per kilometre is a stop, not a pace.
        assertEquals("--:--", formatPaceSecPerKm(3601.0))
    }

    @Test
    fun `the slowest pace still shown is exactly one hour`() {
        assertEquals("60'00\"", formatPaceSecPerKm(3600.0))
    }
}
