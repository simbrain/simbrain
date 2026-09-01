/**
 * GUI-path lifecycle coverage for [org.simbrain.network.core.GapJunction]: delete/undo/redo of the
 * junction itself and via an endpoint neuron, and clipboard copy/paste including the stranded case.
 */
package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.GapJunction
import org.simbrain.network.core.Neuron
import org.simbrain.network.updaterules.ContinuousSigmoidalRule

class GapJunctionUndoClipboardTest : NetworkPanelDeleteUndoTestBase() {

    private suspend fun buildJunction(): Triple<Neuron, Neuron, GapJunction> {
        val a = Neuron(ContinuousSigmoidalRule()).apply { x = 0.0; y = 0.0 }
        val b = Neuron(ContinuousSigmoidalRule()).apply { x = 100.0; y = 0.0 }
        network.addNetworkModel(a)
        network.addNetworkModel(b)
        val junction = GapJunction(a, b, 2.62)
        network.addNetworkModel(junction)
        return Triple(a, b, junction)
    }

    @Test
    fun `delete a gap junction then undo restores it and redo re-deletes it`() = runBlocking {
        val (a, b, junction) = buildJunction()

        selectOnly(junction)
        panel.deleteSelectedObjects()
        assertTrue(network.getModels<GapJunction>().isEmpty())
        assertTrue(a.gapJunctions.isEmpty() && b.gapJunctions.isEmpty())

        panel.undoManager.undo()
        assertEquals(listOf(junction), network.getModels<GapJunction>().toList())
        assertTrue(junction in a.gapJunctions && junction in b.gapJunctions, "undo must re-register both endpoints")

        panel.undoManager.redo()
        assertTrue(network.getModels<GapJunction>().isEmpty())
        assertTrue(a.gapJunctions.isEmpty() && b.gapJunctions.isEmpty())
    }

    @Test
    fun `deleting an endpoint neuron then undo restores the junction`() = runBlocking {
        val (a, b, junction) = buildJunction()

        selectOnly(a)
        panel.deleteSelectedObjects()
        assertTrue(network.getModels<GapJunction>().isEmpty())
        assertTrue(b.gapJunctions.isEmpty())

        panel.undoManager.undo()
        assertEquals(2, network.getModels<Neuron>().size)
        assertEquals(listOf(junction), network.getModels<GapJunction>().toList())
        assertTrue(junction in a.gapJunctions && junction in b.gapJunctions)
    }

    @Test
    fun `pasting two neurons with a gap junction copies the junction onto the pasted neurons`() = runBlocking {
        val (a, b, junction) = buildJunction()

        Clipboard.add(listOf(a, b, junction))
        Clipboard.paste(panel)

        assertEquals(4, network.getModels<Neuron>().size)
        assertEquals(2, network.getModels<GapJunction>().size)
        val pasted = network.getModels<GapJunction>().first { it !== junction }
        assertEquals(2.62, pasted.conductance, 0.0)
        assertTrue(pasted.neuron1 !== a && pasted.neuron1 !== b, "pasted junction must connect the pasted neurons")
        assertTrue(pasted.connects(pasted.neuron1, pasted.neuron2))
        assertTrue(pasted in pasted.neuron1.gapJunctions && pasted in pasted.neuron2.gapJunctions)
    }

    @Test
    fun `pasting a gap junction without both endpoints drops it`() = runBlocking {
        val (a, _, junction) = buildJunction()

        Clipboard.add(listOf(a, junction))
        Clipboard.paste(panel)

        assertEquals(3, network.getModels<Neuron>().size, "only the copied neuron is pasted")
        assertEquals(1, network.getModels<GapJunction>().size, "the stranded junction must be dropped")
    }
}
