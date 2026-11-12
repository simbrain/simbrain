/**
 * Connections that run into each other or into layers they do not join, to show which is drawn over which and where
 * the images on them land. Left to right: a skip connection passing a middle layer, two connections that cross,
 * three layers feeding one, a synapse running across a weight matrix image, and a synapse group arrow running
 * across one.
 */
package org.simbrain.util.uisnapshot

import kotlinx.coroutines.runBlocking
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.point
import java.awt.Component
import java.awt.Dimension
import javax.swing.JDialog
import javax.swing.SwingUtilities

class ConnectorOverlapSnapshot : UiSnapshotDef {
    override val name = "connector_overlap"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component).apply {
            preferredSize = Dimension(1900, 800)
        }

        runBlocking {
            suspend fun array(x: Double, y: Double, size: Int = 6) = NeuronArray(size).apply {
                setLocation(x, y)
                network.addNetworkModel(this, usePlacementManager = false)
            }
            suspend fun matrix(source: NeuronArray, target: NeuronArray) = WeightMatrix(source, target).apply {
                // A fixed pattern, so that an image is easy to tell from what is drawn across it
                setWeights(Array(target.size) { row ->
                    DoubleArray(source.size) { column -> ((row * 3 + column * 5) % 7 - 3) / 3.0 }
                })
                network.addNetworkModel(this, usePlacementManager = false)
            }
            suspend fun neuron(x: Double, y: Double) = Neuron().apply {
                location = point(x, y)
                network.addNetworkModel(this, usePlacementManager = false)
            }

            // A skip connection whose arrow passes the middle layer
            val bottom = array(0.0, 0.0)
            val middle = array(0.0, -260.0)
            val top = array(0.0, -520.0)
            matrix(bottom, middle)
            matrix(middle, top)
            matrix(bottom, top)

            // Two connections that cross
            val lowerLeft = array(380.0, 0.0)
            val lowerRight = array(610.0, 0.0)
            val upperLeft = array(380.0, -520.0)
            val upperRight = array(610.0, -520.0)
            matrix(lowerLeft, upperRight)
            matrix(lowerRight, upperLeft)

            // Three layers feeding one
            val fanTarget = array(1250.0, -420.0)
            listOf(1010.0, 1250.0, 1490.0).forEach { x -> matrix(array(x, 0.0), fanTarget) }

            // A synapse running across a weight matrix image
            matrix(array(1850.0, 0.0), array(1850.0, -400.0))
            network.addNetworkModel(Synapse(neuron(1730.0, -215.0), neuron(1970.0, -215.0), 1.0), usePlacementManager = false)

            // A synapse group arrow running across a weight matrix image
            matrix(array(2400.0, 0.0), array(2400.0, -400.0))
            suspend fun column(x: Double) = NeuronCollection(List(3) { neuron(x, -150.0 - it * 50.0) }).also {
                network.addNetworkModel(it, usePlacementManager = false)
            }
            val groupSource = column(2200.0)
            val groupTarget = column(2600.0)
            network.addNetworkModel(
                SynapseGroup(groupSource, groupTarget).apply {
                    autoVisibility = false
                    displaySynapses = false
                },
                usePlacementManager = false
            )
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
