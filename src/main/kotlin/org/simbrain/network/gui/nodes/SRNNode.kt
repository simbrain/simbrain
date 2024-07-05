package org.simbrain.network.gui.nodes

import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.display
import org.simbrain.workspace.gui.CouplingMenu
import javax.swing.JPopupMenu

class SRNNode(networkPanel: NetworkPanel, private val srn: SRNNetwork):
    SubnetworkNode(networkPanel, srn) {

    init {
        val events = srn.events
    }

    override val model: NetworkModel
        get() = srn

    override val toolTipText: String
        get() = srn.toString()

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            applyBasicActions()

            // Train Submenu
            add(networkPanel.createAction(name = "Train network") {
                srn.getSupervisedTrainingDialog().display()
            })

            // Coupling menu
            addSeparator()
            add(CouplingMenu(networkPanel.networkComponent, srn))
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) { srn.getSupervisedTrainingDialog() }

}