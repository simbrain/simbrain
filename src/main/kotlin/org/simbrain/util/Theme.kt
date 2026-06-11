package org.simbrain.util

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.extras.FlatSVGIcon
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.UIManager
import javax.swing.border.AbstractBorder
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder

/**
 * Registers Simbrain's global FlatLaf custom defaults — accent color, rounded
 * corners, focus ring, chevron arrows — from the classpath package
 * `org.simbrain.util.theme` (see its `FlatLaf.properties`). These merge over
 * FlatLaf's bundled defaults for every theme, so the look is consistent across
 * light and dark.
 *
 * MUST be called before `FlatLightLaf.setup()` / `FlatDarkLaf.setup()`: custom
 * defaults are read only while the look-and-feel builds its UIDefaults during
 * install. The registry is a static on [FlatLaf], so calling this once per JVM
 * is enough and re-registering the same package is harmless.
 */
fun installSimbrainFlatLafDefaults() {
    FlatLaf.registerCustomDefaultsSource("org.simbrain.util.theme")
}

/**
 * Installs the global [FlatSVGIcon.ColorFilter] that recolors single-color SVG icons
 * (see [Icons]) to the Look-and-Feel foreground, so icons track the active theme in both light
 * and dark. The mapper reads `Label.foreground` live and FlatSVGIcon re-applies it on every paint,
 * so a light/dark switch recolors every icon on the next repaint ([FlatLaf.updateUI]) with no cache
 * to flush. Near-black is the single authored icon color; deliberately-colored icons (in
 * `icons/multicolor/`) avoid it and pass through.
 *
 * MUST be called AFTER `FlatLightLaf.setup()` / `FlatDarkLaf.setup()` so `UIManager` is populated.
 */
fun installSimbrainSvgIconColors() {
    FlatSVGIcon.ColorFilter.getInstance().setMapper { color ->
        if (color.red < 40 && color.green < 40 && color.blue < 40)
            UIManager.getColor("Label.foreground") ?: color
        else color
    }
}

/**
 * Color themes Simbrain can run under, persisted in [org.simbrain.workspace.WorkspacePreferences]
 * and chosen in the Workspace Preferences dialog. [SYSTEM] follows the OS appearance (macOS); the
 * Swing content switches live, while the native window frame and macOS screen menu bar match the
 * mode chosen at the previous launch (they can only be set before AWT starts — see Splasher).
 */
enum class ThemeMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark");
    override fun toString() = label
}

/** Whether macOS is currently in dark mode, via `defaults read -g AppleInterfaceStyle`. */
fun isMacSystemDark(): Boolean = try {
    val process = ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
        .redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText().trim()
    process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
    output.equals("Dark", ignoreCase = true)
} catch (e: Exception) {
    false
}

/** Resolves [SYSTEM] to a concrete dark/light decision; non-macOS [SYSTEM] falls back to light. */
fun ThemeMode.resolvedDark(): Boolean = when (this) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> Utils.isMacOSX() && isMacSystemDark()
}

/**
 * Installs the FlatLaf look-and-feel for the given [mode] in the required order: register
 * Simbrain's custom defaults, run the resolved theme's setup, then install the SVG icon color
 * filter (which needs a populated UIManager) and the shared button sizing. Safe to call repeatedly —
 * a live theme switch re-runs this and then calls [FlatLaf.updateUI] to repaint and recolor every
 * open window.
 */
fun setupLookAndFeel(mode: ThemeMode) {
    installSimbrainFlatLafDefaults()
    if (mode.resolvedDark()) FlatDarkLaf.setup() else FlatLightLaf.setup()
    installSimbrainSvgIconColors()
    UIManager.put("Button.minimumWidth", 80)
}

/** Linearly blend [over] onto [base] by [fraction] (0 = all base, 1 = all over). */
fun blend(over: Color, base: Color, fraction: Double): Color {
    val f = fraction.coerceIn(0.0, 1.0)
    fun mix(a: Int, b: Int) = (a * f + b * (1 - f)).toInt().coerceIn(0, 255)
    return Color(mix(over.red, base.red), mix(over.green, base.green), mix(over.blue, base.blue))
}

/**
 * Centralized typography, color, and border tokens for Simbrain's Swing UI.
 *
 * Fonts derive from the Look-and-Feel's `Label.font` so the whole app inherits
 * the current LaF's font family (e.g. FlatLaf's system font), instead of
 * hardcoding "SansSerif" or "Arial" — which on macOS resolves to Helvetica
 * and clashes with FlatLaf chrome.
 *
 * Typography ladder, sized relative to the LaF base font so text tracks the
 * look-and-feel instead of undershooting it. With a 13px base:
 *   title 18B, heading 16B, section 14B, bodyBold 13B, body 13, label 12,
 *   small 11, tiny 10, type 11I
 *
 * Use [font] as an escape hatch for one-off sizes.
 */
