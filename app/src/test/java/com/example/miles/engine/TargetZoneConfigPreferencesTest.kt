package com.example.miles.engine

import com.example.miles.data.local.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** User alert settings → the live target-zone config the alert manager evaluates. */
class TargetZoneConfigPreferencesTest {

    @Test
    fun `default preferences reproduce the previous built-in alert config`() {
        // Existing users must see no behaviour change until they touch the new settings.
        assertEquals(TargetZoneConfig(), TargetZoneConfig().withPreferences(UserPreferences()))
    }

    @Test
    fun `every alert setting is carried into the config`() {
        val prefs = UserPreferences(
            paceAlertEnabled = false,
            paceAlertToleranceSec = 40,
            speedAlertEnabled = true,
            speedAlertMinKmh = 20f,
            speedAlertMaxKmh = 35f,
            hrAlertEnabled = false,
            hrAlertMinBpm = 130,
            hrAlertMaxBpm = 160,
            targetAlertHaptic = false,
            targetAlertVoice = false
        )
        val config = TargetZoneConfig().withPreferences(prefs)

        assertFalse(config.paceAlertEnabled)
        assertEquals(40.0, config.paceToleranceSec, 0.0)
        assertTrue(config.speedAlertEnabled)
        assertEquals(20.0, config.minSpeedKmh!!, 0.0)
        assertEquals(35.0, config.maxSpeedKmh!!, 0.0)
        assertFalse(config.hrAlertEnabled)
        assertEquals(130, config.minHeartRateBpm)
        assertEquals(160, config.maxHeartRateBpm)
        assertFalse(config.hapticEnabled)
        assertFalse(config.voiceEnabled)
    }

    @Test
    fun `target pace chosen in the HUD is never overwritten by settings`() {
        val hudConfig = TargetZoneConfig(targetPaceSecPerKm = 270.0)
        val config = hudConfig.withPreferences(UserPreferences(paceAlertToleranceSec = 10))
        assertEquals(270.0, config.targetPaceSecPerKm!!, 0.0)
    }

    @Test
    fun `inverted ranges are put back in order`() {
        val prefs = UserPreferences(
            hrAlertMinBpm = 170,
            hrAlertMaxBpm = 125,
            speedAlertMinKmh = 18f,
            speedAlertMaxKmh = 9f
        )
        val config = TargetZoneConfig().withPreferences(prefs)
        assertEquals(125, config.minHeartRateBpm)
        assertEquals(170, config.maxHeartRateBpm)
        assertEquals(9.0, config.minSpeedKmh!!, 0.0)
        assertEquals(18.0, config.maxSpeedKmh!!, 0.0)
    }

    @Test
    fun `pace tolerance is clamped to a sane window`() {
        // A corrupted or restored backup value must not disable or spam pace alerts.
        val tooTight = TargetZoneConfig().withPreferences(UserPreferences(paceAlertToleranceSec = 0))
        val tooLoose = TargetZoneConfig().withPreferences(UserPreferences(paceAlertToleranceSec = 999))
        assertEquals(5.0, tooTight.paceToleranceSec, 0.0)
        assertEquals(120.0, tooLoose.paceToleranceSec, 0.0)
    }
}
