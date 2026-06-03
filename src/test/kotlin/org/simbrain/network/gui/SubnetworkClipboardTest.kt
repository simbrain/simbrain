package org.simbrain.network.gui

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.gui.nodes.NeuronNode
import org.simbrain.network.subnetworks.*
import org.simbrain.workspace.gui.SimbrainDesktop

class SubnetworkClipboardTest {

    private val network: Network
    private val networkComponent: NetworkComponent

    init {
        SimbrainDesktop.workspace.clearWorkspace()
        Clipboard.clear()
        network = Network()
        networkComponent = NetworkComponent("Test", network)
        SimbrainDesktop.workspace.addWorkspaceComponent(networkComponent)
    }

    private val panel: NetworkPanel by lazy {
        runBlocking { (SimbrainDesktop.getDesktopComponent(networkComponent) as NetworkDesktopComponent).networkPanel }
    }

    private fun subnetCount() = network.getModels<Subnetwork>().size
    private fun freeTextCount() = network.getModels<NetworkTextObject>().size
    private fun neuronNodeCount() = panel.filterScreenElements<NeuronNode>().size

    /** Node removal on delete is fire-and-forget; poll until the canvas settles to [expected]. */
    private suspend fun awaitNeuronNodeCount(expected: Int) {
        repeat(60) {
            if (neuronNodeCount() == expected) return
            delay(50)
        }
        assertEquals(expected, neuronNodeCount(), "Leftover neuron nodes on canvas (async removal did not settle)")
    }

    private suspend fun duplicate(subnet: Subnetwork) {
        Clipboard.clear()
        Clipboard.add(listOf(subnet))
        Clipboard.paste(panel)
    }

    @Test
    fun `competitive duplicate twice then undo removes all pasted neurons`() = runBlocking {
        val comp = CompetitiveNetwork(20, 20)
        network.addNetworkModel(comp)
        assertEquals(40, network.flatNeuronList.size)

        duplicate(comp)
        // The copy's neurons must be tracked in the model list (regression: previously stayed 40).
        assertEquals(2, subnetCount())
        assertEquals(80, network.flatNeuronList.size, "Pasted competitive neurons not registered in model list")

        val firstCopy = network.getModels<Subnetwork>().last()
        duplicate(firstCopy)
        assertEquals(3, subnetCount())
        assertEquals(120, network.flatNeuronList.size)

        panel.undoManager.undo()
        assertEquals(2, subnetCount(), "Undo should remove exactly the last pasted subnetwork")
        assertEquals(80, network.flatNeuronList.size, "Undo should remove the pasted neurons at the model level")
        awaitNeuronNodeCount(80)
    }

    @Test
    fun `som duplicate then undo removes all pasted neurons`() = runBlocking {
        val som = SOMNetwork(16, 20)
        network.addNetworkModel(som)
        val baseline = network.flatNeuronList.size
        assertEquals(36, baseline)

        duplicate(som)
        assertEquals(2, subnetCount())
        assertEquals(72, network.flatNeuronList.size, "Pasted SOM neurons not registered in model list")

        panel.undoManager.undo()
        assertEquals(1, subnetCount())
        assertEquals(36, network.flatNeuronList.size)
        awaitNeuronNodeCount(36)
    }

    @Test
    fun `hopfield duplicate then undo leaves no leftover`() = runBlocking {
        val hop = Hopfield(9)
        network.addNetworkModel(hop)
        assertEquals(9, network.flatNeuronList.size)

        duplicate(hop)
        assertEquals(2, subnetCount())
        assertEquals(18, network.flatNeuronList.size)

        panel.undoManager.undo()
        assertEquals(1, subnetCount())
        assertEquals(9, network.flatNeuronList.size)
        awaitNeuronNodeCount(9)
    }

    @Test
    fun `duplicating a subnetwork never copies its customInfo as a free text object`() = runBlocking {
        // Selecting a subnetwork via lasso also selects its customInfo (energy/state) InfoText node.
        // That InfoText must not be copied as a standalone NetworkTextObject.
        val hop = Hopfield(9)
        network.addNetworkModel(hop)
        assertEquals(0, freeTextCount())

        Clipboard.clear()
        Clipboard.add(listOfNotNull(hop, hop.customInfo as? NetworkModel))
        Clipboard.paste(panel)

        assertEquals(2, subnetCount())
        assertEquals(0, freeTextCount(), "Subnetwork customInfo leaked into the network as a free text object")
    }

    @Test
    fun `duplicating a SOM with info text selected does not leak free text`() = runBlocking {
        val som = SOMNetwork(16, 20)
        network.addNetworkModel(som)
        assertEquals(0, freeTextCount())

        Clipboard.clear()
        Clipboard.add(listOfNotNull(som, som.customInfo as? NetworkModel))
        Clipboard.paste(panel)

        assertEquals(2, subnetCount())
        assertEquals(0, freeTextCount(), "SOM customInfo leaked into the network as a free text object")
    }

