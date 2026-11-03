/**
 * Bulk deletion from the network panel must not wait on the EDT once per deleted item: with the canvas on
 * screen every such wait can queue behind a full repaint, which made deleting a few hundred synapses take
 * about a minute. These tests hold the EDT busy and require the delete to finish anyway.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.util.finishesWhileEdtIsBlocked

class BulkDeleteEdtTest {

    @Test
    fun `deleting connected neurons does not wait on the edt per synapse`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neurons = List(10) { Neuron() }.also { network.addNetworkModels(it) }
        network.addNetworkModels(neurons.flatMap { a -> neurons.map { b -> Synapse(a, b) } })
        panel.selectionManager.set(neurons.map { panel.modelNodeMap.peek(it)!! })

        assertTrue(finishesWhileEdtIsBlocked { panel.deleteSelectedObjects() }, "delete stalled behind the blocked EDT")
        assertEquals(0, network.freeSynapses.size)
    }

    @Test
    fun `deleting a neuron collection with its neurons does not wait on the edt per neuron`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neurons = List(20) { Neuron() }.also { network.addNetworkModels(it) }
        val collection = NeuronCollection(neurons).also { network.addNetworkModel(it) }
        panel.selectionManager.set((neurons + collection).map { panel.modelNodeMap.peek(it)!! })

        assertTrue(finishesWhileEdtIsBlocked { panel.deleteSelectedObjects() }, "delete stalled behind the blocked EDT")
        assertEquals(0, network.freeNeurons.size)
    }
}
