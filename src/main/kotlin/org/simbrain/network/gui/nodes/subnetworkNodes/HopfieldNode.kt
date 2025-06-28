package org.simbrain.network.gui.nodes.subnetworkNodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.makeTrainerPanel
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.util.StandardDialog
import javax.swing.JPopupMenu

/**
 * PNode representation of Hopfield Network.
 *
 * @author jyoshimi
 */
class HopfieldNode(networkPanel: NetworkPanel, val hopfieldNet: Hopfield)
    : SubnetworkNode(networkPanel, hopfieldNet) {

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            with(networkPanel) {
                applyUnsupervisedActions(hopfieldNet)
            }
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) {hopfieldNet.makeTrainerPanel()}

}
