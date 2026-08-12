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
 * The unrolled view with each layer set to draw itself a different way.
 *
 * The columns are not layers, so nothing makes them follow the real layers' appearance automatically.
 * Here the input layer is a grid, the hidden layer is drawn as neuron circles, and the output layer is
 * vertical, which between them cover every mode a layer has. Each column should match the rolled network
 * on the right layer by layer, in shape as well as in values, and the columns should stay aligned with
 * layers that are no longer all the same size.
 */
class BPTTLayerModesSnapshot : UiSnapshotDef {
    override val name = "bptt_layer_modes"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1400, 800)
        }
        val bptt = BPTTNetwork(9, 4, 5, point(0, 0))
        runBlocking {
            network.addNetworkModel(bptt, usePlacementManager = false)
        }
        SwingUtilities.invokeAndWait {
            JDialog().apply { contentPane = panel; pack() }
            bptt.inputLayer.gridMode = true
            bptt.hiddenLayer.circleMode = true
            bptt.outputLayer.verticalLayout = true
            bptt.unrolledView = true
        }
        repeat(5) { SwingUtilities.invokeAndWait { } }

        runBlocking {
            val trainer = BPTTTrainer(network, bptt)
            repeat(20) { trainer.trainOnce() }
        }
        Thread.sleep(300)
        repeat(8) { SwingUtilities.invokeAndWait { } }

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
