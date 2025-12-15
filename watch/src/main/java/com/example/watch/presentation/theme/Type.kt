package com.example.watch.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography

// --- FIX: Create a @Composable function that returns the Typography ---
@Composable
fun wearTypography(): Typography {
    // Accessing MaterialTheme.typography is now safe inside a @Composable function
    return Typography(
        displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Serif),
        displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Serif),
        displaySmall = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Serif),

        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
        titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
        titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Serif),

        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),

        labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Serif),
        labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Serif),
        labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Serif),
    )
}
