package com.dataguard.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.dataguard.app.domain.model.DisplayUnit
import com.dataguard.app.domain.model.ThemeMode

/** Ambient display unit; provided by [DataGuardTheme], read via [formatBytes]. */
val LocalDisplayUnit = staticCompositionLocalOf { DisplayUnit.AUTO }

// Brand palette
val PrimaryBlue = Color(0xFF2E6BE6)
val PrimaryBlueDark = Color(0xFFA9C5FF)
val SecondaryTeal = Color(0xFF00B894)
val SecondaryTealDark = Color(0xFF55EFC4)

// Usage category colors
val WifiColor = Color(0xFF2E6BE6)
val MobileColor = Color(0xFF00B894)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlueDark,
    secondary = SecondaryTealDark,
)

@Composable
fun DataGuardTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    displayUnit: DisplayUnit = DisplayUnit.AUTO,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalDisplayUnit provides displayUnit) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}
