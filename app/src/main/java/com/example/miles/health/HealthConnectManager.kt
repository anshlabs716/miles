package com.example.miles.health

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

enum class HealthConnectAvailability {
    AVAILABLE,
    UNAVAILABLE,
    PROVIDER_UPDATE_REQUIRED
}

enum class HealthConnectConnectionState {
    CONNECTED,
    AVAILABLE_NOT_PERMITTED,
    UNAVAILABLE,
    PROVIDER_UPDATE_REQUIRED
}

/**
 * Optional Health Connect bridge. Every call is defensive so MILES continues
 * working on devices without a provider, including custom and de-Googled ROMs.
 */
class HealthConnectManager(context: Context) {
    private val appContext = context.applicationContext

    val availability: HealthConnectAvailability
        get() = runCatching {
            availabilityFromSdkStatus(HealthConnectClient.getSdkStatus(appContext))
        }.getOrDefault(HealthConnectAvailability.UNAVAILABLE)

    /** MILES only requests and reads the two record types it currently uses. */
    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(CyclingPedalingCadenceRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ElevationGainedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(FloorsClimbedRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(PowerRecord::class),
        HealthPermission.getReadPermission(RespiratoryRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(SpeedRecord::class),
        HealthPermission.getReadPermission(StepsCadenceRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    private fun clientOrNull(): HealthConnectClient? {
        if (availability != HealthConnectAvailability.AVAILABLE) return null
        return runCatching { HealthConnectClient.getOrCreate(appContext) }.getOrNull()
    }

    suspend fun connectionState(): HealthConnectConnectionState = when (availability) {
        HealthConnectAvailability.UNAVAILABLE -> HealthConnectConnectionState.UNAVAILABLE
        HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED -> HealthConnectConnectionState.PROVIDER_UPDATE_REQUIRED
        HealthConnectAvailability.AVAILABLE -> {
            val granted = runCatching {
                clientOrNull()?.permissionController?.getGrantedPermissions().orEmpty()
            }.getOrDefault(emptySet())
            if (granted.containsAll(permissions)) {
                HealthConnectConnectionState.CONNECTED
            } else {
                HealthConnectConnectionState.AVAILABLE_NOT_PERMITTED
            }
        }
    }

    /** Returns null when Android cannot provide a safe settings destination. */
    fun manageDataIntent(): Intent? {
        if (availability != HealthConnectAvailability.AVAILABLE) return null
        return runCatching { HealthConnectClient.getHealthConnectManageDataIntent(appContext) }.getOrNull()
    }

    suspend fun readSteps(start: Instant, end: Instant): Long {
        val hc = clientOrNull() ?: return 0L
        return runCatching {
            var total = 0L
            var page = hc.readRecords(
                ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end))
            )
            total += page.records.sumOf { it.count }
            while (page.pageToken != null) {
                page = hc.readRecords(
                    ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end), pageToken = page.pageToken)
                )
                total += page.records.sumOf { it.count }
            }
            total
        }.getOrDefault(0L)
    }

    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseSessionRecord> {
        val hc = clientOrNull() ?: return emptyList()
        return runCatching {
            val records = mutableListOf<ExerciseSessionRecord>()
            var page = hc.readRecords(
                ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, end))
            )
            records += page.records
            while (page.pageToken != null) {
                page = hc.readRecords(
                    ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, end), pageToken = page.pageToken)
                )
                records += page.records
            }
            records
        }.getOrDefault(emptyList())
    }

    companion object {
        fun availabilityFromSdkStatus(sdkStatus: Int): HealthConnectAvailability = when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.PROVIDER_UPDATE_REQUIRED
            else -> HealthConnectAvailability.UNAVAILABLE
        }
    }
}
