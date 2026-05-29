package org.simbrain.util

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
    val mutedText: Color get() = UIManager.getColor("Label.disabledForeground") ?: Color(100, 100, 100)
    val divider: Color get() = UIManager.getColor("Component.borderColor") ?: Color(200, 200, 200)
    val cardBg: Color get() = UIManager.getColor("List.background") ?: Color(250, 250, 250)
    val cardBorder: Color get() = UIManager.getColor("Component.borderColor") ?: Color(180, 180, 180)

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
    fun sectionBorder(title: String): Border = HeaderStripBorder(
        title = title,
        titleFont = section,
        titleColor = UIManager.getColor("Label.foreground") ?: Color.BLACK,
        lineColor = divider
    )

    private class HeaderStripBorder(
        private val title: String,
        private val titleFont: Font,
        private val titleColor: Color,
        private val lineColor: Color
    ) : AbstractBorder() {
        private val gapBelowTitle = 3
        private val gapBelowLine = tightGap

        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val fm = g2.getFontMetrics(titleFont)
            g2.font = titleFont
            g2.color = titleColor
            g2.drawString(title, x, y + fm.ascent)
            val lineY = y + fm.height + gapBelowTitle
            g2.color = lineColor
            g2.drawLine(x, lineY, x + width - 1, lineY)
            g2.dispose()
        }

        override fun getBorderInsets(c: Component): Insets {
            val fm = c.getFontMetrics(titleFont)
            val topInset = fm.height + gapBelowTitle + 1 + gapBelowLine
            return Insets(topInset, 0, 0, 0)
        }

        override fun isBorderOpaque(): Boolean = false
    }

    fun roundedBorder(
        radius: Int = 8,
        borderColor: Color = cardBorder,
        fillColor: Color? = cardBg
    ): Border = RoundedBorder(radius, borderColor, fillColor)

    fun roundedCard(
        radius: Int = 8,
        padding: Int = 8,
        borderColor: Color = cardBorder,
        fillColor: Color? = cardBg
    ): Border = CompoundBorder(
        roundedBorder(radius, borderColor, fillColor),
        EmptyBorder(padding / 2, padding, padding / 2, padding)
    )

    private class RoundedBorder(
        private val radius: Int,
        private val borderColor: Color,
        private val fillColor: Color?
    ) : AbstractBorder() {
        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, w: Int, h: Int) {
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val shape = RoundRectangle2D.Float(
                x + 0.5f, y + 0.5f, w - 1f, h - 1f, radius.toFloat(), radius.toFloat()
            )
            if (fillColor != null) {
                g2.color = fillColor
                g2.fill(shape)
            }
            g2.color = borderColor
            g2.draw(shape)
            g2.dispose()
        }

        override fun getBorderInsets(c: Component) = Insets(radius / 2, radius / 2, radius / 2, radius / 2)

        override fun isBorderOpaque() = false
    }
}
