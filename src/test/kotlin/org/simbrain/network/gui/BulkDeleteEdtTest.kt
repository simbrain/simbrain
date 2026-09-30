/**
 * Bulk deletion from the network panel must not wait on the EDT once per deleted item: with the canvas on
 * screen every such wait can queue behind a full repaint, which made deleting a few hundred synapses take
 * about a minute. These tests hold the EDT busy and require the delete to finish anyway.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import java.util.concurrent.CountDownLatch
import javax.swing.SwingUtilities

class BulkDeleteEdtTest {

    /**
     * Runs the panel's delete while the EDT is held, returning whether it finished before the hold was released.
     * A watchdog releases the EDT after a timeout, since a delete stuck on the EDT can't be cancelled while it
     * stays blocked.
     */
    private suspend fun deleteFinishesWhileEdtIsBlocked(panel: NetworkPanel): Boolean {
        val release = CountDownLatch(1)
        val blocked = CountDownLatch(1)
        SwingUtilities.invokeLater {
            blocked.countDown()
            release.await()
        }
        blocked.await()
        val watchdog = Thread {
            try {
                Thread.sleep(10_000)
            } catch (_: InterruptedException) {
            }
            release.countDown()
        }.apply { isDaemon = true; start() }
        try {
            withContext(Dispatchers.Default) { panel.deleteSelectedObjects() }
            return release.count == 1L
        } finally {
            release.countDown()
            watchdog.interrupt()
        }
    }

    @Test
    fun `deleting connected neurons does not wait on the edt per synapse`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neurons = List(10) { Neuron() }.also { network.addNetworkModels(it) }
        network.addNetworkModels(neurons.flatMap { a -> neurons.map { b -> Synapse(a, b) } })
        panel.selectionManager.set(neurons.map { panel.modelNodeMap.peek(it)!! })

        assertTrue(deleteFinishesWhileEdtIsBlocked(panel), "delete stalled behind the blocked EDT")
        assertEquals(0, network.freeSynapses.size)
    }

    @Test
    fun `deleting a neuron collection with its neurons does not wait on the edt per neuron`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neurons = List(20) { Neuron() }.also { network.addNetworkModels(it) }
        val collection = NeuronCollection(neurons).also { network.addNetworkModel(it) }
        panel.selectionManager.set((neurons + collection).map { panel.modelNodeMap.peek(it)!! })

        assertTrue(deleteFinishesWhileEdtIsBlocked(panel), "delete stalled behind the blocked EDT")
        assertEquals(0, network.freeNeurons.size)
    }
}
