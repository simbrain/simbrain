package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.util.flatten

class ProbesTest {

    @Test
    fun `harvestActivations records the probed layer's activations for each input row`() = runBlocking {
        val network = Network()
        val bp = BackpropNetwork(intArrayOf(2, 3, 2), null)
        network.addNetworkModelsAsync(bp)
        val hidden = bp.hiddenLayers().first()

        val inputs = mutableListOf(
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 1.0),
        )

        val expected = inputs.map { row ->
            with(network) {
                bp.inputLayer.setActivations(row.toDoubleArray())
                bp.forwardPass()
            }
            hidden.activationArray.toList()
        }

        val harvested = with(network) { bp.harvestActivations(hidden, inputs) }

        assertEquals(inputs.size, harvested.size)
        expected.zip(harvested).forEach { (expectedRow, harvestedRow) ->
            assertEquals(expectedRow, harvestedRow)
        }
    }

    @Test
    fun `createProbe builds a linear probe from the probed layer to a labeled readout`() = runBlocking {
        val network = Network()
        val hostInput = NeuronArray(4).apply { isClamped = true }
        val hostHidden = NeuronArray(3)
        val hostWm = WeightMatrix(hostInput, hostHidden)
        network.addNetworkModelsAsync(hostInput, hostHidden, hostWm)

        val probe = with(network) {
            createProbe(hostHidden, readoutSize = 2, readoutLabels = arrayOf("No", "Yes"), label = "Test probe")
        }

        assertSame(hostHidden, probe.inputLayer)
        assertEquals(2, probe.outputLayer.size)
        assertEquals(setOf(hostHidden, probe.outputLayer), probe.layers)
        assertEquals(1, probe.weightMatrices.size)
        assertArrayEquals(arrayOf("No", "Yes"), (probe.outputLayer as NeuronArray).labelArray)
        assertTrue(probe in network.getModels<Probe>())
        assertTrue(probe in network.supervisedModels)
        assertTrue(probe.outputLayer in network.getModels<NeuronArray>())
    }

    @Test
    fun `createProbe with hidden sizes builds an MLP probe path`() = runBlocking {
        val network = Network()
        val hostInput = NeuronArray(4).apply { isClamped = true }
        val hostHidden = NeuronArray(3)
        val hostWm = WeightMatrix(hostInput, hostHidden)
        network.addNetworkModelsAsync(hostInput, hostHidden, hostWm)

        val probe = with(network) {
            createProbe(hostHidden, readoutSize = 2, hiddenSizes = listOf(5), label = "MLP probe")
        }

        assertEquals(3, probe.layers.size)
        assertEquals(2, probe.weightMatrices.size)
        val probeHidden = probe.layers.first { it !== hostHidden && it !== probe.outputLayer }
        assertEquals(5, probeHidden.size)
    }

    @Test
    fun `training a probe on a CNN tensor stage uses harvested activations without touching the CNN`() = runBlocking {
        val network = Network()

        val inputTensorLayer = TensorLayer(TensorShape(3, 3, 1)).apply { isClamped = true }
        val convOutShape = TensorShape(3, 3, 1).convOutputShape(3, 1, Padding.SAME, 2)
        val convOut = TensorLayer(convOutShape).apply { activationFunction = TensorActivation.RELU }
        val conv = ConvolutionConnector(inputTensorLayer, convOut, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        val flatArray = NeuronArray(convOutShape.size)
        FlattenConnector(convOut, flatArray)
        val outputArray = NeuronArray(2)
        val dense = WeightMatrix(flatArray, outputArray)
        val cnn = network.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray)
        conv.heInitialize()

        val probe = with(network) {
            createProbe(convOut, readoutSize = 2, label = "Tensor probe")
        }
        val probeWm = probe.weightMatrices.first() as WeightMatrix

        val hostInputs = mutableListOf(
            MutableList(9) { if (it % 2 == 0) 1.0 else 0.0 },
            MutableList(9) { if (it % 2 == 0) 0.0 else 1.0 },
            MutableList(9) { it / 9.0 },
        )
        val harvested = with(network) { cnn.harvestActivations(probe.inputLayer, hostInputs) }

        // Each harvested row is the conv stage's activations, read through the probe's flatten array
        with(network) {
            inputTensorLayer.activations = hostInputs[0].toDoubleArray()
            cnn.update()
        }
        assertEquals(convOut.activationArray.toList(), harvested[0])

        probe.trainingSet = harvestedDataset(
            harvested,
            targets = mutableListOf(
                mutableListOf(1.0, 0.0),
                mutableListOf(0.0, 1.0),
                mutableListOf(1.0, 0.0),
            )
        )

        val kernelsBefore = conv.kernels.copyOf()
        val filterBiasesBefore = conv.filterBiases.copyOf()
        val denseBefore = dense.weights.flatten()
        val probeWmBefore = probeWm.weights.flatten()

        val trainer = SupervisedTrainer(network, probe)
        with(network) {
            repeat(10) {
                trainer.trainBatch(0 until probe.trainingSet.size)
            }
        }

        assertArrayEquals(kernelsBefore, conv.kernels, 0.0) {
            "CNN kernels should be untouched by probe training"
        }
        assertArrayEquals(filterBiasesBefore, conv.filterBiases, 0.0)
        assertArrayEquals(denseBefore, dense.weights.flatten(), 0.0) {
            "CNN dense weights should be untouched by probe training"
        }
        assertFalse(probeWmBefore.contentEquals(probeWm.weights.flatten())) {
            "Probe weights should change during probe training"
        }
    }

    @Test
    fun `CNN pipeline discovery ignores probe branches attached to pipeline layers`() = runBlocking {
        val network = Network()

        val inputTensorLayer = TensorLayer(TensorShape(3, 3, 1)).apply { isClamped = true }
        val convOutShape = TensorShape(3, 3, 1).convOutputShape(3, 1, Padding.SAME, 2)
        val convOut = TensorLayer(convOutShape)
        ConvolutionConnector(inputTensorLayer, convOut, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        val flatArray = NeuronArray(convOutShape.size)
        FlattenConnector(convOut, flatArray)
        val outputArray = NeuronArray(2)
        WeightMatrix(flatArray, outputArray)
        val cnn = network.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray)

        with(network) {
            createProbe(convOut, readoutSize = 2, label = "Tensor probe")
            createProbe(flatArray, readoutSize = 2, label = "Dense probe")
        }

        val trainer = CnnTrainer(network, inputTensorLayer, outputArray, cnn.trainerConfig)
        val output = trainer.forwardPass(DoubleArray(9) { it / 9.0 })
        assertEquals(2, output.size)
    }

    @Test
    fun `custom context menu actions survive network deserialization as an empty list`() = runBlocking {
        val network = Network()
        val array = NeuronArray(3)
        network.addNetworkModelsAsync(array)
        array.customContextMenuActions += org.simbrain.util.createAction(name = "Test action") { }

        val xml = getNetworkXStream().toXML(network)
        val fromXml = getNetworkXStream().fromXML(xml) as Network
        val restored = fromXml.getModels<NeuronArray>().first()

        assertTrue(restored.customContextMenuActions.isEmpty())
        restored.customContextMenuActions += org.simbrain.util.createAction(name = "Another") { }
        assertEquals(1, restored.customContextMenuActions.size)
    }

}
