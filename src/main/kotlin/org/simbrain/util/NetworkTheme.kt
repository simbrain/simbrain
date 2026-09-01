package org.simbrain.util

import org.simbrain.util.NetworkTheme.current
import org.simbrain.util.NetworkTheme.darkPalette
import org.simbrain.util.NetworkTheme.lightPalette
import java.awt.Color
import javax.swing.UIManager

/**
 * Per-mode color palette for the Piccolo2D network canvas.
 *
 * The rest of the app themes through FlatLaf / [Theme] (colors resolved live from `UIManager`). The
 * network editor is a separate Piccolo2D scene whose colors come from
 * [org.simbrain.network.gui.dialogs.NetworkPreferences] and a number of hardcoded literals, so it does
 * not track light/dark on its own. [NetworkTheme] is the single source the canvas resolves its
 * structural colors and its theme-aware preference defaults against (see [ThemedColorPreference]).
 *
 * Semantics are preserved across modes — red stays positive/excitatory and blue negative/inhibitory —
 * only luminance is adjusted so the colors read on both a light and a dark background. The neutral
 * activation midpoint is matched to the canvas background per mode so resting/zero values recede
 * instead of glowing (this is consumed by the activation colormap once that step lands).
 *
 * [refresh] is called from [setupLookAndFeel] — the single choke point hit once at startup and again
 * on every live theme switch — so [current] always reflects the active mode without re-running the
 * (macOS-subprocess) [ThemeMode.resolvedDark] per paint.
 */
object NetworkTheme {

    /** A complete canvas palette for one light/dark mode. */
    class Palette(
        val canvasBackground: Color,
        val nodeOutline: Color,
        val valueText: Color,
        val imageBorder: Color,
        val tabFill: Color,
        val tabFillSupervised: Color,
        val tabText: Color,
        val subnetOutline: Color,
        val selectionHandle: Color,
        val sourceHandle: Color,
        val marquee: Color,
        val hotNode: Color,
        val coolNode: Color,
        val neutralMidpoint: Color,
        val connectionLine: Color,
        val gapJunction: Color,
        val excitatorySynapse: Color,
        val inhibitorySynapse: Color,
        val zeroWeight: Color,
        val spiking: Color,
        val groupArrow: Color,
        val connectorArrow: Color,
        val weightMatrixBoundary: Color,
        val receptiveFieldTrace: Color,
        val backwardTrace: Color,
        val rowHighlight: Color,
        /** Categorical identity colors for small heterogeneous slice sets (a fused projection's chunks). */
        val chunkColors: List<Color>,
    )

    val lightPalette = Palette(
        canvasBackground = Color(0xFAFAFA),
        nodeOutline = Color(0x3A3A3A),
        valueText = Color(0x1A1A1A),
        imageBorder = Color(0x3A3A3A),
        tabFill = Color(0xFFFDE7),
        tabFillSupervised = Color(0xE2F4D8),
        tabText = Color(0x1A1A1A),
        subnetOutline = Color(0x8C8C8C),
        selectionHandle = Color(0x22A745),
        sourceHandle = Color(0xD32F2F),
        marquee = Color(0xEAB308),
        hotNode = Color(0xD5584F),
        coolNode = Color(0x5082C4),
        neutralMidpoint = Color(0xF2F2F2),
        connectionLine = Color(0x444444),
        gapJunction = Color(0x0E7C66),
        excitatorySynapse = Color(0xC0392B),
        inhibitorySynapse = Color(0x2C6FB5),
        zeroWeight = Color(0xBFBFBF),
        spiking = Color(0xDB5A00),
        groupArrow = Color(0x2E9B3D),
        connectorArrow = Color(0xB86E00),
        weightMatrixBoundary = Color(0xB86E00),
        receptiveFieldTrace = Color(0xB86E00),
        backwardTrace = Color(0x2C6FB5),
        rowHighlight = Color(0, 200, 255, 160),
        chunkColors = listOf(Color(0x00897B), Color(0x7B1FA2), Color(0xC2185B)),
    )

    val darkPalette = Palette(
        canvasBackground = Color(0x1E1F22),
        nodeOutline = Color(0xC8CACE),
        valueText = Color(0xE6E6E6),
        imageBorder = Color(0xC8CACE),
        tabFill = Color(0x3A3D42),
        tabFillSupervised = Color(0x2F3A2C),
        tabText = Color(0xE6E6E6),
        subnetOutline = Color(0x6E7177),
        selectionHandle = Color(0x5BD96A),
        sourceHandle = Color(0xFF5A52),
        marquee = Color(0xFFDA4D),
        hotNode = Color(0xC74B43),
        coolNode = Color(0x3F70B0),
        neutralMidpoint = Color(0x242629),
        connectionLine = Color(0x9A9DA3),
        gapJunction = Color(0x3FBFA5),
        excitatorySynapse = Color(0xFF6B57),
        inhibitorySynapse = Color(0x5AB0F0),
        zeroWeight = Color(0x5A5C60),
        spiking = Color(0xFFD23F),
        groupArrow = Color(0x57C257),
        connectorArrow = Color(0xF2A33C),
        weightMatrixBoundary = Color(0xF2A33C),
        receptiveFieldTrace = Color(0xF2A33C),
        backwardTrace = Color(0x6FB1E6),
        rowHighlight = Color(90, 210, 255, 160),
        chunkColors = listOf(Color(0x46B8A9), Color(0xB07BD6), Color(0xE0669A)),
    )

    /**
     * Whether the canvas should use the dark palette, derived live from the active Look-and-Feel:
     * every FlatLaf theme sets the `laf.dark` UIManager flag, so this stays correct no matter how the
     * theme was installed ([setupLookAndFeel], a live switch, or the snapshot harness setting a FlatLaf
     * directly). Cheap enough to read per paint.
     */
    val isDark: Boolean get() = UIManager.getBoolean("laf.dark")

    /** The palette for the active mode. */
    val current: Palette get() = if (isDark) darkPalette else lightPalette

    /**
     * A [ThemeColor] default for a role, taking its light value from [lightPalette] and its hand-tuned
     * dark value from [darkPalette] (so [ThemeColor.useManualDark] is true). Used to seed the editable
     * canvas-color preferences with both modes.
     */
    fun pair(select: (Palette) -> Color): ThemeColor =
        ThemeColor(select(lightPalette), select(darkPalette), useManualDark = true)
}
