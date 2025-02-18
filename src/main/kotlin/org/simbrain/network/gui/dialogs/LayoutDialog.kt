/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
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
