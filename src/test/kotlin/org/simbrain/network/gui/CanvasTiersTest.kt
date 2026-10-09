/**
 * The network canvas is layered bottom to top as connections (a cached tier), spike highlights, then nodes: every
 * connection node, including subclasses such as directed synapse group arrows, lands in the connection tier, and
 * neurons and collections land above it.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.gui.nodes.NeuronCollectionNode
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.SynapseGroupNode
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.util.UiWork

class CanvasTiersTest {

    @Test
    fun `connections sit in the cached tier below spike highlights and nodes`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val (a, b) = network.addNeurons(2)
        network.addNetworkModel(Synapse(a, b))
        val source = NeuronCollection(network.addNeurons(3)).also { network.addNetworkModel(it) }
        val target = NeuronCollection(network.addNeurons(3)).also { network.addNetworkModel(it) }
        network.addNetworkModel(SynapseGroup(source, target))
        UiWork.awaitIdle()

        val layer = panel.canvas.layer
        val edgeIndex = layer.indexOfChild(panel.edgeTier)
        val overlayIndex = layer.indexOfChild(panel.spikeOverlay)
        val nodes = panel.screenElements
        val connections = nodes.filter { it is SynapseNode || it is SynapseGroupNode }
        val others = nodes.filter { it is NeuronNode || it is NeuronCollectionNode }

        assertTrue(connections.any { it is SynapseGroupNode } && connections.any { it is SynapseNode })
        connections.forEach { assertEquals(panel.edgeTier, it.parent, "${it::class.simpleName} is not in the connection tier") }
        others.forEach { node ->
            assertTrue(layer.indexOfChild(node.parent) > overlayIndex, "${node::class.simpleName} is not above the connections")
        }
        assertTrue(edgeIndex < overlayIndex)
    }
}