object Theme {

    private val baseFont: Font
        get() = UIManager.getFont("Label.font") ?: Font(Font.SANS_SERIF, Font.PLAIN, 12)

    fun font(size: Int, style: Int = Font.PLAIN): Font = baseFont.deriveFont(style, size.toFloat())

    private val baseSize: Int get() = baseFont.size

    val title: Font     get() = font(baseSize + 5, Font.BOLD)
    val heading: Font   get() = font(baseSize + 3, Font.BOLD)
    val section: Font   get() = font(baseSize + 1, Font.BOLD)
    val bodyBold: Font  get() = font(baseSize, Font.BOLD)
    val body: Font      get() = font(baseSize)
    val label: Font     get() = font(baseSize - 1)
    val small: Font     get() = font(baseSize - 2)
    val tiny: Font      get() = font(baseSize - 3)
    val type: Font      get() = font(baseSize - 2, Font.ITALIC)

    // Colors derive from UIManager via computed getters (not stored vals) so they track the
    // active LaF and re-resolve on a theme switch; the literal fallbacks preserve the old look.
    @JvmStatic val foreground: Color get() = UIManager.getColor("Label.foreground") ?: Color.BLACK
    @JvmStatic val mutedText: Color get() = UIManager.getColor("Label.disabledForeground") ?: Color(100, 100, 100)
    @JvmStatic val divider: Color get() = UIManager.getColor("Component.borderColor") ?: Color(200, 200, 200)
    @JvmStatic val cardBg: Color get() = UIManager.getColor("List.background") ?: Color(250, 250, 250)
    @JvmStatic val cardBorder: Color get() = UIManager.getColor("Component.borderColor") ?: Color(180, 180, 180)

    const val dialogInsetVertical: Int = 8
    const val dialogInsetHorizontal: Int = 12
    const val sectionGap: Int = 12
    const val componentGap: Int = 8
    const val tightGap: Int = 4

    @JvmStatic
    fun dialogBorder(): Border =
        EmptyBorder(dialogInsetVertical, dialogInsetHorizontal, dialogInsetVertical, dialogInsetHorizontal)

    /**
     * "Header strip" section border — bold title + thin separator line at the top, no surrounding box.
     * Drop-in replacement for [javax.swing.BorderFactory.createTitledBorder] with a modernized look.
     */
    @JvmStatic
    fun sectionBorder(title: String): Border = HeaderStripBorder(title)

    /**
     * Reads its font and colors live from [section]/[foreground]/[divider] every paint, so the
     * ~20 [sectionBorder] sites track a light/dark switch without being rebuilt.
     */
    private class HeaderStripBorder(private val title: String) : AbstractBorder() {
        private val gapBelowTitle = 3
        private val gapBelowLine = tightGap

        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val titleFont = section
            val fm = g2.getFontMetrics(titleFont)
            g2.font = titleFont
            g2.color = foreground
            g2.drawString(title, x, y + fm.ascent)
            val lineY = y + fm.height + gapBelowTitle
            g2.color = divider
            g2.drawLine(x, lineY, x + width - 1, lineY)
            g2.dispose()
        }

        override fun getBorderInsets(c: Component): Insets {
            val fm = c.getFontMetrics(section)
            val topInset = fm.height + gapBelowTitle + 1 + gapBelowLine
            return Insets(topInset, 0, 0, 0)
        }

        override fun isBorderOpaque(): Boolean = false
    }

    fun roundedBorder(
        radius: Int = 8,
        borderColor: Color? = null,
        fillColor: Color? = null
    ): Border = RoundedBorder(radius, borderColor, fillColor)

    fun roundedCard(
        radius: Int = 8,
        padding: Int = 8,
        borderColor: Color? = null,
        fillColor: Color? = null
    ): Border = CompoundBorder(
        roundedBorder(radius, borderColor, fillColor),
        EmptyBorder(padding / 2, padding, padding / 2, padding)
    )

    /**
     * A null [borderColor]/[fillColor] resolves live from [cardBorder]/[cardBg] each paint, so cards
     * track a light/dark switch; pass an explicit color to pin it.
     */
    private class RoundedBorder(
        private val radius: Int,
        private val borderColor: Color?,
        private val fillColor: Color?
    ) : AbstractBorder() {
        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, w: Int, h: Int) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val shape = RoundRectangle2D.Float(
                x + 0.5f, y + 0.5f, w - 1f, h - 1f, radius.toFloat(), radius.toFloat()
            )
            g2.color = fillColor ?: cardBg
            g2.fill(shape)
            g2.color = borderColor ?: cardBorder
            g2.draw(shape)
            g2.dispose()
        }

        override fun getBorderInsets(c: Component) = Insets(radius / 2, radius / 2, radius / 2, radius / 2)

        override fun isBorderOpaque() = false
    }
}
