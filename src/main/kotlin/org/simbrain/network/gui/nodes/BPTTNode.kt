/**
 * Canvas node for [BPTTNetwork].
 *
 * Currently renders the network in its rolled-up form: the hidden layer's self-connection is drawn by
 * the standard [WeightMatrixNode] machinery, which already switches to a recurrent arrow when a
 * matrix's source and target are the same layer.
 */
package org.simbrain.network.gui.nodes

import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getSupervisedTrainingDialog
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.display
import javax.swing.JPopupMenu

class BPTTNode(networkPanel: NetworkPanel, private val bptt: BPTTNetwork) :
    SubnetworkNode(networkPanel, bptt) {

    override val model: NetworkModel
        get() = bptt

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            applyBasicActions()

            add(networkPanel.createAction(name = "Train...") {
                bptt.getSupervisedTrainingDialog().display()
            })
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) { bptt.getSupervisedTrainingDialog() }

}
