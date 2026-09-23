package com.example.miles.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Parsed weather snapshot for the dashboard. */
data class WeatherInfo(
    val temperatureC: Double? = null,
    val condition: String? = null,
    val windSpeedKmh: Double? = null,
    val windDirection: String? = null,
    val humidityPct: Int? = null,
    val uvIndex: Double? = null,
    val uvCategory: String? = null,
    val sunset: String? = null
) {
    val isAvailable: Boolean
        get() = temperatureC != null && condition != null
}

/**
 * Fetches real weather from Open-Meteo (free, no API key, no account).
 * Uses the device's last known GPS/network location; returns an empty
 * [WeatherInfo] when location or network is unavailable.
 */
object WeatherFetcher {

    suspend fun fetch(context: Context): WeatherInfo = withContext(Dispatchers.IO) {
        val loc = lastKnownLocation(context)
        if (loc == null) return@withContext WeatherInfo()

        val endpoint = StringBuilder("https://api.open-meteo.com/v1/forecast")
            .append("?latitude=").append(loc.latitude)
            .append("&longitude=").append(loc.longitude)
            .append("&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m")
            .append("&daily=uv_index_max,sunset")
            .append("&timezone=auto")
            .append("&forecast_days=1")
            .toString()

        return@withContext runCatching {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "MILES-Android-App/2.5 (contact: bhatiaansh716@gmail.com)")
            }
            try {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)

                val current = root.optJSONObject("current")
                val dailyValues = root.optJSONObject("daily")

                val temperature = current
                    ?.optDouble("temperature_2m", Double.NaN)
                    ?.takeIf { !it.isNaN() }
                val humidity = current
                    ?.optInt("relative_humidity_2m", -1)
                    ?.takeIf { it >= 0 }
                val code = current?.optInt("weather_code", -1)
                val windSpeed = current
                    ?.optDouble("wind_speed_10m", Double.NaN)
                    ?.takeIf { !it.isNaN() }
                val windDeg = current
                    ?.optDouble("wind_direction_10m", Double.NaN)
                    ?.takeIf { !it.isNaN() }
                val uv = dailyValues
                    ?.optJSONArray("uv_index_max")
                    ?.optDouble(0, Double.NaN)
                    ?.takeIf { !it.isNaN() }
                val sunsetISO = dailyValues?.optJSONArray("sunset")?.optString(0, "")

                WeatherInfo(
                    temperatureC = temperature,
                    condition = code?.takeIf { it >= 0 }?.let { conditionFor(it) },
                    windSpeedKmh = windSpeed,
                    windDirection = windDeg?.let { compassDirection(it) },
                    humidityPct = humidity,
                    uvIndex = uv,
                    uvCategory = uv?.let { uvCategoryFor(it) },
                    sunset = sunsetISO
                        ?.takeIf { it.isNotBlank() }
                        ?.let { it.substringAfterLast("T").take(5) }
                        ?.takeIf { it.isNotBlank() }
                )
            } finally {
                conn.disconnect()
            }
        }.getOrElse { WeatherInfo() }
    }

    /** WMO weather interpretation codes → human readable label. */
    private fun conditionFor(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mainly Clear"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51 -> "Light Drizzle"
        53 -> "Drizzle"
        55 -> "Heavy Drizzle"
        56, 57 -> "Freezing Drizzle"
        61 -> "Light Rain"
        63 -> "Rain"
        65 -> "Heavy Rain"
        66, 67 -> "Freezing Rain"
        71 -> "Light Snow"
        73 -> "Snow"
        75 -> "Heavy Snow"
        77 -> "Snow Grains"
        80 -> "Light Rain Showers"
        81 -> "Rain Showers"
        82 -> "Heavy Rain Showers"
        85, 86 -> "Snow Showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm & Hail"
        else -> "Unknown"
    }

    private fun compassDirection(deg: Double): String {
        val dirs = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        return dirs[(((deg % 360) + 360) % 360 / 22.5).toInt()]
    }

    private fun uvCategoryFor(index: Double): String = when {
        index < 3.0 -> "Low"
        index < 6.0 -> "Moderate"
        index < 8.0 -> "High"
        index < 11.0 -> "Very High"
        else -> "Extreme"
    }

    private fun lastKnownLocation(context: Context): Location? {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return runCatching {
            listOfNotNull(
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER),
                lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER),
                lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            ).maxByOrNull { it.time }
        }.getOrNull()
    }
}