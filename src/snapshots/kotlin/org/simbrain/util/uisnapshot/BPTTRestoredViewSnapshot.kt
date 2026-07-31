package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

/**
 * A saved network restored with the unrolled view already showing.
 *
 * Worth its own snapshot because of the ordering. The columns are placed against the rolled network's
 * measured extent, and on this path the canvas node is built before anything has been laid out, so that
 * measurement is not available at the moment the view first tries to build itself. It has to come back
 * once the outline exists. If it does not, the columns are simply missing, which no other snapshot
 * would catch: every one of them toggles the view on by hand, after layout.
 */
class BPTTRestoredViewSnapshot : UiSnapshotDef {
    override val name = "bptt_restored_view"

    override fun build(): Component {
        val saved = Network()
        val bptt = BPTTNetwork(5, 4, 5, point(0, 0)).apply { label = "BPTT" }
        runBlocking { saved.addNetworkModel(bptt, usePlacementManager = false) }
        bptt.unrolledView = true

        val xstream = getNetworkXStream()
        val network = xstream.fromXML(xstream.toXML(saved)) as Network
        val restored = requireNotNull(network.getModelByLabel(BPTTNetwork::class.java, "BPTT"))

        val panel = NetworkPanel(NetworkComponent("snapshot", network)).apply {
            preferredSize = Dimension(1300, 700)
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
        }
        repeat(5) { SwingUtilities.invokeAndWait { } }

        runBlocking {
            with(network) {
                restored.inputLayer.setActivations(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0))
                restored.forwardPass()
                restored.inputLayer.setActivations(doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0))
                restored.forwardPass()
            }
        }
        Thread.sleep(300)
        repeat(5) { SwingUtilities.invokeAndWait { } }

        SwingUtilities.invokeAndWait {
            val content = panel.canvas.layer.fullBounds
            panel.canvas.camera.setViewBounds(
                PBounds(content.x - PAD, content.y - PAD, content.width + 2 * PAD, content.height + 2 * PAD)
            )
        }
        repeat(3) { SwingUtilities.invokeAndWait { } }
        return panel
    }

    companion object {
        private const val PAD = 20.0
    }
}
