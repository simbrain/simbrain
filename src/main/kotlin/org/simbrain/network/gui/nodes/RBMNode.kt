package org.simbrain.network.gui.nodes

import org.simbrain.network.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.createTrainOnPatternAction
import org.simbrain.network.gui.dialogs.getUnsupervisedTrainingPanel
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.display
import org.simbrain.workspace.gui.CouplingMenu
import javax.swing.JPopupMenu

class RBMNode(networkPanel: NetworkPanel, private val rbm: RestrictedBoltzmannMachine):
    SubnetworkNode(networkPanel, rbm) {

    override val model: NetworkModel
        get() = rbm

    override val toolTipText: String
        get() = rbm.toString()

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
            contextMenu.add(networkPanel.createAction(name = "Training dialog...") {
                makeTrainerPanel().display()
            })
            // Train once
            contextMenu.add(with(networkPanel.network) { rbm.createTrainOnPatternAction() })

            // Coupling menu
            contextMenu.addSeparator()
            contextMenu.add(CouplingMenu(networkPanel.networkComponent, rbm))

            return contextMenu
        }

    private fun makeTrainerPanel() = with(networkPanel) { getUnsupervisedTrainingPanel(rbm) {
        rbm.trainOnCurrentPattern() }
    }

    override val propertyDialog: StandardDialog
        get() = makeTrainerPanel()

}