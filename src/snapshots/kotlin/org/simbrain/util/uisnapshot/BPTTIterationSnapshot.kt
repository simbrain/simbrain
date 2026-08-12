package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.util.PBounds
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
 * The unrolled view filled by ordinary iteration rather than by training.
 *
 * Training computes a whole window at once, so it can fill every column in one go. A workspace tick or
 * the training dialog's apply-row button computes a single step, so the columns fill from the right as
 * steps accumulate. Only two steps are run here, one short of the four the network is unrolled over,
 * which is the case worth looking at: the two oldest columns should be empty rather than holding
 * whatever was in them before, since those steps have not happened yet.
 */
class BPTTIterationSnapshot : UiSnapshotDef {
    override val name = "bptt_iteration"

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
        repeat(5) { SwingUtilities.invokeAndWait { } }

        runBlocking {
            with(network) {
                bptt.randomize()
                listOf(
                    doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0),
                    doubleArrayOf(0.0, 1.0, 0.0, 0.0, 0.0)
                ).forEach { row ->
                    bptt.inputLayer.setActivations(row)
                    bptt.forwardPass()
                }
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
