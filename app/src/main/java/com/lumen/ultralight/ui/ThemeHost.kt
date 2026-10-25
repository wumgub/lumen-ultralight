package com.lumen.ultralight.ui

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.palette.graphics.Palette
import com.lumen.ultralight.playback.PlayerState
import com.lumen.ultralight.theme.ThemeController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves the active theme into [LumenColors], provides it to the UI, keeps the
 * system bars in step, and (for Blackout's album-art mode) samples an accent
 * colour from the current cover.
 */
@Composable
fun ThemeHost(
    themeController: ThemeController,
    playerState: PlayerState,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val context = view.context

    val id by themeController.themeId.collectAsState()
    val fromArt by themeController.blackoutFromArt.collectAsState()
    val dynArgb by themeController.dynamicAccent.collectAsState()
    val artUri by playerState.artUri.collectAsState()

    LaunchedEffect(id, fromArt, artUri) {
        if (id == "blackout" && fromArt && artUri != null) {
            val argb = withContext(Dispatchers.IO) { samplePaletteAccent(context, artUri!!) }
            themeController.setDynamicAccent(argb)
        } else {
            themeController.setDynamicAccent(null)
        }
    }

    val colors = remember(id, fromArt, dynArgb) {
        if (id == "blackout") {
            val accent = if (fromArt) (dynArgb?.let { Color(it) } ?: BlackoutDefaultAccent) else BlackoutDefaultAccent
            blackoutColors(accent)
        } else {
            colorsFor(id)
        }
    }

    if (!view.isInEditMode) {
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                window.statusBarColor = colors.bg.toArgb()
                window.navigationBarColor = colors.bg.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !colors.isDark
            }
        }
    }

    CompositionLocalProvider(LocalLumenColors provides colors) {
        MaterialTheme(colorScheme = materialSchemeFor(colors)) {
            Surface(color = colors.bg, modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

private fun samplePaletteAccent(context: Context, uriStr: String): Int? = try {
    val uri = Uri.parse(uriStr)
    context.contentResolver.openInputStream(uri)?.use { input ->
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bmp = BitmapFactory.decodeStream(input, null, opts)
        if (bmp == null) {
            null
        } else {
            val palette = Palette.from(bmp).generate()
            val swatch = palette.vibrantSwatch ?: palette.lightVibrantSwatch ?: palette.dominantSwatch
            swatch?.rgb?.let { it or 0xFF000000.toInt() }
        }
    }
} catch (e: Exception) {
    null
}
