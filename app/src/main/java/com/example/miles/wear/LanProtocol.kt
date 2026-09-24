package com.example.miles.wear

import org.json.JSONObject

/**
 * GMS-free local-network protocol shared by MILES Wear and the MILES phone app.
 *
 * No Google Play Services: peers find each other with a small UDP beacon on the
 * local WiFi (or loopback when both apps run on one device), then talk over
 * newline-delimited JSON frames on TCP. Pure functions only — unit tested.
 */
object LanProtocol {

    const val DISCOVERY_PORT = 47821
    const val APP_TAG = "miles-lan"
    const val PROTO_VERSION = 1
    const val BEACON_INTERVAL_MS = 2500L
    const val PEER_TIMEOUT_MS = 8000L

    // Shared message paths (identical strings on both apps)
    const val PATH_HELLO = "/miles/hello"
    const val PATH_HR_STREAM = "/miles/sensors/hr"
    const val PATH_CADENCE_STREAM = "/miles/sensors/cadence"
    const val PATH_WORKOUT_CONTROL = "/miles/workout/control"
    const val PATH_PHONE_METRICS = "/miles/metrics/live"
    const val PATH_FLUSH_QUEUE = "/miles/queue/flush"
    const val PATH_PING = "/miles/ping"
    const val PATH_PONG = "/miles/pong"
    const val PATH_WATCH_SETTINGS = "/miles/watch/settings"
    const val PATH_SYNC_REQUEST = "/miles/watch/sync_request"

    const val ROLE_WEAR = "wear"
    const val ROLE_PHONE = "phone"

    /** A peer advertisement: identity + real live status. */
    data class Beacon(
        val role: String,
        val id: String,
        val name: String,
        val model: String,
        val tcpPort: Int,
        val appVersion: String,
        val battery: Int = -1,
        val status: String = "idle",
        val proto: Int = PROTO_VERSION
    )

    fun encodeBeacon(b: Beacon): String = JSONObject().apply {
        put("app", APP_TAG)
        put("proto", PROTO_VERSION)
        put("role", b.role)
        put("id", b.id)
        put("name", b.name)
        put("model", b.model)
        put("tcp", b.tcpPort)
        put("ver", b.appVersion)
        put("batt", b.battery)
        put("status", b.status)
    }.toString()

    /** Parses a beacon, or null when it isn't a MILES LAN beacon. */
    fun parseBeacon(json: String): Beacon? = try {
        val o = JSONObject(json)
        if (o.optString("app") != APP_TAG) null
        else Beacon(
            role = o.optString("role"),
            id = o.optString("id"),
            name = o.optString("name", "MILES"),
            model = o.optString("model", "Android"),
            tcpPort = o.optInt("tcp", 0),
            appVersion = o.optString("ver", "?"),
            battery = o.optInt("batt", -1),
            status = o.optString("status", "idle"),
            proto = o.optInt("proto", 0)
        )
    } catch (_: Exception) {
        null
    }

    fun encodeMessage(path: String, payloadJson: String): String =
        JSONObject().apply {
            put("path", path)
            put("payload", payloadJson)
        }.toString()

    /** Returns (path, payload) for a valid frame, else null. */
    fun parseMessage(line: String): Pair<String, JSONObject>? = try {
        val o = JSONObject(line)
        val path = o.optString("path")
        if (path.isBlank()) null
        else path to (o.optJSONObject("payload") ?: JSONObject(o.optString("payload", "{}")))
    } catch (_: Exception) {
        null
    }

    /** The /miles/hello payload that introduces this app to a TCP peer. */
    fun helloPayload(b: Beacon): JSONObject = JSONObject().apply {
        put("role", b.role)
        put("id", b.id)
        put("name", b.name)
        put("model", b.model)
        put("tcp", b.tcpPort)
        put("ver", b.appVersion)
        put("batt", b.battery)
        put("status", b.status)
    }

    fun beaconFromHello(payload: JSONObject): Beacon? {
        val role = payload.optString("role")
        val id = payload.optString("id")
        if (role.isBlank() || id.isBlank()) return null
        return Beacon(
            role = role,
            id = id,
            name = payload.optString("name", "MILES"),
            model = payload.optString("model", "Android"),
            tcpPort = payload.optInt("tcp", 0),
            appVersion = payload.optString("ver", "?"),
            battery = payload.optInt("batt", -1),
            status = payload.optString("status", "idle")
        )
    }
}