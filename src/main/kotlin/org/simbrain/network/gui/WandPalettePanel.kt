/**
 * Swing views of the wand palette: the popup list where the primary action and the secondary action, the one
 * applied with Shift held, are chosen and edited, and the toolbar split button that switches to the wand and opens
 * that list.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.Job
import net.miginfocom.swing.MigLayout
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.objectWrapper
import java.awt.*
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

/**
 * UI panel for displaying and managing the wand action palette.
 * Shows a list of configurable actions that can be selected for use with the wand tool.
 *
 * @param persist saves the palette after a change made here; by default to the user's preferences
 */
class WandPalettePanel(
    val palette: WandPalette,
    private val persist: (WandPalette) -> Unit = { NetworkPreferences.wandPalette = it },
    private val onSelectionChanged: () -> Unit = {}
) : JPanel() {

    private val listPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    
    private var isRebuilding = false

    /**
     * Whether a click on a row chooses the secondary action rather than the selected one. Follows the two tabs
     * above the list.
     */
    private var choosingSecondary = false

    private val primaryTab = slotTab("Primary", secondary = false)

    private val secondaryTab = slotTab("Secondary (Shift)", secondary = true)

    /**
     * The popup this panel is shown in, if it is in one.
     */
    private val hostPopup get() = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, this) as JPopupMenu?

    /**
     * The row menu now open, if one is.
     */
    private var rowMenu: JPopupMenu? = null

    /**
     * Whether the palette stays up once [rowMenu] has closed.
     */
    private var keepPaletteAfterRowMenu = false

    init {
        layout = BorderLayout()
        border = EmptyBorder(Theme.componentGap, Theme.componentGap, Theme.componentGap, Theme.componentGap)

        ButtonGroup().apply {
            add(primaryTab)
            add(secondaryTab)
        }
        primaryTab.isSelected = true
        val swapButton = JButton(SwapIcon()).apply {
            putClientProperty("JButton.buttonType", "toolBarButton")
            toolTipText = "Swap the primary and secondary actions (x)"
            addActionListener {
                palette.swapSelectedAndSecondary()
                savePalette()
            }
        }
        val slotPanel = JPanel(MigLayout("insets 0 0 ${Theme.tightGap} 0, fillx, gap 0", "[grow,fill,sg slot][grow,fill,sg slot]${Theme.tightGap}[]")).apply {
            add(primaryTab)
            add(secondaryTab)
            add(swapButton)
        }
        add(slotPanel, BorderLayout.NORTH)

        // Scrollable list area
        val scrollPane = JScrollPane(listPanel).apply {
            border = null
            preferredSize = Dimension(300, 200)
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }
        add(scrollPane, BorderLayout.CENTER)

        val addButton = JButton(Icons.small("plus.png")).apply {
            toolTipText = "Add new action"
            addActionListener { addNewAction() }
        }
        val exportButton = JButton("Export...").apply {
            toolTipText = "Export palette to file"
            addActionListener { exportPalette() }
        }
        val importButton = JButton("Import...").apply {
            toolTipText = "Import palette from file"
            addActionListener { importPalette() }
        }
        val revertButton = JButton("Revert to default").apply {
            toolTipText = "Reset palette to default actions"
            addActionListener { revertToDefault() }
        }

        val buttonPanel = JPanel(MigLayout("insets ${Theme.tightGap} 0 0 0", "[]${Theme.sectionGap}[]${Theme.componentGap}[]push[]")).apply {
            add(addButton)
            add(exportButton)
            add(importButton)
            add(revertButton)
        }
        add(buttonPanel, BorderLayout.SOUTH)

        // Add Esc key binding to close popup
        val inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        val actionMap = actionMap
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "closePopup")
        actionMap.put("closePopup", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                hostPopup?.isVisible = false
            }
        })

        rebuildList()
    }

    private var paletteSubscriptions = emptyList<Job>()

    /**
     * Follows the palette while this panel is on screen. The palette outlives every popup that shows it, so a
     * panel that went on listening after its popup closed would keep switching its network to the wand whenever
     * the selection changed.
     */
    override fun addNotify() {
        super.addNotify()
        paletteSubscriptions.forEach { it.cancel() }
        paletteSubscriptions = with(palette.events) {
            listOf(
                actionAdded.on { rebuildList() },
                actionRemoved.on { rebuildList() },
                paletteChanged.on { rebuildList() },
                selectionChanged.on { updateSelection() },
                secondaryChanged.on { rebuildList() }
            )
        }
        rebuildList()
    }

    override fun removeNotify() {
        paletteSubscriptions.forEach { it.cancel() }
        paletteSubscriptions = emptyList()
        super.removeNotify()
    }

    /**
     * One of the two tabs above the list. Each shows the action it stands for, and selecting it makes clicks on
     * the rows choose that action.
     */
    private fun slotTab(title: String, secondary: Boolean) = JToggleButton(title).apply {
        putClientProperty("JButton.buttonType", "tab")
        horizontalAlignment = SwingConstants.LEFT
        iconTextGap = Theme.tightGap + 2
        isFocusable = false
        addActionListener {
            choosingSecondary = secondary
            rebuildList()
        }
    }

    private fun updateSlotTab(tab: JToggleButton, action: WandAction?, gesture: String) {
        tab.icon = action?.let { WandActionIcon(it.color, 20, it.letter) }
        tab.toolTipText = if (action != null) {
            "$gesture applies: ${action.description}. Select this tab to choose another action from the list"
        } else {
            "No secondary action is chosen, so Shift changes nothing. Select this tab to choose one from the list"
        }
    }

    private fun rebuildList() {
        // Prevent re-entrant calls when events cascade (like when removing an action fires both
        //  actionRemoved and selectionChanged events, each trying to rebuild the list).
        // TODO: Should not be necessary. Try preventing this from happening in the first place.
        if (isRebuilding) return
        isRebuilding = true

        // A row menu belongs to a row that is about to be replaced
        closeRowMenu()

        val secondaryAction = palette.secondaryAction
        updateSlotTab(primaryTab, palette.selectedAction, "Dragging the wand")
        updateSlotTab(secondaryTab, secondaryAction, "Dragging the wand with Shift held")
        
        listPanel.removeAll()
        palette.actions.forEachIndexed { index, action ->
            if (index > 0) {
                listPanel.add(Box.createVerticalStrut(2))
            }
            listPanel.add(createActionRow(index, action, secondaryAction))
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

    private fun createActionRow(index: Int, action: WandAction, secondaryAction: WandAction?): JPanel {
        // The row highlighted is the choice of the tab being chosen for; the other tab's action carries a tag
        val isSelected = if (choosingSecondary) action === secondaryAction else index == palette.selectedIndex
        val otherRole = when {
            choosingSecondary && index == palette.selectedIndex -> "Primary"
            !choosingSecondary && action === secondaryAction -> "Secondary"
            else -> null
        }

        val accent = UIManager.getColor("Component.focusColor") ?: Color(100, 150, 255)
        // Opaque selection wash: blend the accent over the card background so the (opaque) row
        // paints a solid color, rather than an alpha tint that would smear on scroll.
        val selectedBg = blend(accent, Theme.cardBg, 0.16)

        return JPanel(BorderLayout(Theme.tightGap, 0)).apply {
            border = BorderFactory.createCompoundBorder(
                if (isSelected) BorderFactory.createLineBorder(accent, 2)
                else BorderFactory.createLineBorder(Theme.divider, 1),
                EmptyBorder(4, 6, 4, 6)
            )
            background = if (isSelected) selectedBg else Theme.cardBg
            maximumSize = Dimension(Int.MAX_VALUE, 36)
            preferredSize = Dimension(280, 36)

            // Wand icon (double-click to edit)
            val iconLabel = WandIcon(action.color, 24, action.letter).apply {
                toolTipText = "Double-click to configure this action"
            }
            add(iconLabel, BorderLayout.WEST)

            // Description label with tooltip for long descriptions
            val descLabel = JLabel(action.description).apply {
                font = Theme.font(12)
                toolTipText = action.description
            }
            add(descLabel, BorderLayout.CENTER)

            // Right side buttons panel
            val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
                isOpaque = false
            }

            // On the highlighted row, an offer of the primary action's opposite as the secondary when something
            // else is chosen. That row is the primary action in one tab and the chosen secondary in the other, so
            // the offer stays with the selection. Worded as something to do, so that it is not read as saying what
            // the secondary is.
            val suggestion = if (isSelected) palette.suggestedSecondary else null
            if (suggestion != null) {
                val label = if (choosingSecondary) "Use instead" else "Use for Shift"
                buttonPanel.add(JButton(label, WandActionIcon(suggestion.color, 16, suggestion.letter)).apply {
                    iconTextGap = 3
                    font = Theme.font(10)
                    margin = Insets(1, 3, 1, 5)
                    isFocusPainted = false
                    val addsIt = if (suggestion in palette.actions) "" else ", adding it to the palette"
                    val current = secondaryAction?.let { "The secondary is now ${it.description}." }
                        ?: "There is no secondary action now."
                    toolTipText = "<html>Suggestion: make the secondary action the opposite of the primary one, " +
                            "${suggestion.description}$addsIt.<br>$current</html>"
                    addActionListener {
                        palette.useSuggestedSecondary()
                        savePalette()
                    }
                })
            }

            val roleTag = otherRole?.let {
                JLabel(it).apply {
                    font = Theme.font(10)
                    foreground = Theme.mutedText
                    border = BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.divider, 1, true),
                        EmptyBorder(1, 5, 1, 5)
                    )
                    toolTipText = if (it == "Primary") {
                        "The primary action, applied by dragging the wand"
                    } else {
                        "The secondary action, applied by dragging the wand with Shift held"
                    }
                }
            }
            roleTag?.let { buttonPanel.add(it) }

            // Kebab button for the row's menu, shown pressed while that menu is open
            val kebabButton = JToggleButton(KebabIcon()).apply {
                putClientProperty("JButton.buttonType", "toolBarButton")
                preferredSize = Dimension(24, 24)
                margin = Insets(0, 0, 0, 0)
                isFocusable = false
                toolTipText = "Edit or delete this action"
                // The button's own state, not the row's isSelected
                addActionListener { if (model.isSelected) showRowMenu(this, index) else closeRowMenu() }
            }
            buttonPanel.add(kebabButton)

            add(buttonPanel, BorderLayout.EAST)

            // Click to choose for the current tab, shift-click to make secondary, double-click to edit
            val clickListener = object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    if (choosingSecondary || e.isShiftDown) makeSecondary(index) else palette.selectAction(index)
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
            roleTag?.addMouseListener(clickListener)
        }
    }

    private fun makeSecondary(index: Int) {
        palette.secondaryIndex = index
        savePalette()
    }

    /**
     * Opens the menu of the row at [index] under its kebab button, with the palette held open beneath it. Closing
     * the menu backs out one level, leaving the palette up, when the user is still at work in the palette: after a
     * delete, a press elsewhere in the palette, a second press on the kebab button, or Escape. Anything else that
     * closes the menu, such as choosing to edit or a press outside the palette, closes the palette with it.
     */
    private fun showRowMenu(kebab: JToggleButton, index: Int) {
        val host = hostPopup as? WandPalettePopup
        val menu = JPopupMenu()
        menu.add(JMenuItem("Edit...").apply {
            addActionListener {
                // Closed before the dialog opens rather than under it
                hostPopup?.isVisible = false
                editAction(index)
            }
        })
        menu.add(JMenuItem("Delete").apply {
            if (palette.actions.size <= 1) {
                isEnabled = false
                toolTipText = "The palette must have at least one action"
            }
            addActionListener {
                keepPaletteAfterRowMenu = true
                deleteAction(index)
            }
        })

        // Presses in the palette do not close a popup menu shown over it, and Escape would close the palette too
        val watcher = AWTEventListener { event ->
            if (event is KeyEvent) {
                if (event.id == KeyEvent.KEY_PRESSED && event.keyCode == KeyEvent.VK_ESCAPE) {
                    event.consume()
                    closeRowMenu()
                }
            } else if (event.id == MouseEvent.MOUSE_PRESSED) {
                // A press on the kebab button is left to the button, which closes the menu when it is released
                val source = event.source as? Component
                if (source !== kebab && host != null && SwingUtilities.isDescendingFrom(source, host)) closeRowMenu()
            }
        }
        menu.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {}
            override fun popupMenuCanceled(e: PopupMenuEvent) {}
            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(watcher)
                kebab.isSelected = false
                // Once the menu item that closed the menu has run, so that it has its say on the palette
                SwingUtilities.invokeLater { rowMenuClosed(menu, host) }
            }
        })

        rowMenu = menu
        keepPaletteAfterRowMenu = false
        host?.heldOpen = true
        Toolkit.getDefaultToolkit().addAWTEventListener(watcher, AWTEvent.MOUSE_EVENT_MASK or AWTEvent.KEY_EVENT_MASK)
        menu.show(kebab, 0, kebab.height)
    }

    /**
     * Closes the open row menu, if there is one, leaving the palette up.
     */
    private fun closeRowMenu() {
        val menu = rowMenu ?: return
        keepPaletteAfterRowMenu = true
        menu.isVisible = false
    }

    private fun rowMenuClosed(menu: JPopupMenu, host: WandPalettePopup?) {
        // A menu opened since then holds the palette open in its turn
        if (rowMenu !== menu) return
        rowMenu = null
        host ?: return
        host.heldOpen = false
        if (!host.isVisible) return
        if (keepPaletteAfterRowMenu) {
            // The palette is the open menu again, so a press outside it closes it
            MenuSelectionManager.defaultManager().selectedPath = arrayOf(host)
        } else {
            host.isVisible = false
        }
    }

    private fun addNewAction() {
        // Create a default action wrapped for type selection
        val wrapper = objectWrapper("Action", AdjustValueAction() as WandAction)
        AnnotatedPropertyEditor(listOf(wrapper)).displayInDialog(title = "Add wand action") {
            commitChanges(markUnsaved = false)
            val newAction = wrapper.editingObject
            palette.addAction(newAction)
            palette.selectAction(palette.actions.size - 1)
            savePalette()
        }
    }

    private fun editAction(index: Int) {
        // Wrap the action so user can change its type
        val wrapper = objectWrapper("Action", palette.actions[index])
        AnnotatedPropertyEditor(listOf(wrapper)).displayInDialog(title = "Edit wand action") {
            commitChanges(markUnsaved = false)
            // Replace the action in the list (type may have changed)
            palette.actions[index] = wrapper.editingObject
            palette.events.actionEdited.fire(wrapper.editingObject)
            rebuildList()
            onSelectionChanged()
            savePalette()
        }
    }

    private fun deleteAction(index: Int) {
        // The palette must have at least one action
        if (palette.actions.size <= 1) return
        palette.removeAction(index)
        savePalette()
    }

    /**
     * Persists the palette to system preferences.
     */
    private fun savePalette() {
        persist(palette)
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
                palette.setActions(imported.actions, imported.secondaryIndex)
                savePalette()
            } catch (e: Exception) {
                showMessage("Failed to import palette: ${e.message}", MessageKind.ERROR, title = "Import error")
            }
        }
    }

    private fun revertToDefault() {
        if (showConfirm("Reset palette to default actions? This cannot be undone.", okLabel = "Reset", kind = MessageKind.WARNING)) {
            val defaultPalette = WandPalette.createDefault()
            palette.setActions(defaultPalette.actions, defaultPalette.secondaryIndex)
            savePalette()
        }
    }
}

