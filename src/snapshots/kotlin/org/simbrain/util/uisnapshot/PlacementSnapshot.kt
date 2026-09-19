/**
 * Snapshot of automatic placement: a row of added neurons, with the first neuron duplicated twice (the copies go
 * below, since the spot to the right is taken).
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class PlacementSnapshot : UiSnapshotDef {
    override val name = "placement"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(600, 400)
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
        }
        runBlocking {
            network.placementManager.insertionPoint = point(0.0, 0.0)
            val row = List(3) { Neuron().also { network.addNetworkModel(it) } }
            panel.selectionManager.set(panel.screenElements.filter { it.model == row[0] })
            panel.duplicate()
            panel.duplicate()
        }
        SwingUtilities.invokeAndWait {
            network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