    @Test
    fun `competitive paste undo redo restores a functional subnetwork`() = runBlocking {
        val comp = CompetitiveNetwork(20, 20)
        network.addNetworkModel(comp)

        duplicate(comp)
        panel.undoManager.undo()
        // Let the async node removal from undo settle before redo (as it would between user clicks).
        awaitNeuronNodeCount(40)
        panel.undoManager.redo()

        assertEquals(2, subnetCount())
        assertEquals(80, network.flatNeuronList.size)
        awaitNeuronNodeCount(80)

        val restored = network.getModels<Subnetwork>().last() as CompetitiveNetwork
        // Regression: redo previously left the subnetwork's NeuronCollections empty.
        assertEquals(20, restored.competitive.neuronList.size, "Competitive layer lost its neurons on redo")
        assertEquals(20, restored.inputLayer.neuronList.size, "Input layer lost its neurons on redo")

        // The restored network must actually update: a competitive layer settles to exactly one winner.
        restored.inputLayer.neuronList.forEach { it.activation = 0.5 }
        with(network) { repeat(3) { update() } }
        assertEquals(1, restored.competitive.neuronList.count { it.activation > 0.0 },
            "Restored competitive network should produce exactly one winner after redo")
    }

    @Test
    fun `som paste undo redo restores collection membership`() = runBlocking {
        val som = SOMNetwork(16, 20)
        network.addNetworkModel(som)

        duplicate(som)
        panel.undoManager.undo()
        awaitNeuronNodeCount(36)
        panel.undoManager.redo()

        assertEquals(2, subnetCount())
        awaitNeuronNodeCount(72)
        val restored = network.getModels<Subnetwork>().last() as SOMNetwork
        assertEquals(20, restored.som.neuronList.size, "SOM layer lost its neurons on redo")
        assertEquals(16, restored.inputLayer.neuronList.size, "Input layer lost its neurons on redo")
    }

    @Test
    fun `rbm duplicate then undo leaves no leftover subnetwork`() = runBlocking {
        val rbm = RestrictedBoltzmannMachine(25, 20)
        network.addNetworkModel(rbm)
        assertEquals(1, subnetCount())

        duplicate(rbm)
        assertEquals(2, subnetCount())
        assertEquals(0, freeTextCount())

        panel.undoManager.undo()
        assertEquals(1, subnetCount())
    }

    @Test
    fun `feedforward paste undo redo restores internal arrays and weight matrices`() = runBlocking {
        val feedForward = FeedForward(intArrayOf(2, 2), null)
        feedForward.inputLayer.isClamped = true
        feedForward.wmList.single().setWeights(doubleArrayOf(1.0, 0.0, 0.0, 1.0))
        network.addNetworkModel(feedForward)

        duplicate(feedForward)
        panel.undoManager.undo()
        panel.undoManager.redo()

        assertEquals(2, subnetCount())
        assertEquals(0, network.getModels<NeuronArray>().size, "Subnetwork layers should not be top-level arrays")
        assertEquals(0, network.getModels<WeightMatrix>().size, "Subnetwork connectors should not be top-level matrices")

        val restored = network.getModels<Subnetwork>().last() as FeedForward
        assertEquals(2, restored.layerList.size, "Feedforward layer list was not restored after redo")
        assertEquals(1, restored.wmList.size, "Feedforward weight matrix list was not restored after redo")
        assertEquals(restored.inputLayer, restored.wmList.single().source)
        assertEquals(restored.outputLayer, restored.wmList.single().target)

        restored.inputLayer.setActivations(doubleArrayOf(0.4, 0.7))
        restored.inputLayer.isClamped = true
        network.update()
        assertArrayEquals(doubleArrayOf(0.4, 0.7), restored.outputLayer.activationArray, 0.001)
    }

    @Test
    fun `backprop paste undo redo keeps children owned by restored subnetwork`() = runBlocking {
        val backprop = BackpropNetwork(intArrayOf(2, 3, 1), null)
        network.addNetworkModel(backprop)

        duplicate(backprop)
        assertEquals(2, subnetCount())
        assertEquals(0, network.getModels<NeuronArray>().size)
        assertEquals(0, network.getModels<WeightMatrix>().size)

        panel.undoManager.undo()
        assertEquals(1, subnetCount())
        assertEquals(0, network.getModels<NeuronArray>().size)
        assertEquals(0, network.getModels<WeightMatrix>().size)

        panel.undoManager.redo()
        assertEquals(2, subnetCount())
        assertEquals(0, network.getModels<NeuronArray>().size, "Redo leaked backprop layers as top-level arrays")
        assertEquals(0, network.getModels<WeightMatrix>().size, "Redo leaked backprop matrices as top-level connectors")

        val restored = network.getModels<Subnetwork>().last() as BackpropNetwork
        assertEquals(listOf(2, 3, 1), restored.layerList.map { it.size })
        assertEquals(2, restored.wmList.size)
        assertEquals(restored.inputLayer, restored.wmList.first().source)
        assertEquals(restored.outputLayer, restored.wmList.last().target)
    }
}