/**
 * The popup the palette panel is shown in. A popup menu closes as soon as another popup menu opens or a menu item is
 * chosen, which would take the palette away whenever a row's menu was used, so the palette stays up while [heldOpen]
 * is set. Hiding it outright still works.
 */
class WandPalettePopup : JPopupMenu() {

    var heldOpen = false

    override fun menuSelectionChanged(isIncluded: Boolean) {
        if (isIncluded || !heldOpen) super.menuSelectionChanged(isIncluded)
    }
}

/**
 * Icon that draws a wand circle with the given color and letter.
 */
class WandActionIcon(private val color: Color, private val size: Int, private val letter: String = "") : Icon {

    override fun getIconWidth() = size
    override fun getIconHeight() = size

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2.translate(x, y)

        val padding = 2
        val circleSize = size - padding * 2

        // Draw filled circle with action color
        g2.color = color
        g2.fill(Ellipse2D.Double(padding.toDouble(), padding.toDouble(), circleSize.toDouble(), circleSize.toDouble()))

        // Draw border
        g2.color = Theme.foreground
        g2.stroke = BasicStroke(1.5f)
        g2.draw(Ellipse2D.Double(padding.toDouble(), padding.toDouble(), circleSize.toDouble(), circleSize.toDouble()))

        // Draw letter in center
        val displayLetter = letter.firstOrNull()?.toString() ?: ""
        g2.color = Color(0, 0, 0, 200)
        g2.font = Theme.font((size * 0.5).toInt(), Font.BOLD)
        val fm = g2.fontMetrics
        val textX = (size - fm.stringWidth(displayLetter)) / 2
        val textY = (size - fm.height) / 2 + fm.ascent
        g2.drawString(displayLetter, textX, textY)
        g2.dispose()
    }
}

