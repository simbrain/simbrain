/**
 * Convolution connectors in kernel grid view across a range of filter and channel counts, at the default layer
 * spacing. Left to right: 1 filter x 1 channel, 5 x 1, 8 x 5, 16 x 8, then a 4 x 2 grid between layers placed
 * side by side and a 4 x 2 grid between layers placed close together. Grids should fill the gap between their
 * layers without touching them, and tiles should stay square.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.Network
import org.simbrain.network.core.Padding
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class KernelGridSnapshot : UiSnapshotDef {
    override val name = "kernel_grid"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1500, 700)
        }

        runBlocking {
            suspend fun convPair(x: Double, y: Double, dx: Double, dy: Double, channels: Int, filters: Int) {
                val sourceShape = TensorShape(8, 8, channels)
                val source = TensorLayer(sourceShape).apply {
                    activations = DoubleArray(sourceShape.size) { (it % 7) / 7.0 }
                }
                val targetShape = sourceShape.convOutputShape(3, 1, Padding.SAME, filters)
                val target = TensorLayer(targetShape)
                source.setLocation(x, y)
                target.setLocation(x + dx, y + dy)
                val connector = ConvolutionConnector(source, target, 3, filters, 1, Padding.SAME).apply {
                    kernelGridMode = true
                }
                network.addNetworkModel(source, usePlacementManager = false)
                network.addNetworkModel(target, usePlacementManager = false)
                network.addNetworkModel(connector, usePlacementManager = false)
            }
            convPair(0.0, 0.0, 0.0, -400.0, channels = 1, filters = 1)
            convPair(300.0, 0.0, 0.0, -400.0, channels = 1, filters = 5)
            convPair(600.0, 0.0, 0.0, -400.0, channels = 5, filters = 8)
            convPair(900.0, 0.0, 0.0, -400.0, channels = 8, filters = 16)
            convPair(1200.0, 0.0, 400.0, 0.0, channels = 2, filters = 4)
            convPair(1200.0, -400.0, 0.0, 200.0, channels = 2, filters = 4)
        }
        SwingUtilities.invokeAndWait { JDialog().apply { contentPane = panel; pack() } }
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
