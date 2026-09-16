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
    suspend fun saveActivity(activity: ActivityEntity) = activityDao.insertActivity(activity)
    suspend fun updateActivity(activity: ActivityEntity) = activityDao.updateActivity(activity)
    suspend fun moveToTrash(id: String) = activityDao.moveToTrash(id)
    suspend fun restoreFromTrash(id: String) = activityDao.restoreFromTrash(id)
    suspend fun permanentlyDelete(id: String) = activityDao.permanentlyDelete(id)
    suspend fun emptyTrash() = activityDao.emptyTrash()
    suspend fun saveRoute(route: SavedRouteEntity) = routeDao.insertRoute(route)
    suspend fun updateRoute(route: SavedRouteEntity) = routeDao.updateRoute(route)
    suspend fun deleteRoute(id: String) = routeDao.deleteRoute(id)

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
    suspend fun savePrivacyZone(zone: PrivacyZoneEntity) = privacyZoneDao.insertZone(zone)
    suspend fun deletePrivacyZone(id: String) = privacyZoneDao.deleteZone(id)
    suspend fun saveGoal(goal: GoalEntity) = goalDao.insertGoal(goal)
    suspend fun deleteGoal(id: String) = goalDao.deleteGoal(id)

    companion object {
        fun parsePoints(json: String): List<GpsPoint> {
            if (json.isBlank() || json == "[]") return emptyList()
            return runCatching {
                val array = JSONArray(json)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(GpsPoint(
                            latitude = obj.getDouble("lat"), longitude = obj.getDouble("lng"),
                            altitude = obj.optDouble("alt", 0.0), accuracy = obj.optDouble("acc", 0.0).toFloat(),
                            speed = obj.optDouble("spd", 0.0).toFloat(), bearing = obj.optDouble("brg", 0.0).toFloat(),
                            timestamp = obj.optLong("ts", 0L)
                        ))
                    }
                }
            }.getOrDefault(emptyList())
        }
        fun pointsToJson(points: List<GpsPoint>): String {
            val array = JSONArray()
            points.forEach { p ->
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
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val type = runCatching { WaypointType.valueOf(obj.optString("type", WaypointType.CUSTOM.name)) }.getOrDefault(WaypointType.CUSTOM)
                        add(Waypoint(obj.optString("id", UUID.randomUUID().toString()), obj.getString("name"), obj.getDouble("lat"), obj.getDouble("lng"), type, obj.optString("notes", ""), obj.optLong("ts", System.currentTimeMillis())))
                    }
                }
            }.getOrDefault(emptyList())
        }
        fun waypointsToJson(waypoints: List<Waypoint>): String {
            val array = JSONArray()
            waypoints.forEach { w -> array.put(JSONObject().apply { put("id", w.id); put("name", w.name); put("lat", w.latitude); put("lng", w.longitude); put("type", w.type.name); put("notes", w.notes); put("ts", w.timestamp) }) }
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
        return points.filter { point -> zones.none { zone -> calculateDistanceMeters(point.latitude, point.longitude, zone.latitude, zone.longitude) <= zone.radiusMeters } }
    }

    suspend fun exportActivityAsJson(activityId: String, password: String? = null): String = withContext(Dispatchers.IO) {
        val act = activityDao.getActivityByIdOnce(activityId) ?: return@withContext ""
        val points = filterPointsWithPrivacyZones(parsePoints(act.routePointsJson))
        val root = JSONObject().apply {
            put("format", "MILES_ACTIVITY_NATIVE"); put("version", 1); put("exportedAt", System.currentTimeMillis())
            put("activity", JSONObject().apply {
                put("id", act.id); put("title", act.title); put("type", act.activityType); put("startTime", act.startTime); put("endTime", act.endTime)
                put("durationSeconds", act.durationSeconds); put("distanceMeters", act.distanceMeters); put("steps", act.steps); put("avgPaceSecPerKm", act.avgPaceSecPerKm)
                put("avgSpeedKmh", act.avgSpeedKmh); put("elevationGainM", act.elevationGainM); put("calories", act.calories); put("avgHeartRate", act.avgHeartRate)
                put("points", JSONArray(pointsToJson(points))); put("waypoints", JSONArray(act.waypointsJson)); put("notes", act.notes)
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
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<gpx version=\"1.1\" creator=\"MILES\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
            append("<metadata><name>${xmlEscape(act.title)}</name><time>${sdf.format(Date(act.startTime))}</time></metadata>\n<trk><name>${xmlEscape(act.title)}</name><type>${xmlEscape(act.activityType)}</type><trkseg>\n")
            points.forEach { p -> append("<trkpt lat=\"${p.latitude}\" lon=\"${p.longitude}\"><ele>${p.altitude}</ele><time>${sdf.format(Date(p.timestamp))}</time></trkpt>\n") }
            append("</trkseg></trk></gpx>")
        }
    }

    suspend fun exportActivityAsTcx(activityId: String): String = withContext(Dispatchers.IO) {
        val act = activityDao.getActivityByIdOnce(activityId) ?: return@withContext ""
        val points = filterPointsWithPrivacyZones(parsePoints(act.routePointsJson))
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<TrainingCenterDatabase xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\" xmlns:ns2=\"http://www.garmin.com/xmlschemas/UserProfile/v2\" xmlns:ns3=\"http://www.garmin.com/xmlschemas/ActivityExtension/v2\">\n")
            append("<Activities><Activity Sport=\"${xmlEscape(act.activityType)}\"><Id>${sdf.format(Date(act.startTime))}</Id><Lap StartTime=\"${sdf.format(Date(act.startTime))}\">\n")
            append("<TotalTimeSeconds>${act.durationSeconds}</TotalTimeSeconds><DistanceMeters>${act.distanceMeters}</DistanceMeters><Calories>${act.calories.toInt()}</Calories><AverageHeartRateBpm><Value>${act.avgHeartRate}</Value></AverageHeartRateBpm><Track>\n")
            points.forEach { p -> append("<Trackpoint><Time>${sdf.format(Date(p.timestamp))}</Time><Position><LatitudeDegrees>${p.latitude}</LatitudeDegrees><LongitudeDegrees>${p.longitude}</LongitudeDegrees></Position><AltitudeMeters>${p.altitude}</AltitudeMeters></Trackpoint>\n") }
            append("</Track></Lap></Activity></Activities></TrainingCenterDatabase>")
        }
    }

    suspend fun exportActivitiesAsCsv(): String = withContext(Dispatchers.IO) {
        val acts = activityDao.getAllActivitiesOnce()
        buildString {
            append("ID,Title,ActivityType,StartTime,EndTime,DurationSeconds,DistanceMeters,Steps,AvgPaceSecPerKm,AvgSpeedKmh,ElevationGainM,Calories,AvgHeartRate,Notes\n")
            acts.forEach { a ->
                append(listOf(a.id, a.title, a.activityType, a.startTime, a.endTime, a.durationSeconds, a.distanceMeters, a.steps, a.avgPaceSecPerKm, a.avgSpeedKmh, a.elevationGainM, a.calories, a.avgHeartRate, a.notes).joinToString(",") { csvEscape(it.toString()) })
                append('\n')
            }
        }
    }

    suspend fun exportActivitiesAsJson(): String = withContext(Dispatchers.IO) {
        val array = JSONArray()
        activityDao.getAllActivitiesOnce().forEach { a ->
            array.put(JSONObject().apply { put("id", a.id); put("title", a.title); put("activityType", a.activityType); put("startTime", a.startTime); put("endTime", a.endTime); put("durationSeconds", a.durationSeconds); put("distanceMeters", a.distanceMeters); put("steps", a.steps); put("avgPaceSecPerKm", a.avgPaceSecPerKm); put("avgSpeedKmh", a.avgSpeedKmh); put("elevationGainM", a.elevationGainM); put("calories", a.calories); put("avgHeartRate", a.avgHeartRate); put("routePoints", JSONArray(a.routePointsJson)); put("notes", a.notes) })
        }
        array.toString(2)
    }

    private fun csvEscape(value: String): String = if (value.contains(',') || value.contains('"') || value.contains('\n')) "\"${value.replace("\"", "\"\"")}\"" else value
    private fun xmlEscape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")

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
        val raw = parsePoints(act.routePointsJson); if (raw.isEmpty()) return@withContext Pair(false, "No GPS points to repair")
        var fixed = 0; val repaired = mutableListOf<GpsPoint>(); var prev: GpsPoint? = null
        raw.forEach { p ->
            if (p.latitude !in -90.0..90.0 || p.longitude !in -180.0..180.0) { fixed++; return@forEach }
            if (prev != null) {
                val dist = calculateDistanceMeters(prev!!.latitude, prev!!.longitude, p.latitude, p.longitude)
                val dt = ((p.timestamp - prev!!.timestamp).coerceAtLeast(1000L)) / 1000.0
                if (dist > 50 && dist / dt > 45) { fixed++; return@forEach }
            }
            val ts = if (prev != null && p.timestamp <= prev!!.timestamp) { fixed++; prev!!.timestamp + 2000 } else p.timestamp
            val good = p.copy(timestamp = ts); repaired += good; prev = good
        }
        var distance = 0.0
        for (i in 0 until repaired.size - 1) distance += calculateDistanceMeters(repaired[i].latitude, repaired[i].longitude, repaired[i + 1].latitude, repaired[i + 1].longitude)
        activityDao.updateActivity(act.copy(routePointsJson = pointsToJson(repaired), distanceMeters = distance, version = act.version + 1))
        Pair(true, "Data repair complete. Fixed $fixed anomalies and recalculated ${(distance / 1000.0).format(2)} km.")
    }

    suspend fun purgePreloadedSeedData() = withContext(Dispatchers.IO) {
        listOf("seed_run_1", "seed_cycle_2", "seed_walk_3", "seed_hike_4").forEach { activityDao.permanentlyDelete(it) }
        routeDao.deleteRoute("sample_golden_gate")
    }
    suspend fun clearAllActivities() = withContext(Dispatchers.IO) { activityDao.clearAll() }
    suspend fun wipeAllData() = withContext(Dispatchers.IO) { activityDao.clearAll() }
    suspend fun exportEncryptedBackup(context: Context, pass: String): Int = withContext(Dispatchers.IO) { activityDao.getAllActivitiesOnce().size }

    suspend fun importFitnessData(fileContent: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        val trimmed = fileContent.trim()
        try {
            when {
                trimmed.startsWith("{") || trimmed.startsWith("[") -> importFromJson(trimmed).let { it to "Successfully imported $it activities from JSON." }
                trimmed.startsWith("<?xml") || trimmed.contains("<gpx", true) -> importFromGpx(trimmed).let { it to "Successfully imported $it activity from GPX track." }
                trimmed.contains(",") || trimmed.contains(";") -> importFromCsv(trimmed).let { it to "Successfully imported $it activities from CSV / Google Fit export." }
                else -> 0 to "Unrecognized format. Supported formats: CSV, Google Fit, JSON, GPX, and TCX."
            }
        } catch (e: Exception) { 0 to "Import error: ${e.localizedMessage ?: "Invalid file structure"}" }
    }

    private suspend fun importFromJson(jsonStr: String): Int {
        var count = 0
        if (jsonStr.startsWith("[")) { val a = JSONArray(jsonStr); for (i in 0 until a.length()) { insertJsonActivity(a.getJSONObject(i)); count++ } }
        else { val root = JSONObject(jsonStr); when { root.has("activity") -> { insertJsonActivity(root.getJSONObject("activity")); count++ }; root.has("activities") -> { val a = root.getJSONArray("activities"); for (i in 0 until a.length()) { insertJsonActivity(a.getJSONObject(i)); count++ } }; else -> { insertJsonActivity(root); count++ } } }
        return count
    }

    private suspend fun insertJsonActivity(obj: JSONObject) {
        val now = System.currentTimeMillis(); val start = obj.optLong("startTime", now - 3600000L); val end = obj.optLong("endTime", start + 1800000L); val duration = obj.optLong("durationSeconds", (end - start) / 1000)
        val distance = obj.optDouble("distanceMeters", obj.optDouble("distanceKm", 0.0) * 1000.0)
        activityDao.insertActivity(ActivityEntity(UUID.randomUUID().toString(), obj.optString("title", "Imported Workout"), obj.optString("type", obj.optString("activityType", ActivityType.RUNNING.name)), start, end, duration, distance, obj.optInt("steps", 0), if (distance > 0) (duration / (distance / 1000.0)).toLong() else 0L, if (duration > 0) (distance / 1000.0) / (duration / 3600.0) else 0.0, obj.optDouble("elevationGainM", 0.0), obj.optDouble("calories", 0.0), obj.optInt("avgHeartRate", 0), obj.optJSONArray("points")?.toString() ?: obj.optJSONArray("routePoints")?.toString() ?: "[]", "[]", obj.optString("notes", "Imported from file"))
    }

    private suspend fun importFromCsv(csvStr: String): Int {
        val lines = csvStr.lines().filter { it.isNotBlank() }; if (lines.isEmpty()) return 0
        val header = lines.first().lowercase(); val data = if (header.contains("date") || header.contains("title") || header.contains("distance") || header.contains("sport") || header.contains("activitytype")) lines.drop(1) else lines
        var imported = 0
        data.forEach { line ->
            val cols = parseCsvLine(line); if (cols.size < 3) return@forEach
            val title = cols.getOrNull(1)?.ifBlank { "Imported Activity" } ?: "Imported Activity"; val distance = cols.getOrNull(6)?.toDoubleOrNull() ?: cols.getOrNull(1)?.toDoubleOrNull()?.times(1000) ?: 0.0; val duration = cols.getOrNull(5)?.toLongOrNull() ?: cols.getOrNull(2)?.toLongOrNull()?.times(60) ?: 1800L; val steps = cols.getOrNull(7)?.toIntOrNull() ?: 0; val end = System.currentTimeMillis() - imported * 86400000L; val start = end - duration * 1000L
            activityDao.insertActivity(ActivityEntity(UUID.randomUUID().toString(), title, cols.getOrNull(2) ?: ActivityType.RUNNING.name, start, end, duration, distance, steps, if (distance > 0) (duration / (distance / 1000)).toLong() else 0L, if (duration > 0) (distance / 1000) / (duration / 3600.0) else 0.0, cols.getOrNull(10)?.toDoubleOrNull() ?: 0.0, cols.getOrNull(11)?.toDoubleOrNull() ?: 0.0, cols.getOrNull(12)?.toIntOrNull() ?: 0, "[]", "[]", "Imported from CSV")); imported++
        }
        return imported
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = mutableListOf<String>(); val current = StringBuilder(); var quoted = false; var i = 0
        while (i < line.length) { val c = line[i]; when { c == '"' && i + 1 < line.length && line[i + 1] == '"' && quoted -> { current.append('"'); i++ }; c == '"' -> quoted = !quoted; c == ',' && !quoted -> { out += current.toString(); current.clear() }; else -> current.append(c) }; i++ }
        out += current.toString(); return out
    }

    private suspend fun importFromGpx(gpxStr: String): Int {
        val matches = Regex("""<trkpt\s+lat=\"([^\"]+)\"\s+lon=\"([^\"]+)\"""").findAll(gpxStr).toList(); if (matches.isEmpty()) return 0
        val start = System.currentTimeMillis() - matches.size * 2000L; val points = matches.mapIndexed { i, m -> GpsPoint(m.groupValues[1].toDouble(), m.groupValues[2].toDouble(), timestamp = start + i * 2000L, speed = 2.8, accuracy = 4f) }
        var dist = 0.0; for (i in 0 until points.size - 1) dist += calculateDistanceMeters(points[i].latitude, points[i].longitude, points[i + 1].latitude, points[i + 1].longitude)
        val duration = points.size * 2L; val name = Regex("""<name>([^<]+)</name>""").find(gpxStr)?.groupValues?.get(1)?.trim() ?: "GPX Outdoor Route"
        activityDao.insertActivity(ActivityEntity(UUID.randomUUID().toString(), name, ActivityType.RUNNING.name, start, start + duration * 1000L, duration, dist, 0, if (dist > 0) (duration / (dist / 1000)).toLong() else 0L, if (duration > 0) (dist / 1000) / (duration / 3600.0) else 0.0, 0.0, 0.0, 0, pointsToJson(points), "[]", "Imported GPX GPS trace")); return 1
    }
}
