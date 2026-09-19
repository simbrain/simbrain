package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

/**
 * Top row: a closely spaced reciprocal pair with different strengths. Bottom row: a reciprocal pair whose second synapse is
 * deleted after both nodes exist, so the remaining line should run the full length again.
 */
class BidirectionalSynapseSnapshot : UiSnapshotDef {
    override val name = "bidirectional_synapse"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(700, 500)
        }
        runBlocking {
            fun neuron(x: Double, y: Double) = Neuron().apply { location = point(x, y) }
            val a = neuron(-40.0, -80.0)
            val b = neuron(40.0, -80.0)
            val c = neuron(-120.0, 80.0)
            val d = neuron(120.0, 80.0)
            listOf(a, b, c, d).forEach { network.addNetworkModel(it, usePlacementManager = false) }
            network.addNetworkModel(Synapse(a, b, 1.0), usePlacementManager = false)
            network.addNetworkModel(Synapse(b, a, -1.0), usePlacementManager = false)
            network.addNetworkModel(Synapse(c, d, 0.3), usePlacementManager = false)
            val reverse = Synapse(d, c, -0.3)
            network.addNetworkModel(reverse, usePlacementManager = false)
            network.deleteModels(listOf(reverse))
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
