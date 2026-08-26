package org.simbrain.util.uisnapshot

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.couplings.MeanOperation
import org.simbrain.workspace.couplings.ScaleOperation
import org.simbrain.workspace.gui.CouplingTransformDialog
import java.awt.Component
import javax.swing.SwingUtilities

/**
 * The coupling transform editor over a type-bridging chain: an array producer into a scalar consumer
 * through mean and scale, so the endpoint labels, chain rendering, and the valid-chain status line are
 * all exercised.
 */
class CouplingTransformDialogSnapshot : UiSnapshotDef {
    override val name = "coupling_transform_dialog"

    override fun build(): Component {
        val workspace = Workspace()
        val network = Network()
        workspace.addWorkspaceComponent(NetworkComponent("snapshot", network))
        val source = NeuronArray(5)
        val target = Neuron()
        network.addNetworkModelAsync(source)
        network.addNetworkModelAsync(target)

        lateinit var dialog: Component
        with(workspace.couplingManager) {
            val producer = source.getProducer("getActivationArray")
            val consumer = target.getConsumer("setActivation")
            SwingUtilities.invokeAndWait {
                dialog = CouplingTransformDialog(
                    producer,
                    consumer,
                    listOf(MeanOperation(), ScaleOperation(2.0))
                ) { }
            }
        }
        return dialog
    }
}
