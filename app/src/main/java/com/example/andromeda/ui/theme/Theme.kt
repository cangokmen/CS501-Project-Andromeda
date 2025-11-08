// In app/src/main/java/com/example/andromeda/ui/theme/Theme.kt
// (Assuming you have a standard Theme.kt, otherwise apply this to your main composable)

package com.example.andromeda.ui.theme
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

// Your color schemes...

@Composable
fun AndromedaTheme(
    darkTheme: Boolean,
    fontScale: Float = 1.0f, // New parameter
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    // Create a scaled typography
    val originalTypography = Typography()
    val scaledTypography = Typography(
        bodyLarge = originalTypography.bodyLarge.copy(fontSize = originalTypography.bodyLarge.fontSize * fontScale),
        bodyMedium = originalTypography.bodyMedium.copy(fontSize = originalTypography.bodyMedium.fontSize * fontScale),
        bodySmall = originalTypography.bodySmall.copy(fontSize = originalTypography.bodySmall.fontSize * fontScale),
        // Repeat for all other text styles you use: titleLarge, labelSmall, etc.
        headlineLarge = originalTypography.headlineLarge.copy(fontSize = originalTypography.headlineLarge.fontSize * fontScale), // FIX: Changed original_typography to originalTypography
        // ... and so on
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography, // Use the scaled typography
        content = content
    )
}
