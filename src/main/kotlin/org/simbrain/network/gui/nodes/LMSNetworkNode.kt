package org.simbrain.network.gui.nodes

import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.actions.edit.CopyAction
import org.simbrain.network.gui.actions.edit.CutAction
import org.simbrain.network.gui.actions.edit.PasteAction
import org.simbrain.network.gui.dialogs.getTrainingDialog
import org.simbrain.network.subnetworks.LMSNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.display
import org.simbrain.workspace.gui.CouplingMenu
import javax.swing.JPopupMenu

/**
 * GUI representation of KotlinDL deep network.
 */
class LMSNetworkNode(networkPanel: NetworkPanel, private val lmsNet: LMSNetwork):
    SubnetworkNode(networkPanel, lmsNet) {

    init {
        val events = lmsNet.events
    }

    override fun getModel(): NetworkModel {
        return lmsNet
    }

    override fun getToolTipText(): String {
        return lmsNet.toString()
    }

    override fun getContextMenu(): JPopupMenu {
        val contextMenu = JPopupMenu()
        contextMenu.add(CutAction(networkPanel))
        contextMenu.add(CopyAction(networkPanel))
        contextMenu.add(PasteAction(networkPanel))
        contextMenu.addSeparator()

        // Edit Submenu
        contextMenu.add(networkPanel.createAction(name = "Edit network") {
            propertyDialog.display()
        })
        contextMenu.add(networkPanel.networkActions.deleteAction)
        contextMenu.addSeparator()

        // Train Submenu
        contextMenu.add(networkPanel.createAction(name = "Train network") {
            lmsNet.getTrainingDialog().display()
        })

        // Coupling menu
        contextMenu.addSeparator()
        contextMenu.add(CouplingMenu(networkPanel.networkComponent, lmsNet))

        return contextMenu
    }

    override fun getPropertyDialog(): StandardDialog {
        return lmsNet.getTrainingDialog()
    }

}