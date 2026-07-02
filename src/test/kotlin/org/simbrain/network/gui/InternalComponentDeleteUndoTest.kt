package org.simbrain.network.gui

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.gui.nodes.NeuronArrayNode
import org.simbrain.network.gui.nodes.SynapseGroupNode
import org.simbrain.network.gui.nodes.SynapseNode
import org.simbrain.network.subnetworks.*
import org.simbrain.network.trainers.SupervisedModel

/**
 * Deleting a single INTERNAL component of a composite model (a subnetwork or a supervised model) through
 * the GUI delete path, then undoing and redoing. Unlike the paste path (covered by [ClipboardTest] and
 * [SubnetworkClipboardTest]), the delete->undo->redo path for nested components is otherwise unverified.
 * Each test asserts the round-trip restores the exact pre-delete structure and that the network still
 * functions after restore. Free top-level containers are covered by [FreeContainerDeleteUndoTest].
 */
class InternalComponentDeleteUndoTest : NetworkPanelDeleteUndoTestBase() {

    @Test
    fun `delete neuron inside subnetwork collection then undo restores it into the collection`() = runBlocking {
        val comp = CompetitiveNetwork(5, 4)
        network.addNetworkModel(comp)
        assertEquals(4, comp.competitive.neuronList.size)
        assertEquals(20, comp.weights.synapses.size)

        val victim = comp.competitive.neuronList[1]
        val victimId = victim.id
        selectOnly(victim)
        panel.deleteSelectedObjects()
        assertEquals(3, comp.competitive.neuronList.size, "neuron not removed from collection on delete")
        assertEquals(1, network.getModels<Subnetwork>().size, "deleting one neuron must not remove the subnetwork")

        panel.undoManager.undo()
        assertEquals(4, comp.competitive.neuronList.size, "undo must restore the neuron INTO its collection")
        assertEquals(20, comp.weights.synapses.size, "undo must restore the neuron's synapses into the group")
        assertEquals(1, network.getModels<Subnetwork>().size)
        assertEquals(0, network.getModels<Neuron>().count { it.id == victimId },
            "restored neuron leaked as a free top-level model instead of into the subnetwork")

        panel.undoManager.redo()
        assertEquals(3, comp.competitive.neuronList.size, "redo must re-remove the neuron")
    }

    @Test
    fun `delete several neurons from a subnetwork collection then undo restores all of them`() = runBlocking {
        val comp = CompetitiveNetwork(5, 4)
        network.addNetworkModel(comp)
        assertEquals(4, comp.competitive.neuronList.size)
        assertEquals(20, comp.weights.synapses.size)

        // Two of the four competitive neurons (a subset, not the whole collection), deleted together.
        val victims = listOf(comp.competitive.neuronList[0], comp.competitive.neuronList[2])
        val victimIds = victims.map { it.id }.toSet()
        selectOnly(victims)
        panel.deleteSelectedObjects()
        assertEquals(2, comp.competitive.neuronList.size, "both selected neurons should be removed")
        assertEquals(10, comp.weights.synapses.size, "synapses incident to the deleted neurons should be removed")
        assertEquals(1, network.getModels<Subnetwork>().size, "deleting a subset of neurons must not remove the subnetwork")

        panel.undoManager.undo()
        assertEquals(4, comp.competitive.neuronList.size, "undo must restore BOTH neurons into the collection")
        assertEquals(20, comp.weights.synapses.size, "undo must restore all incident synapses into the group")
        assertEquals(0, network.getModels<Neuron>().count { it.id in victimIds },
            "restored neurons leaked as free top-level models instead of into the subnetwork")

        panel.undoManager.redo()
        assertEquals(2, comp.competitive.neuronList.size, "redo must re-remove both neurons")
        assertEquals(10, comp.weights.synapses.size)
    }

