/**
 * An element's footprint is the space it takes up on the canvas whether or not it is selected, and what is laid out
 * around elements, such as a subnetwork's outline, is laid out against footprints.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.piccolo2d.util.PBounds
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.FlattenConnector
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Padding
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.TensorLayer
import org.simbrain.network.core.TensorShape
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.core.addNeurons
import org.simbrain.network.gui.nodes.FlattenConnectorNode
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.gui.nodes.SynapseGroupNode
import org.simbrain.network.gui.nodes.TensorConnectorNode
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.util.onEdt
import java.awt.Dimension
import java.awt.geom.Point2D
import javax.swing.JDialog

class FootprintTest {

    /** A panel laid out at a fixed size and validated, showing [network] with [models] added to it. */
    private fun panelFor(network: Network, vararg models: NetworkModel): NetworkPanel {
        val panel = NetworkPanel(NetworkComponent("test", network))
        runBlocking { models.forEach { network.addNetworkModel(it, usePlacementManager = false) } }
        onEdt {
            panel.preferredSize = Dimension(800, 800)
            JDialog().apply { contentPane = panel; pack() }
            panel.autoZoom = false
        }
        panel.validateCanvas()
        return panel
    }

    private fun arrayAt(x: Double, y: Double, size: Int = 4) =
        NeuronArray(size).apply { location = Point2D.Double(x, y) }

    private fun NetworkPanel.footprintOf(model: NetworkModel): PBounds = onEdt { getNode(model)!!.footprint }

    private fun NetworkPanel.fullBoundsOf(model: NetworkModel): PBounds = onEdt { getNode(model)!!.globalFullBounds }

    /** Selects the node of [model] and waits for its selection handle to be drawn. */
    private fun NetworkPanel.select(model: NetworkModel) {
        onEdt { selectionManager.set(getNode(model)!!) }
        validateCanvas()
    }

    @Test
    fun `selecting a neuron array changes its full bounds but not its footprint`() {
        val array = arrayAt(0.0, 0.0)
        val panel = panelFor(Network(), array)
        val footprint = panel.footprintOf(array)
        val fullBounds = panel.fullBoundsOf(array)
        assertEquals(fullBounds, footprint)

        panel.select(array)

        assertNotEquals(fullBounds, panel.fullBoundsOf(array), "The selection handle should count toward full bounds")
        assertEquals(footprint, panel.footprintOf(array))
    }

    @Test
    fun `selecting a tensor layer does not change its footprint`() {
        val layer = TensorLayer(TensorShape(4, 4, 2)).apply { location = Point2D.Double(0.0, 0.0) }
        val panel = panelFor(Network(), layer)
        val footprint = panel.footprintOf(layer)
        assertEquals(panel.fullBoundsOf(layer), footprint)

        panel.select(layer)

        assertEquals(footprint, panel.footprintOf(layer))
    }

    @Test
    fun `a neuron collection's footprint is its outline with its label box`() {
        val network = Network()
        val collection = NeuronCollection(runBlocking { network.addNeurons(4) })
        val panel = panelFor(network, collection)

        assertEquals(panel.fullBoundsOf(collection), panel.footprintOf(collection))
    }

    @Test
    fun `a weight matrix's footprint is its image, label box and caption, selected or not`() {
        val source = arrayAt(0.0, 0.0)
        val target = arrayAt(0.0, -420.0)
        val matrix = WeightMatrix(source, target)
        val panel = panelFor(Network(), source, target, matrix)
        val footprint = panel.footprintOf(matrix)
        val fullBounds = panel.fullBoundsOf(matrix)
        // The arrow is not in the node, so before selection everything in the node is footprint
        assertEquals(fullBounds, footprint)
        onEdt {
            val node = panel.getNode(matrix) as WeightMatrixNode
            assertTrue(footprint.contains(node.imageBox.globalBounds))
            assertTrue(footprint.contains(node.interactionBox.globalBounds))
        }

        panel.select(matrix)

        assertNotEquals(fullBounds, panel.fullBoundsOf(matrix), "The selection handle should count toward full bounds")
        assertEquals(footprint, panel.footprintOf(matrix))
    }

    @Test
    fun `a convolution's footprint covers whichever kernel drawing is showing`() {
        val shape = TensorShape(4, 4, 2)
        val source = TensorLayer(shape).apply { location = Point2D.Double(0.0, 0.0) }
        val target = TensorLayer(shape.convOutputShape(3, 1, Padding.SAME, 2)).apply {
            location = Point2D.Double(0.0, -500.0)
        }
        val conv = ConvolutionConnector(source, target, 3, 2, 1, Padding.SAME)
        val panel = panelFor(Network(), source, target, conv)
        val node = onEdt { panel.getNode(conv) as TensorConnectorNode }

        // The kernel grid is in the node as well, hidden, and takes up no space
        onEdt {
            assertTrue(node.footprint.contains(node.imageBox.globalBounds))
            assertTrue(node.footprint.contains(node.interactionBox.globalBounds))
            assertTrue(node.globalFullBounds.contains(node.footprint))
            assertTrue(node.footprint.height < node.imageBox.globalBounds.height + 120.0, "footprint ${node.footprint}")
        }

        onEdt { conv.kernelGridMode = true }
        panel.validateCanvas()

        onEdt {
            assertTrue(node.footprint.contains(node.kernelGridGroup.globalBounds))
            assertTrue(node.footprint.contains(node.interactionBox.globalBounds))
        }
        val footprint = panel.footprintOf(conv)
        panel.select(conv)
        assertEquals(footprint, panel.footprintOf(conv))
    }

    @Test
    fun `a flatten connector's footprint is its label box`() {
        val shape = TensorShape(4, 4, 2)
        val source = TensorLayer(shape).apply { location = Point2D.Double(0.0, 0.0) }
        val target = arrayAt(0.0, -420.0, size = shape.size)
        val flatten = FlattenConnector(source, target)
        val panel = panelFor(Network(), source, target, flatten)

        onEdt {
            val node = panel.getNode(flatten) as FlattenConnectorNode
            assertEquals(node.interactionBox.globalBounds, node.footprint)
        }
    }

    @Test
    fun `a synapse group's footprint is its label box`() {
        val network = Network()
        val source = NeuronCollection(runBlocking { network.addNeurons(4) })
        val target = NeuronCollection(runBlocking { network.addNeurons(4) })
        val group = SynapseGroup(source, target).apply {
            autoVisibility = false
            displaySynapses = false
        }
        val panel = panelFor(network, source, target, group)

        onEdt {
            val node = panel.getNode(group) as SynapseGroupNode
            assertEquals(node.interactionBox.globalBounds, node.footprint)
        }
    }

    @Test
    fun `selecting a layer inside a subnetwork leaves the subnetwork's outline where it was`() {
        val backprop = BackpropNetwork(intArrayOf(4, 3, 2))
        val panel = panelFor(Network(), backprop)
        fun outline() = onEdt { (panel.getNode(backprop) as SubnetworkNode).outline.globalFullBounds }
        val before = outline()

        panel.select(backprop.layers.toList()[1])

        assertEquals(before, outline())
    }
}
