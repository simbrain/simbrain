/**
 * Tensor layers and convolution kernels with cells selected for editing, and the effect of graphical bounds.
 *
 * Left pair: the same activations, running from 0 to 4 across the image, drawn with the default bounds, where
 * everything from 1 up saturates, and with an upper bound of 4, where the ramp is visible. The right hand one also
 * has three cells selected. Middle: a convolution in single kernel view with two weights selected. Right: a
 * convolution in kernel grid view with weights selected in two different tiles. Selection outlines should sit
 * exactly on their cells.
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
import org.simbrain.network.gui.nodes.TensorConnectorNode
import org.simbrain.network.gui.nodes.TensorNode
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class TensorCellEditSnapshot : UiSnapshotDef {
    override val name = "tensor_cell_edit"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1300, 700)
        }

        val shape = TensorShape(8, 8, 1)
        fun ramp(x: Double, upperBound: Double) = TensorLayer(shape).apply {
            activations = DoubleArray(shape.size) { (it % 8) * 4.0 / 7 }
            graphicalUpperBound = upperBound
            setLocation(x, 0.0)
        }
        val defaultBounds = ramp(0.0, 1.0)
        val wideBounds = ramp(200.0, 4.0)

        fun convPair(x: Double, grid: Boolean): ConvolutionConnector {
            val sourceShape = TensorShape(8, 8, 2)
            val source = TensorLayer(sourceShape).apply { setLocation(x, 0.0) }
            val target = TensorLayer(sourceShape.convOutputShape(3, 1, Padding.SAME, 3)).apply {
                setLocation(x, -400.0)
            }
            return ConvolutionConnector(source, target, 3, 3, 1, Padding.SAME).apply { kernelGridMode = grid }
        }
        val single = convPair(500.0, grid = false)
        val grid = convPair(800.0, grid = true)

        runBlocking {
            listOf(defaultBounds, wideBounds).forEach { network.addNetworkModel(it, usePlacementManager = false) }
            listOf(single, grid).forEach {
                network.addNetworkModel(it.source, usePlacementManager = false)
                network.addNetworkModel(it.target, usePlacementManager = false)
                network.addNetworkModel(it, usePlacementManager = false)
            }
        }
        SwingUtilities.invokeAndWait { JDialog().apply { contentPane = panel; pack() } }
        Thread.sleep(300)
        repeat(8) { SwingUtilities.invokeAndWait { } }

        SwingUtilities.invokeAndWait {
            (panel.getNode(wideBounds) as TensorNode).pixelSelection =
                setOf(shape.index(0, 0, 0), shape.index(3, 4, 0), shape.index(7, 7, 0))
            (panel.getNode(single) as TensorConnectorNode).pixelSelection =
                setOf(single.kernelIndex(0, 0, 0, 0), single.kernelIndex(0, 0, 1, 2))
            (panel.getNode(grid) as TensorConnectorNode).pixelSelection =
                setOf(grid.kernelIndex(0, 1, 1, 1), grid.kernelIndex(2, 0, 2, 0))
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
