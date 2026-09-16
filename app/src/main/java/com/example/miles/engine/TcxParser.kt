package com.example.miles.engine

import com.example.miles.data.model.GpsPoint
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/** Small, dependency-free TCX reader for activity tracks. */
object TcxParser {
    data class Track(val title: String, val startTime: Long, val points: List<GpsPoint>)

    fun parse(xml: String): Track {
        require(xml.isNotBlank()) { "Empty TCX document" }
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        val nodes = document.getElementsByTagNameNS("*", "Trackpoint")
        val points = mutableListOf<GpsPoint>()
        for (i in 0 until nodes.length) {
            val trackpoint = nodes.item(i) as? Element ?: continue
            val lat = text(trackpoint, "Latitude")?.toDoubleOrNull() ?: continue
            val lon = text(trackpoint, "Longitude")?.toDoubleOrNull() ?: continue
            if (lat !in -90.0..90.0 || lon !in -180.0..180.0) continue
            val altitude = text(trackpoint, "AltitudeMeters")?.toDoubleOrNull() ?: 0.0
            val time = text(trackpoint, "Time")?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() } ?: continue
            val speed = text(trackpoint, "Speed")?.toFloatOrNull() ?: 0f
            points += GpsPoint(lat, lon, altitude, 0f, speed, 0f, time)
        }
        points.sortBy { it.timestamp }
        require(points.isNotEmpty()) { "TCX contains no valid GPS trackpoints" }
        val title = text(document.documentElement, "Activity") ?: "Imported TCX Activity"
        return Track(title, points.first().timestamp, points)
    }

    private fun text(parent: Element, localName: String): String? {
        val nodes = parent.getElementsByTagNameNS("*", localName)
        return if (nodes.length == 0) null else nodes.item(0).textContent?.trim()
    }
}
