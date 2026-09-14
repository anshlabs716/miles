package com.example.miles.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.miles.data.local.BaseThemeOption

import com.example.miles.data.local.AccessibilitySettings
import com.example.miles.data.local.ColorVisionMode
import androidx.compose.ui.text.font.FontWeight

val LocalLiquidGlassEnabled = compositionLocalOf { true }
val LocalAccessibility = compositionLocalOf { AccessibilitySettings() }

// High Contrast Theme
private val HighContrastColorScheme = darkColorScheme(
    primary = Color(0xFFFFFF00), // Pure Vivid Yellow
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF333300),
    onPrimaryContainer = Color(0xFFFFFF99),
    secondary = Color(0xFF00FFFF), // Cyan
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003333),
    onSecondaryContainer = Color(0xFF99FFFF),
    tertiary = Color(0xFFFF5555),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFFFFFFF),
    outline = Color(0xFFFFFFFF)
)

// Protanopia Theme (optimized for red-blind)
private val ProtanopiaColorScheme = darkColorScheme(
    primary = Color(0xFF3A86FF), // Strong Blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003F99),
    onPrimaryContainer = Color(0xFFCCE4FF),
    secondary = Color(0xFFFFBE0B), // Strong Gold
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF664900),
    onSecondaryContainer = Color(0xFFFFEEB3),
    tertiary = Color(0xFFFB5607),
    background = Color(0xFF0D131F),
    onBackground = Color(0xFFEDF2F7),
    surface = Color(0xFF141D30),
    onSurface = Color(0xFFEDF2F7),
    surfaceVariant = Color(0xFF1E2A42),
    onSurfaceVariant = Color(0xFFCAD5E5),
    outline = Color(0xFF475878)
)

// Deuteranopia Theme (optimized for green-blind)
private val DeuteranopiaColorScheme = darkColorScheme(
    primary = Color(0xFF7209B7), // Deep Purple
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A0063),
    onPrimaryContainer = Color(0xFFE9C5FF),
    secondary = Color(0xFFF72585), // Magenta
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF7A043C),
    onSecondaryContainer = Color(0xFFFFCBE1),
    tertiary = Color(0xFF4CC9F0),
    background = Color(0xFF0E071A),
    onBackground = Color(0xFFF3EDFA),
    surface = Color(0xFF180E2B),
    onSurface = Color(0xFFF3EDFA),
    surfaceVariant = Color(0xFF271942),
    onSurfaceVariant = Color(0xFFD4C7E6),
    outline = Color(0xFF5B4385)
)

// Tritanopia Theme (optimized for blue-blind)
private val TritanopiaColorScheme = darkColorScheme(
    primary = Color(0xFFE63946), // Strong Red
    onPrimary = Color.White,
    primaryContainer = Color(0xFF730F17),
    onPrimaryContainer = Color(0xFFFFD4D7),
    secondary = Color(0xFF2A9D8F), // Teal
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF134C45),
    onSecondaryContainer = Color(0xFFBAEAE4),
    tertiary = Color(0xFFE76F51),
    background = Color(0xFF13080A),
    onBackground = Color(0xFFF6EEEE),
    surface = Color(0xFF200F12),
    onSurface = Color(0xFFF6EEEE),
    surfaceVariant = Color(0xFF331A1E),
    onSurfaceVariant = Color(0xFFE1CBCF),
    outline = Color(0xFF6B4248)
)

// MILES Custom Theme Color Palettes
// 1. LIGHT
private val MilesLightColorScheme = lightColorScheme(
    primary = Color(0xFF142980),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF001453),
    secondary = Color(0xFF007799),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7E7F8),
    onSecondaryContainer = Color(0xFF001F29),
    tertiary = Color(0xFFE64A19),
    background = Color(0xFFF6F8FD),
    onBackground = Color(0xFF131722),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF131722),
    surfaceVariant = Color(0xFFE4E8F2),
    onSurfaceVariant = Color(0xFF434752),
    outline = Color(0xFFC6CAD6)
)

// 2. TWILIGHT (The Signature MILES Space Vibe)
private val MilesTwilightColorScheme = darkColorScheme(
    primary = Color(0xFF7A70FF),
    onPrimary = Color(0xFF0C0E24),
    primaryContainer = Color(0xFF332B7A),
    onPrimaryContainer = Color(0xFFE6E2FF),
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F59),
    onSecondaryContainer = Color(0xFF82F2FF),
    tertiary = Color(0xFFFF5252),
    background = Color(0xFF0C0E1F),
    onBackground = Color(0xFFECEEF8),
    surface = Color(0xFF151833),
    onSurface = Color(0xFFECEEF8),
    surfaceVariant = Color(0xFF202447),
    onSurfaceVariant = Color(0xFFB5BACD),
    outline = Color(0xFF383E6E)
)

