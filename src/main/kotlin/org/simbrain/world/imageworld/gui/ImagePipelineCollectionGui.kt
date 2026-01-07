package org.simbrain.world.imageworld.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.simbrain.util.*
import org.simbrain.world.imageworld.ImageWorldDesktopComponent
import org.simbrain.world.imageworld.dialogs.ImageProcessingPipelineDialog
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import org.simbrain.world.imageworld.transformations.ImagePipelineCollection
import java.awt.Color
import java.awt.Dimension
import javax.swing.*

/**
 * Provides a toolbar for adding, deleting, and setting a current pipeline
 * in an ImagePipelineCollection.
 */
class ImagePipelineCollectionGui(
    private val imageWorldDesktopComponent: ImageWorldDesktopComponent,
    private val imagePipelineCollection: ImagePipelineCollection
) {
    private val imageWorld = imageWorldDesktopComponent.workspaceComponent.world

    private val pipelineComboBox = JComboBox<ImageProcessingPipeline>().apply {
        toolTipText = "Which pipeline to view"
        maximumSize = Dimension(200, 100)
        addActionListener { evt ->
            imageWorldDesktopComponent.workspaceComponent.world.launch(Dispatchers.Default) {
                val selectedPipeline = selectedItem as? ImageProcessingPipeline
                if (selectedPipeline != null) {
                    imagePipelineCollection.setCurrentPipeline(selectedPipeline)
                }
            }
        }
    }

    private val addPipelineButton = JButton(imageWorldDesktopComponent.createAction(
        iconPath = "menu_icons/plus.png",
    ) {
        val pipelineName = JOptionPane.showInputDialog(
            imageWorldDesktopComponent,
            "Enter name for new pipeline:",
            "Create Pipeline",
            JOptionPane.PLAIN_MESSAGE
        )
        if (pipelineName != null && pipelineName.isNotBlank()) {
            val newPipeline = ImageProcessingPipeline(pipelineName, imagePipelineCollection.imageSource)
            imagePipelineCollection.addPipeline(newPipeline)
            pipelineComboBox.selectedItem = newPipeline
            imagePipelineCollection.setCurrentPipeline(newPipeline)
        }
    }).apply { toolTipText = "Add pipeline" }

    private val editPipelineButton = JButton(ResourceManager.getSmallIcon("menu_icons/Tools.png")).apply {
        toolTipText = "Edit pipeline"
        addActionListener {
            val currentPipeline = imagePipelineCollection.currentPipeline
            val pipelineDialog = ImageProcessingPipelineDialog(imageWorldDesktopComponent, currentPipeline)
            pipelineDialog.setLocationRelativeTo(imageWorldDesktopComponent)
            pipelineDialog.isVisible = true
        }
    }
    private val deletePipelineButton = JButton(imageWorldDesktopComponent.createAction(iconPath = "menu_icons/minus.png") {
        val currentPipeline = imagePipelineCollection.currentPipeline
        val dialogResult = JOptionPane.showConfirmDialog(
            imageWorldDesktopComponent,
            "Are you sure you want to delete pipeline \"${currentPipeline.name}\"?",
            "Warning",
            JOptionPane.YES_NO_OPTION
        )
        if (dialogResult == JOptionPane.YES_OPTION) {
            imagePipelineCollection.removePipeline(currentPipeline)
            updateComboBox()
            // Set to first pipeline after deletion
            if (imagePipelineCollection.pipelines.isNotEmpty()) {
                imagePipelineCollection.setCurrentPipeline(imagePipelineCollection.pipelines[0])
            }
        }
    }).apply { toolTipText = "Delete pipeline" }

    // Editing tools
    private val fillCanvasAction = imageWorldDesktopComponent.createAction(
        "Fill",
        description = "Fill canvas using current color",
        iconPath = "menu_icons/fill.png"
    ) {
        val confirm = showWarningConfirmDialog("Are you sure you want to fill the canvas?")
        if (confirm == JOptionPane.YES_OPTION) {
            imageWorld.imageAlbum.currentImage.fill(imageWorld.penColor)
            imageWorld.imageAlbum.fireImageUpdate()
        }
    }

    private val clearCanvasAction = imageWorldDesktopComponent.createAction(
        "Clear",
        description = "Clear canvas (with black pixels)",
        iconPath = "menu_icons/Eraser.png"
    ) {
        val confirm = showWarningConfirmDialog("Are you sure you want to clear the canvas?")
        if (confirm == JOptionPane.YES_OPTION) {
            imageWorld.imageAlbum.currentImage.fill(Color.black)
            imageWorld.imageAlbum.fireImageUpdate()
        }
    }

    private val colorPickerButton = ColorPickerButton(imageWorld)
    private val brushSettingsButton = BrushSettingsButton(imageWorld)

    val toolbar = JToolBar().apply {
        add(JLabel("Pipelines:"))
        add(pipelineComboBox)
        add(addPipelineButton)
        add(deletePipelineButton)
        add(editPipelineButton)
        addSeparator()
        add(fillCanvasAction)
        add(clearCanvasAction)
        addSeparator()
        add(colorPickerButton)
        add(brushSettingsButton)
        updateButtonStates()
    }

    init {
        imagePipelineCollection.events.pipelineAdded.on(swingDispatcher) { updateComboBox() }
        imagePipelineCollection.events.pipelineRemoved.on(swingDispatcher) { updateComboBox() }
        imagePipelineCollection.events.pipelineChanged.on(swingDispatcher) { newPipeline: ImageProcessingPipeline, _: ImageProcessingPipeline ->
            setComboBoxSelection(newPipeline)
            updateButtonStates()
        }
        updateComboBox()
    }

    private fun updateButtonStates() {

        val currentPipeline = imagePipelineCollection.currentPipeline
        val isDefaultUnfiltered = imagePipelineCollection.isDefaultUnfilteredPipeline(currentPipeline)
        
        // Disable edit and delete buttons for the default unfiltered pipeline
        editPipelineButton.isEnabled = !isDefaultUnfiltered
        deletePipelineButton.isEnabled = !isDefaultUnfiltered
        
        // Update tooltips to explain why buttons are disabled
        if (isDefaultUnfiltered) {
            editPipelineButton.toolTipText = "Cannot edit the default unfiltered pipeline"
            deletePipelineButton.toolTipText = "Cannot delete the default unfiltered pipeline"
        } else {
            editPipelineButton.toolTipText = "Edit pipeline"
            deletePipelineButton.toolTipText = "Delete pipeline"
        }
    }

    private fun setComboBoxSelection(pipeline: ImageProcessingPipeline?) {
        pipelineComboBox.selectedItem = pipeline
    }

    /**
     * Reset the combo box for the pipeline panels.
     */
    private fun updateComboBox() {
        pipelineComboBox.removeAllItems()
        val selectedPipeline = imagePipelineCollection.currentPipeline
        for (pipeline in imagePipelineCollection.pipelines) {
            pipelineComboBox.addItem(pipeline)
            if (pipeline == selectedPipeline) {
                pipelineComboBox.selectedItem = pipeline
            }
        }
        updateButtonStates()
    }
}