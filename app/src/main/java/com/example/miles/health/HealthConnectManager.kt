package com.example.miles.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

/**
 * Health Connect bridge used for importing/syncing fitness data without requiring Google Fit.
 * The app still needs the user to grant the requested Health Connect permissions.
 */
class HealthConnectManager(context: Context) {
    private val appContext = context.applicationContext
    private val client: HealthConnectClient? = runCatching {
        if (HealthConnectClient.getSdkStatus(appContext) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(appContext)
        } else null
    }.getOrNull()

    val isAvailable: Boolean get() = client != null

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    suspend fun grantedPermissions(): Set<String> =
        client?.permissionController?.getGrantedPermissions().orEmpty()

    fun permissionController() = client?.permissionController

    suspend fun readSteps(start: Instant, end: Instant): Long {
        val hc = client ?: return 0L
        var total = 0L
        var page = hc.readRecords(
            ReadRecordsRequest(
                StepsRecord::class,
                TimeRangeFilter.between(start, end)
            )
        )
        total += page.records.sumOf { it.count }
        while (page.pageToken != null) {
            page = hc.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    TimeRangeFilter.between(start, end),
                    pageToken = page.pageToken
                )
            )
            total += page.records.sumOf { it.count }
        }
        return total
    }

    suspend fun readExerciseSessions(start: Instant, end: Instant): List<ExerciseSessionRecord> {
        val hc = client ?: return emptyList()
        val records = mutableListOf<ExerciseSessionRecord>()
        var page = hc.readRecords(
            ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(start, end))
        )
        records += page.records
        while (page.pageToken != null) {
            page = hc.readRecords(
                ReadRecordsRequest(
                    ExerciseSessionRecord::class,
                    TimeRangeFilter.between(start, end),
                    pageToken = page.pageToken
                )
            )
            records += page.records
        }
        return records
    }
}