// 3. AMOLED (True Pure Black, Ultra Contrast)
private val MilesAmoledColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF003840),
    onPrimaryContainer = Color(0xFFB3F5FF),
    secondary = Color(0xFF69F0AE),
    onSecondary = Color(0xFF00381B),
    secondaryContainer = Color(0xFF005228),
    onSecondaryContainer = Color(0xFFB9FFCE),
    tertiary = Color(0xFFFFAB00),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0D0D0D),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = Color(0xFF2E2E2E)
)

// 4. AMOLED RED (Performance Racing Track Identity)
private val MilesAmoledRedColorScheme = darkColorScheme(
    primary = Color(0xFFFF2D55),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF5A0012),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = Color(0xFFFF6B81),
    onSecondary = Color(0xFF4A000E),
    secondaryContainer = Color(0xFF78081E),
    onSecondaryContainer = Color(0xFFFFDCE2),
    tertiary = Color(0xFFFF9100),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF120306),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF22070C),
    onSurfaceVariant = Color(0xFFD6C0C4),
    outline = Color(0xFF470E18)
)

@Composable
fun MilesTheme(
    themeOption: BaseThemeOption = BaseThemeOption.TWILIGHT,
    liquidGlassEnabled: Boolean = true,
    accessibility: AccessibilitySettings = AccessibilitySettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val baseColorScheme: ColorScheme = when {
        accessibility.highContrast -> HighContrastColorScheme
        accessibility.colorVisionMode == ColorVisionMode.PROTANOPIA -> ProtanopiaColorScheme
        accessibility.colorVisionMode == ColorVisionMode.DEUTERANOPIA -> DeuteranopiaColorScheme
        accessibility.colorVisionMode == ColorVisionMode.TRITANOPIA -> TritanopiaColorScheme
        accessibility.colorVisionMode == ColorVisionMode.HIGH_CONTRAST -> HighContrastColorScheme
        else -> when (themeOption) {
            BaseThemeOption.LIGHT -> MilesLightColorScheme
            BaseThemeOption.TWILIGHT -> MilesTwilightColorScheme
            BaseThemeOption.AMOLED -> MilesAmoledColorScheme
            BaseThemeOption.AMOLED_RED -> MilesAmoledRedColorScheme
            BaseThemeOption.STOCK -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    if (systemDark) MilesTwilightColorScheme else MilesLightColorScheme
                }
            }
        }
    }

    val finalTypography = if (accessibility.largerText || accessibility.boldText) {
        val scale = if (accessibility.largerText) 1.22f else 1.0f
        val weight = if (accessibility.boldText) FontWeight.Bold else FontWeight.Normal
        androidx.compose.material3.Typography(
            displayLarge = Typography.displayLarge.copy(fontSize = Typography.displayLarge.fontSize * scale, fontWeight = weight),
            displayMedium = Typography.displayMedium.copy(fontSize = Typography.displayMedium.fontSize * scale, fontWeight = weight),
            displaySmall = Typography.displaySmall.copy(fontSize = Typography.displaySmall.fontSize * scale, fontWeight = weight),
            headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize * scale, fontWeight = weight),
            headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize * scale, fontWeight = weight),
            headlineSmall = Typography.headlineSmall.copy(fontSize = Typography.headlineSmall.fontSize * scale, fontWeight = weight),
            titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize * scale, fontWeight = weight),
            titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize * scale, fontWeight = weight),
            titleSmall = Typography.titleSmall.copy(fontSize = Typography.titleSmall.fontSize * scale, fontWeight = weight),
            bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize * scale, fontWeight = weight),
            bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize * scale, fontWeight = weight),
            bodySmall = Typography.bodySmall.copy(fontSize = Typography.bodySmall.fontSize * scale, fontWeight = weight),
            labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize * scale, fontWeight = weight),
            labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize * scale, fontWeight = weight),
            labelSmall = Typography.labelSmall.copy(fontSize = Typography.labelSmall.fontSize * scale, fontWeight = weight)
        )
    } else {
        Typography
    }

    CompositionLocalProvider(
        LocalLiquidGlassEnabled provides (liquidGlassEnabled && !accessibility.highContrast && !accessibility.disableAnimations),
        LocalAccessibility provides accessibility
    ) {
        MaterialTheme(
            colorScheme = baseColorScheme,
            typography = finalTypography,
            content = content
        )
    }
}
