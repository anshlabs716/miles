package com.example.ui.theme

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

val LocalLiquidGlassEnabled = compositionLocalOf { true }

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
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()

    val colorScheme: ColorScheme = when (themeOption) {
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

    CompositionLocalProvider(LocalLiquidGlassEnabled provides liquidGlassEnabled) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
