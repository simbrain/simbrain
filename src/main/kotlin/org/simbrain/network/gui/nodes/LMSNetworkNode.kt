package org.simbrain.network.gui.nodes

import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.subnetworks.LMSNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.display
import org.simbrain.workspace.gui.CouplingMenu
import javax.swing.JPopupMenu

/**
 * GUI representation of an LMS Network
 */
class LMSNetworkNode(networkPanel: NetworkPanel, private val lmsNet: LMSNetwork):
    SubnetworkNode(networkPanel, lmsNet) {

    init {
        val events = lmsNet.events
    }

    override val model: NetworkModel
        get() = lmsNet

    override val toolTipText: String
        get() = lmsNet.toString()

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()
            contextMenu.add(networkPanel.networkActions.cutAction)
            contextMenu.add(networkPanel.networkActions.copyAction)
            contextMenu.add(networkPanel.networkActions.pasteAction)
            contextMenu.addSeparator()

            // Edit Submenu
            contextMenu.add(networkPanel.createAction(name = "Edit network") {
                propertyDialog.display()
            })
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()

            // Train Submenu
            contextMenu.add(networkPanel.createAction(name = "Train network") {
                lmsNet.getSupervisedTrainingDialog().display()
            })

            // Coupling menu
            contextMenu.addSeparator()
            contextMenu.add(CouplingMenu(networkPanel.networkComponent, lmsNet))

            return contextMenu
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) { lmsNet.getSupervisedTrainingDialog() }

}