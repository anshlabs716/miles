package com.example.miles.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.miles.data.local.MilesDatabase
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.model.WaypointType
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
class FitnessDataImportTest {

    private lateinit var db: MilesDatabase
    private lateinit var repo: MilesRepository

    private val start = 1_700_000_000_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), MilesDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = MilesRepository(db.activityDao(), db.savedRouteDao(), db.privacyZoneDao(), db.goalDao())
    }

    @After
    fun tearDown() { db.close() }

    private suspend fun only() = db.activityDao().getAllActivitiesOnce().single()

    @Test
    fun `parsePoints keeps valid points when one entry is malformed`() {
        val json = """[
            {"lat":37.1,"lng":-122.1,"ts":$start},
            {"lat":"N/A","lng":null},
            {"lat":38.0,"lng":-123.0,"ts":${start + 1000}}
        ]""".trimIndent()
        val points = MilesRepository.parsePoints(json)
        assertEquals(2, points.size)
        assertEquals(37.1, points[0].latitude, 1e-9)
        assertEquals(38.0, points[1].latitude, 1e-9)
    }

    @Test
    fun `parsePoints drops out of range and null island coordinates`() {
        val json = """[
            {"lat":999.0,"lng":-122.1},
            {"lat":0,"lng":0},
            {"lat":1.0,"lng":1.0},
            {"lat":91.0,"lng":10.0}
        ]""".trimIndent()
        val points = MilesRepository.parsePoints(json)
        assertEquals(1, points.size)
        assertEquals(1.0, points[0].latitude, 1e-9)
    }

    @Test
    fun `parsePoints keeps valid waypoints when one entry is malformed`() {
        val points = MilesRepository.parseWaypoints("""["nope",{"name":"Water stop","lat":1.0,"lng":2.0,"type":"water"}]""")
        assertEquals(1, points.size)
        assertEquals(WaypointType.WATER, points[0].type)
    }

    @Test
    fun `parsePoints reads geojson coordinate arrays`() {
        val points = MilesRepository.parsePoints("""[[-122.1,37.1,12.5],[-122.2,37.2,13.5]]""")
        assertEquals(2, points.size)
        assertEquals(37.1, points[0].latitude, 1e-9)
        assertEquals(-122.1, points[0].longitude, 1e-9)
        assertEquals(12.5, points[0].altitude, 1e-9)
    }

    @Test
    fun `csv import maps named columns and quoted commas`() = runTest {
        val csv = """
            Title,ActivityType,StartTime,DurationSeconds,DistanceMeters,Steps,Calories,AvgHeartRate,MaxHeartRate
            "Morning ""Sunrise"" Run, Easy",RUNNING,${start},1800,5123.4,6100,410,152,178
        """.trimIndent()

        val (count, _) = repo.importFitnessData(csv)
        assertEquals(1, count)

        val activity = only()
        assertEquals("Morning \"Sunrise\" Run, Easy", activity.title)
        assertEquals(ActivityType.RUNNING.name, activity.activityType)
        assertEquals(start, activity.startTime)
        assertEquals(start + 1_800_000L, activity.endTime)
        assertEquals(1800L, activity.durationSeconds)
        assertEquals(5123.4, activity.distanceMeters, 0.001)
        assertEquals(6100, activity.steps)
        assertEquals(410, activity.calories)
        assertEquals(152, activity.avgHeartRate)
        assertEquals(178, activity.maxHeartRate)
    }

    @Test
    fun `csv import understands distance and duration units`() = runTest {
        val csv = """
            Name,Sport,Distance,Duration
            Tempo run,Running,5.2 km,45:30
        """.trimIndent()

        assertEquals(1, repo.importFitnessData(csv).first)
        val activity = only()
        assertEquals("Tempo run", activity.title)
        assertEquals(5200.0, activity.distanceMeters, 0.5)
        assertEquals(2730L, activity.durationSeconds)
    }

    @Test
    fun `csv import keeps embedded newlines inside quoted fields`() = runTest {
        val csv = "Title,Notes\n\"Run, hard\",\"line1\nline2\""
        assertEquals(1, repo.importFitnessData(csv).first)
        val activity = only()
        assertEquals("Run, hard", activity.title)
        assertEquals("line1\nline2", activity.notes)
    }

    /** The app's own CSV export must re-import without dropping the track or the name. */
    @Test
    fun `csv round trip through the app export preserves fields`() = runTest {
        val original = ActivityEntity(
            id = "act-csv",
            title = "Morning \"Sunrise\" Run, Easy",
            activityType = ActivityType.RUNNING.name,
            startTime = start,
            endTime = start + 1_800_000L,
            durationSeconds = 1800,
            distanceMeters = 5123.4,
            steps = 6100,
            avgPaceSecPerKm = 351.2,
            calories = 410,
            avgHeartRate = 152,
            routePointsJson = """[{"lat":37.1,"lng":-122.1,"alt":10.0,"ts":$start}]""",
            notes = "line1\nline2",
            isFavorite = true
        )
        db.activityDao().insertActivity(original)
        val csv = com.example.miles.data.backup.MilesBackupManager(
            db, MilesPreferences(ApplicationProvider.getApplicationContext())
        ).exportCsv()

        val target = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), MilesDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            val targetRepo = MilesRepository(
                target.activityDao(), target.savedRouteDao(), target.privacyZoneDao(), target.goalDao()
            )
            assertEquals(1, targetRepo.importFitnessData(csv).first)
            val restored = target.activityDao().getAllActivitiesOnce().single()
            assertEquals(original.id, restored.id)
            assertEquals(original.title, restored.title)
            assertEquals(original.startTime, restored.startTime)
            assertEquals(original.durationSeconds, restored.durationSeconds)
            assertEquals(original.distanceMeters, restored.distanceMeters, 0.001)
            assertEquals(original.steps, restored.steps)
            assertEquals(original.calories, restored.calories)
            assertEquals(original.avgPaceSecPerKm, restored.avgPaceSecPerKm, 0.001)
            assertEquals(original.notes, restored.notes)
            assertTrue(restored.isFavorite)
            assertEquals(1, MilesRepository.parsePoints(restored.routePointsJson).size)
        } finally {
            target.close()
        }
    }

    @Test
    fun `json import keeps every numeric field from the file`() = runTest {
        val json = """
            {"format":"MILES_DATA_EXPORT","version":1,"activities":[
              {"id":"a1","title":"Tempo","activityType":"RUNNING","startTime":$start,"endTime":${start + 1800000},
               "durationSeconds":1800,"distanceMeters":5123.4,"steps":6100,"avgPaceSecPerKm":351.2,
               "bestPaceSecPerKm":298.0,"avgSpeedKmh":10.25,"maxSpeedKmh":14.5,"elevationGainM":42.0,
               "elevationLossM":38.0,"calories":410,"avgHeartRate":152,"maxHeartRate":178,
               "routePointsJson":"[{\"lat\":37.1,\"lng\":-122.1,\"ts\":$start}]",
               "notes":"hi","isFavorite":true,"sensorSource":"Built-in GPS"}]}
        """.trimIndent()

        assertEquals(1, repo.importFitnessData(json).first)
        val activity = only()
        assertEquals(1800L, activity.durationSeconds)
        assertEquals(351.2, activity.avgPaceSecPerKm, 0.001)
        assertEquals(298.0, activity.bestPaceSecPerKm, 0.001)
        assertEquals(10.25, activity.avgSpeedKmh, 0.001)
        assertEquals(14.5, activity.maxSpeedKmh, 0.001)
        assertEquals(42.0, activity.elevationGainM, 0.001)
        assertEquals(38.0, activity.elevationLossM, 0.001)
        assertEquals(178, activity.maxHeartRate)
        assertTrue(activity.isFavorite)
        assertEquals(1, MilesRepository.parsePoints(activity.routePointsJson).size)
    }

    @Test
    fun `gpx export writes utc timestamps`() = runTest {
        db.activityDao().insertActivity(
            ActivityEntity(
                id = "g1", title = "Track", startTime = start, endTime = start + 2000L,
                durationSeconds = 2, distanceMeters = 10.0,
                routePointsJson = """[{"lat":1.0,"lng":2.0,"ts":$start}]"""
            )
        )
        val gpx = repo.exportActivityAsGpx("g1")
        val iso = Regex("<time>([^<]+)</time>").find(gpx)?.groupValues?.get(1)
        assertEquals("2023-11-14T22:13:20Z", iso)
    }
}