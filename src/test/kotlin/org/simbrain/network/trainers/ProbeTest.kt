package org.simbrain.network.trainers

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.subnetworks.BackpropNetwork

class ProbeTest {

    private suspend fun awaitStale(probe: Probe) {
        withTimeout(2000) {
            while (!probe.stale) delay(20)
        }
    }

    @Test
    fun `createProbe returns a probe referencing the probed layer`() = runBlocking {
        val network = Network()
        val hostInput = NeuronArray(4).apply { isClamped = true }
        val hostHidden = NeuronArray(3)
        network.addNetworkModelsAsync(hostInput, hostHidden, WeightMatrix(hostInput, hostHidden))

        val probe = with(network) { createProbe(hostHidden, readoutSize = 2) }

        assertSame(hostHidden, probe.probedModel)
        assertSame(hostHidden, probe.inputLayer)
    }

    @Test
    fun `createProbe on a tensor stage references the tensor but reads through the flatten array`() = runBlocking {
        val network = Network()
        val inputTensorLayer = TensorLayer(TensorShape(3, 3, 1)).apply { isClamped = true }
        val convOutShape = TensorShape(3, 3, 1).convOutputShape(3, 1, Padding.SAME, 2)
        val convOut = TensorLayer(convOutShape)
        ConvolutionConnector(inputTensorLayer, convOut, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        val flatArray = NeuronArray(convOutShape.size)
        FlattenConnector(convOut, flatArray)
        val outputArray = NeuronArray(2)
        WeightMatrix(flatArray, outputArray)
        network.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray)

        val probe = with(network) { createProbe(convOut, readoutSize = 2) }

        assertSame(convOut, probe.probedModel)
        assertNotSame(convOut, probe.inputLayer)
        assertEquals(convOutShape.size, probe.inputLayer.size)
    }

    @Test
    fun `probe becomes stale when upstream host weights change but not downstream ones`() = runBlocking {
        val network = Network()
        val hostInput = NeuronArray(4).apply { isClamped = true }
        val hostHidden = NeuronArray(3)
        val hostOutput = NeuronArray(2)
        val upstreamWm = WeightMatrix(hostInput, hostHidden)
        val downstreamWm = WeightMatrix(hostHidden, hostOutput)
        network.addNetworkModelsAsync(hostInput, hostHidden, hostOutput, upstreamWm, downstreamWm)

        val probe = with(network) { createProbe(hostHidden, readoutSize = 2) }
        assertFalse(probe.stale)

        downstreamWm.randomize()
        delay(300)
        assertFalse(probe.stale) { "Downstream weight changes should not invalidate the harvest" }

        upstreamWm.randomize()
        awaitStale(probe)
    }

    @Test
    fun `probe on a CNN tensor stage becomes stale when an upstream conv connector changes`() = runBlocking {
        val network = Network()
        val inputTensorLayer = TensorLayer(TensorShape(3, 3, 1)).apply { isClamped = true }
        val convOutShape = TensorShape(3, 3, 1).convOutputShape(3, 1, Padding.SAME, 2)
        val convOut = TensorLayer(convOutShape)
        val conv = ConvolutionConnector(inputTensorLayer, convOut, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        val flatArray = NeuronArray(convOutShape.size)
        FlattenConnector(convOut, flatArray)
        val outputArray = NeuronArray(2)
        WeightMatrix(flatArray, outputArray)
        network.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray)

        val probe = with(network) { createProbe(convOut, readoutSize = 2) }
        assertFalse(probe.stale)

        conv.events.updated.fire()
        awaitStale(probe)
    }

    @Test
    fun `training the probe itself does not mark it stale`() = runBlocking {
        val network = Network()
        val hostInput = NeuronArray(4).apply { isClamped = true }
        val hostHidden = NeuronArray(3)
        network.addNetworkModelsAsync(hostInput, hostHidden, WeightMatrix(hostInput, hostHidden))

        val probe = with(network) { createProbe(hostHidden, readoutSize = 2) }
        probe.trainingSet = harvestedDataset(
            mutableListOf(mutableListOf(1.0, 0.0, 0.5), mutableListOf(0.0, 1.0, 0.5)),
            mutableListOf(mutableListOf(1.0, 0.0), mutableListOf(0.0, 1.0))
        )

        val trainer = SupervisedTrainer(network, probe)
        with(network) {
            repeat(5) { trainer.trainBatch(0 until probe.trainingSet.size) }
        }
        delay(300)
        assertFalse(probe.stale)
    }

    @Test
    fun `probe with probed layer inside a subnetwork survives save and load`() = runBlocking {
        val network = Network()
        val bp = BackpropNetwork(intArrayOf(2, 3, 2), null)
        network.addNetworkModelsAsync(bp)
        val hidden = bp.hiddenLayers().first()

        val probe = with(network) {
            createProbe(hidden, readoutSize = 2, readoutLabels = arrayOf("No", "Yes"), label = "Loop probe")
        }
        probe.targetDescription = "Test targets"
        probe.stale = true

        val xml = getNetworkXStream().toXML(network)
        val fromXml = getNetworkXStream().fromXML(xml) as Network
        val restoredProbe = fromXml.getModels<Probe>().first()
        val restoredBp = fromXml.getModels<BackpropNetwork>().first()
        val restoredHidden = restoredBp.hiddenLayers().first()

        assertSame(restoredHidden, restoredProbe.probedModel)
        assertSame(restoredHidden, restoredProbe.inputLayer)
        assertEquals("Test targets", restoredProbe.targetDescription)
        assertTrue(restoredProbe.stale)

        restoredProbe.stale = false
        restoredBp.wmList.first().randomize()
        awaitStale(restoredProbe)
    }

    @Test
    fun `shuffled control permutes targets and preserves inputs and architecture`() = runBlocking {
        val network = Network()
        val hostInput = NeuronArray(4).apply { isClamped = true }
        val hostHidden = NeuronArray(3)
        network.addNetworkModelsAsync(hostInput, hostHidden, WeightMatrix(hostInput, hostHidden))

        val probe = with(network) {
            createProbe(hostHidden, readoutSize = 2, readoutLabels = arrayOf("No", "Yes"), hiddenSizes = listOf(4))
        }
        probe.trainingSet = harvestedDataset(
            MutableList(10) { i -> MutableList(3) { j -> (i * 3 + j).toDouble() } },
            MutableList(10) { i -> if (i < 7) mutableListOf(1.0, 0.0) else mutableListOf(0.0, 1.0) }
        )

        val control = with(network) { probe.createShuffledControl() }

        assertSame(probe.probedModel, control.probedModel)
        assertSame(probe.inputLayer, control.inputLayer)
        assertEquals(probe.layers.size, control.layers.size)
        assertEquals(probe.trainingSet.inputs, control.trainingSet.inputs)
        assertEquals(
            probe.trainingSet.targets.sortedBy { it[0] },
            control.trainingSet.targets.sortedBy { it[0] }
        )
        assertArrayEquals(arrayOf("No", "Yes"), (control.outputLayer as NeuronArray).labelArray)
        assertTrue(control.targetDescription.contains("control"))
    }

    @Test
    fun `majorityClassProportion computes the majority baseline`() {
        val oneHot = listOf(
            listOf(1.0, 0.0), listOf(1.0, 0.0), listOf(1.0, 0.0), listOf(0.0, 1.0)
        )
        assertEquals(0.75, majorityClassProportion(oneHot))

        val binary = listOf(listOf(0.0), listOf(1.0), listOf(1.0))
        assertEquals(2.0 / 3.0, majorityClassProportion(binary), 1e-12)
    }
}