/**
 * Icon component that draws a wand circle with the given color and letter.
 */
class WandIcon(color: Color, size: Int, letter: String = "") : JLabel(WandActionIcon(color, size, letter)) {

    init {
        preferredSize = Dimension(size, size)
        minimumSize = Dimension(size, size)
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

        g2.color = Theme.mutedText
        val startX = x + 2
        val startY = y + 2

        for (i in 0..2) {
            val dotY = startY + i * (dotSize + dotSpacing)
            g2.fill(Ellipse2D.Double(startX.toDouble(), dotY.toDouble(), dotSize.toDouble(), dotSize.toDouble()))
        }
    }
}

/**
 * Two arrows pointing opposite ways, for swapping the palette's two actions.
 */
class SwapIcon : Icon {

    override fun getIconWidth() = 14
    override fun getIconHeight() = 14

    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g2.translate(x, y)
        g2.color = Theme.foreground
        g2.stroke = BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        val arrows = Path2D.Double().apply {
            // Upper arrow, pointing right
            moveTo(2.0, 4.5)
            lineTo(12.0, 4.5)
            moveTo(9.0, 1.5)
            lineTo(12.0, 4.5)
            lineTo(9.0, 7.5)
            // Lower arrow, pointing left
            moveTo(12.0, 9.5)
            lineTo(2.0, 9.5)
            moveTo(5.0, 6.5)
            lineTo(2.0, 9.5)
            lineTo(5.0, 12.5)
        }
        g2.draw(arrows)
        g2.dispose()
    }
}

