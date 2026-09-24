package com.example.miles.engine

import com.example.miles.data.model.GpsPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * One turn-by-turn leg of a computed route (from OSRM).
 *
 * @param instruction human-readable instruction, e.g. "Turn right onto Broadway"
 * @param maneuverType OSRM maneuver type: depart, turn, new name, arrive, roundabout, merge, …
 * @param modifier OSRM maneuver modifier: left, right, slight left, straight, uturn, …
 */
data class OsmRouteStep(
    val latitude: Double,
    val longitude: Double,
    val instruction: String,
    val roadName: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val maneuverType: String,
    val modifier: String
)

data class OsmRouteResult(
    val points: List<GpsPoint>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<OsmRouteStep>
)

/**
 * Real route computation via the OSRM demo server ("router.project-osrm.org").
 * Free, no API key, no account. Returns a polyline of route points plus
 * turn-by-turn steps with real distances, durations and maneuvers.
 *
 * Coordinates are passed as (latitude, longitude) pairs and are converted
 * to OSRM's (longitude, latitude) format internally.
 */
object RoutingEngine {

    private const val BASE_URL = "https://router.project-osrm.org/route/v1"

    suspend fun fetchRoute(
        coordinates: List<Pair<Double, Double>>,
        profile: String = "foot"
    ): OsmRouteResult = withContext(Dispatchers.IO) {
        require(coordinates.size >= 2) { "At least 2 coordinates required for routing" }

        val coordsParam = coordinates.joinToString(";") { (lat, lon) ->
            String.format(Locale.US, "%.6f,%.6f", lon, lat)
        }
        val url = "$BASE_URL/$profile/$coordsParam?overview=full&geometries=polyline&steps=true&alternatives=false&annotations=false"

        val body: String = runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "MILES-Android-App/3.0 (contact: bhatiaansh716@gmail.com)")
            }
            try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrElse { throw IllegalStateException("Routing service unreachable: ${it.message}") }

        val root = JSONObject(body)
        val code = root.optString("code", "Error")
        if (code != "Ok") {
            val message = root.optString("message", code)
            throw IllegalStateException("No route found: $message")
        }

        val route = root.getJSONArray("routes").getJSONObject(0)
        val distanceMeters = route.optDouble("distance", 0.0)
        val durationSeconds = route.optDouble("duration", 0.0)

        val rawPoints = decodePolyline(route.optString("geometry", ""))
        if (rawPoints.size < 2) throw IllegalStateException("Route geometry is empty")

        val now = System.currentTimeMillis()
        val points = rawPoints.mapIndexed { i, (lat, lng) ->
            GpsPoint(
                latitude = lat,
                longitude = lng,
                altitude = 0.0,
                accuracy = 2.0f,
                timestamp = now + i
            )
        }

        val steps = buildSteps(root.getJSONArray("routes").getJSONObject(0), rawPoints)
        OsmRouteResult(
            points = points,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            steps = steps
        )
    }

    private fun buildSteps(route: JSONObject, geometry: List<Pair<Double, Double>>): List<OsmRouteStep> {
        // Geometry with an explicit start/end so we can label the first real maneuver.
        val start = geometry.first()
        val steps = mutableListOf<OsmRouteStep>()

        val legs = route.optJSONArray("legs") ?: return steps
        for (li in 0 until legs.length()) {
            val leg = legs.getJSONObject(li)
            val legSteps = leg.optJSONArray("steps") ?: continue
            for (si in 0 until legSteps.length()) {
                val s = legSteps.getJSONObject(si)
                val maneuver = s.optJSONObject("maneuver") ?: continue
                val type = maneuver.optString("type", "")
                val modifier = maneuver.optString("modifier", "straight")
                val loc = maneuver.optJSONArray("location") ?: continue
                val stepLon = loc.optDouble(0, 0.0)
                val stepLat = loc.optDouble(1, 0.0)
                val roadName = s.optString("name", "").ifBlank { s.optString("ref", "") }
                val exit = maneuver.optInt("exit", 0)
                val instruction = buildInstruction(type, modifier, roadName, exit, start, geometry)

                steps += OsmRouteStep(
                    latitude = stepLat,
                    longitude = stepLon,
                    instruction = instruction,
                    roadName = roadName,
                    distanceMeters = s.optDouble("distance", 0.0),
                    durationSeconds = s.optDouble("duration", 0.0),
                    maneuverType = type,
                    modifier = modifier
                )
            }
        }
        return steps
    }

    private fun buildInstruction(
        type: String,
        modifier: String,
        roadName: String,
        exit: Int,
        start: Pair<Double, Double>,
        geometry: List<Pair<Double, Double>>
    ): String {
        val onto = if (roadName.isNotBlank()) " onto $roadName" else ""
        val onRoad = if (roadName.isNotBlank()) " on $roadName" else ""
        return when (type) {
            "depart" -> {
                // The depart maneuver sits at the route start itself; use the
                // second geometry point so the initial heading is meaningful.
                val next = geometry.getOrNull(1) ?: start
                "Head ${cardinalBearing(start.first, start.second, next.first, next.second)}$onRoad"
            }
            "arrive" -> "Arrive at your destination"
            "turn", "end of road" -> "Turn ${modifier}${onto}"
            "roundabout turn" -> "Turn ${modifier}${onto}"
            "roundabout", "rotary" -> {
                if (exit > 0) "At the roundabout take exit $exit$onto" else "Take the roundabout$onto"
            }
            "new name" -> "Continue${onto}"
            "continue" -> if (modifier == "straight" || modifier.isBlank()) "Continue$onto" else "Continue ${modifier}$onto"
            "merge" -> "Merge ${modifier}$onto"
            "fork" -> "Keep ${modifier}$onto"
            "on ramp", "off ramp" -> {
                if (roadName.isNotBlank()) "Take the ramp to $roadName" else "Take the ramp"
            }
            "uturn" -> "Make a U-turn$onto"
            "notification" -> "Continue ${modifier}${onto}"
            "exit roundabout" -> "Take the roundabout exit$onto"
            else -> "Continue straight"
        }
    }

    private fun cardinalBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = sin(dLon) * cos(Math.toRadians(lat2))
        val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
            sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
        val deg = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
        val names = arrayOf("north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest")
        return names[((deg + 22.5) / 45.0).toInt() % 8]
    }

    /** Standard Google/OSRM polyline decoding (precision 1e5). */
    private fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
        val out = mutableListOf<Pair<Double, Double>>()
        var index = 0
        var lat = 0
        var lng = 0
        while (index < encoded.length) {
            var result = 0
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result and 1 != 0) (result shr 1).inv() else (result shr 1)
            lat += dLat

            result = 0
            shift = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result and 1 != 0) (result shr 1).inv() else (result shr 1)
            lng += dLng

            out += (lat / 1e5) to (lng / 1e5)
        }
        return out
    }
}