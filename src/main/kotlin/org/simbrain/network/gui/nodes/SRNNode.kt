package org.simbrain.network.gui.nodes

import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.actions.edit.CopyAction
import org.simbrain.network.gui.actions.edit.CutAction
import org.simbrain.network.gui.actions.edit.PasteAction
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

    override fun getModel(): NetworkModel {
        return srn
    }

    override fun getToolTipText(): String {
        return srn.toString()
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
            srn.getTrainingDialog().display()
        })

        // Coupling menu
        contextMenu.addSeparator()
        contextMenu.add(CouplingMenu(networkPanel.networkComponent, srn))

        return contextMenu
    }

    override fun getPropertyDialog(): StandardDialog {
        return srn.getTrainingDialog()
    }

}