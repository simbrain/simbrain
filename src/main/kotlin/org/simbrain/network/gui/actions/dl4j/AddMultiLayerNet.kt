package org.simbrain.network.gui.actions.dl4j

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.showMultiLayerNetworkCreationDialog
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

class AddMultiLayerNet(val networkPanel: NetworkPanel) : AbstractAction("Add Multilayer Network...") {

    override fun actionPerformed(event: ActionEvent) {
        networkPanel.showMultiLayerNetworkCreationDialog()
    }

    init {
        putValue(Action.SHORT_DESCRIPTION, "Add a Dl4J Multi Layer Network to the network")
    }
}