// In ...ui.theme/Theme.kt
package com.skai.lofintrackerapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- DARK THEME COLOR SCHEME ---
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary, // LoFinLightBlue for accents/main interactive elements
    onPrimary = DarkOnPrimary, // White text on primary
    primaryContainer = DarkPrimaryContainer, // LoFinGreenBlue accent
    onPrimaryContainer = DarkOnPrimaryContainer, // White text on primary container

    secondary = LoFinGreenBlue, // Use green-blue as secondary accent
    onSecondary = Black,
    secondaryContainer = LoFinGreenBlue.copy(alpha = 0.2f),
    onSecondaryContainer = White,

    tertiary = LoFinGreenBlue, // Also use green-blue for tertiary (e.g., loan cards)
    onTertiary = White,
    tertiaryContainer = LoFinGreenBlue.copy(alpha = 0.2f),
    onTertiaryContainer = White,

    background = DarkBackground, // The deep dark blue from your logo
    onBackground = DarkOnBackground, // White text on dark background
    surface = DarkSurface, // Slightly lighter dark blue for cards/surfaces
    onSurface = DarkOnSurface, // Off-white text on surface
    surfaceVariant = DarkSurface, // Often similar to surface
    onSurfaceVariant = White.copy(alpha = 0.7f), // Lighter text on surface variant
)
// ------------------------------

// --- LIGHT THEME COLOR SCHEME ---
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary, // LoFinLightBlue for accents/main interactive elements
    onPrimary = LightOnPrimary, // White text on primary
    primaryContainer = LightPrimaryContainer, // LoFinGreenBlue accent
    onPrimaryContainer = LightOnPrimaryContainer, // LoFinDarkBlue text on primary container

    secondary = LoFinGreenBlue, // Use green-blue as secondary accent
    onSecondary = White,
    secondaryContainer = LoFinGreenBlue.copy(alpha = 0.2f),
    onSecondaryContainer = LoFinDarkBlue,

    tertiary = LoFinGreenBlue, // Also use green-blue for tertiary (e.g., loan cards)
    onTertiary = LoFinDarkBlue,
    tertiaryContainer = LoFinGreenBlue.copy(alpha = 0.2f),
    onTertiaryContainer = LoFinDarkBlue,

    background = LightBackground, // Very light background
    onBackground = LightOnBackground, // LoFinDarkBlue text on light background
    surface = LightSurface, // White for cards/surfaces
    onSurface = LightOnSurface, // LoFinDarkBlue text on surface
    surfaceVariant = LightSurface,
    onSurfaceVariant = LoFinDarkBlue.copy(alpha = 0.7f), // Lighter text on surface variant
)
// ---------------------------------

@Composable
fun LoFinTrackerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}