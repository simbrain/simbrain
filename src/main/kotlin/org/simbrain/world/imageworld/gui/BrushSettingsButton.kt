package org.simbrain.world.imageworld.gui

import org.simbrain.util.ResourceManager
import org.simbrain.world.imageworld.ImageWorld
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

/**
 * A button that opens a popup panel with brush settings:
 * pen size, brush shape, and smoothing options.
 */
class BrushSettingsButton(private val imageWorld: ImageWorld) : JButton() {

    private var popupWindow: JWindow? = null
    private val penSizeLabel: JLabel

    init {
        icon = ResourceManager.getSmallIcon("menu_icons/PenToSquare.png")
        toolTipText = "Brush settings"

        // Use fixed-width label to accommodate "30px"
        penSizeLabel = JLabel("30px")
        penSizeLabel.preferredSize = penSizeLabel.preferredSize
        penSizeLabel.text = "${imageWorld.penSize}px"

        addActionListener {
            togglePopup()
        }
    }

    private fun togglePopup() {
        if (popupWindow?.isVisible == true) {
            popupWindow?.isVisible = false
            return
        }

        val window = SwingUtilities.getWindowAncestor(this) ?: return

        popupWindow = JWindow(window).apply {
            val panel = createSettingsPanel()
            contentPane.add(panel)
            pack()

            // Position below the button
            val buttonLocation = this@BrushSettingsButton.locationOnScreen
            setLocation(buttonLocation.x, buttonLocation.y + this@BrushSettingsButton.height)

            // Close when focus is lost
            addWindowFocusListener(object : WindowAdapter() {
                override fun windowLostFocus(e: WindowEvent) {
                    isVisible = false
                }
            })

            isVisible = true
        }
    }

    private fun createSettingsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        )
        panel.background = UIManager.getColor("Panel.background")

        // Pen size section
        val sizePanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        sizePanel.add(JLabel("Size:"))

        val penSizeSlider = JSlider(JSlider.HORIZONTAL, 1, 30, imageWorld.penSize)
        penSizeSlider.preferredSize = Dimension(100, penSizeSlider.preferredSize.height)
        penSizeSlider.addChangeListener {
            imageWorld.penSize = penSizeSlider.value
            penSizeLabel.text = "${imageWorld.penSize}px"
        }
        sizePanel.add(penSizeSlider)
        sizePanel.add(penSizeLabel)
        panel.add(sizePanel)

        panel.add(Box.createVerticalStrut(8))

        // Brush shape section
        val shapePanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        shapePanel.add(JLabel("Shape:"))

        val cbBrushShape = JComboBox(ImageWorld.BrushShape.entries.toTypedArray())
        cbBrushShape.selectedItem = imageWorld.brushShape
        cbBrushShape.addActionListener {
            imageWorld.brushShape = cbBrushShape.selectedItem as ImageWorld.BrushShape
        }
        shapePanel.add(cbBrushShape)
        panel.add(shapePanel)

        panel.add(Box.createVerticalStrut(8))

        // Smoothing section
        val smoothPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))

        val checkBoxSmoothing = JCheckBox("Smoothing")
        checkBoxSmoothing.isSelected = imageWorld.useSmoothing
        checkBoxSmoothing.addItemListener {
            imageWorld.useSmoothing = checkBoxSmoothing.isSelected
        }
        smoothPanel.add(checkBoxSmoothing)
        panel.add(smoothPanel)

        return panel
    }
}
