package com.lumen.ultralight.theme

import org.json.JSONObject

/**
 * The theme token model, matching the web app so themes stay portable. Tokens
 * are kept as hex (or font) strings; the UI layer turns the colour ones into
 * actual colours. The visualizer tokens and font aren't used by this build but
 * are preserved so an exported theme still works on desktop/the regular app.
 */

private const val SYS_FONT =
    "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif"

val TOKEN_KEYS = listOf(
    "bg", "bg-2", "bg-3", "popover-bg",
    "fg", "fg-dim", "accent", "accent-2", "border",
    "viz-bg", "viz-fg", "viz-accent", "viz-accent-2", "font",
)

/** Colour tokens the editor exposes (key -> label). */
val EDITABLE_TOKENS = listOf(
    "bg" to "App background",
    "bg-2" to "Panels & bars",
    "bg-3" to "Hover & elevated",
    "popover-bg" to "Menus & modals",
    "fg" to "Text",
    "fg-dim" to "Muted text",
    "accent" to "Primary accent",
    "accent-2" to "Secondary accent",
    "border" to "Borders",
)

/** Bases you can "start from" in the editor (id -> label). */
val START_FROM = listOf(
    "neon" to "Neon",
    "minimal" to "Flashbang",
    "minimal-dark" to "Minimal Dark",
    "retro" to "Retro",
    "glass" to "Glass",
)

val PRESET_TOKENS: Map<String, Map<String, String>> = mapOf(
    "neon" to mapOf(
        "bg" to "#0a0a12", "bg-2" to "#11111d", "bg-3" to "#1a1a2b", "popover-bg" to "#11111d",
        "fg" to "#f0f0ff", "fg-dim" to "#8a8aa8", "accent" to "#22d3ee", "accent-2" to "#c026d3",
        "border" to "#23233a", "viz-bg" to "#0a0a12", "viz-fg" to "#ffffff",
        "viz-accent" to "#22d3ee", "viz-accent-2" to "#e879f9", "font" to SYS_FONT,
    ),
    "minimal" to mapOf(
        "bg" to "#f7f7f9", "bg-2" to "#ffffff", "bg-3" to "#eeeef2", "popover-bg" to "#ffffff",
        "fg" to "#16161a", "fg-dim" to "#70707c", "accent" to "#5b6ccf", "accent-2" to "#e0607e",
        "border" to "#e2e2e8", "viz-bg" to "#ffffff", "viz-fg" to "#16161a",
        "viz-accent" to "#5b6ccf", "viz-accent-2" to "#e0607e", "font" to SYS_FONT,
    ),
    "minimal-dark" to mapOf(
        "bg" to "#121316", "bg-2" to "#191b1f", "bg-3" to "#24272c", "popover-bg" to "#191b1f",
        "fg" to "#e8eaed", "fg-dim" to "#898f98", "accent" to "#7c8cff", "accent-2" to "#f0879f",
        "border" to "#2b2f35", "viz-bg" to "#121316", "viz-fg" to "#e8eaed",
        "viz-accent" to "#7c8cff", "viz-accent-2" to "#f0879f", "font" to SYS_FONT,
    ),
    "retro" to mapOf(
        "bg" to "#10140f", "bg-2" to "#1a2016", "bg-3" to "#242b1e", "popover-bg" to "#1a2016",
        "fg" to "#c7f270", "fg-dim" to "#7f8f5a", "accent" to "#a3e635", "accent-2" to "#f59e0b",
        "border" to "#39421f", "viz-bg" to "#10140f", "viz-fg" to "#c7f270",
        "viz-accent" to "#a3e635", "viz-accent-2" to "#f59e0b",
        "font" to "'Courier New', ui-monospace, monospace",
    ),
    "glass" to mapOf(
        "bg" to "#0c1018", "bg-2" to "#182030", "bg-3" to "#232d40", "popover-bg" to "#101420",
        "fg" to "#eef2f8", "fg-dim" to "#9fb0c3", "accent" to "#7dd3fc", "accent-2" to "#f0abfc",
        "border" to "#2a3647", "viz-bg" to "#0c1018", "viz-fg" to "#eef2f8",
        "viz-accent" to "#7dd3fc", "viz-accent-2" to "#f0abfc", "font" to SYS_FONT,
    ),
)

fun defaultCustom(): Map<String, String> = PRESET_TOKENS.getValue("neon")

/** Serialize to the shareable `{ "lumenTheme": 1, "tokens": {…} }` string. */
fun exportTokens(tokens: Map<String, String>): String = try {
    val tok = JSONObject()
    for (k in TOKEN_KEYS) tokens[k]?.takeIf { it.isNotBlank() }?.let { tok.put(k, it) }
    JSONObject().put("lumenTheme", 1).put("tokens", tok).toString(2)
} catch (e: Exception) {
    "{}"
}

/**
 * Parse a shared theme string. Tolerates a markdown fence / stray backticks and
 * accepts either the wrapped `{ lumenTheme, tokens }` form or a bare token map.
 * Keeps only known tokens; returns null if nothing usable is found.
 */
fun parseTokens(text: String): Map<String, String>? {
    val src = text.trim()
        .replace(Regex("^`{3,}[a-zA-Z]*\\s*"), "")
        .replace(Regex("`{3,}\\s*$"), "")
        .trim()
        .trim('`')
        .trim()
    val json = runCatching { JSONObject(src) }.getOrNull()
        ?: Regex("\\{[\\s\\S]*\\}").find(src)?.value?.let { runCatching { JSONObject(it) }.getOrNull() }
        ?: return null
    val raw = json.optJSONObject("tokens") ?: json
    val out = HashMap<String, String>()
    for (k in TOKEN_KEYS) {
        val v = raw.optString(k, "").trim()
        if (v.isNotBlank()) out[k] = v
    }
    return if (out.isEmpty()) null else out
}
