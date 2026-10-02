/**
 * Model changes reach node visuals through dirty marks and one per-frame sync pass rather than an EDT task per event:
 * visuals still follow the model, bursts of changes coalesce, a spike shorter than a frame is still drawn, and a node
 * that leaves the canvas stops listening to its model.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.addNeurons
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.util.UiWork
import org.simbrain.util.countEdtTasks
import org.simbrain.workspace.Workspace
import java.util.concurrent.CountDownLatch
import javax.swing.SwingUtilities

class NodeSyncTest {

    private fun holdEdt(): CountDownLatch {
        val release = CountDownLatch(1)
        val held = CountDownLatch(1)
        SwingUtilities.invokeLater {
            held.countDown()
            release.await()
        }
        held.await()
        return release
    }

    @Test
    fun `a node draws the model's activation after the sync pass`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neuron = Neuron().also { network.addNetworkModel(it) }
        val node = panel.getNode(neuron) as NeuronNode

        neuron.activation = 0.7
        UiWork.awaitIdle()

        assertEquals(0.7, node.drawnActivation)
    }

    @Test
    fun `a burst of activation changes costs a few edt tasks, not one each`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neuron = Neuron().also { network.addNetworkModel(it) }
        val node = panel.getNode(neuron) as NeuronNode
        UiWork.awaitIdle()

        val tasks = countEdtTasks {
            val release = holdEdt()
            repeat(1000) { neuron.activation = it.toDouble() }
            release.countDown()
            // Sleep rather than poll, so only the work the changes caused is counted
            Thread.sleep(200)
        }
        UiWork.awaitIdle()

        assertTrue(tasks < 10, "1000 activation changes posted $tasks EDT tasks")
        assertEquals(999.0, node.drawnActivation)
    }

    @Test
    fun `a spike that ends before the next frame is still drawn`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neuron = Neuron(IntegrateAndFireRule()).also { network.addNetworkModel(it) }
        val node = panel.getNode(neuron) as NeuronNode
        UiWork.awaitIdle()

        val release = holdEdt()
        with(network) {
            neuron.isSpike = true
            neuron.isSpike = false
        }
        release.countDown()
        UiWork.awaitIdle()
        assertTrue(node.drawnSpiking, "a spike between frames was never drawn")

        with(network) { neuron.isSpike = false }
        UiWork.awaitIdle()
        assertFalse(node.drawnSpiking)
    }

    @Test
    fun `a deleted neuron's node stops listening and the node undo creates takes over`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neuron = Neuron().also { network.addNetworkModel(it) }
        val original = panel.getNode(neuron) as NeuronNode
        panel.selectionManager.set(original)

        panel.deleteSelectedObjects()
        UiWork.awaitIdle()
        assertFalse(original.nodeScope.coroutineContext[kotlinx.coroutines.Job]!!.isActive, "deleted node still subscribed")

        panel.undoManager.undo()
        val restored = panel.getNode(neuron) as NeuronNode
        assertNotSame(original, restored)
        neuron.activation = 0.4
        UiWork.awaitIdle()
        assertEquals(0.4, restored.drawnActivation)
        assertNotEquals(0.4, original.drawnActivation)
    }

    @Test
    fun `an unpaced run posts edt work per frame, not per iteration`() = runBlocking {
        val workspace = Workspace()
        val component = NetworkComponent("test")
        workspace.addWorkspaceComponent(component)
        val network = component.network
        NetworkPanel(component)
        val neurons = network.addNeurons(100)
        network.addNetworkModels(Sparse(0.1).connectNeurons(neurons, neurons))
        neurons.forEach { it.randomize() }
        UiWork.awaitIdle()

        val iterations = 3000
        val tasks = countEdtTasks {
            runBlocking { workspace.iterateSuspend(iterations) }
            Thread.sleep(200)
        }

        // Before mark/sync every changed neuron posted its own task: ~75 per iteration here
        assertTrue(tasks < iterations / 5, "$iterations iterations posted $tasks EDT tasks")
    }
}
