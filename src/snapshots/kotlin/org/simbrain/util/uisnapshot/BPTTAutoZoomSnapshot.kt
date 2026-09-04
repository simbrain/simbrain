package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

/**
 * Auto zoom refitting itself when the unrolled view appears.
 *
 * Unlike the other network snapshots this one never sets the camera by hand, so what is framed here is
 * whatever auto zoom decided. The columns are several times wider than the rolled network, so a camera
 * still fitted to the rolled form alone would leave them off the left edge entirely. Everything being
 * visible is the whole assertion.
 */
class BPTTAutoZoomSnapshot : UiSnapshotDef {
    override val name = "bptt_auto_zoom"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1200, 700)
        }
        val bptt = BPTTNetwork(5, 4, 5, point(0, 0))
        runBlocking {
            network.addNetworkModel(bptt, usePlacementManager = false)
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
        }
        // Let auto zoom settle on the rolled network before anything is unrolled, so that what follows has
        // to be a refit rather than a first fit.
        Thread.sleep(200)
        repeat(5) { SwingUtilities.invokeAndWait { } }

        SwingUtilities.invokeAndWait {
            bptt.trainerConfig.sequenceLength = 5
            bptt.updateStateInfoText()
            bptt.unrolledView = true
        }
        // The zoom request is debounced, so it needs longer than a drained event queue to arrive.
        Thread.sleep(400)
        repeat(10) { SwingUtilities.invokeAndWait { } }
        return panel
    }
}
