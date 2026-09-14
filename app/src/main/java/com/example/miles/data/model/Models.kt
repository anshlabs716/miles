package com.example.miles.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ActivityType(val displayName: String, val metScore: Double) {
    WALKING("Walking", 3.8),
    RUNNING("Running", 9.8),
    CYCLING("Cycling", 7.5),
    HIKING("Hiking", 6.0),
    OTHER("Workout", 5.0);

    companion object {
        fun fromString(value: String): ActivityType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: WALKING
        }
    }
}

enum class WaypointType(val label: String) {
    WATER("Water Station"),
    REST("Rest Stop"),
    TURN("Key Turn"),
    DESTINATION("Destination"),
    CUSTOM("Point of Interest")
}

data class GpsPoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val speed: Float = 0f, // m/s
    val bearing: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class Waypoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: WaypointType = WaypointType.CUSTOM,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class WeatherSnapshot(
    val temperatureC: Float = 20f,
    val condition: String = "Clear",
    val humidityPercent: Int = 45,
    val windKmh: Float = 8f,
    val isNight: Boolean = false,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val activityType: String = ActivityType.WALKING.name,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val distanceMeters: Double,
    val steps: Int = 0,
    val avgPaceSecPerKm: Double = 0.0,
    val bestPaceSecPerKm: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val elevationLossM: Double = 0.0,
    val calories: Int = 0,
    val avgHeartRate: Int = 0,
    val maxHeartRate: Int = 0,
    val routePointsJson: String = "[]",
    val waypointsJson: String = "[]",
    val weatherJson: String = "",
    val notes: String = "",
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val version: Int = 1,
    val sensorSource: String = "Built-in GPS"
)

@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val name: String = title,
    val description: String = "",
    val activityType: String = ActivityType.WALKING.name,
    val distanceMeters: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val routePointsJson: String = "[]",
    val waypointsJson: String = "[]",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "privacy_zones")
data class PrivacyZoneEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 250f,
    val isEnabled: Boolean = true
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String, // STEPS, DISTANCE, DURATION, ACTIVITIES
    val targetValue: Double,
    val period: String = "DAILY", // DAILY, WEEKLY, MONTHLY
    val createdAt: Long = System.currentTimeMillis()
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val isUnlocked: Boolean,
    val progressPercent: Float = 0f,
    val unlockedAt: Long? = null
)
