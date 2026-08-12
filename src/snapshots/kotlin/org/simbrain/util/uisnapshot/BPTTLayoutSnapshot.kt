package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

/**
 * An SRN and a BPTT network side by side, in the rolled-up form.
 *
 * Two things to look at. The BPTT network's hidden layer connects to itself, which is the one case
 * where a weight matrix's source and target are the same layer, so its recurrent arrow has to stay
 * inside the subnetwork outline and clear of the weight matrices above and below. And the pair shows
 * the structural contrast the two subnetworks exist to draw: a separate clamped context layer versus
 * a single self-connection.
 */
class BPTTLayoutSnapshot : UiSnapshotDef {
    override val name = "bptt_layout"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1000, 700)
        }
        runBlocking {
            network.addNetworkModel(SRNNetwork(5, 4, 5, point(-450, 0)), usePlacementManager = false)
            network.addNetworkModel(BPTTNetwork(5, 4, 5, point(300, 0)), usePlacementManager = false)
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            network.events.zoomToFitPage.fire()
        }
        return panel
    }
}
