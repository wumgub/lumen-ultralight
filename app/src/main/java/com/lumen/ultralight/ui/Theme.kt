package com.lumen.ultralight.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Resolved colour roles for the whole UI. Mirrors the web app's token model
 *  (minus the visualizer tokens, which this build has no use for). */
data class LumenColors(
    val bg: Color,
    val bg2: Color,
    val bg3: Color,
    val popover: Color,
    val fg: Color,
    val fgDim: Color,
    val accent: Color,
    val accent2: Color,
    val border: Color,
    val isDark: Boolean,
)

private fun hex(s: String): Color {
    val h = s.removePrefix("#")
    return when (h.length) {
        6 -> Color(0xFF000000 or h.toLong(16))
        8 -> Color(h.toLong(16))
        else -> Color(0xFF000000)
    }
}

private fun isDark(c: Color): Boolean =
    (0.299f * c.red + 0.587f * c.green + 0.114f * c.blue) < 0.5f

private fun colors(
    bg: String, bg2: String, bg3: String, popover: String,
    fg: String, fgDim: String, accent: String, accent2: String, border: String,
): LumenColors {
    val b = hex(bg)
    return LumenColors(
        bg = b, bg2 = hex(bg2), bg3 = hex(bg3), popover = hex(popover),
        fg = hex(fg), fgDim = hex(fgDim), accent = hex(accent), accent2 = hex(accent2),
        border = hex(border), isDark = isDark(b),
    )
}

/** Blackout's fixed accent (the "pop" when not sampling from album art). */
val BlackoutDefaultAccent = Color(0xFF22D3EE)

fun blackoutColors(accent: Color): LumenColors = LumenColors(
    bg = Color(0xFF000000),
    bg2 = Color(0xFF000000),
    bg3 = Color(0xFF0A0A0A),
    popover = Color(0xFF000000),
    fg = Color(0xFFFFFFFF),
    fgDim = Color(0xFF8A8A8A),
    accent = accent,
    accent2 = accent,
    border = Color(0xFF1B1B1B),
    isDark = true,
)

// Hex values match the web app's BASE_THEMES exactly so themes stay portable.
fun colorsFor(id: String): LumenColors = when (id) {
    "minimal" -> colors("#f7f7f9", "#ffffff", "#eeeef2", "#ffffff", "#16161a", "#70707c", "#5b6ccf", "#e0607e", "#e2e2e8")
    "minimal-dark" -> colors("#121316", "#191b1f", "#24272c", "#191b1f", "#e8eaed", "#898f98", "#7c8cff", "#f0879f", "#2b2f35")
    "retro" -> colors("#10140f", "#1a2016", "#242b1e", "#1a2016", "#c7f270", "#7f8f5a", "#a3e635", "#f59e0b", "#39421f")
    "glass" -> colors("#0c1018", "#182030", "#232d40", "#101420", "#eef2f8", "#9fb0c3", "#7dd3fc", "#f0abfc", "#2a3647")
    "blackout" -> blackoutColors(BlackoutDefaultAccent)
    else -> colors("#0a0a12", "#11111d", "#1a1a2b", "#11111d", "#f0f0ff", "#8a8aa8", "#22d3ee", "#c026d3", "#23233a") // neon
}

fun previewColors(id: String): LumenColors =
    if (id == "blackout") blackoutColors(BlackoutDefaultAccent) else colorsFor(id)

val LocalLumenColors = staticCompositionLocalOf { colorsFor("neon") }

// Read the active theme's roles. These are @Composable getters, so every usage
// in the UI stays exactly as it was — they just resolve from the current theme.
val LumenBg: Color @Composable @ReadOnlyComposable get() = LocalLumenColors.current.bg
val LumenBg2: Color @Composable @ReadOnlyComposable get() = LocalLumenColors.current.bg2
val LumenBg3: Color @Composable @ReadOnlyComposable get() = LocalLumenColors.current.bg3
val LumenFg: Color @Composable @ReadOnlyComposable get() = LocalLumenColors.current.fg
val LumenDim: Color @Composable @ReadOnlyComposable get() = LocalLumenColors.current.fgDim
val LumenAccent: Color @Composable @ReadOnlyComposable get() = LocalLumenColors.current.accent

fun materialSchemeFor(c: LumenColors): ColorScheme {
    val onAccent = if (isDark(c.accent)) Color.White else Color(0xFF141414)
    return if (c.isDark) {
        darkColorScheme(
            background = c.bg, surface = c.bg, surfaceVariant = c.bg2,
            primary = c.accent, onPrimary = onAccent,
            onBackground = c.fg, onSurface = c.fg, onSurfaceVariant = c.fgDim,
        )
    } else {
        lightColorScheme(
            background = c.bg, surface = c.bg, surfaceVariant = c.bg2,
            primary = c.accent, onPrimary = onAccent,
            onBackground = c.fg, onSurface = c.fg, onSurfaceVariant = c.fgDim,
        )
    }
}
