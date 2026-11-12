/**
 * The network canvas is layered bottom to top as the lines and arrows of connections (a cached tier), spike
 * highlights, what connectors draw on their arrows, then nodes. A synapse is wholly in the connection tier. A
 * connector that draws on its arrow has its node in the tier above and only its arrow in the connection tier, where
 * weight matrix arrows sit lowest, then synapses, then synapse group arrows.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.piccolo2d.PNode
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.gui.nodes.ConnectionDrawing
import org.simbrain.network.gui.nodes.NeuronArrayNode
import org.simbrain.network.gui.nodes.NeuronCollectionNode
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.SynapseGroupNode
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.network.gui.nodes.WeightMatrixNode
import org.simbrain.util.UiWork
import org.simbrain.util.onEdt

class CanvasTiersTest {

    @Test
    fun `connections sit in the cached tier below spike highlights, connector drawings and nodes`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val (a, b) = network.addNeurons(2)
        network.addNetworkModel(Synapse(a, b))
        val source = NeuronCollection(network.addNeurons(3)).also { network.addNetworkModel(it) }
        val target = NeuronCollection(network.addNeurons(3)).also { network.addNetworkModel(it) }
        network.addNetworkModel(SynapseGroup(source, target))
        val arrays = List(2) { NeuronArray(3).also { network.addNetworkModel(it) } }
        network.addNetworkModel(WeightMatrix(arrays[0], arrays[1]))
        UiWork.awaitIdle()

        onEdt {
            val layer = panel.canvas.layer
            val edgeIndex = layer.indexOfChild(panel.edgeTier)
            val overlayIndex = layer.indexOfChild(panel.spikeOverlay)
            val connectorIndex = layer.indexOfChild(panel.connectorTier)
            val nodes = panel.screenElements
            val synapses = nodes.filterIsInstance<SynapseNode>()
            val connectors = nodes.filter { it is SynapseGroupNode || it is WeightMatrixNode }
            val others = nodes.filter { it is NeuronNode || it is NeuronCollectionNode || it is NeuronArrayNode }

            assertTrue(synapses.isNotEmpty() && connectors.any { it is SynapseGroupNode } && connectors.any { it is WeightMatrixNode })
            synapses.forEach { assertEquals(panel.edgeTier, it.parent, "A synapse is not in the connection tier") }
            connectors.forEach {
                assertEquals(panel.connectorTier, it.parent, "${it::class.simpleName} is not in the connector tier")
                assertEquals(panel.edgeTier, it.connectionDrawing?.parent, "The arrow of ${it::class.simpleName} is not in the connection tier")
            }
            others.forEach { node ->
                assertTrue(layer.indexOfChild(node.parent) > connectorIndex, "${node::class.simpleName} is not above the connectors")
            }
            assertTrue(edgeIndex < overlayIndex && overlayIndex < connectorIndex)
        }
    }

    @Test
    fun `within the connection tier weight matrix arrows sit below synapses, and synapses below synapse groups`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val arrays = List(2) { NeuronArray(3).also { network.addNetworkModel(it) } }
        network.addNetworkModel(WeightMatrix(arrays[0], arrays[1]))
        val (a, b, c) = network.addNeurons(3)
        network.addNetworkModel(Synapse(a, b))
        val source = NeuronCollection(network.addNeurons(3)).also { network.addNetworkModel(it) }
        val target = NeuronCollection(network.addNeurons(3)).also { network.addNetworkModel(it) }
        network.addNetworkModel(SynapseGroup(source, target))
        // Synapses added after a synapse group still go below it
        network.addNetworkModel(Synapse(b, c))
        network.addNetworkModel(Synapse(c, a))
        UiWork.awaitIdle()

        val band = onEdt { panel.edgeTier.childrenReference.toList() }.map {
            when ((it as? ConnectionDrawing)?.owner ?: it) {
                is WeightMatrixNode -> 0
                is SynapseNode -> 1
                is SynapseGroupNode -> 2
                else -> -1
            }
        }.filter { it >= 0 }
        // The group's own synapses are drawn as synapse nodes as well
        assertEquals(band.sorted(), band)
        assertEquals(listOf(0, 1, 2), band.distinct())
    }

    @Test
    fun `drawing put under the connections stays under connections added after it`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val underlay = PNode()
        onEdt { panel.addUnderConnections(underlay) }
        val arrays = List(2) { NeuronArray(3).also { network.addNetworkModel(it) } }
        network.addNetworkModel(WeightMatrix(arrays[0], arrays[1]))
        val (a, b) = network.addNeurons(2)
        network.addNetworkModel(Synapse(a, b))
        UiWork.awaitIdle()

        onEdt {
            val tier = panel.edgeTier.childrenReference
            assertEquals(panel.connectionUnderlay, underlay.parent)
            assertEquals(0, tier.indexOf(panel.connectionUnderlay))
            assertTrue(tier.any { (it as? ConnectionDrawing)?.owner is WeightMatrixNode } && tier.any { it is SynapseNode })
        }
    }
}
