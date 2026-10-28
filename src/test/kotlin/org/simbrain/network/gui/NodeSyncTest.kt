/**
 * Model changes reach node visuals through dirty marks and one per-frame sync pass rather than an EDT task per event:
 * neuron and synapse visuals still follow the model, bursts of changes coalesce, spikes (including ones shorter than a
 * frame, which show for one frame) reach neurons and their outgoing synapses, the spike overlay follows highlighted
 * synapses and keeps their strength labels, strength labels follow changes too small to resize a synapse, text labels
 * rewritten every iteration coalesce too, a node that leaves the canvas stops listening to its model, and one node
 * whose sync throws does not stop the others syncing.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.piccolo2d.PNode
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
import org.simbrain.util.awaitUntil
import org.simbrain.util.countEdtTasks
import org.simbrain.util.onEdt
import org.simbrain.util.point
import org.simbrain.util.withEdtHeld
import org.simbrain.workspace.Workspace
import java.awt.Color
import java.awt.Rectangle
import java.awt.image.BufferedImage
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

    @Test
    fun `a synapse node follows its neurons when they move`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val source = Neuron().also { network.addNetworkModel(it, usePlacementManager = false) }
        val target = Neuron().also { network.addNetworkModel(it, usePlacementManager = false) }
        source.location = point(0.0, 0.0)
        target.location = point(100.0, 0.0)
        val synapse = Synapse(source, target).also { network.addNetworkModel(it) }
        val node = panel.getNode(synapse) as SynapseNode
        UiWork.awaitIdle()
        val before = onEdt { node.globalFullBounds.centerX }

        target.location = point(500.0, 0.0)
        awaitUntil(3000, "synapse node should move with its target neuron") {
            onEdt { node.globalFullBounds.centerX } > before + 100
        }
    }

    @Test
    fun `a node draws the model's activation after the sync pass`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neuron = Neuron().also { network.addNetworkModel(it) }
        val node = panel.getNode(neuron) as NeuronNode

        neuron.activation = 0.7
        UiWork.awaitIdle()

        assertEquals(0.7, onEdt { node.drawnActivation })
    }

    @Test
    fun `a burst of activation changes costs a few edt tasks, not one each`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neuron = Neuron().also { network.addNetworkModel(it) }
        val node = panel.getNode(neuron) as NeuronNode
        UiWork.awaitIdle()

        val tasks = countEdtTasks(settleMs = 200) {
            withEdtHeld { repeat(1000) { neuron.activation = it.toDouble() } }
        }
        UiWork.awaitIdle()

        assertTrue(tasks < 10, "1000 activation changes posted $tasks EDT tasks")
        assertEquals(999.0, onEdt { node.drawnActivation })
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
        assertFalse(onEdt { node.drawnSpiking }, "a spike that had already ended stayed drawn")
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
        assertFalse(original.nodeScope.isActive, "deleted node still subscribed")

        panel.undoManager.undo()
        val restored = panel.getNode(neuron) as NeuronNode
        assertNotSame(original, restored)
        neuron.activation = 0.4
        UiWork.awaitIdle()
        assertEquals(0.4, onEdt { restored.drawnActivation })
        assertNotEquals(0.4, onEdt { original.drawnActivation })
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
        val tasks = countEdtTasks(settleMs = 200) {
            runBlocking { workspace.iterateSuspend(iterations) }
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
        assertEquals(NetworkPreferences.inhibitorySynapseColor, onEdt { node.drawnCircleColor })

        synapse.strength = 0.5
        UiWork.awaitIdle()
        assertEquals(NetworkPreferences.excitatorySynapseColor, onEdt { node.drawnCircleColor })
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
        assertTrue(onEdt { node.drawnSourceSpiking })

        with(network) { source.isSpike = false }
        UiWork.awaitIdle()
        assertFalse(onEdt { node.drawnSourceSpiking })
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
        val tasks = countEdtTasks(settleMs = 200) {
            runBlocking { workspace.iterateSuspend(iterations) }
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

        val tasks = countEdtTasks(settleMs = 200) {
            withEdtHeld { repeat(1000) { text.text = "update $it" } }
        }
        UiWork.awaitIdle()

        assertTrue(tasks < 10, "1000 text updates posted $tasks EDT tasks")
        val shown = onEdt { node.pStyledText.document.let { it.getText(0, it.length) } }
        assertEquals("update 999", shown)
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
        val drawn = onEdt { node.drawnDiameter }

        synapse.strength = 0.5001
        UiWork.awaitIdle()
        assertEquals(drawn, onEdt { node.drawnDiameter }, "sub-pixel drift redrew the synapse")

        synapse.strength = synapse.upperBound
        UiWork.awaitIdle()
        assertNotEquals(drawn, onEdt { node.drawnDiameter }, "a visible strength change did not redraw")
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
        repeat(2) { paintTier(panel, panel.edgeTier) }
        val before = onEdt { node.drawnStrengthLabel }
        assertNotNull(before)

        synapse.strength = 5.0
        UiWork.awaitIdle()
        paintTier(panel, panel.edgeTier)
        val after = onEdt { node.drawnStrengthLabel }
        assertNotNull(after)
        assertNotEquals(before, after, "the cached tier kept the old strength label")
    }

    /**
     * Paints one tier of the canvas as the canvas would at the panel's zoom (so the connection tier's raster cache is
     * used or rebuilt), into an image with a 5-unit margin around the tier's bounds.
     */
    private fun paintTier(panel: NetworkPanel, tier: PNode): BufferedImage = onEdt {
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
        image
    }

    @Test
    fun `a highlighted synapse stays inside the spike overlay's bounds when its neuron moves`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val source = Neuron(IntegrateAndFireRule()).also { network.addNetworkModel(it, usePlacementManager = false) }
        val target = Neuron().also { network.addNetworkModel(it, usePlacementManager = false) }
        source.location = point(0.0, 0.0)
        target.location = point(100.0, 0.0)
        val synapse = Synapse(source, target).also { network.addNetworkModel(it) }
        val node = panel.getNode(synapse) as SynapseNode
        with(network) { source.isSpike = true }
        UiWork.awaitIdle()
        assertTrue(onEdt { node.drawnSourceSpiking })

        target.location = point(500.0, 0.0)
        awaitUntil(3000, "the synapse node never followed its target") {
            onEdt { node.circleNode!!.globalFullBounds.centerX > 400 }
        }

        val covered = onEdt { panel.spikeOverlay.globalFullBounds.contains(node.globalFullBounds) }
        assertTrue(covered, "the overlay would be culled where the highlighted synapse now is")
    }

    @Test
    fun `a highlighted synapse keeps its strength label`() = runBlocking {
        assumeTrue(NetworkPreferences.showSynapseStrengthLabels, "strength labels are turned off in the preferences")
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val source = Neuron(IntegrateAndFireRule()).also { network.addNetworkModel(it) }
        val target = Neuron().also { network.addNetworkModel(it) }
        val synapse = Synapse(source, target).also { network.addNetworkModel(it) }
        synapse.strength = 1.0
        val node = panel.getNode(synapse) as SynapseNode
        with(network) { source.isSpike = true }
        UiWork.awaitIdle()
        assertTrue(onEdt { node.drawnSourceSpiking })
        SwingUtilities.invokeAndWait { panel.canvas.camera.setViewScale(5.0) }

        // The overlay draws the circle back over the cached one; the middle of the circle must show the label's
        // pixels, not only the circle's fill
        val image = paintTier(panel, panel.spikeOverlay)
        val labelDrawn = onEdt {
            val scale = panel.scalingFactor
            val overlay = panel.spikeOverlay.globalFullBounds
            val circle = node.circleNode!!.globalFullBounds
            val cx = ((circle.centerX - overlay.x + 5) * scale).toInt()
            val cy = ((circle.centerY - overlay.y + 5) * scale).toInt()
            val half = (circle.width / 2 * scale * 0.3).toInt()
            val fill = (node.drawnCircleColor as Color).rgb
            (cx - half..cx + half).any { x -> (cy - half..cy + half).any { y -> image.getRGB(x, y) != fill } }
        }
        assertTrue(labelDrawn, "the spike overlay covered the strength label")
    }

    @Test
    fun `in spiking only mode a synapse can be clicked while its source spikes`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val source = Neuron(IntegrateAndFireRule()).also { network.addNetworkModel(it) }
        val target = Neuron().also { network.addNetworkModel(it) }
        target.location = point(200.0, 0.0)
        val synapse = Synapse(source, target).also { network.addNetworkModel(it) }
        val node = panel.getNode(synapse) as SynapseNode
        panel.synapseSpikingOnlyVisible = true
        UiWork.awaitIdle()

        fun pickedAtCircle(): Boolean = onEdt {
            val camera = panel.canvas.camera
            camera.setBounds(0.0, 0.0, 800.0, 800.0)
            panel.canvas.root.processInputs()
            val center = node.circleNode!!.globalFullBounds.center2D
            val local = camera.viewToLocal(point(center.x, center.y))
            camera.pick(local.x, local.y, 1.0).nodeStackReference.contains(node)
        }

        assertFalse(pickedAtCircle(), "a hidden synapse should not take clicks")
        with(network) { source.isSpike = true }
        UiWork.awaitIdle()
        assertTrue(onEdt { node.drawnSourceSpiking })
        assertTrue(pickedAtCircle(), "the spiking synapse the overlay shows should take clicks")
    }

    @Test
    fun `a node removed from the canvas no longer joins the selection`() = runBlocking {
        val network = Network()
        val panel = NetworkPanel(NetworkComponent("test", network))
        val neuron = Neuron().also { network.addNetworkModel(it) }
        val removed = panel.getNode(neuron)
        neuron.delete()
        UiWork.awaitIdle()
        // Undo puts the same model back, and it gets a new node
        network.addNetworkModel(neuron)
        val current = panel.getNode(neuron)
        assertNotSame(removed, current)

        neuron.select()
        awaitUntil(message = "the current node never joined the selection") {
            onEdt { current in panel.selectionManager.selection }
        }
        // The selected event reaches every subscriber at about the same time; a stale one can only be caught joining
        // late, so give it the same chance before checking it stayed out
        Thread.sleep(200)
        assertEquals(setOf(current), onEdt { panel.selectionManager.selection.toSet() })
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
        withEdtHeld {
            failing.mark()
            other.mark()
        }
        UiWork.awaitIdle()

        other.mark()
        UiWork.awaitIdle()
        assertEquals(2, other.syncs.get())
    }
}
