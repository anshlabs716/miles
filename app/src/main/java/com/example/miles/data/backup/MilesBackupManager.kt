package com.example.miles.data.backup

import com.example.miles.data.local.MilesDatabase
import com.example.miles.data.local.MilesPreferences
import com.example.miles.data.model.ActivityEntity
import com.example.miles.data.model.GoalEntity
import com.example.miles.data.model.GpsPoint
import com.example.miles.data.model.PrivacyZoneEntity
import com.example.miles.data.model.SavedRouteEntity
import com.example.miles.data.repository.MilesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MilesBackupManager(
    private val database: MilesDatabase,
    private val preferences: MilesPreferences
) {
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val activities = database.activityDao().getAllActivitiesOnce()
        val routes = database.savedRouteDao().getAllRoutesOnce()
        val zones = database.privacyZoneDao().getAllZonesOnce()
        val goals = database.goalDao().getAllGoalsOnce()
        JSONObject().apply {
            put("format", "MILES_DATA_EXPORT")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("preferences", JSONObject(preferences.exportRawPreferences()))
            put("activities", JSONArray().also { array -> activities.forEach { array.put(activityToJson(it)) } })
            put("savedRoutes", JSONArray().also { array -> routes.forEach { array.put(routeToJson(it)) } })
            put("privacyZones", JSONArray().also { array -> zones.forEach { array.put(zoneToJson(it)) } })
            put("goals", JSONArray().also { array -> goals.forEach { array.put(goalToJson(it)) } })
        }.toString(2)
    }

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val activities = database.activityDao().getAllActivitiesOnce()
        buildString {
            appendLine("ID,Title,ActivityType,StartTime,EndTime,DurationSeconds,DistanceMeters,Steps,AvgPaceSecPerKm,BestPaceSecPerKm,AvgSpeedKmh,MaxSpeedKmh,ElevationGainM,ElevationLossM,Calories,AvgHeartRate,MaxHeartRate,Notes,SensorSource")
            activities.forEach { a ->
                appendLine(listOf(a.id, a.title, a.activityType, a.startTime, a.endTime, a.durationSeconds, a.distanceMeters, a.steps, a.avgPaceSecPerKm, a.bestPaceSecPerKm, a.avgSpeedKmh, a.maxSpeedKmh, a.elevationGainM, a.elevationLossM, a.calories, a.avgHeartRate, a.maxHeartRate, a.notes, a.sensorSource).joinToString(",") { csvEscape(it.toString()) })
            }
        }
    }

    suspend fun exportGpx(): String = withContext(Dispatchers.IO) {
        val activities = database.activityDao().getAllActivitiesOnce()
        val zones = database.privacyZoneDao().getAllZonesOnce()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gpx version="1.1" creator="MILES" xmlns="http://www.topografix.com/GPX/1/1">""")
            activities.forEach { activity ->
                val points = filterPrivacy(MilesRepository.parsePoints(activity.routePointsJson), zones)
                if (points.isEmpty()) return@forEach
                appendLine("  <trk>")
                appendLine("    <name>${xmlEscape(activity.title)}</name>")
                appendLine("    <type>${xmlEscape(activity.activityType)}</type>")
                appendLine("    <trkseg>")
                points.forEach { p ->
                    append("      <trkpt lat=\"${p.latitude}\" lon=\"${p.longitude}\">")
                    append("<ele>${p.altitude}</ele><time>${sdf.format(Date(p.timestamp))}</time></trkpt>")
                    appendLine()
                }
                appendLine("    </trkseg>")
                appendLine("  </trk>")
            }
            appendLine("</gpx>")
        }
    }

    suspend fun exportTcx(): String = withContext(Dispatchers.IO) {
        val activities = database.activityDao().getAllActivitiesOnce()
        val zones = database.privacyZoneDao().getAllZonesOnce()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<TrainingCenterDatabase xmlns="http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2">""")
            appendLine("  <Activities>")
            activities.forEach { activity ->
                val points = filterPrivacy(MilesRepository.parsePoints(activity.routePointsJson), zones)
                if (points.isEmpty()) return@forEach
                appendLine("    <Activity Sport=\"${xmlEscape(activity.activityType)}\">")
                appendLine("      <Id>${sdf.format(Date(activity.startTime))}</Id>")
                appendLine("      <Lap StartTime=\"${sdf.format(Date(activity.startTime))}\">")
                appendLine("        <TotalTimeSeconds>${activity.durationSeconds}</TotalTimeSeconds>")
                appendLine("        <DistanceMeters>${activity.distanceMeters}</DistanceMeters>")
                appendLine("        <Calories>${activity.calories}</Calories>")
                if (activity.avgHeartRate > 0) appendLine("        <AverageHeartRateBpm><Value>${activity.avgHeartRate}</Value></AverageHeartRateBpm>")
                if (activity.maxHeartRate > 0) appendLine("        <MaximumHeartRateBpm><Value>${activity.maxHeartRate}</Value></MaximumHeartRateBpm>")
                appendLine("        <Track>")
                points.forEach { p ->
                    appendLine("          <Trackpoint><Time>${sdf.format(Date(p.timestamp))}</Time><Position><LatitudeDegrees>${p.latitude}</LatitudeDegrees><LongitudeDegrees>${p.longitude}</LongitudeDegrees></Position><AltitudeMeters>${p.altitude}</AltitudeMeters></Trackpoint>")
                }
                appendLine("        </Track></Lap>")
                appendLine("    </Activity>")
            }
            appendLine("  </Activities></TrainingCenterDatabase>")
        }
    }

    suspend fun createMilesBackup(): String = withContext(Dispatchers.IO) {
        JSONObject().apply {
            put("header", "MILES_BACKUP_V1")
            put("format", "MILES_FULL_BACKUP")
            put("version", 1)
            put("app", "MILES")
            put("exportedAt", System.currentTimeMillis())
            put("preferences", JSONObject(preferences.exportRawPreferences()))
            put("activities", JSONArray().also { a -> database.activityDao().getAllActivitiesIncludingTrashOnce().forEach { a.put(activityToJson(it)) } })
            put("savedRoutes", JSONArray().also { a -> database.savedRouteDao().getAllRoutesOnce().forEach { a.put(routeToJson(it)) } })
            put("privacyZones", JSONArray().also { a -> database.privacyZoneDao().getAllZonesOnce().forEach { a.put(zoneToJson(it)) } })
            put("goals", JSONArray().also { a -> database.goalDao().getAllGoalsOnce().forEach { a.put(goalToJson(it)) } })
        }.toString(2)
    }

    suspend fun restoreMilesBackup(content: String) = withContext(Dispatchers.IO) {
        val root = JSONObject(content)
        require(root.optString("header") == "MILES_BACKUP_V1") { "Not a valid MILES backup" }
        require(root.optInt("version", -1) == 1) { "Unsupported MILES backup version" }
        preferences.importRawPreferences(root.optJSONObject("preferences") ?: JSONObject())
        database.activityDao().clearAll()
        database.savedRouteDao().clearAll()
        database.privacyZoneDao().clearAll()
        database.goalDao().clearAll()
        root.optJSONArray("activities")?.let { a -> database.activityDao().insertActivities((0 until a.length()).map { activityFromJson(a.getJSONObject(it)) }) }
        root.optJSONArray("savedRoutes")?.let { a -> (0 until a.length()).forEach { database.savedRouteDao().insertRoute(routeFromJson(a.getJSONObject(it))) } }
        root.optJSONArray("privacyZones")?.let { a -> (0 until a.length()).forEach { database.privacyZoneDao().insertZone(zoneFromJson(a.getJSONObject(it))) } }
        root.optJSONArray("goals")?.let { a -> (0 until a.length()).forEach { database.goalDao().insertGoal(goalFromJson(a.getJSONObject(it))) } }
    }

    private fun activityToJson(a: ActivityEntity) = JSONObject().apply {
        put("id", a.id); put("title", a.title); put("activityType", a.activityType); put("startTime", a.startTime); put("endTime", a.endTime)
        put("durationSeconds", a.durationSeconds); put("distanceMeters", a.distanceMeters); put("steps", a.steps)
        put("avgPaceSecPerKm", a.avgPaceSecPerKm); put("bestPaceSecPerKm", a.bestPaceSecPerKm); put("avgSpeedKmh", a.avgSpeedKmh); put("maxSpeedKmh", a.maxSpeedKmh)
        put("elevationGainM", a.elevationGainM); put("elevationLossM", a.elevationLossM); put("calories", a.calories); put("avgHeartRate", a.avgHeartRate); put("maxHeartRate", a.maxHeartRate)
        put("routePointsJson", a.routePointsJson); put("waypointsJson", a.waypointsJson); put("weatherJson", a.weatherJson); put("notes", a.notes); put("photoUri", a.photoUri)
        put("isFavorite", a.isFavorite); put("isDeleted", a.isDeleted); put("deletedAt", a.deletedAt); put("version", a.version); put("sensorSource", a.sensorSource)
    }

    private fun activityFromJson(o: JSONObject) = ActivityEntity(
        id = o.getString("id"), title = o.getString("title"), activityType = o.optString("activityType", "WALKING"),
        startTime = o.getLong("startTime"), endTime = o.getLong("endTime"), durationSeconds = o.getLong("durationSeconds"),
        distanceMeters = o.getDouble("distanceMeters"), steps = o.optInt("steps"), avgPaceSecPerKm = o.optDouble("avgPaceSecPerKm"), bestPaceSecPerKm = o.optDouble("bestPaceSecPerKm"),
        avgSpeedKmh = o.optDouble("avgSpeedKmh"), maxSpeedKmh = o.optDouble("maxSpeedKmh"), elevationGainM = o.optDouble("elevationGainM"), elevationLossM = o.optDouble("elevationLossM"),
        calories = o.optInt("calories"), avgHeartRate = o.optInt("avgHeartRate"), maxHeartRate = o.optInt("maxHeartRate"), routePointsJson = o.optString("routePointsJson", "[]"),
        waypointsJson = o.optString("waypointsJson", "[]"), weatherJson = o.optString("weatherJson", ""), notes = o.optString("notes", ""),
        photoUri = if (o.isNull("photoUri")) null else o.optString("photoUri"), isFavorite = o.optBoolean("isFavorite"), isDeleted = o.optBoolean("isDeleted"),
        deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt"), version = o.optInt("version", 1), sensorSource = o.optString("sensorSource", "Built-in GPS")
    )

    private fun routeToJson(r: SavedRouteEntity) = JSONObject().apply {
        put("id", r.id); put("title", r.title); put("name", r.name); put("description", r.description); put("activityType", r.activityType)
        put("distanceMeters", r.distanceMeters); put("elevationGainM", r.elevationGainM); put("routePointsJson", r.routePointsJson); put("waypointsJson", r.waypointsJson)
        put("isFavorite", r.isFavorite); put("createdAt", r.createdAt)
    }

    private fun routeFromJson(o: JSONObject) = SavedRouteEntity(
        id = o.getString("id"), title = o.optString("title"), name = o.optString("name", o.optString("title")), description = o.optString("description"),
        activityType = o.optString("activityType", "WALKING"), distanceMeters = o.optDouble("distanceMeters"), elevationGainM = o.optDouble("elevationGainM"),
        routePointsJson = o.optString("routePointsJson", "[]"), waypointsJson = o.optString("waypointsJson", "[]"), isFavorite = o.optBoolean("isFavorite"), createdAt = o.optLong("createdAt")
    )

    private fun zoneToJson(z: PrivacyZoneEntity) = JSONObject().apply {
        put("id", z.id); put("name", z.name); put("latitude", z.latitude); put("longitude", z.longitude); put("radiusMeters", z.radiusMeters); put("isEnabled", z.isEnabled)
    }

    private fun zoneFromJson(o: JSONObject) = PrivacyZoneEntity(
        id = o.getString("id"), name = o.optString("name"), latitude = o.getDouble("latitude"), longitude = o.getDouble("longitude"),
        radiusMeters = o.optDouble("radiusMeters", 250.0).toFloat(), isEnabled = o.optBoolean("isEnabled", true)
    )

    private fun goalToJson(g: GoalEntity) = JSONObject().apply {
        put("id", g.id); put("type", g.type); put("targetValue", g.targetValue); put("period", g.period); put("createdAt", g.createdAt)
    }

    private fun goalFromJson(o: JSONObject) = GoalEntity(
        id = o.getString("id"), type = o.optString("type"), targetValue = o.optDouble("targetValue"), period = o.optString("period", "DAILY"), createdAt = o.optLong("createdAt")
    )

    private fun filterPrivacy(points: List<GpsPoint>, zones: List<PrivacyZoneEntity>): List<GpsPoint> =
        points.filter { p -> zones.none { z -> z.isEnabled && distanceMeters(p.latitude, p.longitude, z.latitude, z.longitude) <= z.radiusMeters } }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1); val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) + kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) * kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        return 2 * r * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    }

    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    private fun xmlEscape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
}
