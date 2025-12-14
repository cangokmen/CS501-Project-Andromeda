package com.example.watch.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.example.watch.presentation.theme.DarkGreen
import com.example.watch.presentation.theme.DarkerText
import com.example.watch.presentation.theme.LightGreen
import com.example.watch.presentation.theme.MediumGreen
import com.example.watch.presentation.theme.MutedGreen
import com.example.watch.presentation.theme.White
import com.example.watch.presentation.theme.Black
import androidx.wear.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider

private val wearColorPalette: ColorScheme = ColorScheme(
    primary = MediumGreen,
    onPrimary = White,
    secondary = MutedGreen,
    onSecondary = Black,
    surface = DarkGreen,
    onSurface = White,
    onSurfaceVariant = MutedGreen,
    background = DarkGreen,
    onBackground = Black,
    error = Color.Red,
    onError = White
)

@Composable
fun AndromedaTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides wearColorPalette.onSurface,
    ) {
        MaterialTheme(
            colorScheme = wearColorPalette,
            typography = wearTypography(),
            content = content
        )
    }
}
