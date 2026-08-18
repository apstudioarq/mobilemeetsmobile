package com.mobilemeetsmobile.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

// Core brand colors, sampled from the supplied ING references.
val IngOrange = Color(0xFFFF6200)
val IngOrangeDark = Color(0xFFD94F00)
val IngPurple = Color(0xFF525199)
val IngPurpleLight = Color(0xFF8E8BDB)
val IngMagenta = Color(0xFFC00067)
val GoogleRed = Color(0xFFEA4335)
val IngSun = Color(0xFFFFE100)
val IngSky = Color(0xFF89D6FD)

// Calm, high-density surface palette used throughout the reference app.
val LightBackground = Color(0xFFF3F3F3)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceWarm = Color(0xFFEDEDEF)
val LightBorder = Color(0xFFDADADD)
val LightText = Color(0xFF202020)
val LightMuted = Color(0xFF66666A)

val DarkBackground = Color(0xFF121214)
val DarkSurface = Color(0xFF1D1D20)
val DarkSurfaceVariant = Color(0xFF29292D)
val DarkBorder = Color(0xFF414146)
val DarkOnBackground = Color(0xFFF4F4F5)
val DarkOnSurface = Color(0xFFF4F4F5)
val DarkOnSurfaceVariant = Color(0xFFB8B8BE)

// Compatibility accessors keep all existing components theme-aware.
val VibrantBackground: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val VibrantSurface: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
val VibrantSurfaceWarm: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val VibrantBorder: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
val VibrantText: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val VibrantMuted: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val VibrantSoftMuted: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)

// Kept as a theme-aware alias so existing feature code inherits the action colour.
val VibrantBrown: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary

// Track colors
val TrackAiMl = Color(0xFF4285F4)
val TrackAndroid = Color(0xFF34A853)
val TrackWeb = Color(0xFFFBBC04)
val TrackCloud = Color(0xFFEA4335)
val TrackFirebase = Color(0xFFFF6D00)
val TrackFlutter = Color(0xFF42A5F5)
val TrackDesign = Color(0xFFA142F4)

// Track background colors (subtle)
val TrackAiMlBg = Color(0xFF1A2840)
val TrackAndroidBg = Color(0xFF1A3020)
val TrackWebBg = Color(0xFF3A3010)
val TrackCloudBg = Color(0xFF3A1A18)
val TrackFirebaseBg = Color(0xFF3A2510)
val TrackFlutterBg = Color(0xFF1A2838)
val TrackDesignBg = Color(0xFF2A1A3A)
