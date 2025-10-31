package org.simbrain.network.gui.nodes.subnetworkNodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import javax.swing.JPopupMenu

/**
 * PNode representation of a backprop network.
 */
class BackpropNetworkNode(networkPanel: NetworkPanel, private val bp: BackpropNetwork):
    SubnetworkNode(networkPanel, bp) {

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            addDefaultSubnetActions()
            addSeparator()
            add(createEditAction("Edit ${bp.displayName}..."))
            add(createAction("Train...") {
                propertyDialog.run {
                    pack()
                    setLocationRelativeTo(null)
                    isVisible = true
                }
            })
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) { bp.getSupervisedTrainingDialog() }
}