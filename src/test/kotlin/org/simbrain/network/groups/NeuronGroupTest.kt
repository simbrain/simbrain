package org.simbrain.network.groups

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.List

class NeuronGroupTest {

    val workspace = Workspace()
    val networkComponent = NetworkComponent("Test")
    var net = networkComponent.network
    var ng: NeuronCollection

    init {
        val ngNeurons = List(2) { Neuron() }
        ngNeurons.forEach { net.addNetworkModelAsync(it) }
        ng = NeuronCollection(ngNeurons).apply { label = "test" }
        net.addNetworkModelAsync(ng)
        workspace.addWorkspaceComponent(networkComponent)
    }

    @Test
    fun testCopy() {
        val ng2 = ng.copy()
        net.addNetworkModelAsync(ng2)
        assertEquals(2, ng2.neuronList.size)
    }

    @Test
    fun propagateLooseActivations() {
        ng.getNeuron(0).activation = 1.0
        ng.getNeuron(1).activation = -1.0
        val neurons2 = List(2) { Neuron() }
        val ng2 = NeuronCollection(neurons2)
        neurons2.forEach { net.addNetworkModelAsync(it) }
        val wm = WeightMatrix(ng, ng2)
        net.addNetworkModelsAsync(List.of(ng2, wm))
        net.update()
        Assertions.assertArrayEquals(doubleArrayOf(1.0, -1.0), ng2.activationArray)
    }

    @Test
    fun propagateGroupActivation() {
        ng.activationArray = doubleArrayOf(1.0, -1.0)
        val neurons2 = List(2) { Neuron() }
        val ng2 = NeuronCollection(neurons2)
        neurons2.forEach { net.addNetworkModelAsync(it) }
        val wm = WeightMatrix(ng, ng2)
        net.addNetworkModelsAsync(List.of(ng2, wm))
        net.update()
        Assertions.assertArrayEquals(doubleArrayOf(1.0, -1.0), ng2.activationArray)
    }


    @Test
    fun getThenSetActivations() {
            ng.activations // validates the cache. be sure propagation still works
            ng.getNeuron(0).activation = 1.0
            ng.getNeuron(1).activation = -1.0
            val neurons2 = List(2) { Neuron() }
            val ng2 = NeuronCollection(neurons2)
            neurons2.forEach { net.addNetworkModelAsync(it) }
            val wm = WeightMatrix(ng, ng2)
            net.addNetworkModelsAsync(List.of(ng2, wm))
            net.update()
            Assertions.assertArrayEquals(doubleArrayOf(1.0, -1.0), ng2.activationArray)
        }

    @Test
    fun testSoftmax() {
        with(net) {
            ng.randomize()
            val na = NeuronArray(5).apply { updateRule = SoftmaxRule() }
            val wm = WeightMatrix(ng, na)
            net.addNetworkModelsAsync(na, wm)
            net.update()
            assertEquals(1.0, na.activations.sum(), .01)
        }
    }

    @Test
    fun `test serialization with couplings`() {

        with(workspace.couplingManager) {
            ng.getNeuron(0) couple ng.getNeuron(1)
        }

        val serializer = WorkspaceSerializer(workspace)
        val bas = ByteArrayOutputStream()
        serializer.serialize(bas, true)
        bas.close()
        workspace.clearWorkspace()

        // Reopen
        val bis = ByteArrayInputStream(bas.toByteArray())
        runBlocking {
            serializer.deserialize(bis)
        }
        bis.close()
        assertEquals(1, workspace.couplingManager.couplings.size)

    }

    @Test
    fun `bias should only be applied once per update`() {
        // Regression test: biases were being added twice - once in
        // NeuronCollection.accumulateInputs() and again in
        // Neuron.accumulateInputs() called from NeuronGroup.update()

        val biasNeurons = List(3) { Neuron() }
        val biasGroup = NeuronCollection(biasNeurons)
        biasNeurons.forEach { net.addNetworkModelAsync(it) }
        net.addNetworkModelAsync(biasGroup)

        // Set biases and clear activations
        biasGroup.getNeuron(0).bias = 1.0
        biasGroup.getNeuron(1).bias = 2.0
        biasGroup.getNeuron(2).bias = -0.5
        biasGroup.clear()

        // With no inputs and linear update rule (default),
        // activation should equal bias after one update
        net.update()

        assertEquals(1.0, biasGroup.getNeuron(0).activation, 0.001,
            "Neuron 0 activation should equal its bias of 1.0")
        assertEquals(2.0, biasGroup.getNeuron(1).activation, 0.001,
            "Neuron 1 activation should equal its bias of 2.0")
        assertEquals(-0.5, biasGroup.getNeuron(2).activation, 0.001,
            "Neuron 2 activation should equal its bias of -0.5")
    }

    @Test
    fun `bias should accumulate with weighted inputs correctly`() {
        // Verify bias is added correctly when there are also weighted inputs

        val sourceNeurons = List(1) { Neuron() }
        val targetNeurons = List(1) { Neuron() }
        val sourceGroup = NeuronCollection(sourceNeurons)
        val targetGroup = NeuronCollection(targetNeurons)
        sourceNeurons.forEach { net.addNetworkModelAsync(it) }
        targetNeurons.forEach { net.addNetworkModelAsync(it) }
        val wm = WeightMatrix(sourceGroup, targetGroup)
        net.addNetworkModelsAsync(List.of(sourceGroup, targetGroup, wm))

        // Set source activation and target bias
        sourceGroup.getNeuron(0).activation = 2.0
        targetGroup.getNeuron(0).bias = 0.5
        targetGroup.clear()

        // Weight matrix defaults to identity, so input = 2.0
        // With bias = 0.5, activation should be 2.5
        net.update()

        assertEquals(2.5, targetGroup.getNeuron(0).activation, 0.001,
            "Activation should be weighted input (2.0) + bias (0.5) = 2.5")
    }

}