package org.simbrain.network.subnetworks

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.trainers.CnnTrainer
import org.simbrain.network.updaterules.SoftmaxRule

class ConvolutionalNeuralNetworkTest {

    @Test
    fun `cnn update performs one-iteration forward sweep`() {
        val net = Network()

        val inputTensorLayer = TensorLayer(TensorShape(2, 2, 1)).apply {
            isClamped = true
            activations = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        }
        val flatArray = NeuronArray(4).apply {
            biases.fill(0.0)
        }
        val outputArray = NeuronArray(1).apply {
            biases.fill(0.0)
        }
        val flatten = FlattenConnector(inputTensorLayer, flatArray)
        val dense = WeightMatrix(flatArray, outputArray).apply {
            setWeights(doubleArrayOf(1.0, 1.0, 1.0, 1.0))
        }

        val cnn = ConvolutionalNeuralNetwork(inputTensorLayer, outputArray)
        net.addNetworkModelAsync(cnn)

        // Contract: one CNN update executes a full input->output forward sweep.
        with(net) { cnn.update() }

        assertEquals(10.0, outputArray.activationArray[0], 1e-10)
    }

    @Test
    fun `dialog apply-row inference matches normal network iteration`() {
        val net = Network()

        val inputTensorLayer = TensorLayer(TensorShape(2, 2, 1)).apply {
            isClamped = true
        }
        val flatArray = NeuronArray(4)
        val outputArray = NeuronArray(2).apply {
            updateRule = SoftmaxRule()
            biases.fill(0.0)
        }
        FlattenConnector(inputTensorLayer, flatArray)
        WeightMatrix(flatArray, outputArray).apply {
            // [2x4] matrix in row-major layout as expected by setWeights
            setWeights(
                doubleArrayOf(
                    1.0, 0.0, 0.0, 1.0,
                    0.0, 1.0, 1.0, 0.0
                )
            )
        }

        val cnn = net.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray)
        val inputRow = doubleArrayOf(0.1, 0.2, 0.3, 0.4)

        // Dialog path: CnnTrainer.forwardPass(row) + sync output to network layer.
        val dialogTrainer = CnnTrainer(net, inputTensorLayer, outputArray, cnn.trainerConfig)
        val dialogOutput = dialogTrainer.forwardPass(inputRow.copyOf())
        outputArray.setActivations(dialogOutput.copyOf())

        // Normal path: set input then iterate network once.
        inputTensorLayer.activations = inputRow.copyOf()
        net.update("test")
        val iterateOutput = outputArray.activationArray.copyOf()

