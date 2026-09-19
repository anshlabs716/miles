package com.example.miles.health

import androidx.health.connect.client.HealthConnectClient
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectManagerTest {
    @Test
    fun `available sdk status is available`() {
        assertEquals(
            HealthConnectAvailability.AVAILABLE,
            HealthConnectManager.availabilityFromSdkStatus(HealthConnectClient.SDK_AVAILABLE)
        )
    }

    @Test
    fun `unavailable sdk status is unavailable`() {
        assertEquals(
            HealthConnectAvailability.UNAVAILABLE,
            HealthConnectManager.availabilityFromSdkStatus(HealthConnectClient.SDK_UNAVAILABLE)
        )
    }

    @Test
    fun `provider update required sdk status is surfaced`() {
        assertEquals(
            HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED,
            HealthConnectManager.availabilityFromSdkStatus(
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
            )
        )
    }
}
