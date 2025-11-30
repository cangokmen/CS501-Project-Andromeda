// In app/src/main/java/com/example/andromeda/ui/theme/Theme.kt

package com.example.andromeda.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalDensity

// Define the Light Color Scheme using our new green colors
private val LightColorScheme = lightColorScheme(
    primary = DarkGreen,
    secondary = MediumGreen,
    tertiary = MutedGreen,
    background = Color(0xFFF0F5F4),
    surface = LightGreen,
    onPrimary = White,
    onSecondary = White,
    onTertiary = DarkerText,
    onBackground = DarkerText,
    onSurface = OnLightGreen,

    primaryContainer = MutedGreen,    // Color for User's chat bubble
    onPrimaryContainer = DarkerText,  // Text color on User's chat bubble
    secondaryContainer = LightGreen,  // Color for AI's chat bubble
    onSecondaryContainer = OnLightGreen // Text color on AI's chat bubble
)

// Define a Dark Color Scheme if you want to support dark mode
private val DarkColorScheme = darkColorScheme(
    primary = MediumGreen,
    secondary = MutedGreen,
    tertiary = LightGreen,
    background = Color(0xFF001A16),
    surface = Color(0xFF00382F),
    onPrimary = DarkerText,
    onSecondary = DarkerText,
    onTertiary = DarkerText,
    onBackground = White,
    onSurface = White,

    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = White,
    secondaryContainer = Color(0xFF00382F),
    onSecondaryContainer = MutedGreen
)


@Composable
fun AndromedaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    // Apply the font scale using CompositionLocalProvider
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density,
            fontScale = fontScale
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
