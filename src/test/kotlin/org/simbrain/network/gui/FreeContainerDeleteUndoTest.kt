package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.SynapseGroup

/**
 * Deleting all members of a FREE, top-level container that self-deletes when empty — a [NeuronCollection]
 * (all neurons) or a [SynapseGroup] (all synapses) — through the GUI delete path, then undoing/redoing.
 * The container's empty-cascade is captured by [Network.deleteModels] (last member hits the
 * isLastChildOfParent branch), so undo must restore the container and re-group its members. For internal
 * components of composite models see [InternalComponentDeleteUndoTest].
 */
class FreeContainerDeleteUndoTest : NetworkPanelDeleteUndoTestBase() {

    @Test
    fun `delete all neurons of a free collection then undo restores the collection and regroups them`() = runBlocking {
        val neurons = List(4) { Neuron() }
        neurons.forEach { network.addNetworkModel(it) }
        val nc = NeuronCollection(neurons)
        network.addNetworkModel(nc)
        assertEquals(1, network.getModels<NeuronCollection>().size)
        assertEquals(4, nc.neuronList.size)

        selectOnly(neurons)
        panel.deleteSelectedObjects()
        assertEquals(0, network.getModels<NeuronCollection>().size, "a free collection auto-deletes when emptied")
        assertEquals(0, network.getModels<Neuron>().size)

        panel.undoManager.undo()
        assertEquals(4, network.getModels<Neuron>().size, "undo must restore the neurons")
        assertEquals(1, network.getModels<NeuronCollection>().size, "undo must restore the emptied collection")
        assertEquals(4, network.getModels<NeuronCollection>().first().neuronList.size,
            "the neurons must be put back into the collection")

        panel.undoManager.redo()
        assertEquals(0, network.getModels<NeuronCollection>().size, "redo must re-delete the collection")
        assertEquals(0, network.getModels<Neuron>().size, "redo must re-delete the neurons")
    }

    @Test
    fun `resize a free collection then empty it then undo twice regroups all neurons`() = runBlocking {
        val neurons = List(5) { Neuron() }
        neurons.forEach { network.addNetworkModel(it) }
        val nc = NeuronCollection(neurons)
        network.addNetworkModel(nc)

        // Delete 3 (a resize); the collection survives with 2.
        selectOnly(neurons.take(3))
        panel.deleteSelectedObjects()
        assertEquals(2, nc.neuronList.size)

        // Delete the remaining 2; the collection empties and is captured.
        selectOnly(neurons.drop(3))
        panel.deleteSelectedObjects()
        assertEquals(0, network.getModels<NeuronCollection>().size)

        // Undo the empty: the 2 neurons must come back INSIDE the collection, not as free neurons.
        panel.undoManager.undo()
        assertEquals(1, network.getModels<NeuronCollection>().size, "collection restored")
        assertEquals(2, network.getModels<NeuronCollection>().first().neuronList.size,
            "the last 2 neurons must be restored into the collection, not as free neurons")

        // Undo the resize: all 5 must be grouped.
        panel.undoManager.undo()
        assertEquals(5, network.getModels<NeuronCollection>().first().neuronList.size,
            "all five neurons must be regrouped into the collection")
    }

    @Test
    fun `delete all synapses of a free synapse group then undo restores the group`() = runBlocking {
        val srcNeurons = List(2) { Neuron() }
        srcNeurons.forEach { network.addNetworkModel(it) }
        val src = NeuronCollection(srcNeurons)
        network.addNetworkModel(src)
        val tgtNeurons = List(2) { Neuron() }
        tgtNeurons.forEach { network.addNetworkModel(it) }
        val tgt = NeuronCollection(tgtNeurons)
        network.addNetworkModel(tgt)
        val sg = SynapseGroup(src, tgt)
        network.addNetworkModel(sg)
        val synapseCount = sg.synapses.size
        assertTrue(synapseCount > 0, "the group should have synapses")
        assertEquals(1, network.getModels<SynapseGroup>().size)

        selectOnly(sg.synapses.toList())
        panel.deleteSelectedObjects()
        assertEquals(0, network.getModels<SynapseGroup>().size,
            "the group auto-deletes when all its synapses are removed")

        panel.undoManager.undo()
        assertEquals(1, network.getModels<SynapseGroup>().size, "undo must restore the synapse group")
        assertEquals(synapseCount, network.getModels<SynapseGroup>().first().synapses.size,
            "the synapses must be restored into the group")
    }
}
