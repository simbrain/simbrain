package org.simbrain.network.gui.nodes.subnetworkNodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.util.StandardDialog
import javax.swing.JPopupMenu

/**
 * PNode representation of a backprop network.
 */
class BackpropNetworkNode(networkPanel: NetworkPanel, private val bp: BackpropNetwork):
    SubnetworkNode(networkPanel, bp) {

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(createEditAction("Edit / Train Backprop..."))
            addDefaultSubnetActions()
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) { bp.getSupervisedTrainingDialog() }
}