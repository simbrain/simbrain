package org.simbrain.network.gui.dialogs

import org.simbrain.network.core.Neuron
import org.simbrain.network.core.centerLocation
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.layouts.Layout.LayoutEditor
import org.simbrain.util.StandardDialog
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor

/**
 * Allows the user to define the layout of a network.
 */
class LayoutDialog(
    private val networkPanel: NetworkPanel
) : StandardDialog() {

    private val layoutEditor = LayoutEditor()

    private val mainPanel = AnnotatedPropertyEditor(layoutEditor)

    init {
        contentPane = mainPanel
    }

    override fun closeDialogOk() {
        super.closeDialogOk()
        commitChanges()
        val neurons = networkPanel.selectionManager.filterSelectedModels(Neuron::class.java)
        val locations = neurons.map { it.location } // For undo/redo
        layoutEditor.layout.setInitialLocation(neurons.centerLocation)
        layoutEditor.layout.layoutNeurons(neurons)
        networkPanel.repaint()
        networkPanel.undoManager.addUndoableAction(
            description = "Layout neurons with ${layoutEditor.layout}",
            undo = { neurons.zip(locations).forEach{(n,l) -> n.location = l} },
            redo = {
                layoutEditor.layout.setInitialLocation(neurons.centerLocation)
                layoutEditor.layout.layoutNeurons(neurons)
            }
        )
    }

    fun commitChanges() {
        mainPanel.commitChanges()
    }
}
