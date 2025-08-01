package org.simbrain.world.imageworld.gui

import org.simbrain.util.ResourceManager
import org.simbrain.util.swingDispatcher
import org.simbrain.world.imageworld.ImageWorldDesktopComponent
import org.simbrain.world.imageworld.dialogs.ImageProcessingPipelineDialog
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import org.simbrain.world.imageworld.transformations.ImagePipelineCollection
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

    private val pipelineComboBox = JComboBox<ImageProcessingPipeline>().apply {
        toolTipText = "Which pipeline to view"
        maximumSize = Dimension(200, 100)
        addActionListener { evt ->
            val selectedPipeline = selectedItem as? ImageProcessingPipeline
            if (selectedPipeline != null) {
                imagePipelineCollection.setCurrentPipeline(selectedPipeline)
            }
        }
    }

    private val addPipelineButton = JButton(ResourceManager.getSmallIcon("menu_icons/plus.png")).apply {
        toolTipText = "Add Pipeline"
        addActionListener {
            val pipelineName = JOptionPane.showInputDialog(
                imageWorldDesktopComponent,
                "Enter name for new pipeline:",
                "Create Pipeline",
                JOptionPane.PLAIN_MESSAGE
            )
            if (pipelineName != null && pipelineName.isNotBlank()) {
                val newPipeline = ImageProcessingPipeline(pipelineName, imagePipelineCollection.imageSource)
                imagePipelineCollection.addPipeline(newPipeline)
                updateComboBox()
                pipelineComboBox.selectedItem = newPipeline
                imagePipelineCollection.setCurrentPipeline(newPipeline)
            }
        }
    }
    private val editPipelineButton = JButton(ResourceManager.getSmallIcon("menu_icons/Tools.png")).apply {
        toolTipText = "Edit Pipeline"
        addActionListener {
            val currentPipeline = imagePipelineCollection.currentPipeline
            val pipelineDialog = ImageProcessingPipelineDialog(imageWorldDesktopComponent, currentPipeline)
            pipelineDialog.setLocationRelativeTo(imageWorldDesktopComponent)
            pipelineDialog.isVisible = true
        }
    }
    private val deletePipelineButton = JButton(ResourceManager.getSmallIcon("menu_icons/minus.png")).apply {
            toolTipText = "Delete Pipeline"
            addActionListener {
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
            }
        }

    val toolbar = JToolBar().apply {
        add(JLabel("Pipelines:"))
        add(pipelineComboBox)
        add(addPipelineButton)
        add(deletePipelineButton)
        add(editPipelineButton)
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
        // Safety check: buttons might not be initialized yet during construction
        if (editPipelineButton == null || deletePipelineButton == null) {
            return
        }
        
        val currentPipeline = imagePipelineCollection.currentPipeline
        val isDefaultUnfiltered = imagePipelineCollection.isDefaultUnfilteredPipeline(currentPipeline)
        
        // Disable edit and delete buttons for the default unfiltered pipeline
        editPipelineButton?.isEnabled = !isDefaultUnfiltered
        deletePipelineButton?.isEnabled = !isDefaultUnfiltered
        
        // Update tooltips to explain why buttons are disabled
        if (isDefaultUnfiltered) {
            editPipelineButton?.toolTipText = "Cannot edit the default unfiltered pipeline"
            deletePipelineButton?.toolTipText = "Cannot delete the default unfiltered pipeline"
        } else {
            editPipelineButton?.toolTipText = "Edit Pipeline"
            deletePipelineButton?.toolTipText = "Delete Pipeline"
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