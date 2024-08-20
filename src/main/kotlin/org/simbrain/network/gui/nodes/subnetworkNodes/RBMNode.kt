package org.simbrain.network.gui.nodes.subnetworkNodes

import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.makeTrainerPanel
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.util.StandardDialog
import org.simbrain.workspace.gui.CouplingMenu
import javax.swing.JPopupMenu

class RBMNode(networkPanel: NetworkPanel, private val rbm: RestrictedBoltzmannMachine):
    SubnetworkNode(networkPanel, rbm) {

    override val model: NetworkModel
        get() = rbm

    override val toolTipText: String
        get() = rbm.toString()

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            with(networkPanel) {
                applyUnsupervisedActions(rbm)
            }
            addSeparator()
            add(CouplingMenu(networkPanel.networkComponent, rbm))
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) {rbm.makeTrainerPanel()}

}