package org.simbrain.network.gui.nodes

import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getTrainingDialog
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
        get() {
            val contextMenu = JPopupMenu()
            contextMenu.add(networkPanel.networkActions.cutAction)
            contextMenu.add(networkPanel.networkActions.copyAction)
            contextMenu.add(networkPanel.networkActions.pasteAction)
            contextMenu.addSeparator()

            // Edit Submenu
            contextMenu.add(networkPanel.createAction(name = "Edit network") {
                propertyDialog?.display()
            })
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()

            // Train Submenu
            contextMenu.add(networkPanel.createAction(name = "Train network") {
                srn.getTrainingDialog().display()
            })

            // Coupling menu
            contextMenu.addSeparator()
            contextMenu.add(CouplingMenu(networkPanel.networkComponent, srn))

            return contextMenu
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) { srn.getTrainingDialog() }

}