/**
 * Split button for wand actions.
 * Main button area activates wand mode, triangle dropdown shows palette popup.
 * Displays the current action's letter in a black/white icon for consistency with other toolbar buttons.
 */
class WandPaletteButton(val palette: WandPalette, val networkPanel: NetworkPanel) : JPanel() {

    private var popupMenu: JPopupMenu? = null
    private val mainButton: JToggleButton
    private val dropdownButton: JButton

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false

        // Main button - activates wand mode. A toggle so it shows the flat toolbar "active"
        // highlight while the wand is the current edit mode (driven by editMode below).
        mainButton = JToggleButton().apply {
            putClientProperty("JButton.buttonType", "toolBarButton")
            isFocusable = false
            icon = WandButtonIcon(palette, 18)
            addActionListener {
                networkPanel.wandEventHandler.refreshCursor()
                networkPanel.mouseCursor = MouseEventHandler.MouseCursor.Wand
                isSelected = true
            }
        }

        // Dropdown button - shows palette popup
        dropdownButton = JButton().apply {
            putClientProperty("JButton.buttonType", "toolBarButton")
            isFocusable = false
            toolTipText = "Select wand action"
            icon = DropdownArrowIcon(6, 18)
            addActionListener {
                showPalettePopup()
            }
        }

        add(mainButton)
        add(dropdownButton)

