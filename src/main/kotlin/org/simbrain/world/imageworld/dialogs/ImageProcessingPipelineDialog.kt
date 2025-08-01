package org.simbrain.world.imageworld.dialogs

import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.world.imageworld.ImageWorldDesktopComponent
import org.simbrain.world.imageworld.filters.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * Dialog for managing image processing pipelines.
 * Allows users to add, remove, reorder, and configure multiple operations in a pipeline.
 */
class ImageProcessingPipelineDialog(
    private val parent: ImageWorldDesktopComponent,
    private val pipeline: ImageProcessingPipeline
) : StandardDialog() {

    private val activeOperationsListModel = DefaultListModel<ImageOperation>()
    private val activeOperationsList = JList(activeOperationsListModel)
    private val availableOperationsListModel = DefaultListModel<Class<out ImageOperation>>()
    private val availableOperationsList = JList(availableOperationsListModel)

    init {
        title = "Image Processing Pipeline - ${pipeline.name}"
        setContentPane(createPipelinePanel())
        isModal = false
        pack()

        // Listen for pipeline events
        pipeline.events.operationAdded.on { operation: ImageOperation ->
            SwingUtilities.invokeLater {
                activeOperationsListModel.addElement(operation)
                updatePipelineDisplay()
            }
        }

        pipeline.events.operationRemoved.on { operation: ImageOperation ->
            SwingUtilities.invokeLater {
                activeOperationsListModel.removeElement(operation)
                updatePipelineDisplay()
            }
        }

        pipeline.events.operationOrderChanged.on {
            SwingUtilities.invokeLater {
                refreshActiveOperationsList()
                updatePipelineDisplay()
            }
        }

        // Initialize lists
        refreshActiveOperationsList()
        refreshAvailableOperationsList()
    }

    private fun createPipelinePanel(): JPanel {
        val mainPanel = JPanel(BorderLayout())

        // Pipeline name editor at the top
        val namePanel = createNamePanel()
        mainPanel.add(namePanel, BorderLayout.NORTH)

        // Create split pane with available operations on left, active operations on right
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)

        // Available operations panel
        val availablePanel = createAvailableOperationsPanel()
        splitPane.leftComponent = availablePanel

        // Active operations panel  
        val activePanel = createActiveOperationsPanel()
        splitPane.rightComponent = activePanel

        splitPane.resizeWeight = 0.4
        mainPanel.add(splitPane, BorderLayout.CENTER)

        // Button panel
        val buttonPanel = createButtonPanel()
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)

        return mainPanel
    }

    private fun createNamePanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.border = BorderFactory.createTitledBorder("Pipeline Settings")

        panel.add(JLabel("Name:"))
        
        val nameField = JTextField(pipeline.name, 20)
        
        // Check if this is the default unfiltered pipeline
        val isDefaultUnfiltered = parent.workspaceComponent.world.imagePipelineCollection.isDefaultUnfilteredPipeline(pipeline)
        
        if (isDefaultUnfiltered) {
            nameField.isEnabled = false
            nameField.toolTipText = "Cannot rename the default unfiltered pipeline"
        } else {
            nameField.addActionListener { 
                updatePipelineName(nameField.text)
            }
            
            // Also update on focus lost
            nameField.addFocusListener(object : java.awt.event.FocusAdapter() {
                override fun focusLost(e: java.awt.event.FocusEvent?) {
                    updatePipelineName(nameField.text)
                }
            })
        }
        
        panel.add(nameField)

        return panel
    }

    private fun updatePipelineName(newName: String) {
        if (newName.isNotBlank() && newName != pipeline.name) {
            pipeline.name = newName
            title = "Image Processing Pipeline - $newName"
            // Update parent UI to reflect name change (e.g., in dropdown lists)
            parent.repaint()
        }
    }

    private fun createAvailableOperationsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Available Operations")

        // Configure available operations list
        availableOperationsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        availableOperationsList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is Class<*>) {
                    text = value.simpleName?.replace("Operation", "") ?: "Unknown"
                }
                return this
            }
        }

        // Double-click to add operation
        availableOperationsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    addSelectedAvailableOperation()
                }
            }
        })

        val scrollPane = JScrollPane(availableOperationsList)
        scrollPane.preferredSize = Dimension(150, 200)
        panel.add(scrollPane, BorderLayout.CENTER)

        // Add button
        val addButton = JButton("Add >>")
        addButton.addActionListener { addSelectedAvailableOperation() }
        panel.add(addButton, BorderLayout.SOUTH)

        return panel
    }

    private fun createActiveOperationsPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = BorderFactory.createTitledBorder("Pipeline Operations")

        // Configure active operations list
        activeOperationsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        activeOperationsList.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is ImageOperation) {
                    val prefix = "${activeOperationsListModel.indexOf(value) + 1}. "
                    text = "$prefix${value.name} ${if (value.enabled) "(Enabled)" else "(Disabled)"}"
                    foreground = if (value.enabled) Color.BLACK else Color.GRAY
                }
                return this
            }
        }

        // Double-click to edit operation
        activeOperationsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    editSelectedActiveOperation()
                }
            }
        })

        val scrollPane = JScrollPane(activeOperationsList)
        scrollPane.preferredSize = Dimension(200, 200)
        panel.add(scrollPane, BorderLayout.CENTER)

        // Control buttons
        val controlPanel = createActiveOperationControlPanel()
        panel.add(controlPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun createActiveOperationControlPanel(): JPanel {
        val panel = JPanel(FlowLayout())

        val editButton = JButton("Edit")
        editButton.addActionListener { editSelectedActiveOperation() }
        panel.add(editButton)

        val toggleButton = JButton("Enable/Disable")
        toggleButton.addActionListener { toggleSelectedOperation() }
        panel.add(toggleButton)

        val upButton = JButton("▲")
        upButton.toolTipText = "Move Up"
        upButton.addActionListener { moveSelectedOperationUp() }
        panel.add(upButton)

        val downButton = JButton("▼")
        downButton.toolTipText = "Move Down"
        downButton.addActionListener { moveSelectedOperationDown() }
        panel.add(downButton)

        val removeButton = JButton("Remove")
        removeButton.addActionListener { removeSelectedActiveOperation() }
        panel.add(removeButton)

        return panel
    }

    private fun createButtonPanel(): JPanel {
        val panel = JPanel(FlowLayout())

        val clearAllButton = JButton("Clear All")
        clearAllButton.addActionListener {
            val result = JOptionPane.showConfirmDialog(
                this,
                "Remove all operations from the pipeline?",
                "Clear All Operations",
                JOptionPane.YES_NO_OPTION
            )
            if (result == JOptionPane.YES_OPTION) {
                pipeline.clearOperations()
            }
        }
        panel.add(clearAllButton)

        val closeButton = JButton("Close")
        closeButton.addActionListener { isVisible = false }
        panel.add(closeButton)

        return panel
    }

    private fun addSelectedAvailableOperation() {
        val selectedType = availableOperationsList.selectedValue
        if (selectedType != null) {
            createAndAddOperation(selectedType)
        }
    }

    private fun createAndAddOperation(operationType: Class<out ImageOperation>) {
        try {
            // Create a new instance of the operation
            val operation = operationType.getDeclaredConstructor().newInstance()
            
            // If the operation has configurable parameters, show config dialog
            if (hasConfigurableParameters(operation)) {
                val editor = AnnotatedPropertyEditor(operation)
                val configDialog = StandardDialog()
                configDialog.setContentPane(editor)
                configDialog.title = "Configure ${operation.name}"
                configDialog.addCommitTask {
                    editor.commitChanges()
                    pipeline.addOperation(operation)
                }
                configDialog.pack()
                configDialog.setLocationRelativeTo(this)
                configDialog.isVisible = true
            } else {
                // No configuration needed, add directly
                pipeline.addOperation(operation)
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                this,
                "Failed to create operation: ${e.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun hasConfigurableParameters(operation: ImageOperation): Boolean {
        // Check if the operation has configurable parameters
        return when (operation) {
            is GrayscaleOperation -> false  // No configurable parameters
            else -> true  // All other operations have configurable parameters
        }
    }

    private fun editSelectedActiveOperation() {
        val selectedOperation = activeOperationsList.selectedValue
        if (selectedOperation != null) {
            val editor = AnnotatedPropertyEditor(selectedOperation)
            val dialog = StandardDialog()
            dialog.setContentPane(editor)
            dialog.title = "Edit ${selectedOperation.name}"
            dialog.addCommitTask {
                editor.commitChanges()
                activeOperationsList.repaint()
                updatePipelineDisplay()
            }
            dialog.pack()
            dialog.setLocationRelativeTo(this)
            dialog.isVisible = true
        }
    }

    private fun toggleSelectedOperation() {
        val selectedOperation = activeOperationsList.selectedValue
        if (selectedOperation != null) {
            selectedOperation.enabled = !selectedOperation.enabled
            activeOperationsList.repaint()
            updatePipelineDisplay()
        }
    }

    private fun moveSelectedOperationUp() {
        val selectedOperation = activeOperationsList.selectedValue
        if (selectedOperation != null) {
            pipeline.moveOperationUp(selectedOperation)
        }
    }

    private fun moveSelectedOperationDown() {
        val selectedOperation = activeOperationsList.selectedValue
        if (selectedOperation != null) {
            pipeline.moveOperationDown(selectedOperation)
        }
    }

    private fun removeSelectedActiveOperation() {
        val selectedOperation = activeOperationsList.selectedValue
        if (selectedOperation != null) {
            pipeline.removeOperation(selectedOperation)
        }
    }

    private fun refreshActiveOperationsList() {
        activeOperationsListModel.clear()
        pipeline.getOperations().forEach { operation ->
            activeOperationsListModel.addElement(operation)
        }
    }

    private fun refreshAvailableOperationsList() {
        availableOperationsListModel.clear()
        // Add available operation types
        getAvailableOperationTypes().forEach { operationType ->
            availableOperationsListModel.addElement(operationType)
        }
    }

    private fun getAvailableOperationTypes(): List<Class<out ImageOperation>> {
        return listOf(
            ResizeOperation::class.java,
            GrayscaleOperation::class.java,
            ThresholdOperation::class.java,
            EdgeDetectionFilter::class.java,
            GaborFilter::class.java
        )
    }

    private fun updatePipelineDisplay() {
        // Trigger a repaint of the image world to show pipeline effects
        pipeline.applyPipeline()
        parent.repaint()
    }
} 