// In ...ui.theme/Color.kt
package com.skai.lofintrackerapp.ui.theme

import androidx.compose.ui.graphics.Color

// Your Logo's Dark Blue background
val LoFinDarkBlue = Color(0xFF0F1A33) // A deep, rich dark blue
// Your Logo's Green-Blue (from the shield/rupee symbol)
val LoFinGreenBlue = Color(0xFF20C997) // Vibrant green-blue
// Your Logo's Lighter Blue (from the 'Lo' text)
val LoFinLightBlue = Color(0xFF4285F4) // A brighter, friendly blue

// Additional colors for theme consistency
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val Gray400 = Color(0xFFBDBDBD)
val Gray800 = Color(0xFF424242)

// Derived for use in themes
val LightPrimary = LoFinLightBlue
val LightPrimaryContainer = LoFinGreenBlue.copy(alpha = 0.2f) // Light accent
val LightOnPrimary = White
val LightOnPrimaryContainer = LoFinDarkBlue

val DarkPrimary = LoFinLightBlue
val DarkPrimaryContainer = LoFinGreenBlue.copy(alpha = 0.4f) // Stronger accent for dark theme
val DarkOnPrimary = White
val DarkOnPrimaryContainer = White

val LightBackground = Color(0xFFF0F2F5) // A very light off-white
val LightSurface = Color(0xFFFFFFFF)
val LightOnBackground = LoFinDarkBlue // Dark text on light background
val LightOnSurface = LoFinDarkBlue

val DarkBackground = LoFinDarkBlue // Your logo's background for dark mode
val DarkSurface = Color(0xFF1A284A) // Slightly lighter dark blue for cards
val DarkOnBackground = White
val DarkOnSurface = White.copy(alpha = 0.9f)
val FabGreen = Color(0xFF00C853)