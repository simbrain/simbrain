package org.simbrain.world.imageworld.gui

import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.swingDispatcher
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.world.imageworld.ImageSource
import org.simbrain.world.imageworld.ImageWorldDesktopComponent
import org.simbrain.world.imageworld.dialogs.CreateTransformationDialog
import org.simbrain.world.imageworld.dialogs.FilterSelectionDialog
import org.simbrain.world.imageworld.filters.Filter
import org.simbrain.world.imageworld.filters.FilterManager
import org.simbrain.world.imageworld.transformations.TransformationCollection
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

/**
 * Provides a toolbar for adding, deleting, and setting a current transformation
 * in a TransformationCollection.
 * Renamed from FilterCollectionGui to better distinguish from the new multi-filter system.
 */
class TransformationCollectionGui(
    private val parent: ImageWorldDesktopComponent, 
    private val transformationCollection: TransformationCollection,
    private val filterManager: FilterManager
) {

    private val transformationComboBox = JComboBox<Filter>()

    init {
        transformationCollection.events.transformationAdded.on(swingDispatcher) { updateComboBox() }
        transformationCollection.events.transformationRemoved.on(swingDispatcher) { updateComboBox() }
        transformationCollection.events.transformationChanged.on(swingDispatcher) { newTransformation: Filter, _: Filter ->
            setComboBoxSelection(newTransformation)
        }
        transformationCollection.events.transformationSelectionChanged.on(swingDispatcher) { transformation: Filter ->
            transformationCollection.setCurrentTransformation(transformation)
        }
    }

    fun getToolBar(): JToolBar {
        val transformationToolbar = JToolBar()

        transformationToolbar.add(JLabel("Transformations:"))
        transformationToolbar.add(transformationComboBox)
        transformationComboBox.toolTipText = "Which transformation to view"
        updateComboBox()
        transformationComboBox.selectedItem = transformationCollection.currentTransformation
        transformationComboBox.maximumSize = Dimension(200, 100)
        transformationComboBox.addActionListener { evt ->
            val selectedTransformation = transformationComboBox.selectedItem as? Filter
            if (selectedTransformation != null) {
                transformationCollection.setCurrentTransformation(selectedTransformation)
                transformationCollection.events.transformationSelectionChanged.fire(selectedTransformation)
            }
        }

        // Add Transformation
        val addTransformation = JButton(ResourceManager.getSmallIcon("menu_icons/plus.png"))
        addTransformation.toolTipText = "Add Transformation"
        addTransformation.addActionListener {
            val dialog = CreateTransformationDialog(transformationCollection)
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
        transformationToolbar.add(addTransformation)

        // Edit Transformation
        val editTransformation = JButton(ResourceManager.getSmallIcon("menu_icons/Tools.png"))
        editTransformation.toolTipText = "Edit Transformation"
        editTransformation.addActionListener {

            // Create a dialog to edit the transformation
            val transformationEditorDialog = StandardDialog()
            val dialogPanel = JPanel()
            dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)
            transformationEditorDialog.contentPane = dialogPanel

            // Edit the top level transformation, basically just a name
            val transformation = transformationCollection.currentTransformation!!
            val topLevelTransformationEditor = AnnotatedPropertyEditor(transformation)
            dialogPanel.add(topLevelTransformationEditor)
            transformationEditorDialog.addCommitTask { topLevelTransformationEditor.commitChanges() }

            // If the transformation is a filtered image source, edit it too
            val imageSource = transformation.source
            transformationEditorDialog.title = "Edit ${transformation.name}"
            val transformationEditor = AnnotatedPropertyEditor(imageSource as EditableObject)
            dialogPanel.add(transformationEditor)
            transformationEditorDialog.addCommitTask {
                transformationEditor.commitChanges()
                transformation.applyFilter()
                transformationComboBox.updateUI()
                parent.repaint()
            }

            // Delete transformation
            val deleteTransformation = JButton("Delete Transformation")
            deleteTransformation.toolTipText = "Delete Transformation"
            deleteTransformation.alignmentX = Component.CENTER_ALIGNMENT
            deleteTransformation.addActionListener { e ->
                if (transformation.id.equals("Unfiltered", ignoreCase = true)) {
                    JOptionPane.showMessageDialog(transformationEditorDialog, "Can't remove unfiltered option")
                    return@addActionListener
                }
                val dialogResult = JOptionPane.showConfirmDialog(
                    transformationEditorDialog,
                    "Are you sure you want to delete transformation \"${transformation.name}\"?", "Warning",
                    JOptionPane.YES_NO_OPTION
                )
                if (dialogResult == JOptionPane.YES_OPTION) {
                    transformationCollection.removeTransformation(transformation)
                    updateComboBox()
                }
                transformationEditorDialog.isVisible = false
            }
            dialogPanel.add(deleteTransformation)

            transformationEditorDialog.pack()
            transformationEditorDialog.setLocationRelativeTo(null)
            transformationEditorDialog.isVisible = true
        }
        transformationToolbar.add(editTransformation)

        transformationToolbar.addSeparator()

        // Filters button
        val filtersButton = JButton(ResourceManager.getSmallIcon("menu_icons/Tools.png"))
        filtersButton.toolTipText = "Edit image processing filters"
        filtersButton.addActionListener {
            val filterDialog = FilterSelectionDialog(parent, filterManager)
            filterDialog.setLocationRelativeTo(parent)
            filterDialog.isVisible = true
        }
        transformationToolbar.add(filtersButton)

        return transformationToolbar
    }

    private fun setComboBoxSelection(transformation: Filter?) {
        transformationComboBox.selectedItem = transformation
    }

    /**
     * Reset the combo box for the transformation panels.
     */
    private fun updateComboBox() {
        transformationComboBox.removeAllItems()
        val selectedTransformation = transformationCollection.currentTransformation
        for (transformation in transformationCollection.transformations) {
            transformationComboBox.addItem(transformation)
            if (transformation == selectedTransformation) {
                transformationComboBox.selectedItem = transformation
            }
        }
    }

    fun getTransformationComboBox() = transformationComboBox
}