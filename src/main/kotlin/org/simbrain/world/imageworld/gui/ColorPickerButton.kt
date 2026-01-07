package org.simbrain.world.imageworld.gui

import org.simbrain.world.imageworld.ImageWorld
import java.awt.*
import javax.swing.*

/**
 * A compact color picker button that displays the current color as a swatch
 * and opens a popup panel with color options when clicked.
 */
class ColorPickerButton(private val imageWorld: ImageWorld) : JButton() {

    private val popupPanel: JPopupMenu

    private val presetColors = arrayOf(
        Color.white,
        Color.black,
        Color.red,
        Color.blue,
        Color.green,
        Color.yellow,
        Color.cyan,
        Color.magenta,
        Color.orange,
        Color.pink,
        Color.gray,
        Color.darkGray
    )

    init {
        toolTipText = "Select pen color"
        icon = ColorSwatchIcon(imageWorld, 18)

        popupPanel = createPopupPanel()

        addActionListener {
            popupPanel.show(this, 0, height)
        }
    }

    private fun createPopupPanel(): JPopupMenu {
        val popup = JPopupMenu()
        val panel = JPanel(BorderLayout(5, 5))
        panel.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

        // Color grid
        val colorGrid = JPanel(GridLayout(2, 6, 2, 2))
        for (color in presetColors) {
            val colorButton = createColorSwatchButton(color)
            colorButton.addActionListener {
                imageWorld.penColor = color
                popup.isVisible = false
                repaint()
            }
            colorGrid.add(colorButton)
        }
        panel.add(colorGrid, BorderLayout.CENTER)

        // Custom color button
        val customButton = JButton("Custom...")
        customButton.addActionListener {
            val newColor = JColorChooser.showDialog(
                SwingUtilities.getWindowAncestor(this),
                "Choose Color",
                imageWorld.penColor
            )
            if (newColor != null) {
                imageWorld.penColor = newColor
                repaint()
            }
            popup.isVisible = false
        }
        panel.add(customButton, BorderLayout.SOUTH)

        popup.add(panel)
        return popup
    }

    private fun createColorSwatchButton(color: Color): JButton {
        return JButton().apply {
            preferredSize = Dimension(24, 24)
            background = color
            isOpaque = true
            isBorderPainted = true
            border = BorderFactory.createLineBorder(Color.GRAY, 1)
            toolTipText = getColorName(color)
        }
    }

    private fun getColorName(color: Color): String {
        return when (color) {
            Color.white -> "White"
            Color.black -> "Black"
            Color.red -> "Red"
            Color.blue -> "Blue"
            Color.green -> "Green"
            Color.yellow -> "Yellow"
            Color.cyan -> "Cyan"
            Color.magenta -> "Magenta"
            Color.orange -> "Orange"
            Color.pink -> "Pink"
            Color.gray -> "Gray"
            Color.darkGray -> "Dark Gray"
            else -> "RGB(${color.red}, ${color.green}, ${color.blue})"
        }
    }

    /**
     * Icon that draws the current pen color as a swatch.
     */
    private class ColorSwatchIcon(private val imageWorld: ImageWorld, private val size: Int) : Icon {
        override fun getIconWidth() = size
        override fun getIconHeight() = size

        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Draw color swatch
            g2d.color = imageWorld.penColor
            g2d.fillRect(x + 1, y + 1, size - 2, size - 2)

            // Draw border
            g2d.color = Color.GRAY
            g2d.drawRect(x + 1, y + 1, size - 3, size - 3)
        }
    }
}
