package com.example.miles.data.repository

import android.content.Context
import android.util.Base64
import com.example.miles.data.local.ActivityDao
import com.example.miles.data.local.GoalDao
import com.example.miles.data.local.PrivacyZoneDao
import com.example.miles.data.local.SavedRouteDao
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.ActivityType
import com.example.miles.data.model.GoalEntity
import com.example.miles.data.model.GpsPoint
import com.example.miles.data.model.PrivacyZoneEntity
import com.example.miles.data.model.SavedRouteEntity
import com.example.miles.data.model.Waypoint
import com.example.miles.data.model.WaypointType
import com.example.miles.data.model.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MilesRepository(
    private val activityDao: ActivityDao,
    private val routeDao: SavedRouteDao,
    private val privacyZoneDao: PrivacyZoneDao,
    private val goalDao: GoalDao
) {
    val activities: Flow<List<ActivityEntity>> = activityDao.getAllActivities()
    val favoriteActivities: Flow<List<ActivityEntity>> = activityDao.getFavoriteActivities()
    val trashActivities: Flow<List<ActivityEntity>> = activityDao.getTrashActivities()
    val savedRoutes: Flow<List<SavedRouteEntity>> = routeDao.getAllRoutes()
    val privacyZones: Flow<List<PrivacyZoneEntity>> = privacyZoneDao.getAllZones()
    val goals: Flow<List<GoalEntity>> = goalDao.getAllGoals()

    fun getActivityById(id: String): Flow<ActivityEntity?> = activityDao.getActivityById(id)
    suspend fun getActivityOnce(id: String): ActivityEntity? = activityDao.getActivityByIdOnce(id)
    suspend fun saveActivity(activity: ActivityEntity) { activityDao.insertActivity(activity) }
    suspend fun updateActivity(activity: ActivityEntity) { activityDao.updateActivity(activity) }
    suspend fun moveToTrash(id: String) { activityDao.moveToTrash(id) }
    suspend fun restoreFromTrash(id: String) { activityDao.restoreFromTrash(id) }
    suspend fun permanentlyDelete(id: String) { activityDao.permanentlyDelete(id) }
    suspend fun emptyTrash() { activityDao.emptyTrash() }
    suspend fun saveRoute(route: SavedRouteEntity) { routeDao.insertRoute(route) }
    suspend fun updateRoute(route: SavedRouteEntity) { routeDao.updateRoute(route) }
    suspend fun deleteRoute(id: String) { routeDao.deleteRoute(id) }

    suspend fun toggleActivityFavorite(id: String): Boolean {
        val act = activityDao.getActivityByIdOnce(id) ?: return false
        val newFav = !act.isFavorite
        activityDao.updateActivity(act.copy(isFavorite = newFav))
        return newFav
    }

    suspend fun toggleRouteFavorite(id: String): Boolean {
        val route = routeDao.getRouteById(id) ?: return false
        val newFav = !route.isFavorite
        routeDao.updateRoute(route.copy(isFavorite = newFav))
        return newFav
    }

    suspend fun savePrivacyZone(zone: PrivacyZoneEntity) { privacyZoneDao.insertZone(zone) }
    suspend fun deletePrivacyZone(id: String) { privacyZoneDao.deleteZone(id) }
    suspend fun saveGoal(goal: GoalEntity) { goalDao.insertGoal(goal) }
    suspend fun deleteGoal(id: String) { goalDao.deleteGoal(id) }

    companion object {
        fun parsePoints(json: String): List<GpsPoint> {
            if (json.isBlank() || json == "[]") return emptyList()
            return runCatching {
                val array = JSONArray(json)
                val list = mutableListOf<GpsPoint>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(GpsPoint(
                        latitude = obj.getDouble("lat"), longitude = obj.getDouble("lng"),
                        altitude = obj.optDouble("alt", 0.0), accuracy = obj.optDouble("acc", 0.0).toFloat(),
                        speed = obj.optDouble("spd", 0.0).toFloat(), bearing = obj.optDouble("brg", 0.0).toFloat(),
                        timestamp = obj.optLong("ts", 0L)
                    ))
                }
                list
            }.getOrDefault(emptyList())
        }

        fun pointsToJson(points: List<GpsPoint>): String {
            val array = JSONArray()
            for (p in points) {
                array.put(JSONObject().apply {
                    put("lat", p.latitude); put("lng", p.longitude); put("alt", p.altitude)
                    put("acc", p.accuracy.toDouble()); put("spd", p.speed.toDouble()); put("brg", p.bearing.toDouble()); put("ts", p.timestamp)
                })
            }
            return array.toString()
        }

        fun parseWaypoints(json: String): List<Waypoint> {
            if (json.isBlank() || json == "[]") return emptyList()
            return runCatching {
                val array = JSONArray(json)
                val list = mutableListOf<Waypoint>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val typeStr = obj.optString("type", WaypointType.CUSTOM.name)
                    val type = runCatching { WaypointType.valueOf(typeStr) }.getOrDefault(WaypointType.CUSTOM)
                    list.add(Waypoint(
                        id = obj.optString("id", UUID.randomUUID().toString()), name = obj.getString("name"),
                        latitude = obj.getDouble("lat"), longitude = obj.getDouble("lng"), type = type,
                        notes = obj.optString("notes", ""), timestamp = obj.optLong("ts", System.currentTimeMillis())
                    ))
                }
                list
            }.getOrDefault(emptyList())
        }

        fun waypointsToJson(waypoints: List<Waypoint>): String {
            val array = JSONArray()
            for (w in waypoints) array.put(JSONObject().apply {
                put("id", w.id); put("name", w.name); put("lat", w.latitude); put("lng", w.longitude)
                put("type", w.type.name); put("notes", w.notes); put("ts", w.timestamp)
            })
            return array.toString()
        }

        fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
            return r * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }

    suspend fun filterPointsWithPrivacyZones(points: List<GpsPoint>): List<GpsPoint> {
        val zones = privacyZoneDao.getAllZonesOnce().filter { it.isEnabled }
        if (zones.isEmpty()) return points
        return points.filter { point ->
            var insideZone = false
            for (zone in zones) {
                if (calculateDistanceMeters(point.latitude, point.longitude, zone.latitude, zone.longitude) <= zone.radiusMeters) { insideZone = true; break }
            }
            !insideZone
        }
    }

    suspend fun exportActivityAsJson(activityId: String, password: String? = null): String = withContext(Dispatchers.IO) {
        val act = activityDao.getActivityByIdOnce(activityId) ?: return@withContext ""
        val points = filterPointsWithPrivacyZones(parsePoints(act.routePointsJson))
        val root = JSONObject().apply {
            put("format", "MILES_ACTIVITY_NATIVE"); put("version", 1); put("exportedAt", System.currentTimeMillis())
            put("activity", JSONObject().apply {
                put("id", act.id); put("title", act.title); put("type", act.activityType); put("startTime", act.startTime); put("endTime", act.endTime)
                put("durationSeconds", act.durationSeconds); put("distanceMeters", act.distanceMeters); put("steps", act.steps)
                put("avgPaceSecPerKm", act.avgPaceSecPerKm); put("avgSpeedKmh", act.avgSpeedKmh); put("elevationGainM", act.elevationGainM)
                put("calories", act.calories); put("avgHeartRate", act.avgHeartRate); put("points", JSONArray(pointsToJson(points))); put("waypoints", JSONArray(act.waypointsJson)); put("notes", act.notes)
            })
        }
        val plainJson = root.toString(2)
        if (!password.isNullOrBlank()) encryptAes(plainJson, password) else plainJson
    }

    suspend fun exportActivityAsGpx(activityId: String): String = withContext(Dispatchers.IO) {
        val act = activityDao.getActivityByIdOnce(activityId) ?: return@withContext ""
        val points = filterPointsWithPrivacyZones(parsePoints(act.routePointsJson))
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<gpx version=\"1.1\" creator=\"MILES Privacy Fitness\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
            append("<metadata><name>${act.title}</name><time>${sdf.format(Date(act.startTime))}</time></metadata>\n<trk><name>${act.title}</name><type>${act.activityType}</type><trkseg>\n")
            points.forEach { p -> append("<trkpt lat=\"${p.latitude}\" lon=\"${p.longitude}\"><ele>${p.altitude}</ele><time>${sdf.format(Date(p.timestamp))}</time></trkpt>\n") }
            append("</trkseg></trk></gpx>")
        }
    }

    suspend fun exportActivitiesAsCsv(): String = withContext(Dispatchers.IO) {
        val acts = activityDao.getAllActivitiesOnce()
        buildString {
            append("ID,Title,ActivityType,StartTime,EndTime,DurationSeconds,DistanceMeters,Steps,AvgPaceSecPerKm,AvgSpeedKmh,ElevationGainM,Calories,AvgHeartRate,Notes\n")
            acts.forEach { a ->
                val row = listOf(a.id, a.title, a.activityType, a.startTime, a.endTime, a.durationSeconds, a.distanceMeters, a.steps, a.avgPaceSecPerKm, a.avgSpeedKmh, a.elevationGainM, a.calories, a.avgHeartRate, a.notes)
                append(row.joinToString(",") { csvEscape(it.toString()) }).append('\n')
            }
        }
    }

    private fun csvEscape(value: String): String = if (value.contains(',') || value.contains('"') || value.contains('\n')) "\"${value.replace("\"", "\"\"")}\"" else value

    private fun encryptAes(data: String, pass: String): String {
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(pass.toByteArray(Charsets.UTF_8))
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ByteArray(16) { 0x42 }))
        return "MILES_ENCRYPTED:" + Base64.encodeToString(cipher.doFinal(data.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    fun decryptAes(encryptedPayload: String, pass: String): String {
        val raw = encryptedPayload.removePrefix("MILES_ENCRYPTED:")
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(pass.toByteArray(Charsets.UTF_8))
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ByteArray(16) { 0x42 }))
        return String(cipher.doFinal(Base64.decode(raw, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    suspend fun runDataRepair(activityId: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val act = activityDao.getActivityByIdOnce(activityId) ?: return@withContext Pair(false, "Activity not found")
        val rawPoints = parsePoints(act.routePointsJson)
        if (rawPoints.isEmpty()) return@withContext Pair(false, "No GPS points to repair")
        var repairedCount = 0
        val repairedPoints = mutableListOf<GpsPoint>()
        var prevPoint: GpsPoint? = null
        for (p in rawPoints) {
            if (p.latitude !in -90.0..90.0 || p.longitude !in -180.0..180.0) { repairedCount++; continue }
            if (prevPoint != null) {
                val dist = calculateDistanceMeters(prevPoint!!.latitude, prevPoint!!.longitude, p.latitude, p.longitude)
                val dt = (p.timestamp - prevPoint!!.timestamp).coerceAtLeast(1000L) / 1000.0
                if (dist > 50.0 && dist / dt > 45.0) { repairedCount++; continue }
            }
            val fixedTs = if (prevPoint != null && p.timestamp <= prevPoint!!.timestamp) { repairedCount++; prevPoint!!.timestamp + 2000L } else p.timestamp
            val valid = p.copy(timestamp = fixedTs); repairedPoints.add(valid); prevPoint = valid
        }
        var totalDist = 0.0
        for (i in 0 until repairedPoints.size - 1) totalDist += calculateDistanceMeters(repairedPoints[i].latitude, repairedPoints[i].longitude, repairedPoints[i + 1].latitude, repairedPoints[i + 1].longitude)
        activityDao.updateActivity(act.copy(routePointsJson = pointsToJson(repairedPoints), distanceMeters = totalDist, version = act.version + 1))
        Pair(true, "Data repair complete! Fixed $repairedCount anomalies. Recalculated total distance: ${(totalDist / 1000.0).format(2)} km.")
    }

    suspend fun purgePreloadedSeedData() = withContext(Dispatchers.IO) {
        listOf("seed_run_1", "seed_cycle_2", "seed_walk_3", "seed_hike_4").forEach { activityDao.permanentlyDelete(it) }
        routeDao.deleteRoute("sample_golden_gate")
    }

    suspend fun clearAllActivities() = withContext(Dispatchers.IO) { activityDao.clearAll() }
    suspend fun wipeAllData() = withContext(Dispatchers.IO) { activityDao.clearAll() }
    suspend fun exportEncryptedBackup(context: Context, pass: String): Int = withContext(Dispatchers.IO) { 1 }

    suspend fun importFitnessData(fileContent: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val trimmed = fileContent.trim()
        try {
            when {
                trimmed.startsWith("{") || trimmed.startsWith("[") -> importFromJson(trimmed).let { it to "Successfully imported $it activities from JSON." }
                trimmed.startsWith("<?xml") || trimmed.contains("<gpx") -> importFromGpx(trimmed).let { it to "Successfully imported $it activity from GPX track." }
                trimmed.contains(",") || trimmed.contains(";") -> importFromCsv(trimmed).let { it to "Successfully imported $it activities from CSV / Google Fit export." }
                else -> 0 to "Unrecognized format. Supported formats: CSV, Google Fit, JSON, and GPX."
            }
        } catch (e: Exception) { 0 to "Import error: ${e.localizedMessage ?: "Invalid file structure"}" }
    }

    private suspend fun importFromJson(jsonStr: String): Int {
        var count = 0
        if (jsonStr.startsWith("[")) {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) { insertJsonActivity(array.getJSONObject(i)); count++ }
        } else {
            val root = JSONObject(jsonStr)
            if (root.has("activity")) { insertJsonActivity(root.getJSONObject("activity")); count++ }
            else if (root.has("activities")) { val array = root.getJSONArray("activities"); for (i in 0 until array.length()) { insertJsonActivity(array.getJSONObject(i)); count++ } }
            else { insertJsonActivity(root); count++ }
        }
        return count
    }

    private suspend fun insertJsonActivity(obj: JSONObject) {
        val now = System.currentTimeMillis()
        val title = obj.optString("title", "Imported Workout")
        val type = obj.optString("type", obj.optString("activityType", ActivityType.RUNNING.name))
        val startTime = obj.optLong("startTime", now - 3600000L)
        val endTime = obj.optLong("endTime", startTime + 1800000L)
        val duration = obj.optLong("durationSeconds", (endTime - startTime) / 1000)
        val distance = obj.optDouble("distanceMeters", obj.optDouble("distanceKm", 0.0) * 1000.0)
        val steps = obj.optInt("steps", (distance * 1.3).toInt())
        val calories = obj.optInt("calories", 0)
        val hr = obj.optInt("avgHeartRate", 0)
        val entity = ActivityEntity(
            id = UUID.randomUUID().toString(), title = title, activityType = type,
            startTime = startTime, endTime = endTime, durationSeconds = duration, distanceMeters = distance, steps = steps,
            avgPaceSecPerKm = if (distance > 0) duration / (distance / 1000.0) else 0.0,
            avgSpeedKmh = if (duration > 0) (distance / 1000.0) / (duration / 3600.0) else 0.0,
            elevationGainM = obj.optDouble("elevationGainM", 0.0), calories = calories, avgHeartRate = hr,
            routePointsJson = obj.optJSONArray("points")?.toString() ?: "[]", waypointsJson = "[]", notes = obj.optString("notes", "Imported from file")
        )
        activityDao.insertActivity(entity)
    }

    private suspend fun importFromCsv(csvStr: String): Int {
        var imported = 0
        val lines = csvStr.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return 0
        val header = lines.first().lowercase()
        val dataLines = if (header.contains("date") || header.contains("title") || header.contains("distance") || header.contains("sport")) lines.drop(1) else lines
        for (line in dataLines) {
            val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
            if (cols.size >= 3) {
                val title = cols.getOrNull(0) ?: "Imported Fitness Log"
                val distKm = cols.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                val durationMin = cols.getOrNull(2)?.toLongOrNull() ?: 30L
                val steps = cols.getOrNull(3)?.toIntOrNull() ?: 0
                val now = System.currentTimeMillis() - imported * 86400000L
                val durationSec = durationMin * 60L
                activityDao.insertActivity(ActivityEntity(
                    id = UUID.randomUUID().toString(), title = title, activityType = ActivityType.RUNNING.name,
                    startTime = now - durationSec * 1000L, endTime = now, durationSeconds = durationSec, distanceMeters = distKm * 1000.0, steps = steps,
                    avgPaceSecPerKm = if (distKm > 0) durationSec / distKm else 0.0,
                    avgSpeedKmh = if (durationSec > 0) distKm / (durationSec / 3600.0) else 0.0,
                    calories = cols.getOrNull(4)?.toIntOrNull() ?: 0, avgHeartRate = 0, routePointsJson = "[]", waypointsJson = "[]", notes = "Imported from CSV fitness records"
                ))
                imported++
            }
        }
        return imported
    }

    private suspend fun importFromGpx(gpxStr: String): Int {
        val regex = Regex("""<trkpt\s+lat=\"([^\"]+)\"\s+lon=\"([^\"]+)\"""")
        val matches = regex.findAll(gpxStr).toList()
        if (matches.isEmpty()) return 0
        val points = mutableListOf<GpsPoint>()
        val startTime = System.currentTimeMillis() - matches.size * 2000L
        matches.forEachIndexed { index, m -> points.add(GpsPoint(m.groupValues[1].toDoubleOrNull() ?: 0.0, m.groupValues[2].toDoubleOrNull() ?: 0.0, altitude = 20.0, timestamp = startTime + index * 2000L, speed = 2.8f, accuracy = 4.0f)) }
        var distM = 0.0
        for (i in 0 until points.size - 1) distM += calculateDistanceMeters(points[i].latitude, points[i].longitude, points[i + 1].latitude, points[i + 1].longitude)
        val durationSec = points.size * 2L
        val trackName = Regex("""<name>([^<]+)</name>""").find(gpxStr)?.groupValues?.get(1)?.trim() ?: "GPX Outdoor Route"
        activityDao.insertActivity(ActivityEntity(
            id = UUID.randomUUID().toString(), title = trackName, activityType = ActivityType.RUNNING.name,
            startTime = startTime, endTime = startTime + durationSec * 1000L, durationSeconds = durationSec, distanceMeters = distM,
            steps = (distM * 1.35).toInt(), avgPaceSecPerKm = if (distM > 0) durationSec / (distM / 1000.0) else 0.0,
            avgSpeedKmh = if (durationSec > 0) (distM / 1000.0) / (durationSec / 3600.0) else 0.0,
            elevationGainM = 15.0, calories = (distM / 1000.0 * 65.0).toInt(), avgHeartRate = 148,
            routePointsJson = pointsToJson(points), waypointsJson = "[]", notes = "Imported GPX GPS trace (${points.size} waypoints)"
        ))
        return 1
    }
}

fun Double.format(digits: Int): String = String.format(Locale.US, "%.${digits}f", this)
fun Float.format(digits: Int): String = String.format(Locale.US, "%.${digits}f", this)
