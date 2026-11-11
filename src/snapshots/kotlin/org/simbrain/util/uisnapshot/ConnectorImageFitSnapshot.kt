/**
 * Weight matrices and single-kernel convolution connectors between layers at a range of separations. Top row,
 * left to right: weight matrices between neuron arrays stacked close together, at a moderate distance and far
 * apart, a reciprocal pair, a self-connection, and a matrix between arrays placed side by side. Bottom row:
 * convolutions in single kernel view between tensor layers close together, at the default distance and far
 * apart, then two between layers side by side. Each image should stay square and clear of its layers, filling a
 * short gap and leaving the arrow showing on either side of it in a long one; layers too close for the smallest
 * image are the exception.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.Padding
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.NetworkPanel
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class ConnectorImageFitSnapshot : UiSnapshotDef {
    override val name = "connector_image_fit"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1700, 1100)
        }

        runBlocking {
            suspend fun array(x: Double, y: Double, size: Int = 8) = NeuronArray(size).apply {
                setLocation(x, y)
                network.addNetworkModel(this, usePlacementManager = false)
            }
            suspend fun matrixPair(x: Double, y: Double, dx: Double, dy: Double, reciprocal: Boolean = false) {
                val source = array(x, y)
                val target = array(x + dx, y + dy, 5)
                network.addNetworkModel(WeightMatrix(source, target), usePlacementManager = false)
                if (reciprocal) network.addNetworkModel(WeightMatrix(target, source), usePlacementManager = false)
            }
            matrixPair(0.0, 0.0, 0.0, -130.0)
            matrixPair(300.0, 0.0, 0.0, -250.0)
            matrixPair(600.0, 0.0, 0.0, -500.0)
            matrixPair(900.0, 0.0, 0.0, -350.0, reciprocal = true)
            val recurrent = array(1300.0, -200.0)
            network.addNetworkModel(WeightMatrix(recurrent, recurrent), usePlacementManager = false)
            matrixPair(1150.0, 0.0, 450.0, 0.0)

            suspend fun convPair(x: Double, y: Double, dx: Double, dy: Double) {
                val sourceShape = TensorShape(8, 8, 2)
                val source = TensorLayer(sourceShape).apply {
                    activations = DoubleArray(sourceShape.size) { (it % 7) / 7.0 }
                }
                val target = TensorLayer(sourceShape.convOutputShape(3, 1, Padding.SAME, 4))
                source.setLocation(x, y)
                target.setLocation(x + dx, y + dy)
                network.addNetworkModel(source, usePlacementManager = false)
                network.addNetworkModel(target, usePlacementManager = false)
                network.addNetworkModel(
                    ConvolutionConnector(source, target, 3, 4, 1, Padding.SAME), usePlacementManager = false
                )
            }
            convPair(0.0, 900.0, 0.0, -230.0)
            convPair(300.0, 900.0, 0.0, -400.0)
            convPair(600.0, 900.0, 0.0, -600.0)
            convPair(950.0, 900.0, 300.0, 0.0)
            convPair(950.0, 600.0, 550.0, 0.0)
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
