package org.simbrain.network.gui.dialogs

import org.simbrain.network.core.*
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.trainers.probesReading
import org.simbrain.util.createAction
import org.simbrain.util.table.createApplyAction
import org.simbrain.util.table.deleteRowAction
import org.simbrain.util.table.insertRowAction
import org.simbrain.util.toColumnVector
import org.simbrain.workspace.gui.SimbrainDesktop
import smile.math.matrix.Matrix
import java.awt.Dimension
import javax.swing.JCheckBox
import javax.swing.JCheckBoxMenuItem
import javax.swing.JLabel

/**
 * Panel for sending inputs from a table to a [Layer].
 */
fun NetworkPanel.createTestInputPanel(layer: Layer)= createTestInputPanel(layer.inputData) {
    if (layer is NeuronCollection && layer.isAllClamped) {
        layer.neuronList.activations = this.table.model.getCurrentDoubleRow()
    } else if (layer is NeuronArray && layer.isClamped) {
        layer.setActivations(this.table.model.getCurrentDoubleRow().toDoubleArray())
    } else {
        layer.addInputs(this.table.model.getCurrentDoubleRow().toDoubleArray().toColumnVector())
    }
    with(network) {
        layer.update()
        probesReading(listOf(layer)).forEach { it.refreshOutput() }
    }
}

/**
 * Panel for sending inputs from a table to a list of [Neuron].
 */
fun createTestInputPanel(neurons: List<Neuron>, initData: Matrix = Matrix.eye(neurons.size)) = createTestInputPanel(initData) { selectedRow ->
    neurons.activations = table.model.getCurrentDoubleRow()
}

private fun createTestInputPanel(initData: Matrix, applyInputs: suspend MatrixEditor.(selectedRow: Int) -> Unit) = MatrixEditor(initData).apply {
    var workspaceMode = true
    preferredSize = Dimension(600, 250)
    toolbar.addSeparator()
    toolbar.add(table.insertRowAction)
    toolbar.add(table.deleteRowAction)
    toolbar.addSeparator()
    val advanceRowCheckbox = JCheckBox("Auto advance").apply { isSelected = true }
    toolbar.add(table.createApplyAction("Apply inputs") {
        applyInputs(it)
        if (workspaceMode) {
            SimbrainDesktop.workspace.updater.iterate(1)
        }
        if (advanceRowCheckbox.isSelected) {
            incrementSelectedRow()
        }
    })
    toolbar.add(advanceRowCheckbox)
    toolbar.add(JCheckBox(createAction(
        description = "Workspace Mode"
    ) { event ->
        event?.source.let {
            workspaceMode = if (it is JCheckBoxMenuItem) it.state else !workspaceMode
        }
    }).apply { this.isSelected = workspaceMode })
    toolbar.add(JLabel("Workspace Mode"))
}