        // Reflect the active tool: highlight while the wand is the current edit mode.
        mainButton.isSelected = networkPanel.mouseCursor == MouseEventHandler.MouseCursor.Wand
        networkPanel.addPropertyChangeListener("editMode") {
            mainButton.isSelected = networkPanel.mouseCursor == MouseEventHandler.MouseCursor.Wand
        }

        // Set initial tooltip after button is created
        updateTooltip()

        // Update icon, tooltip and cursor whenever the selected or secondary action may have changed: a new
        // selection or secondary, an edited, added or removed action, or a loaded palette
        palette.events.apply {
            // The palette outlives the network panel, so these end with the panel's view
            listOf(
                selectionChanged.on { refresh() },
                secondaryChanged.on { refresh() },
                actionEdited.on { refresh() },
                actionAdded.on { refresh() },
                actionRemoved.on { refresh() },
                paletteChanged.on { refresh() }
            ).forEach { it.cancelWith(networkPanel.viewScope) }
        }

        // Set initial cursor
        networkPanel.wandEventHandler.refreshCursor()
    }

    private fun refresh() {
        networkPanel.wandEventHandler.refreshCursor()
        updateTooltip()
        // The icon is wider when there is a secondary action to show
        mainButton.revalidate()
        mainButton.repaint()
    }

    private fun updateTooltip() {
        val actionDesc = palette.selectedAction?.description ?: "No action selected"
        val secondaryDesc = palette.secondaryAction?.let { ". With Shift held: ${it.description} (x swaps the two)" } ?: ""
        mainButton.toolTipText = "Wand tool (d) - $actionDesc$secondaryDesc"
    }

    private fun showPalettePopup() {
        popupMenu?.isVisible = false

        popupMenu = WandPalettePopup().apply {
            add(WandPalettePanel(palette) {
                // On selection changed, activate wand mode and update
                networkPanel.wandEventHandler.refreshCursor()
                networkPanel.mouseCursor = MouseEventHandler.MouseCursor.Wand
                mainButton.repaint()
            })
        }
        popupMenu?.show(mainButton, 0, mainButton.height)
    }

    /**
     * Icon for the wand palette button that shows the current action's letter and, smaller and to its lower right,
     * the letter of the secondary action when the palette has one.
     * Uses black/white colors for consistency with other toolbar icons.
     */
    private class WandButtonIcon(private val palette: WandPalette, private val size: Int) : Icon {
        private val secondarySize = size * 0.62
        private val secondaryOverhang = (size * 0.56).toInt()

        override fun getIconWidth() = if (palette.secondaryAction != null) size + secondaryOverhang else size
        override fun getIconHeight() = size

        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val padding = 1
            val circleSize = size - padding * 2
            val circle = Ellipse2D.Double((x + padding).toDouble(), (y + padding).toDouble(), circleSize.toDouble(), circleSize.toDouble())

            palette.secondaryAction?.let { secondary ->
                // Drawn only where it is clear of the main circle, with a gap between the two
                val gap = 1.5
                val hidden = Ellipse2D.Double(circle.x - gap, circle.y - gap, circle.width + gap * 2, circle.height + gap * 2)
                val oldClip = g2.clip
                g2.clip(Area(Rectangle(x, y, iconWidth, iconHeight)).apply { subtract(Area(hidden)) })
                val secondaryCircle = Ellipse2D.Double(
                    x + iconWidth - secondarySize - 0.5,
                    y + iconHeight - secondarySize - 0.5,
                    secondarySize,
                    secondarySize
                )
                g2.color = blend(Theme.foreground, c?.background ?: Theme.cardBg, 0.7)
                g2.stroke = BasicStroke(1.1f)
                g2.draw(secondaryCircle)
                drawLetter(g2, secondary.letter, secondaryCircle, (secondarySize * 0.62).toFloat())
                g2.clip = oldClip
            }

            // Draw circle outline, tracking the theme foreground like other toolbar icons
            g2.color = Theme.foreground
            g2.stroke = BasicStroke(1.5f)
            g2.draw(circle)

            // Draw the letter in the center
            drawLetter(g2, palette.selectedAction?.letter ?: "", circle, size * 0.6f)
        }

        private fun drawLetter(g2: Graphics2D, letter: String, circle: Ellipse2D, fontSize: Float) {
            val displayLetter = letter.firstOrNull()?.toString() ?: ""
            g2.font = Theme.font(1, Font.BOLD).deriveFont(fontSize)
            val fm = g2.fontMetrics
            val textX = (circle.centerX - fm.stringWidth(displayLetter) / 2.0).toFloat()
            val textY = (circle.centerY - fm.height / 2.0 + fm.ascent).toFloat()
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

            g2.color = Theme.foreground
            g2.fill(triangle)
        }
    }
}
