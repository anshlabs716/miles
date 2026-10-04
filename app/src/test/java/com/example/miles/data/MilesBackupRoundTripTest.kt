package com.example.miles.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.miles.data.backup.MilesBackupManager
import com.example.miles.data.local.MilesDatabase
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.model.GoalEntity
import com.example.miles.data.model.PrivacyZoneEntity
import com.example.miles.data.model.SavedRouteEntity
import com.example.miles.data.repository.MilesRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MilesBackupRoundTripTest {

    private lateinit var db: MilesDatabase
    private lateinit var prefs: MilesPreferences

    private val start = 1_700_000_000_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MilesDatabase::class.java
        ).allowMainThreadQueries().build()
        prefs = MilesPreferences(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleActivity() = ActivityEntity(
        id = "act-1",
        title = "Morning \"Sunrise\" Run, Easy",
        activityType = ActivityType.RUNNING.name,
        startTime = start,
        endTime = start + 1_800_000L,
        durationSeconds = 1800,
        distanceMeters = 5123.4,
        steps = 6100,
        avgPaceSecPerKm = 351.2,
        bestPaceSecPerKm = 298.0,
        avgSpeedKmh = 10.25,
        maxSpeedKmh = 14.5,
        elevationGainM = 42.0,
        elevationLossM = 38.0,
        calories = 410,
        avgHeartRate = 152,
        maxHeartRate = 178,
        routePointsJson = "[{\"lat\":37.1,\"lng\":-122.1,\"alt\":10.0,\"ts\":$start}]",
        waypointsJson = "[]",
        notes = "line1\nline2",
        isFavorite = true,
        sensorSource = "Built-in GPS"
    )

    @Test
    fun `miles backup restores activities with all fields`() = runTest {
        db.activityDao().insertActivity(sampleActivity())
        val manager = MilesBackupManager(db, prefs)
        val backup = manager.createMilesBackup()

        val target = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), MilesDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            MilesBackupManager(target, prefs).restoreMilesBackup(backup)
            val restored = target.activityDao().getAllActivitiesOnce().single()
            assertEquals("act-1", restored.id)
            assertEquals("Morning \"Sunrise\" Run, Easy", restored.title)
            assertEquals(start, restored.startTime)
            assertEquals(5123.4, restored.distanceMeters, 0.001)
            assertEquals(6100, restored.steps)
            assertTrue(restored.isFavorite)
            assertEquals(1, MilesRepository.parsePoints(restored.routePointsJson).size)
        } finally {
            target.close()
        }
    }

    @Test
    fun `miles backup restores routes zones goals`() = runTest {
        db.savedRouteDao().insertRoute(SavedRouteEntity(id = "r1", title = "Coast", routePointsJson = "[{\"lat\":1.0,\"lng\":2.0,\"ts\":5}]", distanceMeters = 10.0))
        db.privacyZoneDao().insertZone(PrivacyZoneEntity(id = "z1", name = "Home", latitude = 1.0, longitude = 2.0, radiusMeters = 100f))
        db.goalDao().insertGoal(GoalEntity(id = "g1", type = "STEPS", targetValue = 9000.0))
        val manager = MilesBackupManager(db, prefs)
        val backup = manager.createMilesBackup()

        val target = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), MilesDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            MilesBackupManager(target, prefs).restoreMilesBackup(backup)
            assertEquals(1, target.savedRouteDao().getAllRoutesOnce().size)
            assertEquals(1, target.privacyZoneDao().getAllZonesOnce().size)
            assertEquals(1, target.goalDao().getAllGoalsOnce().size)
        } finally {
            target.close()
        }
    }

    /**
     * Long preferences are re-read as Int after a JSON round trip because JSON has one number type.
     * That used to throw ClassCastException inside SharedPreferences and abort the whole restore.
     */
    @Test
    fun `miles backup restores long preferences`() = runTest {
        prefs.setCounterIntervalMs(777L)
        prefs.setTelemetryIntervalMs(8888L)
        prefs.setSensorHardwareControls(refreshRateMs = 9999L)
        prefs.setMoveReminderSettings(enabled = true, intervalMinutes = 17)

        val backup = MilesBackupManager(db, prefs).createMilesBackup()
        prefs.resetAllPreferences()
        MilesBackupManager(db, prefs).restoreMilesBackup(backup)

        val restored = prefs.userPreferences.value
        assertEquals(777L, restored.counterIntervalMs)
        assertEquals(8888L, restored.telemetryIntervalMs)
        assertEquals(9999L, restored.sensorRefreshRateMs)
        assertEquals(17, restored.moveReminderIntervalMinutes)
        assertTrue(restored.moveReminderEnabled)
    }

    @Test
    fun `miles backup restores float preferences`() = runTest {
        prefs.updatePreferences { it.copy(userWeightKg = 81.5f, barometerQnhHpa = 1003.25f) }
        val backup = MilesBackupManager(db, prefs).createMilesBackup()
        prefs.resetAllPreferences()
        MilesBackupManager(db, prefs).restoreMilesBackup(backup)

        val restored = prefs.userPreferences.value
        assertEquals(81.5f, restored.userWeightKg, 0.001f)
        assertEquals(1003.25f, restored.barometerQnhHpa, 0.001f)
    }

    /** A single unreadable entry must not wipe the whole database mid-restore. */
    @Test
    fun `miles backup tolerates malformed entries`() = runTest {
        val backup = """
            {"header":"MILES_BACKUP_V1","format":"MILES_FULL_BACKUP","version":1,"preferences":{},
             "activities":[
               {"id":"ok1","title":"Good","startTime":$start,"endTime":$start,"durationSeconds":10,"distanceMeters":100.0},
               "not-an-object",
               {"id":"ok2","title":"Also good","startTime":$start,"endTime":$start,"durationSeconds":10,"distanceMeters":200.0}
             ]}
        """.trimIndent()

        MilesBackupManager(db, prefs).restoreMilesBackup(backup)
        val titles = db.activityDao().getAllActivitiesOnce().map { it.title }.sorted()
        assertEquals(listOf("Also good", "Good"), titles)
    }

    @Test
    fun `backup preserves steps and calories instead of estimating them`() = runTest {
        // Distance implies ~23k steps and ~333 kcal; the recorded values must win.
        db.activityDao().insertActivity(sampleActivity().copy(id = "a2", steps = 0, calories = 0))
        val backup = MilesBackupManager(db, prefs).createMilesBackup()
        val target = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), MilesDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            MilesBackupManager(target, prefs).restoreMilesBackup(backup)
            val restored = target.activityDao().getAllActivitiesOnce().single()
            assertEquals(0, restored.steps)
            assertEquals(0, restored.calories)
        } finally {
            target.close()
        }
    }
}
