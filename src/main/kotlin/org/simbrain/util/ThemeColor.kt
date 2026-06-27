package org.simbrain.util

import java.awt.Color
import kotlin.math.roundToInt

/**
 * A theme-aware color: a [light] color, a [dark] color, and a [useManualDark] flag. [resolve] returns
 * the right color for the active mode — [light] in light mode; in dark mode the explicit [dark] when
 * [useManualDark], otherwise a value [inferred][inferDark] from [light].
 *
 * Used for the user-editable network canvas colors (see [org.simbrain.network.gui.dialogs.NetworkPreferences]
 * and `ThemeColorPreference`), so a single preference carries both light and dark variants. Edited via
 * the compact two-swatch widget in the property editor.
 */
class ThemeColor(
    val light: Color = Color.GRAY,
    val dark: Color = inferDark(light),
    val useManualDark: Boolean = false,
) {
    fun resolve(isDark: Boolean): Color = when {
        !isDark -> light
        useManualDark -> dark
        else -> inferDark(light)
    }

    /** The color for the currently active [NetworkTheme] mode. */
    val resolved: Color get() = resolve(NetworkTheme.isDark)

    fun copy(light: Color = this.light, dark: Color = this.dark, useManualDark: Boolean = this.useManualDark) =
        ThemeColor(light, dark, useManualDark)

    override fun equals(other: Any?) = other is ThemeColor &&
            other.light == light && other.dark == dark && other.useManualDark == useManualDark

    override fun hashCode(): Int {
        var result = light.hashCode()
        result = 31 * result + dark.hashCode()
        result = 31 * result + useManualDark.hashCode()
        return result
    }

    override fun toString() = "ThemeColor(light=$light, dark=$dark, useManualDark=$useManualDark)"
}

/**
 * Infer a dark-mode variant of a light-mode color by inverting HSL lightness while keeping hue and
 * saturation, clamped so the result never reaches pure black or white. This lightens dark colors and
 * darkens light ones — the right behavior for canvas elements that must read on the opposite
 * background — and approximates the hand-tuned dark palette (e.g. a light red maps to a lighter red).
 */
fun inferDark(light: Color): Color {
    val (h, s, l) = light.toHSL()
    val invertedL = (1f - l).coerceIn(0.16f, 0.92f)
    return hslColor(h, s, invertedL)
}

/** Convert to HSL as (hue, saturation, lightness), each in 0..1. */
fun Color.toHSL(): Triple<Float, Float, Float> {
    val r = red / 255f
    val g = green / 255f
    val b = blue / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> (g - b) / d + (if (g < b) 6f else 0f)
        g -> (b - r) / d + 2f
        else -> (r - g) / d + 4f
    } / 6f
    return Triple(h, s, l)
}

/** Build an opaque color from HSL components, each in 0..1. */
fun hslColor(h: Float, s: Float, l: Float): Color {
    if (s == 0f) {
        val v = (l * 255f).roundToInt().coerceIn(0, 255)
        return Color(v, v, v)
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    fun hueToChannel(t0: Float): Float {
        var t = t0
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    fun channel(v: Float) = (v * 255f).roundToInt().coerceIn(0, 255)
    return Color(channel(hueToChannel(h + 1f / 3f)), channel(hueToChannel(h)), channel(hueToChannel(h - 1f / 3f)))
}
