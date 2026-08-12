package org.simbrain.util.uisnapshot

import com.formdev.flatlaf.FlatDarkLaf
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
 * The unrolled view built under the light theme and then switched to dark at runtime.
 *
 * The diagram bakes theme colours into its shapes when it is drawn, so it has to be redrawn on a theme
 * switch rather than recoloured in place. A correct dark result proves that redraw actually happens
 * through the live recolour hook, not just that the colours are right at construction time. The network
 * is trained first so the columns hold activations, which the redraw has to carry across rather than
 * blanking.
 */
class BPTTUnrolledDarkSnapshot : UiSnapshotDef {
    override val name = "bptt_unrolled_dark"

    override fun build(): Component {
        setupTheme("light")
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

        // Trained before the theme switch, so the switch has real values to preserve through its rebuild.
        runBlocking {
            val trainer = BPTTTrainer(network, bptt)
            repeat(20) { trainer.trainOnce() }
        }
        Thread.sleep(300)
        repeat(5) { SwingUtilities.invokeAndWait { } }

        SwingUtilities.invokeAndWait {
            FlatDarkLaf.setup()
            panel.preferenceLoader()
        }
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
