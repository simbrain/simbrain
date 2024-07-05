package org.simbrain.network.gui.nodes

import org.simbrain.network.core.NetworkModel
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
        get() = JPopupMenu().apply {
            applyBasicActions()

            // Train Submenu
            add(networkPanel.createAction(name = "Training dialog...") {
                makeTrainerPanel().display()
            })
            // Train once
            add(with(networkPanel.network) { rbm.createTrainOnPatternAction() })

            // Coupling menu
            addSeparator()
            add(CouplingMenu(networkPanel.networkComponent, rbm))

        }

    private fun makeTrainerPanel() = with(networkPanel) { getUnsupervisedTrainingPanel(rbm) {
        rbm.trainOnCurrentPattern() }
    }

    override val propertyDialog: StandardDialog
        get() = makeTrainerPanel()

}