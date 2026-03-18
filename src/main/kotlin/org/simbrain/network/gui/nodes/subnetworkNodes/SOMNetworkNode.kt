package org.simbrain.network.gui.nodes.subnetworkNodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.makeTrainerPanel
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.subnetworks.SOMNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import javax.swing.JPopupMenu

/**
 * PNode representation of SOM Network.
 */
class SOMNetworkNode(networkPanel: NetworkPanel, val somNet: SOMNetwork):
    SubnetworkNode(networkPanel, somNet) {

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            with(networkPanel) {
                applyUnsupervisedActions(somNet)
                addSeparator()
                add(createAction(name = "Reset SOM network") { somNet.reset() })
                add(createAction(name = "Recall SOM memory") { somNet.recall() })
            }
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) {somNet.makeTrainerPanel()}

}
