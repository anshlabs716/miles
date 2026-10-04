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
import java.util.TimeZone
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
            val cleanJson = json.trim().let {
                if (it.length > 1 && it.startsWith("\"") && it.endsWith("\"") && !it.startsWith("\"{")) {
                    it.substring(1, it.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
                } else {
                    it
                }
            }
            val array = runCatching { JSONArray(cleanJson) }.getOrNull() ?: return emptyList()
            val list = mutableListOf<GpsPoint>()
            val fallbackStart = System.currentTimeMillis()
            for (i in 0 until array.length()) {
                // One malformed entry must not discard the whole track: parse each item
                // independently and skip only the entries that cannot be read.
                val item = runCatching { array.opt(i) }.getOrNull() ?: continue
                val point = runCatching {
                    when (item) {
                        is JSONObject -> parsePointObject(item, fallbackStart + (i * 1000L))
                        is JSONArray -> parsePointArray(item, fallbackStart + (i * 1000L))
                        else -> null
                    }
                }.getOrNull()
                if (point != null && isPlausibleCoordinate(point.latitude, point.longitude)) {
                    list.add(point)
                }
            }
            return list
        }

        /** Drops the classic (0, 0) null-island fix plus out-of-range values. */
        private fun isPlausibleCoordinate(lat: Double, lng: Double): Boolean {
            if (!lat.isFinite() || !lng.isFinite()) return false
            if (lat < -90.0 || lat > 90.0 || lng < -180.0 || lng > 180.0) return false
            return !(lat == 0.0 && lng == 0.0)
        }

        private fun parsePointObject(item: JSONObject, fallbackTimestamp: Long): GpsPoint? {
            val lat = jsonDouble(item, "lat", "latitude", "y") ?: return null
            val lng = jsonDouble(item, "lng", "lon", "long", "longitude", "x") ?: return null
            val alt = jsonDouble(item, "alt", "ele", "altitude", "elevation") ?: 0.0
            val acc = jsonDouble(item, "acc", "accuracy", "horizontalAccuracy")?.toFloat() ?: 3.0f
            val spd = jsonDouble(item, "spd", "speed")?.toFloat() ?: 0.0f
            val brg = jsonDouble(item, "brg", "bearing", "heading")?.toFloat() ?: 0.0f
            val ts = jsonTimestamp(item, "ts", "time", "timestamp", "t") ?: fallbackTimestamp
            return GpsPoint(
                latitude = lat,
                longitude = lng,
                altitude = alt,
                accuracy = acc,
                speed = spd,
                bearing = brg,
                timestamp = ts
            )
        }

        /** GeoJSON style coordinate pair: [lon, lat, alt?] or [lon, lat, alt?, ts?]. */
        private fun parsePointArray(item: JSONArray, fallbackTimestamp: Long): GpsPoint? {
            if (item.length() < 2) return null
            val lon = item.optDoubleOrNull(0) ?: return null
            val lat = item.optDoubleOrNull(1) ?: return null
            val alt = if (item.length() >= 3) item.optDoubleOrNull(2) ?: 0.0 else 0.0
            val ts = if (item.length() >= 4) item.optLongOrNull(3) ?: fallbackTimestamp else fallbackTimestamp
            return GpsPoint(
                latitude = lat,
                longitude = lon,
                altitude = alt,
                accuracy = 3.0f,
                timestamp = ts
            )
        }

        /** Returns the first key that holds a readable, finite double. */
        private fun jsonDouble(obj: JSONObject, vararg keys: String): Double? {
            for (key in keys) {
                if (!obj.has(key) || obj.isNull(key)) continue
                val raw = runCatching { obj.opt(key) }.getOrNull() ?: continue
                val value = when (raw) {
                    is Number -> raw.toDouble()
                    is String -> raw.trim().toDoubleOrNull()
                    else -> continue
                }
                if (value != null && value.isFinite()) return value
            }
            return null
        }

        /** Epoch millis from a numeric field, or millis parsed from an ISO-8601 string. */
        private fun jsonTimestamp(obj: JSONObject, vararg keys: String): Long? {
            for (key in keys) {
                if (!obj.has(key) || obj.isNull(key)) continue
                val raw = runCatching { obj.opt(key) }.getOrNull() ?: continue
                when (raw) {
                    is Number -> return raw.toLong()
                    is String -> {
                        parseTimestamp(raw)?.let { return it }
                        raw.trim().toLongOrNull()?.let { return it }
                    }
                }
            }
            return null
        }

        private fun JSONArray.optDoubleOrNull(index: Int): Double? {
            if (index < 0 || index >= length()) return null
            val raw = runCatching { opt(index) }.getOrNull() ?: return null
            return when (raw) {
                is Number -> raw.toDouble().takeIf { it.isFinite() }
                is String -> raw.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
                else -> null
            }
        }

        private fun JSONArray.optLongOrNull(index: Int): Long? {
            if (index < 0 || index >= length()) return null
            val raw = runCatching { opt(index) }.getOrNull() ?: return null
            return when (raw) {
                is Number -> raw.toLong()
                is String -> raw.trim().toLongOrNull()
                else -> null
            }
        }

        /**
         * Accepts epoch millis, epoch seconds, and the ISO-8601 shapes produced by GPX/TCX/KML
         * exports. Zone-less values are read as UTC so exports round-trip to the same instant.
         */
        fun parseTimestamp(value: String): Long? {
            val text = value.trim()
            if (text.isEmpty()) return null
            text.toLongOrNull()?.let { return normalizeEpoch(it) }
            text.toDoubleOrNull()?.let { return normalizeEpoch(it.toLong()) }

            val normalized = text.replace('T', ' ').trim()
            val patterns = listOf(
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy-MM-dd"
            )
            for (pattern in patterns) {
                val parsed = runCatching {
                    SimpleDateFormat(pattern, Locale.US).apply {
                        isLenient = false
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.parse(normalized)
                }.getOrNull()
                if (parsed != null) return parsed.time
            }
            return null
        }

        private fun normalizeEpoch(value: Long): Long = when {
            value <= 0L -> value
            value < 100_000_000_000L -> value * 1000L // seconds
            else -> value
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
            val cleanJson = json.trim().let {
                if (it.length > 1 && it.startsWith("\"") && it.endsWith("\"") && !it.startsWith("\"{")) {
                    it.substring(1, it.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
                } else {
                    it
                }
            }
            val array = runCatching { JSONArray(cleanJson) }.getOrNull() ?: return emptyList()
            val list = mutableListOf<Waypoint>()
            for (i in 0 until array.length()) {
                // Skip only the unreadable entry instead of losing every waypoint in the file.
                val obj = runCatching { array.optJSONObject(i) }.getOrNull() ?: continue
                val lat = jsonDouble(obj, "lat", "latitude", "y") ?: continue
                val lng = jsonDouble(obj, "lng", "lon", "long", "longitude", "x") ?: continue
                if (!isPlausibleCoordinate(lat, lng)) continue
                val typeStr = obj.optString("type", obj.optString("waypointType", WaypointType.CUSTOM.name))
                val type = WaypointType.entries.firstOrNull { it.name.equals(typeStr, ignoreCase = true) }
                    ?: WaypointType.CUSTOM
                list.add(Waypoint(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    name = obj.optString("name", obj.optString("title", "Pin ${i + 1}")),
                    latitude = lat,
                    longitude = lng,
                    type = type,
                    notes = obj.optString("notes", ""),
                    timestamp = jsonTimestamp(obj, "ts", "timestamp", "time", "t") ?: System.currentTimeMillis()
                ))
            }
            return list
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
                put("avgPaceSecPerKm", act.avgPaceSecPerKm); put("bestPaceSecPerKm", act.bestPaceSecPerKm)
                put("avgSpeedKmh", act.avgSpeedKmh); put("maxSpeedKmh", act.maxSpeedKmh)
                put("elevationGainM", act.elevationGainM); put("elevationLossM", act.elevationLossM)
                put("calories", act.calories); put("avgHeartRate", act.avgHeartRate); put("maxHeartRate", act.maxHeartRate)
                put("isFavorite", act.isFavorite); put("sensorSource", act.sensorSource)
                put("points", JSONArray(pointsToJson(points))); put("waypoints", JSONArray(waypointsToJson(parseWaypoints(act.waypointsJson))))
                put("notes", act.notes)
            })
        }
        val plainJson = root.toString(2)
        if (!password.isNullOrBlank()) encryptAes(plainJson, password) else plainJson
    }

    suspend fun exportActivityAsGpx(activityId: String): String = withContext(Dispatchers.IO) {
        val act = activityDao.getActivityByIdOnce(activityId) ?: return@withContext ""
        val points = filterPointsWithPrivacyZones(parsePoints(act.routePointsJson))
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<gpx version=\"1.1\" creator=\"MILES Privacy Fitness\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
            append("<metadata><name>${act.title}</name><time>${sdf.format(Date(act.startTime))}</time></metadata>\n<trk><name>${act.title}</name><type>${act.activityType}</type><trkseg>\n")
            points.forEach { p -> append("<trkpt lat=\"${p.latitude}\" lon=\"${p.longitude}\"><ele>${p.altitude}</ele><time>${sdf.format(Date(p.timestamp))}</time></trkpt>\n") }
            append("</trkseg></trk></gpx>")
        }
    }

    suspend fun exportActivityAsKml(activityId: String): String = withContext(Dispatchers.IO) {
        val act = activityDao.getActivityByIdOnce(activityId) ?: return@withContext ""
        val points = filterPointsWithPrivacyZones(parsePoints(act.routePointsJson))
        buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
            append("  <Document>\n")
            append("    <name>${act.title}</name>\n")
            append("    <description>Distance: ${(act.distanceMeters / 1000.0).format(2)} km, Duration: ${act.durationSeconds / 60} min</description>\n")
            append("    <Placemark>\n")
            append("      <name>Track</name>\n")
            append("      <LineString>\n")
            append("        <tessellate>1</tessellate>\n")
            append("        <coordinates>\n")
            points.forEach { p ->
                append("          ${p.longitude},${p.latitude},${p.altitude}\n")
            }
            append("        </coordinates>\n")
            append("      </LineString>\n")
            append("    </Placemark>\n")
            append("  </Document>\n")
            append("</kml>")
        }
    }

    suspend fun exportActivityAsTcx(activityId: String): String = withContext(Dispatchers.IO) {
        val act = activityDao.getActivityByIdOnce(activityId) ?: return@withContext ""
        val points = filterPointsWithPrivacyZones(parsePoints(act.routePointsJson))
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<TrainingCenterDatabase xmlns=\"http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2\">\n")
            append("  <Activities>\n")
            append("    <Activity Sport=\"${act.activityType}\">\n")
            append("      <Id>${sdf.format(Date(act.startTime))}</Id>\n")
            append("      <Lap StartTime=\"${sdf.format(Date(act.startTime))}\">\n")
            append("        <TotalTimeSeconds>${act.durationSeconds}</TotalTimeSeconds>\n")
            append("        <DistanceMeters>${act.distanceMeters}</DistanceMeters>\n")
            append("        <Calories>${act.calories}</Calories>\n")
            append("        <Track>\n")
            points.forEach { p ->
                append("          <Trackpoint>\n")
                append("            <Time>${sdf.format(Date(p.timestamp))}</Time>\n")
                append("            <Position>\n")
                append("              <LatitudeDegrees>${p.latitude}</LatitudeDegrees>\n")
                append("              <LongitudeDegrees>${p.longitude}</LongitudeDegrees>\n")
                append("            </Position>\n")
                append("            <AltitudeMeters>${p.altitude}</AltitudeMeters>\n")
                append("          </Trackpoint>\n")
            }
            append("        </Track>\n")
            append("      </Lap>\n")
            append("    </Activity>\n")
            append("  </Activities>\n")
            append("</TrainingCenterDatabase>")
        }
    }

    suspend fun exportActivityAsGeoJson(activityId: String): String = withContext(Dispatchers.IO) {
        val act = activityDao.getActivityByIdOnce(activityId) ?: return@withContext ""
        val points = filterPointsWithPrivacyZones(parsePoints(act.routePointsJson))
        val coords = JSONArray()
        points.forEach { p ->
            coords.put(JSONArray().apply {
                put(p.longitude)
                put(p.latitude)
                put(p.altitude)
            })
        }
        val feature = JSONObject().apply {
            put("type", "Feature")
            put("geometry", JSONObject().apply {
                put("type", "LineString")
                put("coordinates", coords)
            })
            put("properties", JSONObject().apply {
                put("title", act.title)
                put("type", act.activityType)
                put("distanceMeters", act.distanceMeters)
                put("durationSeconds", act.durationSeconds)
                put("calories", act.calories)
                put("startTime", act.startTime)
            })
        }
        JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", JSONArray().apply { put(feature) })
        }.toString(2)
    }

    suspend fun exportRouteAsKml(routeId: String): String = withContext(Dispatchers.IO) {
        val route = routeDao.getRouteById(routeId) ?: return@withContext ""
        val points = parsePoints(route.routePointsJson)
        buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
            append("  <Document>\n")
            append("    <name>${route.name}</name>\n")
            append("    <description>MILES Saved Route: ${(route.distanceMeters / 1000.0).format(2)} km</description>\n")
            append("    <Placemark>\n")
            append("      <name>${route.name}</name>\n")
            append("      <LineString>\n")
            append("        <tessellate>1</tessellate>\n")
            append("        <coordinates>\n")
            points.forEach { p ->
                append("          ${p.longitude},${p.latitude},${p.altitude}\n")
            }
            append("        </coordinates>\n")
            append("      </LineString>\n")
            append("    </Placemark>\n")
            append("  </Document>\n")
            append("</kml>")
        }
    }

    suspend fun exportRouteAsGeoJson(routeId: String): String = withContext(Dispatchers.IO) {
        val route = routeDao.getRouteById(routeId) ?: return@withContext ""
        val points = parsePoints(route.routePointsJson)
        val coords = JSONArray()
        points.forEach { p ->
            coords.put(JSONArray().apply {
                put(p.longitude)
                put(p.latitude)
                put(p.altitude)
            })
        }
        val feature = JSONObject().apply {
            put("type", "Feature")
            put("geometry", JSONObject().apply {
                put("type", "LineString")
                put("coordinates", coords)
            })
            put("properties", JSONObject().apply {
                put("name", route.name)
                put("activityType", route.activityType)
                put("distanceMeters", route.distanceMeters)
                put("estimatedDurationSeconds", (route.distanceMeters / 2.8).toLong())
                put("elevationGainM", route.elevationGainM)
            })
        }
        JSONObject().apply {
            put("type", "FeatureCollection")
            put("features", JSONArray().apply { put(feature) })
        }.toString(2)
    }

    suspend fun importRouteFromKml(kmlStr: String): SavedRouteEntity? = withContext(Dispatchers.IO) {
        val nameRegex = Regex("""<name>([^<]+)</name>""")
        val name = nameRegex.find(kmlStr)?.groupValues?.get(1)?.trim() ?: "Imported KML Route"
        val coordMatch = Regex("""<coordinates>([\s\S]*?)</coordinates>""").find(kmlStr) ?: return@withContext null
        val coordText = coordMatch.groupValues[1].trim()
        val points = mutableListOf<GpsPoint>()
        val tokens = coordText.split(Regex("""\s+"""))
        val now = System.currentTimeMillis()
        tokens.forEachIndexed { idx, token ->
            val parts = token.split(",")
            if (parts.size >= 2) {
                val lon = parts[0].toDoubleOrNull() ?: return@forEachIndexed
                val lat = parts[1].toDoubleOrNull() ?: return@forEachIndexed
                val alt = if (parts.size >= 3) parts[2].toDoubleOrNull() ?: 10.0 else 10.0
                points.add(GpsPoint(latitude = lat, longitude = lon, altitude = alt, timestamp = now + (idx * 2000L)))
            }
        }
        if (points.isEmpty()) return@withContext null
        var totalDist = 0.0
        for (i in 0 until points.size - 1) {
            totalDist += calculateDistanceMeters(points[i].latitude, points[i].longitude, points[i + 1].latitude, points[i + 1].longitude)
        }
        val route = SavedRouteEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            title = name,
            activityType = ActivityType.RUNNING.name,
            distanceMeters = totalDist,
            elevationGainM = 20.0,
            routePointsJson = pointsToJson(points),
            waypointsJson = "[]",
            createdAt = now
        )
        routeDao.insertRoute(route)
        route
    }

    suspend fun importRouteFromGeoJson(geoJsonStr: String): SavedRouteEntity? = withContext(Dispatchers.IO) {
        val root = JSONObject(geoJsonStr)
        var feature = root
        if (root.optString("type") == "FeatureCollection") {
            val features = root.optJSONArray("features") ?: return@withContext null
            if (features.length() == 0) return@withContext null
            feature = features.getJSONObject(0)
        }
        val geometry = feature.optJSONObject("geometry") ?: return@withContext null
        val coords = geometry.optJSONArray("coordinates") ?: return@withContext null
        val points = mutableListOf<GpsPoint>()
        val now = System.currentTimeMillis()
        for (i in 0 until coords.length()) {
            val item = coords.optJSONArray(i) ?: continue
            if (item.length() >= 2) {
                val lon = item.getDouble(0)
                val lat = item.getDouble(1)
                val alt = if (item.length() >= 3) item.getDouble(2) else 10.0
                points.add(GpsPoint(latitude = lat, longitude = lon, altitude = alt, timestamp = now + (i * 2000L)))
            }
        }
        if (points.isEmpty()) return@withContext null
        var totalDist = 0.0
        for (i in 0 until points.size - 1) {
            totalDist += calculateDistanceMeters(points[i].latitude, points[i].longitude, points[i + 1].latitude, points[i + 1].longitude)
        }
        val props = feature.optJSONObject("properties")
        val name = props?.optString("name", props.optString("title", "Imported GeoJSON Route")) ?: "Imported GeoJSON Route"
        val activityType = props?.optString("activityType", props.optString("type", ActivityType.RUNNING.name)) ?: ActivityType.RUNNING.name
        val route = SavedRouteEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            title = name,
            activityType = activityType,
            distanceMeters = totalDist,
            elevationGainM = props?.optDouble("elevationGainM", 15.0) ?: 15.0,
            routePointsJson = pointsToJson(points),
            waypointsJson = "[]",
            createdAt = now
        )
        routeDao.insertRoute(route)
        route
    }

    suspend fun importFromKml(kmlStr: String): Int = withContext(Dispatchers.IO) {
        val nameRegex = Regex("""<name>([^<]+)</name>""")
        // <name> under <Placemark>/<trk> names the track; the leading Document name is the folder.
        val name = Regex("""<Placemark>[\s\S]*?<name>([^<]+)</name>""").find(kmlStr)?.groupValues?.get(1)?.trim()
            ?: nameRegex.find(kmlStr)?.groupValues?.get(1)?.trim()
            ?: "Imported KML Workout"
        val coordMatch = Regex("""<coordinates>([\s\S]*?)</coordinates>""").find(kmlStr) ?: return@withContext 0
        val coordText = coordMatch.groupValues[1].trim()
        val points = mutableListOf<GpsPoint>()
        val tokens = coordText.split(Regex("""\s+"""))
        // Fall back to now only so point timestamps stay monotonic when KML has no <when>.
        val firstTime = Regex("""<when>([^<]+)</when>""").find(kmlStr)?.groupValues?.get(1)?.let { parseTimestamp(it) }
        val now = firstTime ?: System.currentTimeMillis()
        tokens.forEachIndexed { idx, token ->
            val parts = token.split(",")
            if (parts.size >= 2) {
                val lon = parts[0].toDoubleOrNull() ?: return@forEachIndexed
                val lat = parts[1].toDoubleOrNull() ?: return@forEachIndexed
                val alt = if (parts.size >= 3) parts[2].toDoubleOrNull() ?: 10.0 else 10.0
                points.add(GpsPoint(latitude = lat, longitude = lon, altitude = alt, timestamp = now + (idx * 2000L), speed = 2.8f, accuracy = 4.0f))
            }
        }
        if (points.isEmpty()) return@withContext 0
        var totalDist = 0.0
        for (i in 0 until points.size - 1) {
            totalDist += calculateDistanceMeters(points[i].latitude, points[i].longitude, points[i + 1].latitude, points[i + 1].longitude)
        }
        val durationSec = (points.size * 2L).coerceAtLeast(60L)
        val entity = ActivityEntity(
            id = UUID.randomUUID().toString(),
            title = name,
            activityType = ActivityType.RUNNING.name,
            startTime = now - (durationSec * 1000L),
            endTime = now,
            durationSeconds = durationSec,
            distanceMeters = totalDist,
            steps = (totalDist * 1.3).toInt(),
            avgPaceSecPerKm = if (totalDist > 0) durationSec / (totalDist / 1000.0) else 0.0,
            avgSpeedKmh = if (durationSec > 0) (totalDist / 1000.0) / (durationSec / 3600.0) else 0.0,
            elevationGainM = 20.0,
            calories = (totalDist / 1000.0 * 65.0).toInt(),
            avgHeartRate = 145,
            routePointsJson = pointsToJson(points),
            waypointsJson = "[]",
            notes = "Imported KML track (${points.size} points)"
        )
        activityDao.insertActivity(entity)
        1
    }

    suspend fun importFromGeoJson(geoJsonStr: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(geoJsonStr)
        val featureList = mutableListOf<JSONObject>()
        if (root.optString("type") == "FeatureCollection") {
            val features = root.optJSONArray("features")
            if (features != null) {
                for (i in 0 until features.length()) {
                    featureList.add(features.getJSONObject(i))
                }
            }
        } else if (root.optString("type") == "Feature") {
            featureList.add(root)
        }

        var imported = 0
        val now = System.currentTimeMillis()
        for (f in featureList) {
            val geometry = f.optJSONObject("geometry") ?: continue
            val coords = geometry.optJSONArray("coordinates") ?: continue
            val points = mutableListOf<GpsPoint>()
            for (i in 0 until coords.length()) {
                val item = coords.optJSONArray(i) ?: continue
                if (item.length() >= 2) {
                    val lon = item.getDouble(0)
                    val lat = item.getDouble(1)
                    val alt = if (item.length() >= 3) item.getDouble(2) else 10.0
                    points.add(GpsPoint(latitude = lat, longitude = lon, altitude = alt, timestamp = now + (i * 2000L), speed = 2.8f, accuracy = 4.0f))
                }
            }
            if (points.isEmpty()) continue
            var totalDist = 0.0
            for (i in 0 until points.size - 1) {
                totalDist += calculateDistanceMeters(points[i].latitude, points[i].longitude, points[i + 1].latitude, points[i + 1].longitude)
            }
            val props = f.optJSONObject("properties")
            val title = props?.optString("title", props.optString("name", "Imported GeoJSON Workout")) ?: "Imported GeoJSON Workout"
            val type = props?.optString("type", props.optString("activityType", ActivityType.RUNNING.name)) ?: ActivityType.RUNNING.name
            val durationSec = props?.optLong("durationSeconds", (points.size * 2L).coerceAtLeast(60L)) ?: (points.size * 2L).coerceAtLeast(60L)
            val cals = props?.optInt("calories", (totalDist / 1000.0 * 65.0).toInt()) ?: (totalDist / 1000.0 * 65.0).toInt()

            val entity = ActivityEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                activityType = type,
                startTime = now - (durationSec * 1000L),
                endTime = now,
                durationSeconds = durationSec,
                distanceMeters = totalDist,
                steps = (totalDist * 1.3).toInt(),
                avgPaceSecPerKm = if (totalDist > 0) durationSec / (totalDist / 1000.0) else 0.0,
                avgSpeedKmh = if (durationSec > 0) (totalDist / 1000.0) / (durationSec / 3600.0) else 0.0,
                elevationGainM = props?.optDouble("elevationGainM", 20.0) ?: 20.0,
                calories = cals,
                avgHeartRate = 145,
                routePointsJson = pointsToJson(points),
                waypointsJson = "[]",
                notes = "Imported GeoJSON workout"
            )
            activityDao.insertActivity(entity)
            imported++
        }
        imported
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
                trimmed.contains("<kml") || trimmed.contains("<Document") -> importFromKml(trimmed).let { it to "Successfully imported $it activity from KML track." }
                trimmed.startsWith("<?xml") || trimmed.contains("<gpx") -> importFromGpx(trimmed).let { it to "Successfully imported $it activity from GPX track." }
                (trimmed.startsWith("{") && (trimmed.contains("\"Feature\"") || trimmed.contains("\"FeatureCollection\""))) -> importFromGeoJson(trimmed).let { it to "Successfully imported $it activity from GeoJSON track." }
                trimmed.startsWith("{") || trimmed.startsWith("[") -> importFromJson(trimmed).let { it to "Successfully imported $it activities from JSON." }
                trimmed.contains(",") || trimmed.contains(";") -> importFromCsv(trimmed).let { it to "Successfully imported $it activities from CSV / Google Fit export." }
                else -> 0 to "Unrecognized format. Supported formats: GPX, KML, GeoJSON, CSV, Google Fit, and JSON."
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
            if (root.has("activity")) {
                insertJsonActivity(root.getJSONObject("activity"))
                count++
            } else if (root.has("activities")) {
                val array = root.getJSONArray("activities")
                for (i in 0 until array.length()) { insertJsonActivity(array.getJSONObject(i)); count++ }
            } else {
                insertJsonActivity(root)
                count++
            }

            // Also restore savedRoutes if this is a full MILES backup file
            if (root.has("savedRoutes")) {
                val routesArray = root.getJSONArray("savedRoutes")
                for (i in 0 until routesArray.length()) {
                    val rObj = routesArray.getJSONObject(i)
                    val rawPts = rObj.opt("routePointsJson") ?: rObj.opt("points") ?: rObj.opt("routePoints")
                    val pts = parsePoints(rawPts?.toString() ?: "[]")
                    val wpPts = parseWaypoints(rObj.optString("waypointsJson", "[]"))
                    val route = SavedRouteEntity(
                        id = rObj.optString("id", UUID.randomUUID().toString()),
                        name = rObj.optString("name", rObj.optString("title", "Imported Route")),
                        title = rObj.optString("title", rObj.optString("name", "Imported Route")),
                        activityType = rObj.optString("activityType", ActivityType.WALKING.name),
                        distanceMeters = rObj.optDouble("distanceMeters", 0.0),
                        elevationGainM = rObj.optDouble("elevationGainM", 0.0),
                        routePointsJson = pointsToJson(pts),
                        waypointsJson = waypointsToJson(wpPts),
                        createdAt = rObj.optLong("createdAt", System.currentTimeMillis())
                    )
                    routeDao.insertRoute(route)
                }
            }
        }
        return count
    }

    private suspend fun insertJsonActivity(obj: JSONObject) {
        val now = System.currentTimeMillis()
        val title = obj.optString("title").ifBlank { obj.optString("name").ifBlank { "Imported Workout" } }
        val type = obj.optString("activityType").ifBlank {
            obj.optString("type").ifBlank { obj.optString("sport").ifBlank { ActivityType.RUNNING.name } }
        }.let { ActivityType.fromString(it).name }

        // Timestamps arrive as epoch millis, epoch seconds, or ISO-8601 depending on the exporter.
        val startTime = jsonInstant(obj, "startTime", "startTimestamp", "start", "startDate", "date", "time")
            ?: (now - 3600000L)
        val endTime = jsonInstant(obj, "endTime", "endTimestamp", "end", "endDate")
            ?: (startTime + 1800000L)
        val duration = jsonDurationSeconds(obj)
            ?: if (endTime > startTime) (endTime - startTime) / 1000L else 0L

        var distance = jsonDistanceMeters(obj, "distanceMeters", "distanceKm", "distance")
            ?: jsonDoubleField(obj, "distanceMeters", "distance", "distanceKm", "distanceMiles")
                ?.let { if (obj.has("distanceKm")) it * 1000.0 else it }
            ?: 0.0

        // Robust point parsing supporting any key: routePointsJson, points, routePoints, track, coordinates
        val rawPoints = obj.opt("routePointsJson") ?: obj.opt("points") ?: obj.opt("routePoints") ?: obj.opt("track") ?: obj.opt("coordinates")
        val pointsList = if (rawPoints != null) parsePoints(rawPoints.toString()) else emptyList()

        // Only derive distance from the track when the file did not supply one.
        if (distance <= 0.0 && pointsList.size >= 2) {
            var sumDist = 0.0
            for (i in 0 until pointsList.size - 1) {
                sumDist += calculateDistanceMeters(
                    pointsList[i].latitude, pointsList[i].longitude,
                    pointsList[i + 1].latitude, pointsList[i + 1].longitude
                )
            }
            distance = sumDist
        }

        val rawWaypoints = obj.opt("waypointsJson") ?: obj.opt("waypoints")
        val waypointsList = if (rawWaypoints != null) parseWaypoints(rawWaypoints.toString()) else emptyList()

        val steps = jsonIntField(obj, "steps", "stepCount") ?: (distance * 1.3).toInt()
        val calories = jsonIntField(obj, "calories", "kcal") ?: (distance / 1000.0 * 65.0).toInt()

        val entity = ActivityEntity(
            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
            title = title,
            activityType = type,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = duration,
            distanceMeters = distance,
            steps = steps,
            // Keep file-provided pace/speed; only derive them when absent.
            avgPaceSecPerKm = jsonDoubleField(obj, "avgPaceSecPerKm", "averagePaceSecPerKm", "paceSecPerKm")
                ?: if (distance > 0) duration / (distance / 1000.0) else 0.0,
            bestPaceSecPerKm = jsonDoubleField(obj, "bestPaceSecPerKm", "bestPace") ?: 0.0,
            avgSpeedKmh = jsonDoubleField(obj, "avgSpeedKmh", "averageSpeedKmh")
                ?: if (duration > 0) (distance / 1000.0) / (duration / 3600.0) else 0.0,
            maxSpeedKmh = jsonDoubleField(obj, "maxSpeedKmh", "maxSpeed") ?: 0.0,
            elevationGainM = jsonDoubleField(obj, "elevationGainM", "elevationGain") ?: 0.0,
            elevationLossM = jsonDoubleField(obj, "elevationLossM", "elevationLoss") ?: 0.0,
            calories = calories,
            avgHeartRate = jsonIntField(obj, "avgHeartRate", "averageHeartRate") ?: 0,
            maxHeartRate = jsonIntField(obj, "maxHeartRate", "maximumHeartRate") ?: 0,
            routePointsJson = pointsToJson(pointsList),
            waypointsJson = waypointsToJson(waypointsList),
            weatherJson = obj.optString("weatherJson"),
            notes = obj.optString("notes").ifBlank { "Imported from file" },
            photoUri = if (obj.isNull("photoUri")) null else obj.optString("photoUri").takeIf { it.isNotBlank() },
            isFavorite = obj.optBoolean("isFavorite", false),
            sensorSource = obj.optString("sensorSource").ifBlank { "Imported" }
        )
        activityDao.insertActivity(entity)
    }

    private fun jsonInstant(obj: JSONObject, vararg keys: String): Long? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            when (val raw = obj.opt(key)) {
                is Number -> return normalizeEpochMillis(raw.toLong())
                is String -> raw.trim().takeIf { it.isNotEmpty() }?.let { text ->
                    parseTimestamp(text)?.let { return it }
                }
            }
        }
        return null
    }

    /**
     * Duration in seconds. An explicit `durationSeconds` is already seconds; a bare `duration` is
     * treated as milliseconds when large, and colon-separated values are read as hh:mm:ss.
     */
    private fun jsonDurationSeconds(obj: JSONObject): Long? {
        val hasSecondsKey = obj.has("durationSeconds") && !obj.isNull("durationSeconds")
        val raw = if (hasSecondsKey) obj.opt("durationSeconds") else obj.opt("duration")
        when (raw) {
            is Number -> {
                val value = raw.toLong()
                return if (hasSecondsKey) value else if (value > 100_000L) value / 1000L else value
            }
            is String -> {
                val text = raw.trim()
                text.toLongOrNull()?.let { return if (!hasSecondsKey && it > 100_000L) it / 1000L else it }
                text.toDoubleOrNull()?.let { return it.toLong() }
                if (text.contains(':') && text.split(':').all { it.toDoubleOrNull() != null }) {
                    return text.split(':').fold(0L) { acc, part -> acc * 60 + part.toDouble().toLong() }
                }
                return text.toDoubleOrNull()?.toLong()
            }
        }
        return null
    }

    /** Distance in meters from any of the distance keys, applying the key's unit suffix. */
    private fun jsonDistanceMeters(obj: JSONObject, vararg keys: String): Double? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val raw = obj.opt(key)
            val value = when (raw) {
                is Number -> raw.toDouble()
                is String -> raw.trim().toDoubleOrNull()
                else -> continue
            } ?: continue
            return when {
                key.endsWith("Km") || key.endsWith("km") -> value * 1000.0
                key.endsWith("Meters") || key.endsWith("meters") || key.endsWith("Metres") -> value
                else -> value
            }
        }
        return null
    }

    private fun jsonDoubleField(obj: JSONObject, vararg keys: String): Double? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            when (val raw = obj.opt(key)) {
                is Number -> return raw.toDouble().takeIf { it.isFinite() }
                is String -> raw.trim().toDoubleOrNull()?.takeIf { it.isFinite() }?.let { return it }
            }
        }
        return null
    }

    private fun jsonIntField(obj: JSONObject, vararg keys: String): Int? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            when (val raw = obj.opt(key)) {
                is Number -> return raw.toInt()
                is String -> raw.trim().toDoubleOrNull()?.let { return it.toInt() }
            }
        }
        return null
    }

    private fun normalizeEpochMillis(value: Long): Long = when {
        value <= 0L -> value
        value < 100_000_000_000L -> value * 1000L // seconds
        else -> value
    }

    private suspend fun importFromCsv(csvStr: String): Int {
        val rows = parseCsv(csvStr)
        if (rows.isEmpty()) return 0

        val normalizedHeader = rows.first().map { normalizeHeader(it) }
        // A real header has no cells that parse as numbers; a header-less first row always does.
        val hasHeader = normalizedHeader.any { it.isNotEmpty() } &&
            rows.first().all { cell ->
                val normalized = normalizeHeader(cell)
                normalized.isEmpty() || (normalized.first().isLetter() && normalized.none { it.isDigit() })
            }
        val header = if (hasHeader) normalizedHeader else emptyList()
        val dataRows = if (hasHeader) rows.drop(1) else rows

        var imported = 0
        val baseTime = System.currentTimeMillis()
        for (cells in dataRows) {
            if (cells.all { it.isBlank() }) continue

            // Named columns take priority; positional fallbacks keep header-less logs importable.
            fun named(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
                header.indexOfFirst { it == key }
                    .takeIf { it >= 0 }
                    ?.let { cells.getOrNull(it) }
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            }

            val title = named("title", "name", "activityname", "workout", "activity", "label")
                ?: cells.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Imported Fitness Log"

            val startTime = named("starttime", "startdate", "date", "timestamp", "start", "time")
                ?.let { parseTimestamp(it) }
                ?: (baseTime - imported * 86400000L)

            val durationSec = parseCsvDuration(
                named("durationseconds", "durationsec", "durationsecs", "duration", "elapsedtime", "movingtime")
            ) ?: parseCsvDuration(cells.getOrNull(2))?.let { if (cells.getOrNull(2)!!.contains(':')) it else it * 60L }
                ?: 1800L

            val endTime = named("endtime", "enddate", "end")
                ?.let { parseTimestamp(it) }
                ?: (startTime + durationSec * 1000L)

            val distanceMeters = parseCsvDistanceMeters(
                named("distancemeters", "distancekm", "distancem", "distance")
            ) ?: parseCsvDistanceMeters(cells.getOrNull(1), assumeKm = !hasHeader) ?: 0.0

            val distanceKm = distanceMeters / 1000.0
            val steps = parseCsvInt(named("steps", "stepcount")) ?: parseCsvInt(cells.getOrNull(3)) ?: 0
            val calories = parseCsvInt(named("calories", "kcal", "caloriesburned"))
                ?: parseCsvInt(cells.getOrNull(4)) ?: 0

            // Track data only survives when the export included it; otherwise it stays empty.
            val points = named("routepointsjson", "routepoints", "points", "track", "coordinates")
                ?.let { parsePoints(it) }.orEmpty()
            val waypoints = named("waypointsjson", "waypoints", "pins")
                ?.let { parseWaypoints(it) }.orEmpty()

            activityDao.insertActivity(ActivityEntity(
                id = named("id", "activityid") ?: UUID.randomUUID().toString(),
                title = title,
                activityType = named("activitytype", "type", "sport", "activity")
                    ?.let { ActivityType.fromString(it).name }
                    ?: ActivityType.RUNNING.name,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSec,
                distanceMeters = distanceMeters,
                steps = steps,
                avgPaceSecPerKm = parseCsvDouble(named("avgpacesecperkm", "averagepace", "pace"))
                    ?: if (distanceKm > 0) durationSec / distanceKm else 0.0,
                bestPaceSecPerKm = parseCsvDouble(named("bestpacesecperkm", "bestpace")) ?: 0.0,
                avgSpeedKmh = parseCsvDouble(named("avgspeedkmh", "averagespeedkmh"))
                    ?: if (durationSec > 0) distanceKm / (durationSec / 3600.0) else 0.0,
                maxSpeedKmh = parseCsvDouble(named("maxspeedkmh", "maxspeed")) ?: 0.0,
                elevationGainM = parseCsvDouble(named("elevationgainm", "elevationgain")) ?: 0.0,
                elevationLossM = parseCsvDouble(named("elevationlossm", "elevationloss")) ?: 0.0,
                calories = calories,
                avgHeartRate = parseCsvInt(named("avgheartrate", "averageheartrate", "heartrate", "hr")) ?: 0,
                maxHeartRate = parseCsvInt(named("maxheartrate", "maximumheartrate")) ?: 0,
                routePointsJson = pointsToJson(points),
                waypointsJson = waypointsToJson(waypoints),
                notes = named("notes", "note", "comment") ?: "Imported from CSV fitness records",
                isFavorite = parseCsvBoolean(named("isfavorite", "favorite", "favourite")) ?: false,
                sensorSource = named("sensorsource", "source", "device") ?: "Imported"
            ))
            imported++
        }
        return imported
    }

    /**
     * Splits CSV text into rows of cells, honouring quoted fields, embedded commas/newlines, and
     * doubled-quote escapes. Plain [String.split] silently shifted every column after the first
     * quoted value, which is how titles containing commas were lost.
     */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    cell.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && (c == ',' || c == ';' || c == '\t') -> { row.add(cell.toString()); cell.setLength(0) }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(cell.toString()); cell.setLength(0)
                    if (row.any { it.isNotBlank() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            if (row.any { it.isNotBlank() }) rows.add(row)
        }
        return rows
    }

    private fun normalizeHeader(value: String): String =
        value.trim().lowercase(Locale.US).filter { it.isLetterOrDigit() }

    /** Accepts "45:30", "2700", "45 min", or "2700000 ms". */
    private fun parseCsvDuration(raw: String?): Long? {
        val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        text.toLongOrNull()?.let { return if (it > 100_000L) it / 1000L else it }
        text.toDoubleOrNull()?.let { return it.toLong().coerceAtLeast(0L) }
        if (text.contains(':')) {
            val parts = text.split(':')
            if (parts.all { it.toDoubleOrNull() != null }) {
                return parts.fold(0L) { acc, p -> acc * 60 + (p.toDouble().toLong()) }
            }
        }
        val number = Regex("\\d+(\\.\\d+)?").find(text)?.value?.toDoubleOrNull() ?: return null
        val unit = text.substringAfter(number.toString(), "").trim().lowercase(Locale.US)
        return when {
            unit.startsWith("h") -> (number * 3600).toLong()
            unit.startsWith("min") || unit == "m" -> (number * 60).toLong()
            unit.startsWith("s") || unit.startsWith("sec") -> number.toLong()
            unit.startsWith("ms") -> (number / 1000).toLong()
            else -> number.toLong()
        }
    }

    /** Returns the value in meters, applying any unit suffix found in the cell. */
    private fun parseCsvDistanceMeters(raw: String?, assumeKm: Boolean = false): Double? {
        val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val number = Regex("-?\\d+(\\.\\d+)?").find(text)?.value?.toDoubleOrNull() ?: return null
        val unit = text.substringAfter(number.toString(), "").trim().lowercase(Locale.US)
        return when {
            unit.startsWith("km") || unit.startsWith("kilomet") -> number * 1000.0
            unit.startsWith("mi") || unit.startsWith("mile") -> number * 1609.344
            unit.startsWith("ft") || unit.startsWith("feet") -> number * 0.3048
            unit.startsWith("m") -> number
            assumeKm -> number * 1000.0
            else -> number
        }
    }

    private fun parseCsvInt(raw: String?): Int? {
        val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        text.toIntOrNull()?.let { return it }
        text.toDoubleOrNull()?.let { return it.toInt() }
        return Regex("-?\\d+").find(text)?.value?.toIntOrNull()
    }

    private fun parseCsvDouble(raw: String?): Double? {
        val text = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        text.toDoubleOrNull()?.takeIf { it.isFinite() }?.let { return it }
        return Regex("-?\\d+(\\.\\d+)?").find(text)?.value?.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    private fun parseCsvBoolean(raw: String?): Boolean? {
        val text = raw?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotEmpty() } ?: return null
        return when (text) {
            "true", "yes", "y", "1" -> true
            "false", "no", "n", "0" -> false
            else -> null
        }
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
