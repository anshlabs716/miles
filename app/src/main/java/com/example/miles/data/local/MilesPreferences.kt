package com.example.miles.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BaseThemeOption(val label: String) {
    TWILIGHT("Cosmic Twilight"),
    AMOLED("Pure AMOLED"),
    AMOLED_RED("AMOLED Crimson"),
    LIGHT("Clean Light"),
    STOCK("Material You")
}

enum class AppIconOption(
    val label: String,
    val aliasClass: String,
    val colorHex: Long = 0xFF00E5FF,
    val subtitle: String = ""
) {
    DEFAULT("Twilight Cyan", "com.example.MainActivityDefault", 0xFF00E5FF, "Original electric cyan runner on dark navy"),
    AMOLED_RED("AMOLED Crimson", "com.example.MainActivityAmoledRed", 0xFFFF2D55, "Deep crimson runner on pure pitch AMOLED black"),
    TWILIGHT("Twilight Violet", "com.example.MainActivityTwilight", 0xFFB388FF, "Neon purple runner in twilight nebula"),
    EMERALD("Emerald Sprint", "com.example.MainActivityEmerald", 0xFF00E676, "Hyper emerald green on deep forest"),
    MONOCHROME("Pitch Monochrome", "com.example.MainActivityMonochrome", 0xFFEEEEEE, "Minimalist stark white on obsidian"),
    SOLAR_GOLD("Solar Kinetic Gold", "com.example.MainActivitySolar", 0xFFFFD600, "Radiant amber gold on carbon dark"),
    CYBER_CYAN("Cyberpunk Neon", "com.example.MainActivityCyber", 0xFF00F0FF, "Dual-tone hot pink & electric cyan"),
    ARCTIC_FROST("Arctic Frost", "com.example.MainActivityArctic", 0xFF00B0FF, "Ice cyan runner on glacial silver"),
    SUNSET_BLAZE("Sunset Blaze", "com.example.MainActivitySunset", 0xFFFF6D00, "Tangerine coral flame on dark slate"),
    RETRO_SYNTH("Retro 80s Synth", "com.example.MainActivityRetro", 0xFFFF007F, "Hot magenta runner with synthwave grid"),
    ELECTRIC_LIME("Electric Lime", "com.example.MainActivityLime", 0xFFAEEA00, "High-vis acid lime sprint runner"),
    ROYAL_GOLD("Royal Obsidian", "com.example.MainActivityRoyal", 0xFFFFC107, "Metallic luxury gold on deep onyx");

    val displayName: String get() = label
}

enum class DistanceUnit(val label: String, val paceLabel: String, val symbol: String) {
    METRIC("Kilometers (km)", "min/km", "km"),
    IMPERIAL("Miles (mi)", "min/mi", "mi")
}

enum class ColorVisionMode(val label: String, val description: String) {
    NORMAL("Normal Vision", "Standard vibrant color reproduction"),
    PROTANOPIA("Protanopia", "Red-weak deficiency (enhanced cyan & amber contrast)"),
    DEUTERANOPIA("Deuteranopia", "Green-weak deficiency (high contrast blue-orange shifts)"),
    TRITANOPIA("Tritanopia", "Blue-yellow deficiency (crisp magenta and teal accents)"),
    HIGH_CONTRAST("High Contrast Monochrome", "Ultra-high luminance pure black & white edges")
}

enum class PetType(val displayName: String, val emoji: String, val defaultName: String, val favoriteTreat: String) {
    OFF("Keep Off", "🚫", "None", "None"),
    DOG("Dog", "🐕", "Barkley", "Bone 🦴"),
    CAT("Cat", "🐱", "Mochi", "Fish 🐟"),
    PARROT("Parrot", "🦜", "Rio", "Seed 🌻"),
    BUNNY("Bunny", "🐰", "Thumper", "Carrot 🥕")
}

enum class TrackingSourceMode(val label: String, val shortLabel: String, val description: String) {
    ALL("All 3 Combined (Fused)", "All 3", "GPS + Step Sensors + Bluetooth Tunneling"),
    GPS_ONLY("GPS Only", "GPS", "High precision satellite telemetry only"),
    SENSORS_ONLY("Sensors Only", "Sensors", "Built-in hardware pedometer & accelerometer"),
    BLUETOOTH_TUNNEL("Bluetooth Tunnel", "BT Tunnel", "External BLE sensors & Wear OS watch bridge"),
    CUSTOM("Custom Combination", "Custom", "Toggle individual sensor feeds manually")
}

data class AccessibilitySettings(
    val largerText: Boolean = false,
    val largerUi: Boolean = false,
    val boldText: Boolean = false,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val disableAnimations: Boolean = false,
    val colorVisionMode: ColorVisionMode = ColorVisionMode.NORMAL,
    val largeTouchTargets: Boolean = false
)

