package org.simbrain.world.imageworld.dialogs

import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.imageworld.ImageWorldDesktopComponent
import org.simbrain.world.imageworld.filters.FilterManager
import org.simbrain.world.imageworld.filters.ImageFilter
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * Dialog for managing image processing filters.
 * Allows users to add, remove, reorder, and configure multiple filters.
 */
class FilterSelectionDialog(
    private val parent: ImageWorldDesktopComponent,
    private val filterManager: FilterManager
) : StandardDialog() {

    private val activeFiltersListModel = DefaultListModel<ImageFilter>()
    private val activeFiltersList = JList(activeFiltersListModel)
    private val availableFiltersListModel = DefaultListModel<Class<out ImageFilter>>()
    private val availableFiltersList = JList(availableFiltersListModel)

    init {
        title = "Image Filters"
        setContentPane(createFilterPanel())
        isModal = false
        pack()

        // Listen for filter manager events
        filterManager.events.filterAdded.on { filter: ImageFilter ->
            SwingUtilities.invokeLater {
                activeFiltersListModel.addElement(filter)
                updateFilterDisplay()
            }
        }

        filterManager.events.filterRemoved.on { filter: ImageFilter ->
            SwingUtilities.invokeLater {
                activeFiltersListModel.removeElement(filter)
                updateFilterDisplay()
            }
        }

        filterManager.events.filterOrderChanged.on {
            SwingUtilities.invokeLater {
                refreshActiveFiltersList()
                updateFilterDisplay()
            }
        }

        // Initialize lists
        refreshActiveFiltersList()
        refreshAvailableFiltersList()
    }

    private fun createFilterPanel(): JPanel {
        val mainPanel = JPanel(BorderLayout())

        // Create split pane with available filters on left, active filters on right
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)

        // Available filters panel
        val availablePanel = createAvailableFiltersPanel()
        splitPane.leftComponent = availablePanel

        // Active filters panel  
        val activePanel = createActiveFiltersPanel()
        splitPane.rightComponent = activePanel

        splitPane.resizeWeight = 0.4
        mainPanel.add(splitPane, BorderLayout.CENTER)

        // Button panel
        val buttonPanel = createButtonPanel()
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)

        return mainPanel
    }

    private fun createAvailableFiltersPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Available Filters")

        // Configure available filters list
        availableFiltersList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        availableFiltersList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is Class<*>) {
                    text = value.simpleName?.replace("Filter", "") ?: "Unknown"
                }
                return this
            }
        }

        // Double-click to add filter
        availableFiltersList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    addSelectedAvailableFilter()
                }
            }
        })

        val scrollPane = JScrollPane(availableFiltersList)
        scrollPane.preferredSize = Dimension(150, 200)
        panel.add(scrollPane, BorderLayout.CENTER)

        // Add button
        val addButton = JButton("Add >>")
        addButton.addActionListener { addSelectedAvailableFilter() }
        panel.add(addButton, BorderLayout.SOUTH)

        return panel
    }

    private fun createActiveFiltersPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Active Filters")

        // Configure active filters list
        activeFiltersList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        activeFiltersList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is ImageFilter) {
                    text = "${value.name} ${if (value.enabled) "(Enabled)" else "(Disabled)"}"
                    foreground = if (value.enabled) Color.BLACK else Color.GRAY
                }
                return this
            }
        }

        // Double-click to edit filter
        activeFiltersList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    editSelectedActiveFilter()
                }
            }
        })

        val scrollPane = JScrollPane(activeFiltersList)
        scrollPane.preferredSize = Dimension(200, 200)
        panel.add(scrollPane, BorderLayout.CENTER)

        // Control buttons
        val controlPanel = createActiveFilterControlPanel()
        panel.add(controlPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun createActiveFilterControlPanel(): JPanel {
        val panel = JPanel(FlowLayout())

        val editButton = JButton("Edit")
        editButton.addActionListener { editSelectedActiveFilter() }
        panel.add(editButton)

        val toggleButton = JButton("Enable/Disable")
        toggleButton.addActionListener { toggleSelectedFilter() }
        panel.add(toggleButton)

        val upButton = JButton("▲")
        upButton.toolTipText = "Move Up"
        upButton.addActionListener { moveSelectedFilterUp() }
        panel.add(upButton)

        val downButton = JButton("▼")
        downButton.toolTipText = "Move Down"
        downButton.addActionListener { moveSelectedFilterDown() }
        panel.add(downButton)

        val removeButton = JButton("Remove")
        removeButton.addActionListener { removeSelectedActiveFilter() }
        panel.add(removeButton)

        return panel
    }

    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout())

        val clearAllButton = JButton("Clear All")
        clearAllButton.addActionListener {
            val result = JOptionPane.showConfirmDialog(
                this,
                "Remove all active filters?",
                "Clear All Filters",
                JOptionPane.YES_NO_OPTION
            )
            if (result == JOptionPane.YES_OPTION) {
                filterManager.clearAllFilters()
            }
        }
        panel.add(clearAllButton)

        val closeButton = JButton("Close")
        closeButton.addActionListener { isVisible = false }
        panel.add(closeButton)

        return panel
    }

    private fun addSelectedAvailableFilter() {
        val selectedType = availableFiltersList.selectedValue
        if (selectedType != null) {
            filterManager.createAndAddFilter(selectedType)
        }
    }

    private fun editSelectedActiveFilter() {
        val selectedFilter = activeFiltersList.selectedValue
        if (selectedFilter != null) {
            val editor = AnnotatedPropertyEditor(selectedFilter)
            val dialog = StandardDialog()
            dialog.setContentPane(editor)
            dialog.title = "Edit ${selectedFilter.name}"
            dialog.addCommitTask {
                editor.commitChanges()
                activeFiltersList.repaint()
                updateFilterDisplay()
            }
            dialog.pack()
            dialog.setLocationRelativeTo(this)
            dialog.isVisible = true
        }
    }

    private fun toggleSelectedFilter() {
        val selectedFilter = activeFiltersList.selectedValue
        if (selectedFilter != null) {
            selectedFilter.enabled = !selectedFilter.enabled
            activeFiltersList.repaint()
            updateFilterDisplay()
        }
    }

    private fun moveSelectedFilterUp() {
        val selectedFilter = activeFiltersList.selectedValue
        if (selectedFilter != null) {
            filterManager.moveFilterUp(selectedFilter)
        }
    }

    private fun moveSelectedFilterDown() {
        val selectedFilter = activeFiltersList.selectedValue
        if (selectedFilter != null) {
            filterManager.moveFilterDown(selectedFilter)
        }
    }

    private fun removeSelectedActiveFilter() {
        val selectedFilter = activeFiltersList.selectedValue
        if (selectedFilter != null) {
            filterManager.removeFilter(selectedFilter)
        }
    }

    private fun refreshActiveFiltersList() {
        activeFiltersListModel.clear()
        filterManager.getActiveFilters().forEach { filter ->
            activeFiltersListModel.addElement(filter)
        }
    }

    private fun refreshAvailableFiltersList() {
        availableFiltersListModel.clear()
        filterManager.getAvailableFilterTypes().forEach { filterType ->
            availableFiltersListModel.addElement(filterType)
        }
    }

    private fun updateFilterDisplay() {
        // Trigger a repaint of the image world to show filter effects
        parent.repaint()
    }
}