    @Test
    fun `delete neurons from two different collections in one subnetwork then undo restores both`() = runBlocking {
        val comp = CompetitiveNetwork(5, 4)
        network.addNetworkModel(comp)
        assertEquals(5, comp.inputLayer.neuronList.size)
        assertEquals(4, comp.competitive.neuronList.size)
        assertEquals(20, comp.weights.synapses.size)

        // One neuron from the input layer and one from the competitive layer, deleted together.
        selectOnly(listOf(comp.inputLayer.neuronList[0], comp.competitive.neuronList[0]))
        panel.deleteSelectedObjects()
        assertEquals(4, comp.inputLayer.neuronList.size, "input neuron should be removed")
        assertEquals(3, comp.competitive.neuronList.size, "competitive neuron should be removed")
        assertEquals(1, network.getModels<Subnetwork>().size)

        panel.undoManager.undo()
        assertEquals(5, comp.inputLayer.neuronList.size, "undo must restore the input neuron into its collection")
        assertEquals(4, comp.competitive.neuronList.size, "undo must restore the competitive neuron into its collection")
        assertEquals(20, comp.weights.synapses.size, "undo must restore all incident synapses")
        assertEquals(0, network.getModels<Neuron>().size, "no subnetwork neuron should leak to the top level")
    }

    @Test
    fun `delete neuron array from feedforward then undo restores its node on the canvas`() = runBlocking {
        val ff = FeedForward(intArrayOf(2, 2, 2), null)
        network.addNetworkModel(ff)
        val hidden = ff.layerList[1]
        fun hiddenNodePresent() = panel.filterScreenElements<NeuronArrayNode>().any { it.neuronArray === hidden }
        awaitUntil { hiddenNodePresent() }
        assertTrue(hiddenNodePresent(), "precondition: the hidden array's node should exist on the canvas")

        selectOnly(hidden)
        panel.deleteSelectedObjects()
        // Let the asynchronous, debounced node removal actually settle, as it does in a real session
        // before the user presses undo.
        awaitUntil { !hiddenNodePresent() }
        assertFalse(hiddenNodePresent(), "the array's node should be removed from the canvas after delete")

        panel.undoManager.undo()
        awaitUntil { hiddenNodePresent() }
        assertTrue(hiddenNodePresent(), "undo must bring the array's node back onto the canvas")
    }

    @Test
    fun `delete hidden layer of feedforward then undo restores a functional network`() = runBlocking {
        val ff = FeedForward(intArrayOf(2, 2, 2), null)
        ff.wmList.forEach { it.setWeights(doubleArrayOf(1.0, 0.0, 0.0, 1.0)) }
        ff.inputLayer.isClamped = true
        network.addNetworkModel(ff)
        assertEquals(5, ff.modelList.size)

        ff.inputLayer.setActivations(doubleArrayOf(0.4, 0.7))
        network.update()
        val expected = ff.outputLayer.activationArray.clone()

        val hidden = ff.layerList[1]
        selectOnly(hidden)
        panel.deleteSelectedObjects()
        assertTrue(ff.modelList.size < 5, "deleting the hidden layer should cascade to its weight matrices")

        panel.undoManager.undo()
        assertEquals(5, ff.modelList.size, "undo must restore the layer and its weight matrices")
        assertEquals(0, network.getModels<NeuronArray>().size, "restored layer leaked as a top-level array")
        assertEquals(0, network.getModels<WeightMatrix>().size, "restored matrices leaked as top-level connectors")

        ff.inputLayer.setActivations(doubleArrayOf(0.4, 0.7))
        ff.inputLayer.isClamped = true
        network.update()
        assertArrayEquals(expected, ff.outputLayer.activationArray, 0.001,
            "restored feedforward must propagate activations as it did before deletion")
    }

    @Test
    fun `delete weight matrix of supervised model then undo restores a functional model`() = runBlocking {
        val source = NeuronArray(3).apply { isClamped = true }
        val target = NeuronArray(2)
        network.addNetworkModel(source)
        network.addNetworkModel(target)
        val wm = WeightMatrix(source, target)
        wm.setWeights(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0))
        network.addNetworkModel(wm)
        val supervised = SupervisedModel(source, target)
        network.addNetworkModel(supervised)

        source.setActivations(doubleArrayOf(0.3, 0.6, 0.9))
        repeat(3) { network.update() }
        val expected = target.activationArray.clone()

        selectOnly(wm)
        panel.deleteSelectedObjects()
        assertEquals(0, network.getModels<SupervisedModel>().size,
            "deleting an internal matrix should remove the supervised model overlay")

        panel.undoManager.undo()
        assertEquals(1, network.getModels<SupervisedModel>().size, "undo must restore the supervised model")
        assertEquals(1, network.getModels<WeightMatrix>().size, "undo must restore the weight matrix")

