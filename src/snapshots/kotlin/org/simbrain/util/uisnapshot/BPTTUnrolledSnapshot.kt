package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.network.trainers.BPTTTrainer
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

/**
 * A BPTT network with its unrolled-over-time view switched on, which is the arrangement of figure
 * 16.3 in the source text: the network actually being trained on the left, its unrolled form beside
 * it. Trained briefly first so the columns hold real per-timestep activations rather than the empty
 * strips they start as.
 */
class BPTTUnrolledSnapshot : UiSnapshotDef {
    override val name = "bptt_unrolled"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1300, 700)
        }
        val bptt = BPTTNetwork(5, 4, 5, point(0, 0))
        runBlocking {
            network.addNetworkModel(bptt, usePlacementManager = false)
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            bptt.unrolledView = true
        }
        // The toggle is applied by an event-thread task that queues further layout work, so drain the
        // queue before measuring. The camera is then set directly rather than through zoomToFitPage,
        // whose handler defers setViewBounds into yet another task that would not have run by the time
        // the image is captured. Zoom-to-fit does cover the unrolled view in the running app, since it
        // unions screen element bounds and the view is a child of one.
        repeat(5) { SwingUtilities.invokeAndWait { } }

        // Trained after the view is showing, since the trainer only collects per-timestep activations
        // when something is drawing them.
        runBlocking {
            val trainer = BPTTTrainer(network, bptt)
            repeat(20) { trainer.trainOnce() }
        }
        // The activation event is throttled, so give its window time to close before capturing.
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
