package com.lumen.ultralight.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// Lumen's default "neon" palette. A full theme system (presets + Blackout +
// custom editor) lands in Phase 2; for now these are the single source of colour.
val LumenBg = Color(0xFF0A0A12)
val LumenBg2 = Color(0xFF14141F)
val LumenBg3 = Color(0xFF1E1E2D)
val LumenFg = Color(0xFFE8E8F0)
val LumenDim = Color(0xFF9A9AB0)
val LumenAccent = Color(0xFF5BE9D6)

val LumenColorScheme = darkColorScheme(
    background = LumenBg,
    surface = LumenBg,
    surfaceVariant = LumenBg2,
    primary = LumenAccent,
    onPrimary = LumenBg,
    onBackground = LumenFg,
    onSurface = LumenFg,
    onSurfaceVariant = LumenDim,
)
