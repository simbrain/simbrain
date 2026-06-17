package org.simbrain.util.widgets

import org.simbrain.util.Theme
import org.simbrain.util.UserParameter
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.toHSB
import java.awt.*
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.*

/**
 * Renders a population's activations as a single "field image". The most active item is drawn at the center,
 * largest and most saturated. Less active items above [threshold] are arranged radially around the center,
 * with size, saturation, and proximity to the center scaling with their activation.
 *
 * Items below [threshold] are not drawn.
 *
 * Two ways to drive it:
 *   1. Pull-mode: pass a [source] lambda that returns current `(label, activation)` pairs.
 *   2. Push-mode: call [setData] with a labels list and an activations array (e.g. from a vector coupling).
 *      Calling [setData] switches the panel into push-mode until [clearData] is called.
 *
 * Future work: this is intended to migrate to `org.simbrain.plot` as a couplable workspace component.
 */
class FieldImagePanel(
    private val source: () -> List<Pair<String, Double>> = { emptyList() }
) : JPanel() {

    var threshold: Double = 0.1
    var maxItems: Int = 8
    private val settings = FieldImageSettings()

    var textColor: Color
        get() = settings.textColor
        set(value) {
            settings.textColor = value
            repaint()
        }

    var scaleSaturationByActivation: Boolean
        get() = settings.scaleSaturationByActivation
        set(value) {
            settings.scaleSaturationByActivation = value
            repaint()
        }

    private var pushedLabels: List<String>? = null
    private var pushedActivations: DoubleArray? = null

    // TODO: Move these into component-level preferences if Field Image becomes a workspace component.
    private val preferencesButton = JButton("Preferences...").apply {
        toolTipText = "Edit field image preferences"
        isFocusable = false
        addActionListener {
            settings.createEditorDialog(
                titleName = "Field Image Preferences",
                parent = SwingUtilities.getWindowAncestor(this@FieldImagePanel)
            ) {
                repaint()
            }.display()
        }
    }

    init {
        layout = BorderLayout()
        background = Color.WHITE
        preferredSize = Dimension(360, 360)
        minimumSize = Dimension(120, 120)
        add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 4)).apply {
            isOpaque = false
            add(preferencesButton)
        }, BorderLayout.NORTH)
    }

    fun setData(labels: List<String>, activations: DoubleArray) {
        pushedLabels = labels
        pushedActivations = activations
        repaint()
    }

    fun clearData() {
        pushedLabels = null
        pushedActivations = null
        repaint()
    }

    private fun currentItems(): List<Pair<String, Double>> {
        val pl = pushedLabels
        val pa = pushedActivations
        return if (pl != null && pa != null) {
            val n = min(pl.size, pa.size)
            (0 until n).map { i -> pl[i] to pa[i] }
        } else {
            source()
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val baseDim = min(width, height)
        val maxFont = (baseDim * 0.18).toInt().coerceAtLeast(14)
        val minFont = (baseDim * 0.05).toInt().coerceAtLeast(8)

        val cx = width / 2
        val cy = height / 2

        val items = currentItems()
            .filter { abs(it.second) > threshold }
            .sortedByDescending { abs(it.second) }

        if (items.isEmpty()) {
            g2.color = Color(0xAAAAAA)
            val msgFont = (baseDim * 0.05).toInt().coerceAtLeast(10)
            g2.font = Theme.font(msgFont, Font.ITALIC)
            val msg = "(quiet)"
            val fm = g2.fontMetrics
            g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy + (fm.ascent - fm.descent) / 2)
            return
        }

        val center = items.first()
        val centerLabel = center.first.ifBlank { "?" }
        val aMax = abs(center.second)

        val outer = items.drop(1).take(maxItems - 1)
        if (outer.isNotEmpty()) {
            val maxRadius = (baseDim / 2 - maxFont - 8).coerceAtLeast(20)
            val minRadius = maxFont / 2 + 8
            val angleStep = 2 * PI / outer.size
            outer.forEachIndexed { i, (label, act) ->
                val ratio = (abs(act) / aMax).coerceIn(0.0, 1.0).toFloat()
                val angle = -PI / 2 + i * angleStep
                val r = (minRadius + (1 - ratio) * (maxRadius - minRadius)).toInt()
                val px = cx + (r * cos(angle)).toInt()
                val py = cy + (r * sin(angle)).toInt()
                drawLabel(g2, label.ifBlank { "?" }, px, py, ratio, minFont, maxFont)
            }
        }

        drawLabel(g2, centerLabel, cx, cy, 1f, minFont, maxFont)
    }

    private fun drawLabel(g2: Graphics2D, text: String, cx: Int, cy: Int, intensity: Float, minFont: Int, maxFont: Int) {
        val size = (minFont + intensity * (maxFont - minFont)).toInt().coerceAtLeast(8)
        g2.font = Theme.font(size, Font.BOLD)
        g2.color = settings.textColor.withIntensity(intensity)
        val fm = g2.fontMetrics
        g2.drawString(text, cx - fm.stringWidth(text) / 2, cy + (fm.ascent - fm.descent) / 2)
    }

    private fun Color.withIntensity(intensity: Float): Color {
        if (!settings.scaleSaturationByActivation) return this
        val (hue, saturation, brightness) = toHSB()
        val i = intensity.coerceIn(0f, 1f)
        val adjustedSaturation = (0.35f + 0.65f * i) * saturation
        val adjustedBrightness = 0.55f + 0.45f * brightness
        return Color.getHSBColor(hue, adjustedSaturation.coerceIn(0f, 1f), adjustedBrightness.coerceIn(0f, 1f))
    }

    companion object {
        val DEFAULT_TEXT_COLOR: Color = Color(0x118F20)
    }

    private class FieldImageSettings : EditableObject {

        @UserParameter(label = "Text color", description = "Base color used for field image labels.", order = 10)
        var textColor: Color = DEFAULT_TEXT_COLOR

        @UserParameter(
            label = "Scale saturation by activation",
            description = "If true, less active labels are drawn with lower saturation.",
            order = 20
        )
        var scaleSaturationByActivation: Boolean = true
    }
}