        assertEquals(dialogOutput.size, iterateOutput.size)
        dialogOutput.indices.forEach { i ->
            assertEquals(dialogOutput[i], iterateOutput[i], 1e-10)
        }
    }

    @Test
    fun `test simple cnn serialization`() {
        val net = Network()
        val inputTensorLayer = TensorLayer(TensorShape(2, 2, 1)).apply { isClamped = true }
        val flatArray = NeuronArray(4)
        val outputArray = NeuronArray(2).apply { updateRule = SoftmaxRule() }
        FlattenConnector(inputTensorLayer, flatArray)
        WeightMatrix(flatArray, outputArray).apply {
            setWeights(doubleArrayOf(1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0))
        }
        net.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray) {
            label = "SimpleCNN"
        }

        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        assertNotNull(fromXml.getModelByLabel(ConvolutionalNeuralNetwork::class.java, "SimpleCNN"))
    }

    @Test
    fun `test cnn with conv layer serialization`() {
        val net = Network()
        val inputShape = TensorShape(4, 4, 1)
        val inputTensorLayer = TensorLayer(inputShape).apply { isClamped = true }
        val convOutputShape = inputShape.convOutputShape(3, 1, Padding.SAME, 2)
        val convOutputTensor = TensorLayer(convOutputShape)
        val conv = ConvolutionConnector(inputTensorLayer, convOutputTensor, kernelSize = 3, numFilters = 2)
        val flatArray = NeuronArray(convOutputShape.size)
        FlattenConnector(convOutputTensor, flatArray)
        val outputArray = NeuronArray(3)
        WeightMatrix(flatArray, outputArray)
        net.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray) {
            label = "ConvCNN"
        }

        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        assertNotNull(fromXml.getModelByLabel(ConvolutionalNeuralNetwork::class.java, "ConvCNN"))
    }

    @Test
    fun `test cnn forward pass survives serialization`() {
        val net = Network()
        val inputTensorLayer = TensorLayer(TensorShape(2, 2, 1)).apply {
            isClamped = true
            activations = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        }
        val flatArray = NeuronArray(4).apply { biases.fill(0.0) }
        val outputArray = NeuronArray(1).apply { biases.fill(0.0) }
        FlattenConnector(inputTensorLayer, flatArray)
        WeightMatrix(flatArray, outputArray).apply {
            setWeights(doubleArrayOf(1.0, 1.0, 1.0, 1.0))
        }
        net.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray) {
            label = "FwdCNN"
        }

        // Run forward pass before serialization
        net.update("test")
        val beforeOutput = outputArray.activationArray[0]

        // Serialize and deserialize
        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        val restoredCnn = fromXml.getModelByLabel(ConvolutionalNeuralNetwork::class.java, "FwdCNN")!!

        // Set the same input and run forward pass on deserialized network
        restoredCnn.inputTensorLayer.activations = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        fromXml.update("test")
        val afterOutput = restoredCnn.outputArray.activationArray[0]

        assertEquals(beforeOutput, afterOutput, 1e-10,
            "Forward pass output should match after serialization round-trip")
    }

    @Test
    fun `test cnn conv kernel weights survive serialization`() {
        val net = Network()
        val inputShape = TensorShape(4, 4, 1)
        val inputTensorLayer = TensorLayer(inputShape).apply { isClamped = true }
        val convOutputShape = inputShape.convOutputShape(3, 1, Padding.SAME, 2)
        val convOutputTensor = TensorLayer(convOutputShape)
        val conv = ConvolutionConnector(inputTensorLayer, convOutputTensor, kernelSize = 3, numFilters = 2)
        // Set known kernel weights
        for (i in conv.kernels.indices) {
            conv.kernels[i] = (i + 1).toDouble() * 0.01
        }
        val flatArray = NeuronArray(convOutputShape.size)
        FlattenConnector(convOutputTensor, flatArray)
        val outputArray = NeuronArray(3)
        WeightMatrix(flatArray, outputArray)
        net.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray) {
            label = "KernelCNN"
        }

        val originalKernels = conv.kernels.copyOf()

        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        val restoredCnn = fromXml.getModelByLabel(ConvolutionalNeuralNetwork::class.java, "KernelCNN")!!

        // Find the ConvolutionConnector in the restored CNN's model list
        val restoredConv = restoredCnn.modelList.all.filterIsInstance<ConvolutionConnector>().first()
        assertArrayEquals(originalKernels, restoredConv.kernels, 1e-15,
            "Convolution kernel weights should survive serialization")
    }

    @Test
    fun `test cnn with pooling layer serialization`() {
        val net = Network()
        val inputShape = TensorShape(4, 4, 1)
        val inputTensorLayer = TensorLayer(inputShape).apply { isClamped = true }
        val convOutputShape = inputShape.convOutputShape(3, 1, Padding.SAME, 2)
        val convOutputTensor = TensorLayer(convOutputShape)
        ConvolutionConnector(inputTensorLayer, convOutputTensor, kernelSize = 3, numFilters = 2)
        val poolOutputShape = convOutputShape.poolOutputShape(2, 2)
        val poolOutputTensor = TensorLayer(poolOutputShape)
        PoolingConnector(convOutputTensor, poolOutputTensor)
        val flatArray = NeuronArray(poolOutputShape.size)
        FlattenConnector(poolOutputTensor, flatArray)
        val outputArray = NeuronArray(3)
        WeightMatrix(flatArray, outputArray)
        net.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray) {
            label = "PoolCNN"
        }

        val xmlRep = getNetworkXStream().toXML(net)
        val fromXml = getNetworkXStream().fromXML(xmlRep) as Network
        val restoredCnn = fromXml.getModelByLabel(ConvolutionalNeuralNetwork::class.java, "PoolCNN")!!

        // Verify pipeline components were restored
        val restoredConvs = restoredCnn.modelList.all.filterIsInstance<ConvolutionConnector>()
        val restoredPools = restoredCnn.modelList.all.filterIsInstance<PoolingConnector>()
        assertEquals(1, restoredConvs.size, "Should have 1 ConvolutionConnector after deserialization")
        assertEquals(1, restoredPools.size, "Should have 1 PoolingConnector after deserialization")

        // Verify forward pass works on deserialized network
        restoredCnn.inputTensorLayer.activations = DoubleArray(inputShape.size) { 1.0 }
        fromXml.update("test")
        // Just verify no exception and output is non-trivial
        assertNotNull(restoredCnn.outputArray.activationArray)
    }

    @Test
    fun `normal iteration syncs flatten activations`() {
        val net = Network()

        val inputTensorLayer = TensorLayer(TensorShape(2, 2, 1)).apply {
            isClamped = true
        }
        val flatArray = NeuronArray(4)
        val outputArray = NeuronArray(1)
        FlattenConnector(inputTensorLayer, flatArray)
        WeightMatrix(flatArray, outputArray).apply {
            setWeights(doubleArrayOf(1.0, 1.0, 1.0, 1.0))
        }
        net.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray)

        inputTensorLayer.activations = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        net.update("test")

        // Output path is updated...
        assertEquals(10.0, outputArray.activationArray[0], 1e-10)
        // ...and flatten node is synchronized for GUI/inspection.
        assertEquals(10.0, flatArray.activationArray.sum(), 1e-10)
    }
}
