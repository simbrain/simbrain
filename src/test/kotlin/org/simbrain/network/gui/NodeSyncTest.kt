/**
 * Model changes reach node visuals through dirty marks and one per-frame sync pass rather than an EDT task per event:
 * neuron and synapse visuals still follow the model, bursts of changes coalesce, spikes (including ones shorter than a
 * frame, which show for one frame) reach neurons and their outgoing synapses, strength labels follow changes too small
 * to resize a synapse, text labels rewritten every iteration coalesce too, a node that leaves the canvas stops
 * listening to its model, and one node whose sync throws does not stop the others syncing.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.addNeurons
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.gui.nodes.ScreenElement
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.network.gui.nodes.TextNode
import org.simbrain.network.updaterules.IntegrateAndFireRule
import org.simbrain.util.UiWork
import org.simbrain.util.countEdtTasks
import org.simbrain.workspace.Workspace
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities
import kotlin.math.ceil

class NodeSyncTest {

    private class ProbeNode(panel: NetworkPanel, override val model: NetworkModel, val failFirstSync: Boolean) :
        ScreenElement(panel) {
        val syncs = AtomicInteger()
        private var failed = false
        override val isDraggable = false
        fun mark() = markDirty(1)
        override fun syncFromModel(bits: Int) {
            if (failFirstSync && !failed) {
                failed = true
                error("sync failure")
            }
            syncs.incrementAndGet()
        }
    }

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
    fun `a synapse node follows its neurons when they move`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val source = Neuron().also { network.addNetworkModel(it, usePlacementManager = false) }
        val target = Neuron().also { network.addNetworkModel(it, usePlacementManager = false) }
        source.location = java.awt.geom.Point2D.Double(0.0, 0.0)
        target.location = java.awt.geom.Point2D.Double(100.0, 0.0)
        val synapse = Synapse(source, target).also { network.addNetworkModel(it) }
        val node = panel.getNode(synapse) as SynapseNode
        UiWork.awaitIdle()
        SwingUtilities.invokeAndWait { }
        val before = node.globalFullBounds.centerX

        target.location = java.awt.geom.Point2D.Double(500.0, 0.0)
        var after = before
        val deadline = System.currentTimeMillis() + 3000
        while (after == before && System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait { after = node.globalFullBounds.centerX }
            Thread.sleep(10)
        }
        assertTrue(after > before + 100, "synapse node should move with its target neuron: $before -> $after")
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
    fun `a spike that ends before the next frame is drawn for that frame and cleared on the next`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neuron = Neuron(IntegrateAndFireRule()).also { network.addNetworkModel(it) }
        val node = panel.getNode(neuron) as NeuronNode
        UiWork.awaitIdle()

        // The spike starts and ends, then that frame's sync runs, all in one EDT task so no other sync slips between
        var drawnOnSpikeFrame = false
        SwingUtilities.invokeAndWait {
            with(network) {
                neuron.isSpike = true
                neuron.isSpike = false
            }
            node.syncPending()
            drawnOnSpikeFrame = node.drawnSpiking
        }
        UiWork.awaitIdle()

        assertTrue(drawnOnSpikeFrame, "a spike between frames was never drawn")
        assertFalse(node.drawnSpiking, "a spike that had already ended stayed drawn")
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

    @Test
    fun `a synapse redraws its strength after the sync pass`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val (a, b) = network.addNeurons(2)
        val synapse = Synapse(a, b).also { network.addNetworkModel(it) }
        val node = panel.getNode(synapse) as SynapseNode
        UiWork.awaitIdle()

        synapse.strength = -0.5
        UiWork.awaitIdle()
        assertEquals(NetworkPreferences.inhibitorySynapseColor, node.drawnCircleColor)

        synapse.strength = 0.5
        UiWork.awaitIdle()
        assertEquals(NetworkPreferences.excitatorySynapseColor, node.drawnCircleColor)
    }

    @Test
    fun `a neuron's spike shows on its outgoing synapse and clears`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val source = Neuron(IntegrateAndFireRule()).also { network.addNetworkModel(it) }
        val target = Neuron().also { network.addNetworkModel(it) }
        val synapse = Synapse(source, target).also { network.addNetworkModel(it) }
        val node = panel.getNode(synapse) as SynapseNode
        UiWork.awaitIdle()

        with(network) { source.isSpike = true }
        UiWork.awaitIdle()
        assertTrue(node.drawnSourceSpiking)

        with(network) { source.isSpike = false }
        UiWork.awaitIdle()
        assertFalse(node.drawnSourceSpiking)
    }

    @Test
    fun `an unpaced spiking run posts edt work per frame, not per spike event`() = runBlocking {
        val workspace = Workspace()
        val component = NetworkComponent("test")
        workspace.addWorkspaceComponent(component)
        val network = component.network
        NetworkPanel(component)
        val neurons = network.addNeurons(100) {
            updateRule = IntegrateAndFireRule().apply { backgroundCurrent = 20.0 }
        }
        network.addNetworkModels(Sparse(0.1).connectNeurons(neurons, neurons))
        UiWork.awaitIdle()

        val iterations = 3000
        val tasks = countEdtTasks {
            runBlocking { workspace.iterateSuspend(iterations) }
            Thread.sleep(200)
        }

        // Every synapse used to listen to its source's spike event, which fires every iteration: ~990 tasks each
        assertTrue(tasks < iterations / 5, "$iterations spiking iterations posted $tasks EDT tasks")
    }

    @Test
    fun `a burst of text updates costs a few edt tasks and shows the last text`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val text = NetworkTextObject("start").also { network.addNetworkModel(it) }
        val node = panel.getNode(text) as TextNode
        UiWork.awaitIdle()

        val tasks = countEdtTasks {
            val release = holdEdt()
            repeat(1000) { text.text = "update $it" }
            release.countDown()
            // Sleep rather than poll, so only the work the updates caused is counted
            Thread.sleep(200)
        }
        UiWork.awaitIdle()

        assertTrue(tasks < 10, "1000 text updates posted $tasks EDT tasks")
        val document = node.pStyledText.document
        assertEquals("update 999", document.getText(0, document.length))
    }

    @Test
    fun `a strength change too small to see does not redraw the synapse`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val (a, b) = network.addNeurons(2)
        val synapse = Synapse(a, b).also { network.addNetworkModel(it) }
        val node = panel.getNode(synapse) as SynapseNode
        synapse.strength = 0.5
        UiWork.awaitIdle()
        val drawn = node.drawnDiameter

        synapse.strength = 0.5001
        UiWork.awaitIdle()
        assertEquals(drawn, node.drawnDiameter, "sub-pixel drift redrew the synapse")

        synapse.strength = synapse.upperBound
        UiWork.awaitIdle()
        assertNotEquals(drawn, node.drawnDiameter, "a visible strength change did not redraw")
    }

    @Test
    fun `a strength label follows a change too small to resize the circle`() = runBlocking {
        assumeTrue(NetworkPreferences.showSynapseStrengthLabels, "strength labels are turned off in the preferences")
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val (a, b) = network.addNeurons(2)
        // A wide strength range makes the change below far too small to resize the circle
        val synapse = Synapse(a, b).apply { upperBound = 1000.0 }.also { network.addNetworkModel(it) }
        synapse.strength = 1.0
        val node = panel.getNode(synapse) as SynapseNode
        UiWork.awaitIdle()
        // Zoomed in far enough to show labels, and painted twice so the connection tier holds a cached image
        SwingUtilities.invokeAndWait { panel.canvas.camera.setViewScale(5.0) }
        repeat(2) { paintConnectionTier(panel) }
        val before = node.drawnStrengthLabel
        assertNotNull(before)

        synapse.strength = 5.0
        UiWork.awaitIdle()
        paintConnectionTier(panel)
        assertNotNull(node.drawnStrengthLabel)
        assertNotEquals(before, node.drawnStrengthLabel, "the cached tier kept the old strength label")
    }

    /** Paints the connection tier as the canvas would at the panel's zoom, so its raster cache is used or rebuilt. */
    private fun paintConnectionTier(panel: NetworkPanel) = SwingUtilities.invokeAndWait {
        val tier = panel.edgeTier
        // What the canvas does before each paint: brings bounds up to date and turns invalidations into repaints
        panel.canvas.root.processInputs()
        val scale = panel.scalingFactor
        val bounds = tier.fullBounds
        val image = BufferedImage(
            ceil((bounds.width + 10) * scale).toInt(), ceil((bounds.height + 10) * scale).toInt(),
            BufferedImage.TYPE_INT_RGB
        )
        val g = image.createGraphics()
        g.clip = Rectangle(0, 0, image.width, image.height)
        g.scale(scale, scale)
        g.translate(5 - bounds.x, 5 - bounds.y)
        tier.fullPaint(PPaintContext(g))
        g.dispose()
    }

    @Test
    fun `a node queued behind one whose sync throws keeps syncing`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val failing = ProbeNode(panel, Neuron(), failFirstSync = true)
        val other = ProbeNode(panel, Neuron(), failFirstSync = false)
        SwingUtilities.invokeAndWait {
            panel.canvas.layer.addChild(failing)
            panel.canvas.layer.addChild(other)
        }
        UiWork.awaitIdle()

        // Both marks land in one sync pass, with the failing node first
        val release = holdEdt()
        failing.mark()
        other.mark()
        release.countDown()
        UiWork.awaitIdle()

        other.mark()
        UiWork.awaitIdle()
        assertEquals(2, other.syncs.get())
    }
}