data class UserPreferences(
    val hasCompletedSetup: Boolean = false,
    val healthConnectFirstBootHandled: Boolean = false,
    val permissionPromptShown: Boolean = false,
    val userName: String = "",
    val primarySport: String = "RUNNING",
    val dailyStepGoal: Int = 8000,
    val dailyActiveMinutesGoal: Int = 45,
    val dailyCaloriesGoal: Int = 500,
    val weeklyDistanceGoalKm: Float = 25.0f,
    val theme: BaseThemeOption = BaseThemeOption.TWILIGHT,
    val liquidGlassEnabled: Boolean = true,
    val appIcon: AppIconOption = AppIconOption.DEFAULT,
    val unit: DistanceUnit = DistanceUnit.METRIC,
    val gamificationEnabled: Boolean = true,
    val voiceAnnouncements: Boolean = false,
    val autoPause: Boolean = true,
    val liveActivities: Boolean = true,
    val smartDnd: Boolean = false,
    val batterySaverTracking: Boolean = false,
    val counterIntervalMs: Long = 100L,
    val telemetryIntervalMs: Long = 3000L,
    val accessibility: AccessibilitySettings = AccessibilitySettings(),

    // Athlete Profile & Biometrics
    val userWeightKg: Float = 70.0f,
    val userHeightCm: Float = 175.0f,
    val userAge: Int = 28,
    val userGender: String = "UNSPECIFIED",
    val maxHeartRateBpm: Int = 190,
    val restingHeartRateBpm: Int = 60,
    val lactateThresholdHrBpm: Int = 168,
    val athleteLevel: String = "INTERMEDIATE",

    // Workout & Audio Coaching
    val audioCueIntervalKm: Float = 1.0f,
    val audioCueVolumePercent: Int = 80,
    val audioSpeechRate: Float = 1.0f,
    val audioAnnouncePace: Boolean = true,
    val audioAnnounceHeartRate: Boolean = true,
    val audioAnnounceDistance: Boolean = true,
    val audioAnnounceCadence: Boolean = false,
    val countdownSeconds: Int = 3,
    val autoPauseSensitivityKmh: Float = 1.5f,
    val cadenceMetronomeEnabled: Boolean = false,
    val targetCadenceSpm: Int = 170,
    val hrZoneAlarmEnabled: Boolean = false,
    val hrZoneAlarmBpm: Int = 180,

    // Map & Navigation
    val defaultMapLayer: String = "OPEN_STREET_MAP",
    val mapAutoFollow: Boolean = true,
    val mapKeepScreenOn: Boolean = true,
    val mapPolylineColorMode: String = "SPEED_GRADIENT",
    val mapPolylineStrokeWidthDp: Int = 5,
    val mapHeadingRotationMode: String = "NORTH_UP",
    val ghostPacerMode: String = "PERSONAL_RECORD",
    val targetPaceSecPerKm: Double = 330.0,

    // Developer & Diagnostics
    val devModeUnlocked: Boolean = true,
    val mockGpsEnabled: Boolean = false,
    val mockGpsLat: Double = 37.7749,
    val mockGpsLon: Double = -122.4194,
    val mockHeartRateBpm: Int = 0,
    val minGpsAccuracyFilterMeters: Float = 25.0f,
    val verboseLogging: Boolean = false,
    val gpsProviderMode: String = "HARDWARE_GPS",

    // Nerdy GNSS & Kalman Engine
    val kalmanProcessNoiseQ: Float = 0.005f,
    val kalmanMeasurementNoiseR: Float = 6.0f,
    val minHeadingSpeedMps: Float = 0.8f,
    val maxHdopThreshold: Float = 4.0f,
    val gnssGpsEnabled: Boolean = true,
    val gnssGlonassEnabled: Boolean = true,
    val gnssGalileoEnabled: Boolean = true,
    val gnssBeidouEnabled: Boolean = true,
    val gnssQzssEnabled: Boolean = true,
    val gnssSbasEnabled: Boolean = true,
    val deadReckoningDurationSec: Int = 10,
    val multipathFilterEnabled: Boolean = true,
    val nmeaLoggingEnabled: Boolean = false,

    // Nerdy Biomechanics & Physics
    val runningPowerEnabled: Boolean = true,
    val dragCoefficientCdA: Float = 0.24f,
    val airDensityRho: Float = 1.225f,
    val windSpeedKmh: Float = 0.0f,
    val gapAlgorithm: String = "MINETTI_2002",
    val groundContactTimeMs: Int = 240,
    val verticalOscillationCm: Float = 8.5f,
    val vdotModel: String = "JACK_DANIELS",
    val trimpModel: String = "BANNISTER_EXPONENTIAL",
    val stepSensitivityThreshold: Float = 1.2f,
    val barometerQnhHpa: Float = 1013.25f,

    // Virtual Fitness Companion Pet (Dog, Cat, Parrot, Bunny, or Off)
    val petType: PetType = PetType.DOG,
    val petName: String = "Barkley",

    // Lazy Days (Cheat / Rest Days - max 2 to 3 per week)
    val isTodayLazyDay: Boolean = false,
    val lazyDaysThisWeek: Int = 0,
    val maxLazyDaysPerWeek: Int = 3,
    val lastLazyDayDate: String = "",

    // Hardware Sensors & Tunneling Controls
    val trackingSourceMode: TrackingSourceMode = TrackingSourceMode.ALL,
    val gpsSensorEnabled: Boolean = true,
    val stepSensorHardwareEnabled: Boolean = true,
    val bluetoothTunnelingEnabled: Boolean = true,
    val heartRateSensorEnabled: Boolean = true,
    val sensorRefreshRateMs: Long = 1000L,

    // Custom Get Up & Move Idle Reminders
    val moveReminderEnabled: Boolean = true,
    val moveReminderIntervalMinutes: Int = 45,
    val moveReminderCustomText: String = "Time to stretch and get moving! Take 250 steps."
)

class MilesPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("miles_settings", Context.MODE_PRIVATE)

    private val _userPreferences = MutableStateFlow(loadPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        val hasSetup = prefs.getBoolean("has_completed_setup", false)
        val healthConnectFirstBootHandled = prefs.getBoolean("health_connect_first_boot_handled", false)
        val permissionPromptShown = prefs.getBoolean("permission_prompt_shown", false)
        val userName = prefs.getString("user_name", "") ?: ""
        val primarySport = prefs.getString("primary_sport", "RUNNING") ?: "RUNNING"
        val stepGoal = prefs.getInt("daily_step_goal", 8000)
        val activeMinGoal = prefs.getInt("daily_active_min_goal", 45)
        val calorieGoal = prefs.getInt("daily_calorie_goal", 500)
        val weeklyDistGoal = prefs.getFloat("weekly_dist_goal", 25.0f)

        val themeStr = prefs.getString("theme", BaseThemeOption.TWILIGHT.name) ?: BaseThemeOption.TWILIGHT.name
        val theme = runCatching { BaseThemeOption.valueOf(themeStr) }.getOrDefault(BaseThemeOption.TWILIGHT)

        val iconStr = prefs.getString("app_icon", AppIconOption.DEFAULT.name) ?: AppIconOption.DEFAULT.name
        val appIcon = runCatching { AppIconOption.valueOf(iconStr) }.getOrDefault(AppIconOption.DEFAULT)

        val unitStr = prefs.getString("unit", DistanceUnit.METRIC.name) ?: DistanceUnit.METRIC.name
        val unit = runCatching { DistanceUnit.valueOf(unitStr) }.getOrDefault(DistanceUnit.METRIC)

        val colorVisionStr = prefs.getString("color_vision", ColorVisionMode.NORMAL.name) ?: ColorVisionMode.NORMAL.name
        val colorVision = runCatching { ColorVisionMode.valueOf(colorVisionStr) }.getOrDefault(ColorVisionMode.NORMAL)

        val access = AccessibilitySettings(
            largerText = prefs.getBoolean("acc_larger_text", false),
            largerUi = prefs.getBoolean("acc_larger_ui", false),
            boldText = prefs.getBoolean("acc_bold_text", false),
            highContrast = prefs.getBoolean("acc_high_contrast", false),
            reducedMotion = prefs.getBoolean("acc_reduced_motion", false),
            disableAnimations = prefs.getBoolean("acc_disable_anim", false),
            colorVisionMode = colorVision,
            largeTouchTargets = prefs.getBoolean("acc_large_targets", false)
        )

        return UserPreferences(
            hasCompletedSetup = hasSetup,
            healthConnectFirstBootHandled = healthConnectFirstBootHandled,
            permissionPromptShown = permissionPromptShown,
            userName = userName,
            primarySport = primarySport,
            dailyStepGoal = stepGoal,
            dailyActiveMinutesGoal = activeMinGoal,
            dailyCaloriesGoal = calorieGoal,
            weeklyDistanceGoalKm = weeklyDistGoal,
            theme = theme,
            liquidGlassEnabled = prefs.getBoolean("liquid_glass", true),
            appIcon = appIcon,
            unit = unit,
            gamificationEnabled = prefs.getBoolean("gamification", true),
            voiceAnnouncements = prefs.getBoolean("voice_announcements", false),
            autoPause = prefs.getBoolean("auto_pause", true),
            liveActivities = prefs.getBoolean("live_activities", true),
            smartDnd = prefs.getBoolean("smart_dnd", false),
            batterySaverTracking = prefs.getBoolean("battery_saver_tracking", false),
            counterIntervalMs = prefs.getLong("counter_interval_ms", 100L).coerceIn(10L, 600_000L),
            telemetryIntervalMs = prefs.getLong("telemetry_interval_ms", 3000L).coerceIn(100L, 600_000L),
            accessibility = access,

            userWeightKg = prefs.getFloat("user_weight_kg", 70.0f),
            userHeightCm = prefs.getFloat("user_height_cm", 175.0f),
            userAge = prefs.getInt("user_age", 28),
            userGender = prefs.getString("user_gender", "UNSPECIFIED") ?: "UNSPECIFIED",
            maxHeartRateBpm = prefs.getInt("max_hr_bpm", 190),
            restingHeartRateBpm = prefs.getInt("resting_hr_bpm", 60),
            lactateThresholdHrBpm = prefs.getInt("lactate_threshold_hr_bpm", 168),
            athleteLevel = prefs.getString("athlete_level", "INTERMEDIATE") ?: "INTERMEDIATE",

            audioCueIntervalKm = prefs.getFloat("audio_cue_interval_km", 1.0f),
            audioCueVolumePercent = prefs.getInt("audio_cue_vol", 80),
            audioSpeechRate = prefs.getFloat("audio_speech_rate", 1.0f),
            audioAnnouncePace = prefs.getBoolean("audio_ann_pace", true),
            audioAnnounceHeartRate = prefs.getBoolean("audio_ann_hr", true),
            audioAnnounceDistance = prefs.getBoolean("audio_ann_dist", true),
            audioAnnounceCadence = prefs.getBoolean("audio_ann_cad", false),
            countdownSeconds = prefs.getInt("countdown_seconds", 3),
            autoPauseSensitivityKmh = prefs.getFloat("auto_pause_sens", 1.5f),
            cadenceMetronomeEnabled = prefs.getBoolean("cadence_metronome", false),
            targetCadenceSpm = prefs.getInt("target_cadence_spm", 170),
            hrZoneAlarmEnabled = prefs.getBoolean("hr_zone_alarm", false),
            hrZoneAlarmBpm = prefs.getInt("hr_zone_alarm_bpm", 180),

            defaultMapLayer = prefs.getString("default_map_layer", "OPEN_STREET_MAP") ?: "OPEN_STREET_MAP",
            mapAutoFollow = prefs.getBoolean("map_auto_follow", true),
            mapKeepScreenOn = prefs.getBoolean("map_keep_screen_on", true),
            mapPolylineColorMode = prefs.getString("map_polyline_color", "SPEED_GRADIENT") ?: "SPEED_GRADIENT",
            mapPolylineStrokeWidthDp = prefs.getInt("map_polyline_stroke", 5),
            mapHeadingRotationMode = prefs.getString("map_heading_mode", "NORTH_UP") ?: "NORTH_UP",
            ghostPacerMode = prefs.getString("ghost_pacer_mode", "PERSONAL_RECORD") ?: "PERSONAL_RECORD",
            targetPaceSecPerKm = prefs.getFloat("target_pace_sec_km", 330.0f).toDouble(),

            devModeUnlocked = prefs.getBoolean("dev_mode_unlocked", true),
            mockGpsEnabled = prefs.getBoolean("mock_gps_enabled", false),
            mockGpsLat = prefs.getFloat("mock_gps_lat", 37.7749f).toDouble(),
            mockGpsLon = prefs.getFloat("mock_gps_lon", -122.4194f).toDouble(),
            mockHeartRateBpm = prefs.getInt("mock_hr_bpm", 0),
            minGpsAccuracyFilterMeters = prefs.getFloat("min_gps_acc_filter", 25.0f),
            verboseLogging = prefs.getBoolean("verbose_logging", false),
            gpsProviderMode = prefs.getString("gps_provider_mode", "HARDWARE_GPS") ?: "HARDWARE_GPS",

            kalmanProcessNoiseQ = prefs.getFloat("kalman_q", 0.005f),
            kalmanMeasurementNoiseR = prefs.getFloat("kalman_r", 6.0f),
            minHeadingSpeedMps = prefs.getFloat("min_heading_speed", 0.8f),
            maxHdopThreshold = prefs.getFloat("max_hdop", 4.0f),
            gnssGpsEnabled = prefs.getBoolean("gnss_gps", true),
            gnssGlonassEnabled = prefs.getBoolean("gnss_glonass", true),
            gnssGalileoEnabled = prefs.getBoolean("gnss_galileo", true),
            gnssBeidouEnabled = prefs.getBoolean("gnss_beidou", true),
            gnssQzssEnabled = prefs.getBoolean("gnss_qzss", true),
            gnssSbasEnabled = prefs.getBoolean("gnss_sbas", true),
            deadReckoningDurationSec = prefs.getInt("dead_reckoning_sec", 10),
            multipathFilterEnabled = prefs.getBoolean("multipath_filter", true),
            nmeaLoggingEnabled = prefs.getBoolean("nmea_logging", false),

            runningPowerEnabled = prefs.getBoolean("running_power_en", true),
            dragCoefficientCdA = prefs.getFloat("drag_cda", 0.24f),
            airDensityRho = prefs.getFloat("air_rho", 1.225f),
            windSpeedKmh = prefs.getFloat("wind_kmh", 0.0f),
            gapAlgorithm = prefs.getString("gap_algo", "MINETTI_2002") ?: "MINETTI_2002",
            groundContactTimeMs = prefs.getInt("gct_ms", 240),
            verticalOscillationCm = prefs.getFloat("vert_osc_cm", 8.5f),
            vdotModel = prefs.getString("vdot_model", "JACK_DANIELS") ?: "JACK_DANIELS",
            trimpModel = prefs.getString("trimp_model", "BANNISTER_EXPONENTIAL") ?: "BANNISTER_EXPONENTIAL",
            stepSensitivityThreshold = prefs.getFloat("step_sens", 1.2f),
            barometerQnhHpa = prefs.getFloat("baro_qnh", 1013.25f),

            petType = runCatching { PetType.valueOf(prefs.getString("pet_type", PetType.DOG.name) ?: PetType.DOG.name) }.getOrDefault(PetType.DOG),
            petName = prefs.getString("pet_name", "Barkley") ?: "Barkley",

            isTodayLazyDay = prefs.getBoolean("is_today_lazy", false),
            lazyDaysThisWeek = prefs.getInt("lazy_days_week", 0),
            maxLazyDaysPerWeek = prefs.getInt("max_lazy_days", 3),
            lastLazyDayDate = prefs.getString("last_lazy_date", "") ?: "",

            trackingSourceMode = runCatching {
                TrackingSourceMode.valueOf(prefs.getString("tracking_source_mode", TrackingSourceMode.ALL.name) ?: TrackingSourceMode.ALL.name)
            }.getOrDefault(TrackingSourceMode.ALL),
            gpsSensorEnabled = prefs.getBoolean("gps_sensor_en", true),
            stepSensorHardwareEnabled = prefs.getBoolean("step_sensor_hw_en", true),
            bluetoothTunnelingEnabled = prefs.getBoolean("bt_tunnel_en", true),
            heartRateSensorEnabled = prefs.getBoolean("hr_sensor_en", true),
            sensorRefreshRateMs = prefs.getLong("sensor_refresh_ms", 1000L),

            moveReminderEnabled = prefs.getBoolean("move_rem_en", true),
            moveReminderIntervalMinutes = prefs.getInt("move_rem_int", 45),
            moveReminderCustomText = prefs.getString("move_rem_text", "Time to stretch and get moving! Take 250 steps.") ?: "Time to stretch and get moving! Take 250 steps."
        )
    }

    fun setCounterIntervalMs(intervalMs: Long) {
        val clamped = intervalMs.coerceIn(10L, 600_000L)
        prefs.edit().putLong("counter_interval_ms", clamped).apply()
        _userPreferences.value = _userPreferences.value.copy(counterIntervalMs = clamped)
    }

    fun setCounterInterval(intervalMs: Long) = setCounterIntervalMs(intervalMs)

    fun setTelemetryIntervalMs(intervalMs: Long) {
        val clamped = intervalMs.coerceIn(100L, 600_000L)
        prefs.edit().putLong("telemetry_interval_ms", clamped).apply()
        _userPreferences.value = _userPreferences.value.copy(telemetryIntervalMs = clamped)
    }

    fun setTelemetryInterval(intervalMs: Long) = setTelemetryIntervalMs(intervalMs)

    fun completeSetup(
        userName: String,
        primarySport: String,
        dailyStepGoal: Int,
        dailyActiveMinutesGoal: Int,
        weeklyDistanceGoalKm: Float,
        unit: DistanceUnit,
        theme: BaseThemeOption,
        liquidGlassEnabled: Boolean
    ) {
        prefs.edit()
            .putBoolean("has_completed_setup", true)
            .putString("user_name", userName)
            .putString("primary_sport", primarySport)
            .putInt("daily_step_goal", dailyStepGoal)
            .putInt("daily_active_min_goal", dailyActiveMinutesGoal)
            .putFloat("weekly_dist_goal", weeklyDistanceGoalKm)
            .putString("unit", unit.name)
            .putString("theme", theme.name)
            .putBoolean("liquid_glass", liquidGlassEnabled)
            .apply()

        _userPreferences.value = _userPreferences.value.copy(
            hasCompletedSetup = true,
            userName = userName,
            primarySport = primarySport,
            dailyStepGoal = dailyStepGoal,
            dailyActiveMinutesGoal = dailyActiveMinutesGoal,
            weeklyDistanceGoalKm = weeklyDistanceGoalKm,
            unit = unit,
            theme = theme,
            liquidGlassEnabled = liquidGlassEnabled
        )
    }

    fun resetSetup() {
        prefs.edit().putBoolean("has_completed_setup", false).apply()
        _userPreferences.value = _userPreferences.value.copy(hasCompletedSetup = false)
    }

    fun markHealthConnectFirstBootHandled() {
        prefs.edit().putBoolean("health_connect_first_boot_handled", true).apply()
        _userPreferences.value = _userPreferences.value.copy(healthConnectFirstBootHandled = true)
    }

    fun markPermissionPromptShown() {
        prefs.edit().putBoolean("permission_prompt_shown", true).apply()
        _userPreferences.value = _userPreferences.value.copy(permissionPromptShown = true)
    }

    fun setTheme(theme: BaseThemeOption) {
        prefs.edit().putString("theme", theme.name).apply()
        _userPreferences.value = _userPreferences.value.copy(theme = theme)
    }

    fun setLiquidGlass(enabled: Boolean) {
        prefs.edit().putBoolean("liquid_glass", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(liquidGlassEnabled = enabled)
    }

    val appIconManager = com.example.miles.engine.AppIconManager(context)

    fun setAppIcon(icon: AppIconOption) {
        prefs.edit().putString("app_icon", icon.name).apply()
        _userPreferences.value = _userPreferences.value.copy(appIcon = icon)
        appIconManager.setAppIcon(icon)
    }

    fun refreshAppIconState(): Pair<AppIconOption, String> {
        val (activeIcon, message) = appIconManager.refreshLauncherStatus()
        // Ensure current preference syncs if out of sync
        if (_userPreferences.value.appIcon != activeIcon) {
            prefs.edit().putString("app_icon", activeIcon.name).apply()
            _userPreferences.value = _userPreferences.value.copy(appIcon = activeIcon)
        } else {
            // Re-assert desired state in PackageManager
            appIconManager.setAppIcon(activeIcon)
        }
        return Pair(activeIcon, message)
    }

    fun setUnit(unit: DistanceUnit) {
        prefs.edit().putString("unit", unit.name).apply()
        _userPreferences.value = _userPreferences.value.copy(unit = unit)
    }

    fun setGamification(enabled: Boolean) {
        prefs.edit().putBoolean("gamification", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(gamificationEnabled = enabled)
    }

    fun setVoiceAnnouncements(enabled: Boolean) {
        prefs.edit().putBoolean("voice_announcements", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(voiceAnnouncements = enabled)
    }

    fun setAutoPause(enabled: Boolean) {
        prefs.edit().putBoolean("auto_pause", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(autoPause = enabled)
    }

    fun setLiveActivities(enabled: Boolean) {
        prefs.edit().putBoolean("live_activities", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(liveActivities = enabled)
    }

    fun setSmartDnd(enabled: Boolean) {
        prefs.edit().putBoolean("smart_dnd", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(smartDnd = enabled)
    }

    fun setBatterySaverTracking(enabled: Boolean) {
        prefs.edit().putBoolean("battery_saver_tracking", enabled).apply()
        _userPreferences.value = _userPreferences.value.copy(batterySaverTracking = enabled)
    }

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        val updated = transform(_userPreferences.value)
        prefs.edit()
            .putString("user_name", updated.userName)
            .putFloat("user_weight_kg", updated.userWeightKg)
            .putFloat("user_height_cm", updated.userHeightCm)
            .putInt("user_age", updated.userAge)
            .putString("user_gender", updated.userGender)
            .putInt("max_hr_bpm", updated.maxHeartRateBpm)
            .putInt("resting_hr_bpm", updated.restingHeartRateBpm)
            .putInt("lactate_threshold_hr_bpm", updated.lactateThresholdHrBpm)
            .putString("athlete_level", updated.athleteLevel)
            .putFloat("audio_cue_interval_km", updated.audioCueIntervalKm)
            .putInt("audio_cue_vol", updated.audioCueVolumePercent)
            .putFloat("audio_speech_rate", updated.audioSpeechRate)
            .putBoolean("audio_ann_pace", updated.audioAnnouncePace)
            .putBoolean("audio_ann_hr", updated.audioAnnounceHeartRate)
            .putBoolean("audio_ann_dist", updated.audioAnnounceDistance)
            .putBoolean("audio_ann_cad", updated.audioAnnounceCadence)
            .putInt("countdown_seconds", updated.countdownSeconds)
            .putFloat("auto_pause_sens", updated.autoPauseSensitivityKmh)
            .putBoolean("cadence_metronome", updated.cadenceMetronomeEnabled)
            .putInt("target_cadence_spm", updated.targetCadenceSpm)
            .putBoolean("hr_zone_alarm", updated.hrZoneAlarmEnabled)
            .putInt("hr_zone_alarm_bpm", updated.hrZoneAlarmBpm)
            .putString("default_map_layer", updated.defaultMapLayer)
            .putBoolean("map_auto_follow", updated.mapAutoFollow)
            .putBoolean("map_keep_screen_on", updated.mapKeepScreenOn)
            .putString("map_polyline_color", updated.mapPolylineColorMode)
            .putInt("map_polyline_stroke", updated.mapPolylineStrokeWidthDp)
            .putString("map_heading_mode", updated.mapHeadingRotationMode)
            .putString("ghost_pacer_mode", updated.ghostPacerMode)
            .putFloat("target_pace_sec_km", updated.targetPaceSecPerKm.toFloat())
            .putBoolean("dev_mode_unlocked", updated.devModeUnlocked)
            .putBoolean("mock_gps_enabled", updated.mockGpsEnabled)
            .putFloat("mock_gps_lat", updated.mockGpsLat.toFloat())
            .putFloat("mock_gps_lon", updated.mockGpsLon.toFloat())
            .putInt("mock_hr_bpm", updated.mockHeartRateBpm)
            .putFloat("min_gps_acc_filter", updated.minGpsAccuracyFilterMeters)
            .putBoolean("verbose_logging", updated.verboseLogging)
            .putString("gps_provider_mode", updated.gpsProviderMode)
            .putFloat("kalman_q", updated.kalmanProcessNoiseQ)
            .putFloat("kalman_r", updated.kalmanMeasurementNoiseR)
            .putFloat("min_heading_speed", updated.minHeadingSpeedMps)
            .putFloat("max_hdop", updated.maxHdopThreshold)
            .putBoolean("gnss_gps", updated.gnssGpsEnabled)
            .putBoolean("gnss_glonass", updated.gnssGlonassEnabled)
            .putBoolean("gnss_galileo", updated.gnssGalileoEnabled)
            .putBoolean("gnss_beidou", updated.gnssBeidouEnabled)
            .putBoolean("gnss_qzss", updated.gnssQzssEnabled)
            .putBoolean("gnss_sbas", updated.gnssSbasEnabled)
            .putInt("dead_reckoning_sec", updated.deadReckoningDurationSec)
            .putBoolean("multipath_filter", updated.multipathFilterEnabled)
            .putBoolean("nmea_logging", updated.nmeaLoggingEnabled)
            .putBoolean("running_power_en", updated.runningPowerEnabled)
            .putFloat("drag_cda", updated.dragCoefficientCdA)
            .putFloat("air_rho", updated.airDensityRho)
            .putFloat("wind_kmh", updated.windSpeedKmh)
            .putString("gap_algo", updated.gapAlgorithm)
            .putInt("gct_ms", updated.groundContactTimeMs)
            .putFloat("vert_osc_cm", updated.verticalOscillationCm)
            .putString("vdot_model", updated.vdotModel)
            .putString("trimp_model", updated.trimpModel)
            .putFloat("step_sens", updated.stepSensitivityThreshold)
            .putFloat("baro_qnh", updated.barometerQnhHpa)
            .putString("pet_type", updated.petType.name)
            .putString("pet_name", updated.petName)
            .putBoolean("is_today_lazy", updated.isTodayLazyDay)
            .putInt("lazy_days_week", updated.lazyDaysThisWeek)
            .putInt("max_lazy_days", updated.maxLazyDaysPerWeek)
            .putString("last_lazy_date", updated.lastLazyDayDate)
            .putBoolean("gps_sensor_en", updated.gpsSensorEnabled)
            .putBoolean("step_sensor_hw_en", updated.stepSensorHardwareEnabled)
            .putBoolean("bt_tunnel_en", updated.bluetoothTunnelingEnabled)
            .putBoolean("hr_sensor_en", updated.heartRateSensorEnabled)
            .putLong("sensor_refresh_ms", updated.sensorRefreshRateMs)
            .putBoolean("move_rem_en", updated.moveReminderEnabled)
            .putInt("move_rem_int", updated.moveReminderIntervalMinutes)
            .putString("move_rem_text", updated.moveReminderCustomText)
            .apply()
        _userPreferences.value = updated
    }

    fun toggleLazyDay(): Boolean {
        val current = _userPreferences.value
        if (!current.isTodayLazyDay && current.lazyDaysThisWeek >= current.maxLazyDaysPerWeek) {
            return false // Limit reached
        }
        val nextIsLazy = !current.isTodayLazyDay
        val nextUsed = if (nextIsLazy) current.lazyDaysThisWeek + 1 else (current.lazyDaysThisWeek - 1).coerceAtLeast(0)
        prefs.edit()
            .putBoolean("is_today_lazy", nextIsLazy)
            .putInt("lazy_days_week", nextUsed)
            .apply()
        _userPreferences.value = current.copy(
            isTodayLazyDay = nextIsLazy,
            lazyDaysThisWeek = nextUsed
        )
        return true
    }

    fun setDailyCaloriesGoal(calories: Int) {
        val clamped = calories.coerceIn(100, 10000)
        prefs.edit().putInt("daily_calorie_goal", clamped).apply()
        _userPreferences.value = _userPreferences.value.copy(dailyCaloriesGoal = clamped)
    }

    fun setMaxLazyDaysPerWeek(maxDays: Int) {
        val clamped = maxDays.coerceIn(2, 3)
        prefs.edit().putInt("max_lazy_days", clamped).apply()
        _userPreferences.value = _userPreferences.value.copy(maxLazyDaysPerWeek = clamped)
    }

    fun setPet(type: PetType, name: String? = null) {
        val chosenName = name ?: if (type != PetType.OFF) type.defaultName else "None"
        prefs.edit()
            .putString("pet_type", type.name)
            .putString("pet_name", chosenName)
            .apply()
        _userPreferences.value = _userPreferences.value.copy(
            petType = type,
            petName = chosenName
        )
    }

    fun setTrackingSourceMode(mode: TrackingSourceMode) {
        val cur = _userPreferences.value
        val (gps, steps, bt) = when (mode) {
            TrackingSourceMode.ALL -> Triple(true, true, true)
            TrackingSourceMode.GPS_ONLY -> Triple(true, false, false)
            TrackingSourceMode.SENSORS_ONLY -> Triple(false, true, false)
            TrackingSourceMode.BLUETOOTH_TUNNEL -> Triple(false, false, true)
            TrackingSourceMode.CUSTOM -> Triple(cur.gpsSensorEnabled, cur.stepSensorHardwareEnabled, cur.bluetoothTunnelingEnabled)
        }
        prefs.edit()
            .putString("tracking_source_mode", mode.name)
            .putBoolean("gps_sensor_en", gps)
            .putBoolean("step_sensor_hw_en", steps)
            .putBoolean("bt_tunnel_en", bt)
            .apply()
        _userPreferences.value = cur.copy(
            trackingSourceMode = mode,
            gpsSensorEnabled = gps,
            stepSensorHardwareEnabled = steps,
            bluetoothTunnelingEnabled = bt
        )
    }

    fun setSensorHardwareControls(
        gpsEnabled: Boolean? = null,
        stepHwEnabled: Boolean? = null,
        btTunnelEnabled: Boolean? = null,
        hrEnabled: Boolean? = null,
        refreshRateMs: Long? = null
    ) {
        val cur = _userPreferences.value
        val newGps = gpsEnabled ?: cur.gpsSensorEnabled
        val newStep = stepHwEnabled ?: cur.stepSensorHardwareEnabled
        val newBt = btTunnelEnabled ?: cur.bluetoothTunnelingEnabled
        val newHr = hrEnabled ?: cur.heartRateSensorEnabled
        val newRefresh = refreshRateMs ?: cur.sensorRefreshRateMs

        prefs.edit()
            .putBoolean("gps_sensor_en", newGps)
            .putBoolean("step_sensor_hw_en", newStep)
            .putBoolean("bt_tunnel_en", newBt)
            .putBoolean("hr_sensor_en", newHr)
            .putLong("sensor_refresh_ms", newRefresh)
            .apply()

        _userPreferences.value = cur.copy(
            gpsSensorEnabled = newGps,
            stepSensorHardwareEnabled = newStep,
            bluetoothTunnelingEnabled = newBt,
            heartRateSensorEnabled = newHr,
            sensorRefreshRateMs = newRefresh
        )
    }

    fun setMoveReminderSettings(
        enabled: Boolean,
        intervalMinutes: Int = 45,
        customMessage: String = "Time to stretch and get moving! Take 250 steps."
    ) {
        prefs.edit()
            .putBoolean("move_rem_en", enabled)
            .putInt("move_rem_int", intervalMinutes)
            .putString("move_rem_text", customMessage)
            .apply()

        _userPreferences.value = _userPreferences.value.copy(
            moveReminderEnabled = enabled,
            moveReminderIntervalMinutes = intervalMinutes,
            moveReminderCustomText = customMessage
        )
    }

    fun updateAccessibility(transform: (AccessibilitySettings) -> AccessibilitySettings) {
        val updated = transform(_userPreferences.value.accessibility)
        prefs.edit()
            .putBoolean("acc_larger_text", updated.largerText)
            .putBoolean("acc_larger_ui", updated.largerUi)
            .putBoolean("acc_bold_text", updated.boldText)
            .putBoolean("acc_high_contrast", updated.highContrast)
            .putBoolean("acc_reduced_motion", updated.reducedMotion)
            .putBoolean("acc_disable_anim", updated.disableAnimations)
            .putString("color_vision", updated.colorVisionMode.name)
            .putBoolean("acc_large_targets", updated.largeTouchTargets)
            .apply()
        _userPreferences.value = _userPreferences.value.copy(accessibility = updated)
    }

    fun resetAllPreferences() {
        prefs.edit().clear().apply()
        _userPreferences.value = loadPreferences()
    }

    fun exportRawPreferences(): Map<String, *> = prefs.all.toMap()

    fun importRawPreferences(values: org.json.JSONObject) {
        val editor = prefs.edit().clear()
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val value = values.get(key)) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Double -> editor.putFloat(key, value.toFloat())
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                else -> editor.putString(key, value.toString())
            }
        }
        editor.apply()
        _userPreferences.value = loadPreferences()
    }
}
