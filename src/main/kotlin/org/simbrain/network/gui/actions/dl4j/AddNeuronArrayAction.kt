package org.simbrain.network.gui.actions.dl4j

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.showNeuronArrayCreationDialog
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

class AddNeuronArrayAction(val networkPanel: NetworkPanel) : AbstractAction("Add Neuron Array...") {

    override fun actionPerformed(event: ActionEvent) {
        networkPanel.showNeuronArrayCreationDialog()
    }

    init {
        putValue(Action.SHORT_DESCRIPTION, "Add a neuron array to the network")
    }
}