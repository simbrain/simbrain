package org.simbrain.util.widgets

import org.simbrain.util.Theme
import org.simbrain.util.blend
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JToggleButton
import javax.swing.UIManager

class SimbrainToggleButton(
    text: String? = null,
    icon: Icon? = null,
    private val stateGetter: (() -> Boolean)? = null,
    private val stateSetter: ((Boolean) -> Unit)? = null,
    private val tooltipGenerator: ((Boolean) -> String)? = null
) : JToggleButton(text, icon) {

    init {
        // Padding only; the 1px outline and background are painted in paintComponent so they track
        // the active light/dark theme instead of being baked to fixed grays.
        border = BorderFactory.createEmptyBorder(2, 3, 2, 3)
        isContentAreaFilled = false
        isFocusPainted = false
        // Repaint on rollover/selection/press transitions so the background reflects the new state.
        addChangeListener { repaint() }

        if (stateGetter != null && stateSetter != null && tooltipGenerator != null) {
            updateFromExternalState()
            addActionListener {
                val newState = isSelected
                stateSetter(newState)
                updateFromExternalState()
            }
        }
    }

    /**
     * Updates the button state from external changes
     */
    fun updateFromExternalState() {
        if (stateGetter != null && tooltipGenerator != null) {
            isSelected = stateGetter()
            toolTipText = tooltipGenerator(isSelected)
        }
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val base = UIManager.getColor("Button.background") ?: Color(238, 238, 238)
        val accent = UIManager.getColor("Label.foreground") ?: Color.BLACK
        // Mix a little foreground into the button color for the active/pressed/hover states, so the
        // emphasis reads correctly in both light (darkens) and dark (lightens) themes.
        val bgColor = when {
            isSelected -> blend(accent, base, 0.18)
            model.isPressed -> blend(accent, base, 0.12)
            model.isRollover -> blend(accent, base, 0.06)
            else -> base
        }
        g2d.color = bgColor
        g2d.fillRect(0, 0, width, height)
        g2d.color = Theme.divider
        g2d.drawRect(0, 0, width - 1, height - 1)

        super.paintComponent(g)
    }
}
