package com.lumen.ultralight.theme

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Holds the chosen theme + Blackout accent-source setting, persisted across
 * launches. `dynamicAccent` (ARGB int) is the colour sampled from the current
 * album art for Blackout's "accent from album art" mode; null means fall back
 * to the fixed accent.
 */
class ThemeController(private val prefs: SharedPreferences) {

    val themeId = MutableStateFlow(prefs.getString(KEY_THEME, "neon") ?: "neon")
    val blackoutFromArt = MutableStateFlow(prefs.getBoolean(KEY_BLACKOUT_ART, true))
    val dynamicAccent = MutableStateFlow<Int?>(null)

    fun setTheme(id: String) {
        themeId.value = id
        prefs.edit().putString(KEY_THEME, id).apply()
    }

    fun setBlackoutFromArt(enabled: Boolean) {
        blackoutFromArt.value = enabled
        prefs.edit().putBoolean(KEY_BLACKOUT_ART, enabled).apply()
    }

    fun setDynamicAccent(argb: Int?) {
        dynamicAccent.value = argb
    }

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_BLACKOUT_ART = "blackoutFromArt"
    }
}
