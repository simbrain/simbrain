/**
 * Renders gap junctions in three situations: a plain junction (straight line, midpoint channel glyph),
 * a junction bowed around a parallel chemical synapse, and an inert junction on a non-voltage neuron
 * (dashed).
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.GapJunction
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.updaterules.ContinuousSigmoidalRule
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class GapJunctionSnapshot : UiSnapshotDef {
    override val name = "gap_junction"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(700, 640)
        }
        runBlocking {
            fun voltageNeuron(x: Double, y: Double) = Neuron(ContinuousSigmoidalRule()).apply {
                location = point(x, y)
            }
            val a = voltageNeuron(-120.0, -80.0).apply { label = "AFD" }
            val b = voltageNeuron(120.0, -80.0).apply { label = "AIB" }
            val c = voltageNeuron(-120.0, 80.0).apply { label = "AIY" }
            val d = voltageNeuron(120.0, 80.0).apply { label = "AIZ" }
            val e = voltageNeuron(-120.0, 220.0).apply { label = "DMN" }
            val f = Neuron().apply { location = point(120.0, 220.0); label = "Linear" }
            val g = voltageNeuron(-120.0, 360.0).apply { label = "Strong" }
            val h = voltageNeuron(120.0, 360.0).apply { label = "Strong" }
            val i = voltageNeuron(-120.0, 500.0).apply { label = "Zero" }
            val j = voltageNeuron(120.0, 500.0).apply { label = "Zero" }
            listOf(a, b, c, d, e, f, g, h, i, j).forEach { network.addNetworkModel(it, usePlacementManager = false) }
            network.addNetworkModel(GapJunction(a, b, 2.62), usePlacementManager = false)
            network.addNetworkModel(GapJunction(c, d, 1.5), usePlacementManager = false)
            network.addNetworkModel(Synapse(c, d, 0.7), usePlacementManager = false)
            network.addNetworkModel(GapJunction(e, f, 1.0), usePlacementManager = false)
            network.addNetworkModel(GapJunction(g, h, 9.0), usePlacementManager = false)
            network.addNetworkModel(GapJunction(i, j, 0.0), usePlacementManager = false)
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
