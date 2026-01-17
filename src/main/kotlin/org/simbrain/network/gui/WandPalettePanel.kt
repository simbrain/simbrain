package org.simbrain.network.gui

import org.simbrain.util.displayInDialog
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.util.showOpenDialog
import org.simbrain.util.showSaveDialog
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * UI panel for displaying and managing the wand action palette.
 * Shows a list of configurable actions that can be selected for use with the wand tool.
 */
class WandPalettePanel(
    val palette: WandPalette,
    private val onSelectionChanged: () -> Unit = {}
) : JPanel() {

    private val listPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    
    private var isRebuilding = false

    init {
        layout = BorderLayout()
        border = EmptyBorder(8, 8, 8, 8)

        // Scrollable list area
        val scrollPane = JScrollPane(listPanel).apply {
            border = null
            preferredSize = Dimension(300, 200)
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }
        add(scrollPane, BorderLayout.CENTER)

        // Bottom buttons panel
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))

        val addButton = JButton("+").apply {
            toolTipText = "Add new action"
            addActionListener { addNewAction() }
        }
        buttonPanel.add(addButton)

        buttonPanel.add(Box.createHorizontalStrut(20))

        val exportButton = JButton("Export...").apply {
            toolTipText = "Export palette to file"
            addActionListener { exportPalette() }
        }
        buttonPanel.add(exportButton)

        val importButton = JButton("Import...").apply {
            toolTipText = "Import palette from file"
            addActionListener { importPalette() }
        }
        buttonPanel.add(importButton)

        add(buttonPanel, BorderLayout.SOUTH)

        // Listen for palette changes
        palette.events.apply {
            actionAdded.on { rebuildList() }
            actionRemoved.on { rebuildList() }
            paletteChanged.on { rebuildList() }
            selectionChanged.on { updateSelection() }
        }

        rebuildList()
    }

    private fun rebuildList() {
        // Prevent re-entrant calls when events cascade (like when removing an action fires both
        //  actionRemoved and selectionChanged events, each trying to rebuild the list).
        // TODO: Should not be necessary. Try preventing this from happening in the first place.
        if (isRebuilding) return
        isRebuilding = true
        
        listPanel.removeAll()
        palette.actions.forEachIndexed { index, action ->
            if (index > 0) {
                listPanel.add(Box.createVerticalStrut(2))
            }
            listPanel.add(createActionRow(index, action))
        }
        listPanel.add(Box.createVerticalGlue())
        listPanel.revalidate()
        listPanel.repaint()
        
        isRebuilding = false
    }

    private fun updateSelection() {
        // Rebuild to update selection highlighting
        rebuildList()
        onSelectionChanged()
    }

    private fun createActionRow(index: Int, action: WandAction): JPanel {
        val isSelected = index == palette.selectedIndex

        return JPanel(BorderLayout(5, 0)).apply {
            border = BorderFactory.createCompoundBorder(
                if (isSelected) BorderFactory.createLineBorder(Color(100, 150, 255), 2)
                else BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                EmptyBorder(4, 6, 4, 6)
            )
            background = if (isSelected) Color(230, 240, 255) else Color.WHITE
            maximumSize = Dimension(Int.MAX_VALUE, 36)
            preferredSize = Dimension(280, 36)

            // Wand icon (double-click to edit)
            val iconLabel = WandIcon(action.color, 24, action.letter).apply {
                toolTipText = "Double-click to configure this action"
            }
            add(iconLabel, BorderLayout.WEST)

            // Description label with tooltip for long descriptions
            val descLabel = JLabel(action.description).apply {
                font = font.deriveFont(12f)
                toolTipText = action.description
            }
            add(descLabel, BorderLayout.CENTER)

            // Right side buttons panel
            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
                isOpaque = false
            }

            // Kebab menu button for editing
            val kebabButton = JButton().apply {
                icon = KebabIcon()
                preferredSize = Dimension(20, 24)
                margin = Insets(0, 0, 0, 0)
                isFocusPainted = false
                isContentAreaFilled = false
                isBorderPainted = false
                toolTipText = "Edit this action"
                addActionListener { editAction(index) }
            }
            buttonPanel.add(kebabButton)

            // Delete button
            val deleteButton = JButton("×").apply {
                preferredSize = Dimension(24, 24)
                margin = Insets(0, 0, 0, 0)
                isFocusPainted = false
                toolTipText = "Delete this action"
                addActionListener { deleteAction(index) }
            }
            buttonPanel.add(deleteButton)

            add(buttonPanel, BorderLayout.EAST)

            // Click to select, double-click to edit
            val clickListener = object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    palette.selectAction(index)
                }
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        editAction(index)
                    }
                }
            }
            // Add listener to panel and its main children so clicks anywhere work
            addMouseListener(clickListener)
            iconLabel.addMouseListener(clickListener)
            descLabel.addMouseListener(clickListener)
        }
    }

    private fun addNewAction() {
        // Create a default action wrapped for type selection
        val wrapper = objectWrapper("Action", AdjustValueAction() as WandAction)
        AnnotatedPropertyEditor(listOf(wrapper)).displayInDialog {
            commitChanges()
            val newAction = wrapper.editingObject
            palette.addAction(newAction)
            palette.selectAction(palette.actions.size - 1)
        }
    }

    private fun editAction(index: Int) {
        // Wrap the action so user can change its type
        val wrapper = objectWrapper("Action", palette.actions[index])
        AnnotatedPropertyEditor(listOf(wrapper)).displayInDialog {
            commitChanges()
            // Replace the action in the list (type may have changed)
            palette.actions[index] = wrapper.editingObject
            rebuildList()
            onSelectionChanged()
        }
    }

    private fun deleteAction(index: Int) {
        if (palette.actions.size > 1) {
            palette.removeAction(index)
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Cannot delete the last action. The palette must have at least one action.",
                "Cannot Delete",
                JOptionPane.WARNING_MESSAGE
            )
        }
    }

    private fun exportPalette() {
        showSaveDialog(initialFileName = "wand_palette.xml") {
            val xml = getSimbrainXStream().toXML(palette)
            writeText(xml)
        }
    }

    private fun importPalette() {
        showOpenDialog(extension = "xml") {
            try {
                val imported = getSimbrainXStream().fromXML(readText()) as WandPalette
                palette.setActions(imported.actions)
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    this@WandPalettePanel,
                    "Failed to import palette: ${e.message}",
                    "Import Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }
}

/**
 * Icon component that draws a wand circle with the given color and letter.
 */
class WandIcon(private val color: Color, private val size: Int, private val letter: String = "") : JLabel() {

    init {
        preferredSize = Dimension(size, size)
        minimumSize = Dimension(size, size)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val padding = 2
        val circleSize = size - padding * 2

        // Draw filled circle with action color
        g2.color = color
        g2.fill(Ellipse2D.Double(padding.toDouble(), padding.toDouble(), circleSize.toDouble(), circleSize.toDouble()))

        // Draw border
        g2.color = Color.DARK_GRAY
        g2.stroke = BasicStroke(1.5f)
        g2.draw(Ellipse2D.Double(padding.toDouble(), padding.toDouble(), circleSize.toDouble(), circleSize.toDouble()))

        // Draw letter in center
        val displayLetter = letter.firstOrNull()?.toString() ?: ""
        g2.color = Color(0, 0, 0, 200)
        g2.font = Font(Font.SANS_SERIF, Font.BOLD, (size * 0.5).toInt())
        val fm = g2.fontMetrics
        val textX = (size - fm.stringWidth(displayLetter)) / 2
        val textY = (size - fm.height) / 2 + fm.ascent
        g2.drawString(displayLetter, textX, textY)
    }
}

/**
 * Kebab menu icon (three vertical dots).
 */
class KebabIcon : Icon {
    private val dotSize = 2
    private val dotSpacing = 2
    private val width = dotSize + 4
    private val height = (dotSize * 3) + (dotSpacing * 2) + 4

    override fun getIconWidth() = width
    override fun getIconHeight() = height

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2.color = Color.DARK_GRAY
        val startX = x + 2
        val startY = y + 2

        for (i in 0..2) {
            val dotY = startY + i * (dotSize + dotSpacing)
            g2.fill(Ellipse2D.Double(startX.toDouble(), dotY.toDouble(), dotSize.toDouble(), dotSize.toDouble()))
        }
    }
}

/**
 * Split button for wand actions.
 * Main button area activates wand mode, triangle dropdown shows palette popup.
 * Displays the current action's letter in a black/white icon for consistency with other toolbar buttons.
 */
class WandPaletteButton(val palette: WandPalette, val networkPanel: NetworkPanel) : JPanel() {

    private var popupMenu: JPopupMenu? = null
    private val mainButton: JButton
    private val dropdownButton: JButton

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false

        // Main button - activates wand mode
        mainButton = JButton().apply {
            toolTipText = "Wand tool (d) - click to activate"
            icon = WandButtonIcon(palette, 18)
            addActionListener {
                networkPanel.mouseCursor = MouseEventHandler.MouseCursor.Wand
                updateWandCursorColor()
            }
        }

        // Dropdown button - shows palette popup
        dropdownButton = JButton().apply {
            toolTipText = "Select wand action"
            icon = DropdownArrowIcon(6, 18)
            addActionListener {
                showPalettePopup()
            }
        }

        add(mainButton)
        add(dropdownButton)

        // Update icon and cursor color when selection changes
        palette.events.selectionChanged.on {
            updateWandCursorColor()
            mainButton.repaint()
        }

        // Set initial cursor color
        updateWandCursorColor()
    }

    private fun updateWandCursorColor() {
        palette.selectedAction?.color?.let { color ->
            MouseEventHandler.MouseCursor.Wand.wandColor = color
        }
    }

    private fun showPalettePopup() {
        popupMenu?.isVisible = false

        popupMenu = JPopupMenu().apply {
            add(WandPalettePanel(palette) {
                // On selection changed, activate wand mode and update
                networkPanel.mouseCursor = MouseEventHandler.MouseCursor.Wand
                updateWandCursorColor()
                mainButton.repaint()
            })
        }
        popupMenu?.show(mainButton, 0, mainButton.height)
    }

    /**
     * Icon for the wand palette button that shows the current action's letter.
     * Uses black/white colors for consistency with other toolbar icons.
     */
    private class WandButtonIcon(private val palette: WandPalette, private val size: Int) : Icon {
        override fun getIconWidth() = size
        override fun getIconHeight() = size

        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val padding = 1
            val circleSize = size - padding * 2

            // Draw circle outline (black/white style)
            g2.color = Color.DARK_GRAY
            g2.stroke = BasicStroke(1.5f)
            g2.draw(Ellipse2D.Double((x + padding).toDouble(), (y + padding).toDouble(), circleSize.toDouble(), circleSize.toDouble()))

            // Draw the letter in the center
            val displayLetter = palette.selectedAction?.letter?.firstOrNull()?.toString() ?: ""

            g2.color = Color.DARK_GRAY
            g2.font = Font(Font.SANS_SERIF, Font.BOLD, (size * 0.6).toInt())
            val fm = g2.fontMetrics
            val textX = x + (size - fm.stringWidth(displayLetter)) / 2
            val textY = y + (size - fm.height) / 2 + fm.ascent
            g2.drawString(displayLetter, textX, textY)
        }
    }

    /**
     * Small dropdown arrow icon for the split button.
     */
    private class DropdownArrowIcon(private val width: Int, private val height: Int) : Icon {
        override fun getIconWidth() = width
        override fun getIconHeight() = height

        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Draw small downward-pointing triangle
            val arrowWidth = 6
            val arrowHeight = 4
            val arrowX = x + (width - arrowWidth) / 2
            val arrowY = y + (height - arrowHeight) / 2

            val triangle = java.awt.Polygon()
            triangle.addPoint(arrowX, arrowY)
            triangle.addPoint(arrowX + arrowWidth, arrowY)
            triangle.addPoint(arrowX + arrowWidth / 2, arrowY + arrowHeight)

            g2.color = Color.DARK_GRAY
            g2.fill(triangle)
        }
    }
}
