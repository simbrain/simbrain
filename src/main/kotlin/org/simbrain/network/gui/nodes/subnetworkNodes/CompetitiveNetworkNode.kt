package org.simbrain.network.gui.nodes.subnetworkNodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.makeTrainerPanel
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.util.StandardDialog
import javax.swing.JPopupMenu

/**
 * PNode representation of competitive network.
 *
 * @author Jeff Yoshimi
 */
class CompetitiveNetworkNode(networkPanel: NetworkPanel, val competitiveNet: CompetitiveNetwork)
    : SubnetworkNode(networkPanel, competitiveNet) {

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            with(networkPanel) {
                applyUnsupervisedActions(competitiveNet)
            }
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) {competitiveNet.makeTrainerPanel()}
}