        source.setActivations(doubleArrayOf(0.3, 0.6, 0.9))
        source.isClamped = true
        repeat(3) { network.update() }
        assertArrayEquals(expected, target.activationArray, 0.001,
            "restored supervised model must propagate activations as it did before deletion")

        panel.undoManager.redo()
        assertEquals(0, network.getModels<SupervisedModel>().size, "redo must remove the supervised model again")
    }

    // A free deletable item is included alongside the protected models in these tests so the selection
    // stays non-empty and the (modal, blocking) warning dialog for an all-protected selection isn't hit.

    @Test
    fun `subnetworkProtectedModels identifies structural collections and full neuron sets`() = runBlocking {
        val net = Network()
        val hop = Hopfield(9)
        net.addNetworkModel(hop)
        val neurons = hop.neuronGroup.neuronList.toList()

        assertEquals(setOf<NetworkModel>(hop.neuronGroup), net.subnetworkProtectedModels(listOf(hop.neuronGroup)),
            "a subnetwork-owned collection is protected")
        assertEquals(neurons.toSet(), net.subnetworkProtectedModels(neurons),
            "all of a subnetwork collection's neurons are protected")
        assertTrue(net.subnetworkProtectedModels(neurons.take(3)).isEmpty(),
            "a partial neuron selection (a resize) is not protected")
        assertTrue(net.subnetworkProtectedModels(emptyList()).isEmpty())
    }

    @Test
    fun `a subnetwork's neuron collection cannot be deleted on its own`() = runBlocking {
        val hop = Hopfield(9)
        network.addNetworkModel(hop)
        val freeNeuron = Neuron()
        network.addNetworkModel(freeNeuron)

        selectOnly(listOf(hop.neuronGroup, freeNeuron))
        panel.deleteSelectedObjects()

        assertEquals(0, network.getModels<Neuron>().size, "the free neuron should be deleted")
        assertTrue(hop.neuronGroup in hop.modelList.all, "the subnetwork's collection must NOT be ungrouped")
        assertEquals(9, hop.neuronGroup.neuronList.size, "the collection's neurons must be untouched")
        assertEquals(1, network.getModels<Subnetwork>().size, "the subnetwork must remain intact")
    }

    @Test
    fun `deleting all neurons of a subnetwork collection is disallowed`() = runBlocking {
        val hop = Hopfield(9)
        network.addNetworkModel(hop)
        val freeNeuron = Neuron()
        network.addNetworkModel(freeNeuron)

        selectOnly(hop.neuronGroup.neuronList + freeNeuron)
        panel.deleteSelectedObjects()

        assertEquals(0, network.getModels<Neuron>().size, "the free neuron should be deleted")
        assertEquals(9, hop.neuronGroup.neuronList.size, "emptying a subnetwork collection must be disallowed")
        assertEquals(1, network.getModels<Subnetwork>().size, "the subnetwork must remain intact")
    }

    @Test
    fun `cannot empty a subnetwork collection even after a prior resize`() = runBlocking {
        val hop = Hopfield(9)
        network.addNetworkModel(hop)
        val freeNeuron = Neuron()
        network.addNetworkModel(freeNeuron)

        // First delete one neuron (an allowed resize). Protection reads live modelList membership (not the
        // childToParentMap), so it must still recognize the collection as subnetwork-owned afterwards.
        selectOnly(hop.neuronGroup.neuronList.take(1))
        panel.deleteSelectedObjects()
        assertEquals(8, hop.neuronGroup.neuronList.size)

        // Now attempt to delete the remaining neurons; emptying the collection must still be disallowed.
        selectOnly(hop.neuronGroup.neuronList + freeNeuron)
        panel.deleteSelectedObjects()
        assertEquals(8, hop.neuronGroup.neuronList.size, "emptying after a resize must still be disallowed")
        assertEquals(1, network.getModels<Subnetwork>().size, "subnetwork must remain intact")
    }

    @Test
    fun `deleting some neurons of a subnetwork collection still resizes it`() = runBlocking {
        val hop = Hopfield(9)
        network.addNetworkModel(hop)

        selectOnly(hop.neuronGroup.neuronList.take(3))
        panel.deleteSelectedObjects()
        assertEquals(6, hop.neuronGroup.neuronList.size, "deleting some (not all) neurons should resize the collection")
        assertEquals(1, network.getModels<Subnetwork>().size)

        panel.undoManager.undo()
        assertEquals(9, hop.neuronGroup.neuronList.size, "undo must restore the resized-away neurons")
    }

    @Test
    fun `delete hidden layer of backprop then undo restores its structure`() = runBlocking {
        val backprop = BackpropNetwork(intArrayOf(2, 3, 1), null)
        network.addNetworkModel(backprop)
        val baseline = backprop.modelList.size

        val hidden = backprop.layerList[1]
        selectOnly(hidden)
        panel.deleteSelectedObjects()
        assertTrue(backprop.modelList.size < baseline, "deleting the hidden layer should cascade to its weight matrices")

        panel.undoManager.undo()
        assertEquals(baseline, backprop.modelList.size, "undo must restore the layer and its weight matrices")
        assertEquals(0, network.getModels<NeuronArray>().size, "restored layer leaked as a top-level array")
        assertEquals(0, network.getModels<WeightMatrix>().size, "restored matrices leaked as top-level connectors")
        assertEquals(listOf(2, 3, 1), backprop.layerList.map { it.size }, "backprop layer list not restored")
        assertEquals(2, backprop.wmList.size, "backprop weight matrix list not restored")

        backprop.inputLayer.setActivations(doubleArrayOf(0.5, 0.5))
        backprop.inputLayer.isClamped = true
        network.update()
        assertTrue(backprop.outputLayer.activationArray.all { it.isFinite() }, "restored backprop must update without error")

        panel.undoManager.redo()
        assertTrue(backprop.modelList.size < baseline, "redo must re-remove the layer")
    }

    @Test
    fun `delete context layer of srn then undo restores its structure`() = runBlocking {
        val srn = SRNNetwork(3, 4, 2)
        network.addNetworkModel(srn)
        val baseline = srn.modelList.size
        val contextToHidden = srn.contextToHidden

        selectOnly(srn.contextLayer)
        panel.deleteSelectedObjects()
        assertTrue(srn.modelList.size < baseline, "deleting the context layer should cascade to contextToHidden")

        panel.undoManager.undo()
        assertEquals(baseline, srn.modelList.size, "undo must restore the context layer and its weight matrix")
        assertEquals(0, network.getModels<NeuronArray>().size, "restored context layer leaked as a top-level array")
        assertEquals(0, network.getModels<WeightMatrix>().size, "restored context matrix leaked as a top-level connector")
        assertSame(contextToHidden, srn.contextToHidden, "contextToHidden alias should still reference the restored matrix")

        srn.inputLayer.setActivations(doubleArrayOf(0.2, 0.4, 0.6))
        srn.inputLayer.isClamped = true
        network.update()
        assertTrue(srn.outputLayer.activationArray.all { it.isFinite() }, "restored SRN must update without error")
    }

    @Test
    fun `synapse group auto-collapses and expands without dual representation on undo`() = runBlocking {
        val comp = CompetitiveNetwork(20, 12) // 20x12 = 240 > 200 threshold -> starts collapsed
        network.addNetworkModel(comp)
        val sg = comp.weights
        fun visibleLoose() = panel.filterScreenElements<SynapseNode>().count { it.synapse in sg.synapses && it.visible }

        awaitUntil { !sg.displaySynapses && visibleLoose() == 0 }
        assertFalse(sg.displaySynapses, "240 synapses should start collapsed")
        assertEquals(0, visibleLoose(), "a collapsed group shows no loose synapse nodes")

        // Drop below the threshold (20x8 = 160) -> should auto-expand.
        selectOnly(comp.competitive.neuronList.take(4))
        panel.deleteSelectedObjects()
        awaitUntil { sg.displaySynapses && visibleLoose() == sg.synapses.size }
        assertTrue(sg.displaySynapses, "dropping below the threshold should auto-expand the group")
        assertEquals(sg.synapses.size, visibleLoose(), "an expanded group shows all of its synapses")

        // Delete the group and undo -> restored still below threshold -> expanded, no arrow alongside.
        selectOnly(sg)
        panel.deleteSelectedObjects()
        panel.undoManager.undo()
        awaitUntil { sg.displaySynapses && visibleLoose() == sg.synapses.size }
        assertEquals(sg.synapses.size, visibleLoose(), "a restored below-threshold group is expanded")

        // Undo the neuron deletion -> back above threshold (240) -> collapse, loose nodes removed.
        panel.undoManager.undo()
        awaitUntil { !sg.displaySynapses && visibleLoose() == 0 }
        assertFalse(sg.displaySynapses, "crossing back above the threshold should collapse the group")
        assertEquals(0, visibleLoose(),
            "no loose synapse nodes may remain alongside the collapsed arrow (the dual-representation bug)")
    }

    @Test
    fun `delete synapse group of competitive network then undo restores its node`() = runBlocking {
        val comp = CompetitiveNetwork(5, 4)
        network.addNetworkModel(comp)
        val sg = comp.weights
        fun sgNodePresent() = panel.filterScreenElements<SynapseGroupNode>().any { it.synapseGroup === sg }
        awaitUntil { sgNodePresent() }
        assertTrue(sgNodePresent(), "precondition: the synapse group node exists on the canvas")

        selectOnly(sg)
        panel.deleteSelectedObjects()
        awaitUntil { !sgNodePresent() }
        assertFalse(sgNodePresent(), "the synapse group node should be removed from the canvas after delete")

        panel.undoManager.undo()
        awaitUntil { sgNodePresent() }
        assertTrue(sgNodePresent(), "undo must bring the synapse group node back onto the canvas")
        assertTrue(sg in comp.modelList.all, "the synapse group must be restored into the subnetwork")
        assertEquals(20, sg.synapses.size, "the synapse group's synapses must be restored")
    }

    @Test
    fun `cnn pipeline components cannot be deleted individually`() = runBlocking {
        val inputTensorLayer = TensorLayer(TensorShape(2, 2, 1)).apply { isClamped = true }
        val flatArray = NeuronArray(4)
        val outputArray = NeuronArray(2)
        FlattenConnector(inputTensorLayer, flatArray)
        WeightMatrix(flatArray, outputArray)
        val cnn = ConvolutionalNeuralNetwork(inputTensorLayer, outputArray)
        network.addNetworkModel(cnn)
        val freeNeuron = Neuron()
        network.addNetworkModel(freeNeuron)

        // Deleting any CNN pipeline component would asynchronously self-delete the whole net (un-undoable),
        // so the pipeline is protected. Select a component + a free neuron so the warning dialog isn't hit.
        selectOnly(listOf(outputArray, freeNeuron))
        panel.deleteSelectedObjects()

        assertEquals(0, network.getModels<Neuron>().size, "the free neuron should be deleted")
        assertEquals(1, network.getModels<Subnetwork>().size, "the CNN must survive (its pipeline is protected)")
        assertTrue(outputArray in cnn.modelList.all, "a CNN pipeline component must not be individually deletable")
    }

    @Test
    fun `all neurons of a supervised model's collection layer cannot be deleted`() = runBlocking {
        val inputNeurons = List(3) { Neuron() }
        inputNeurons.forEach { network.addNetworkModel(it) }
        val inputLayer = NeuronCollection(inputNeurons)
        network.addNetworkModel(inputLayer)
        val outputNeurons = List(2) { Neuron() }
        outputNeurons.forEach { network.addNetworkModel(it) }
        val outputLayer = NeuronCollection(outputNeurons)
        network.addNetworkModel(outputLayer)
        network.addNetworkModel(WeightMatrix(inputLayer, outputLayer))
        network.addNetworkModel(SupervisedModel(inputLayer, outputLayer))
        val freeNeuron = Neuron()
        network.addNetworkModel(freeNeuron)

        selectOnly(inputLayer.neuronList + freeNeuron)
        panel.deleteSelectedObjects()

        assertEquals(3, inputLayer.neuronList.size, "a supervised model's collection layer must not be emptiable")
        assertEquals(1, network.getModels<SupervisedModel>().size, "the supervised overlay must survive")
    }

    @Test
    fun `delete hidden layer of rbm then undo restores its structure`() = runBlocking {
        val rbm = RestrictedBoltzmannMachine(6, 4)
        network.addNetworkModel(rbm)
        val baseline = rbm.modelList.size
        val visibleToHidden = rbm.visibleToHidden

        selectOnly(rbm.hiddenLayer)
        panel.deleteSelectedObjects()
        assertTrue(rbm.modelList.size < baseline, "deleting the hidden layer should cascade to its weight matrix")

        panel.undoManager.undo()
        assertEquals(baseline, rbm.modelList.size, "undo must restore the hidden layer and its weight matrix")
        assertEquals(0, network.getModels<NeuronArray>().size, "restored layer leaked as a top-level array")
        assertEquals(0, network.getModels<WeightMatrix>().size, "restored matrix leaked as a top-level connector")
        assertSame(visibleToHidden, rbm.visibleToHidden, "visibleToHidden alias should still reference the restored matrix")

        rbm.visibleLayer.setActivations(doubleArrayOf(1.0, 0.0, 1.0, 0.0, 1.0, 0.0))
        network.update()
        assertTrue(rbm.hiddenLayer.activationArray.all { it.isFinite() }, "restored RBM must update without error")
    }

    @Test
    fun `delete hidden layer of srn with three incident matrices then undo restores function`() = runBlocking {
        val srn = SRNNetwork(3, 4, 2)
        network.addNetworkModel(srn)
        val baseline = srn.modelList.size

        // The SRN hidden layer is the shared target/source of three weight matrices (input->hidden,
        // hidden->output, context->hidden); restoring it must re-register all three exactly once.
        selectOnly(srn.hiddenLayer)
        panel.deleteSelectedObjects()
        assertTrue(srn.modelList.size < baseline, "deleting the hidden layer should cascade to its three matrices")

        panel.undoManager.undo()
        assertEquals(baseline, srn.modelList.size, "undo must restore the hidden layer and all three matrices")
        assertEquals(0, network.getModels<NeuronArray>().size, "restored layer leaked as a top-level array")
        assertEquals(0, network.getModels<WeightMatrix>().size, "restored matrices leaked as top-level connectors")

        srn.inputLayer.setActivations(doubleArrayOf(0.2, 0.4, 0.6))
        srn.inputLayer.isClamped = true
        network.update()
        assertTrue(srn.outputLayer.activationArray.all { it.isFinite() }, "restored SRN must update without error")
    }

    @Test
    fun `delete output layer of backprop then undo restores its structure`() = runBlocking {
        val backprop = BackpropNetwork(intArrayOf(2, 3, 1), null)
        network.addNetworkModel(backprop)
        val baseline = backprop.modelList.size

        selectOnly(backprop.outputLayer)
        panel.deleteSelectedObjects()
        assertTrue(backprop.modelList.size < baseline, "deleting the output layer should cascade to its weight matrix")

        panel.undoManager.undo()
        assertEquals(baseline, backprop.modelList.size, "undo must restore the output layer and its weight matrix")
        assertEquals(0, network.getModels<NeuronArray>().size, "restored layer leaked as a top-level array")
        assertEquals(0, network.getModels<WeightMatrix>().size, "restored matrix leaked as a top-level connector")

        backprop.inputLayer.setActivations(doubleArrayOf(0.5, 0.5))
        backprop.inputLayer.isClamped = true
        network.update()
        assertTrue(backprop.outputLayer.activationArray.all { it.isFinite() }, "restored backprop must update without error")
    }

    @Test
    fun `a CNN is deletable as a whole via its subnetwork model`() = runBlocking {
        val inputTensorLayer = TensorLayer(TensorShape(2, 2, 1)).apply { isClamped = true }
        val flatArray = NeuronArray(4)
        val outputArray = NeuronArray(2)
        FlattenConnector(inputTensorLayer, flatArray)
        WeightMatrix(flatArray, outputArray)
        val cnn = ConvolutionalNeuralNetwork(inputTensorLayer, outputArray)
        network.addNetworkModel(cnn)
        assertEquals(1, network.getModels<Subnetwork>().size)

        // The pipeline children are all protected; the escape hatch is selecting the CNN subnetwork itself.
        selectOnly(cnn)
        panel.deleteSelectedObjects()
        assertEquals(0, network.getModels<Subnetwork>().size, "deleting the CNN subnetwork removes the whole pipeline")
    }
}
