package com.example.miles.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A tester found that MILES still asked for BODY_SENSORS on Android 16, where that
 * permission no longer covers Sensor.TYPE_HEART_RATE — so the sensor stayed
 * unreadable even after it was granted. These pin the API-level split.
 */
class HeartRatePermissionTest {

    @Test
    fun `android 16 and newer require the granular health permission`() {
        assertEquals("android.permission.health.READ_HEART_RATE", heartRatePermission(36))
        assertEquals("android.permission.health.READ_HEART_RATE", heartRatePermission(40))
    }

    @Test
    fun `older systems still use BODY_SENSORS`() {
        assertEquals("android.permission.BODY_SENSORS", heartRatePermission(35))
        assertEquals("android.permission.BODY_SENSORS", heartRatePermission(30))
        assertEquals("android.permission.BODY_SENSORS", heartRatePermission(26)) // minSdk
    }
}
