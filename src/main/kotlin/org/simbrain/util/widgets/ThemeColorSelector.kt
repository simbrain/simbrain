package org.simbrain.util.widgets

import org.simbrain.util.SimbrainConstants.NULL_STRING
import org.simbrain.util.Theme
import org.simbrain.util.ThemeColor
import org.simbrain.util.inferDark
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JColorChooser
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Compact editor for a [ThemeColor]: a light swatch, a dark swatch, and an "auto" checkbox that derives
 * the dark color from the light one. While auto is on, the dark swatch is disabled and previews the
 * inferred color; turning auto off lets the dark color be picked directly. A null state, shown as "...", stands in
 * for the swatches when several edited objects have different colors; any edit clears it.
 */
class ThemeColorSelector : JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)) {

    private var lightColor: Color = Color.GRAY
    private var manualDarkColor: Color = Color.DARK_GRAY
    private var useManualDark: Boolean = false

    /**
     * True while no single color is being shown.
     */
    var isNull: Boolean = false
        private set

    /**
     * Called whenever the value is edited, whether by a swatch, the auto checkbox, or the [value] setter.
     */
    var onChanged: (() -> Unit)? = null

    private val nullLabel = JLabel(NULL_STRING).apply { isVisible = false }
    private val lightLabel = JLabel("Light")
    private val darkLabel = JLabel("Dark")

    private val lightSwatch = swatch { chooseColor("Choose Light Color", lightColor) { lightColor = it } }
    private val darkSwatch = swatch { chooseColor("Choose Dark Color", effectiveDark()) { manualDarkColor = it } }

    private val autoCheckBox = JCheckBox("auto").apply {
        toolTipText = "Derive the dark-mode color from the light color"
        addActionListener {
            useManualDark = !isSelected
            leaveNullState()
            refreshSwatches()
            onChanged?.invoke()
        }
    }

    private val swatchComponents: List<JComponent> = listOf(lightLabel, lightSwatch, darkLabel, darkSwatch, autoCheckBox)

    init {
        add(lightLabel)
        add(lightSwatch)
        add(darkLabel)
        add(darkSwatch)
        add(autoCheckBox)
        add(nullLabel)
        refreshSwatches()
    }

    /**
     * Show "..." instead of the swatches, until the color is edited.
     */
    fun setNull() {
        isNull = true
        swatchComponents.forEach { it.isVisible = false }
        nullLabel.isVisible = true
    }

    private fun leaveNullState() {
        if (!isNull) return
        isNull = false
        swatchComponents.forEach { it.isVisible = true }
        nullLabel.isVisible = false
    }

    private fun swatch(onClick: () -> Unit) = JPanel().apply {
        preferredSize = Dimension(26, 16)
        border = BorderFactory.createLineBorder(Theme.divider)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (isEnabled) onClick()
            }
        })
    }

    private fun chooseColor(title: String, initial: Color, onChosen: (Color) -> Unit) {
        JColorChooser.showDialog(this, title, initial)?.let {
            onChosen(it)
            leaveNullState()
            refreshSwatches()
            onChanged?.invoke()
        }
    }

    private fun effectiveDark(): Color = if (useManualDark) manualDarkColor else inferDark(lightColor)

    private fun refreshSwatches() {
        lightSwatch.background = lightColor
        darkSwatch.background = effectiveDark()
        darkSwatch.isEnabled = useManualDark
        darkSwatch.cursor = Cursor.getPredefinedCursor(
            if (useManualDark) Cursor.HAND_CURSOR else Cursor.DEFAULT_CURSOR
        )
        autoCheckBox.isSelected = !useManualDark
    }

    var value: ThemeColor
        get() = ThemeColor(lightColor, manualDarkColor, useManualDark)
        set(themeColor) {
            lightColor = themeColor.light
            manualDarkColor = themeColor.dark
            useManualDark = themeColor.useManualDark
            leaveNullState()
            refreshSwatches()
            onChanged?.invoke()
        }
}
