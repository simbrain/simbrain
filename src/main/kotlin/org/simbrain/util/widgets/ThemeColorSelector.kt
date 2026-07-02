package org.simbrain.util.widgets

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
import javax.swing.JColorChooser
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Compact editor for a [ThemeColor]: a light swatch, a dark swatch, and an "auto" checkbox that derives
 * the dark color from the light one. While auto is on, the dark swatch is disabled and previews the
 * inferred color; turning auto off lets the dark color be picked directly.
 */
class ThemeColorSelector : JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)) {

    private var lightColor: Color = Color.GRAY
    private var manualDarkColor: Color = Color.DARK_GRAY
    private var useManualDark: Boolean = false

    private val lightSwatch = swatch { chooseColor("Choose Light Color", lightColor) { lightColor = it } }
    private val darkSwatch = swatch { chooseColor("Choose Dark Color", effectiveDark()) { manualDarkColor = it } }

    private val autoCheckBox = JCheckBox("auto").apply {
        toolTipText = "Derive the dark-mode color from the light color"
        addActionListener {
            useManualDark = !isSelected
            refreshSwatches()
        }
    }

    init {
        add(JLabel("Light"))
        add(lightSwatch)
        add(JLabel("Dark"))
        add(darkSwatch)
        add(autoCheckBox)
        refreshSwatches()
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
            refreshSwatches()
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
            refreshSwatches()
        }
}
