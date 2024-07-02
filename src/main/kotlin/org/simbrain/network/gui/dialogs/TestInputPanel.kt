/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.dialogs

import org.simbrain.network.core.*
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.createAction
import org.simbrain.util.table.*
import org.simbrain.util.toMatrix
import org.simbrain.workspace.gui.SimbrainDesktop
import smile.math.matrix.Matrix
import javax.swing.JCheckBox
import javax.swing.JCheckBoxMenuItem
import javax.swing.JLabel

/**
 * Panel for sending inputs from a table to a [Layer].
 */
fun NetworkPanel.createTestInputPanel(layer: Layer)= createTestInputPanel(layer.inputData) {
    if (layer is AbstractNeuronCollection && layer.isAllClamped) {
        layer.neuronList.activations = this.table.model.getCurrentDoubleRow()
    } else if (layer is NeuronArray && layer.isClamped) {
        layer.applyActivations(this.table.model.getCurrentDoubleRow().toDoubleArray())
    } else {
        layer.addInputs(this.table.model.getCurrentDoubleRow().toDoubleArray().toMatrix())
    }
    with(network) { layer.update() }
}

/**
 * Panel for sending inputs from a table to a list of [Neuron].
 */
fun createTestInputPanel(neurons: List<Neuron>, initData: Matrix = Matrix.eye(neurons.size)) = createTestInputPanel(initData) { selectedRow ->
    neurons.activations = table.model.getCurrentDoubleRow()
}

private fun createTestInputPanel(initData: Matrix, applyInputs: suspend MatrixEditor.(selectedRow: Int) -> Unit) = MatrixEditor(initData).apply {
    var workspaceMode = false
    toolbar.addSeparator()
    toolbar.add(JLabel("Workspace Mode"))
    toolbar.add(JCheckBox(createAction(
        description = "Workspace Mode"
    ) { event ->
        event.source.let {
            workspaceMode = if (it is JCheckBoxMenuItem) it.state else !workspaceMode
        }
    }).apply { this.isSelected = workspaceMode })
    toolbar.addSeparator()
    toolbar.add(table.createApplyAction("Apply Inputs") {
        applyInputs(it)
        if (workspaceMode) {
            SimbrainDesktop.workspace.updater.iterate(1)
        }
    })
    toolbar.add(table.createAdvanceRowAction())
    toolbar.add(table.createApplyAndAdvanceAction {
        applyInputs(it)
        if (workspaceMode) {
            SimbrainDesktop.workspace.updater.iterate(1)
        }
    })
    toolbar.add(table.insertRowAction)
    toolbar.add(table.deleteRowAction)
}