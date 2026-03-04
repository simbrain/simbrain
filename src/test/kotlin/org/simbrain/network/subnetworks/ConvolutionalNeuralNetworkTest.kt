package org.simbrain.network.subnetworks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.simbrain.network.core.*
import org.simbrain.network.trainers.CnnTrainer
import org.simbrain.network.updaterules.SoftmaxRule

class ConvolutionalNeuralNetworkTest {

    @Test
    fun `cnn update performs one-iteration forward sweep`() {
        val net = Network()

        val inputTensor = Tensor(TensorShape(2, 2, 1)).apply {
            isClamped = true
            setActivations(doubleArrayOf(1.0, 2.0, 3.0, 4.0))
        }
        val flatArray = NeuronArray(4).apply {
            biases.fill(0.0)
        }
        val outputArray = NeuronArray(1).apply {
            biases.fill(0.0)
        }
        val flatten = FlattenConnector(inputTensor, flatArray)
        val dense = WeightMatrix(flatArray, outputArray).apply {
            setWeights(doubleArrayOf(1.0, 1.0, 1.0, 1.0))
        }

        val cnn = ConvolutionalNeuralNetwork(inputTensor, outputArray)
        net.addNetworkModelAsync(cnn)

        // Contract: one CNN update executes a full input->output forward sweep.
        with(net) { cnn.update() }

        assertEquals(10.0, outputArray.activationArray[0], 1e-10)
    }

    @Test
    fun `dialog apply-row inference matches normal network iteration`() {
        val net = Network()

        val inputTensor = Tensor(TensorShape(2, 2, 1)).apply {
            isClamped = true
        }
        val flatArray = NeuronArray(4)
        val outputArray = NeuronArray(2).apply {
            updateRule = SoftmaxRule()
            biases.fill(0.0)
        }
        FlattenConnector(inputTensor, flatArray)
        WeightMatrix(flatArray, outputArray).apply {
            // [2x4] matrix in row-major layout as expected by setWeights
            setWeights(
                doubleArrayOf(
                    1.0, 0.0, 0.0, 1.0,
                    0.0, 1.0, 1.0, 0.0
                )
            )
        }

        val cnn = net.addConvolutionalNeuralNetwork(inputTensor, outputArray)
        val inputRow = doubleArrayOf(0.1, 0.2, 0.3, 0.4)

        // Dialog path: CnnTrainer.forwardPass(row) + sync output to network layer.
        val dialogTrainer = CnnTrainer(net, inputTensor, outputArray, cnn.trainerConfig)
        val dialogOutput = dialogTrainer.forwardPass(inputRow.copyOf())
        outputArray.setActivations(dialogOutput.copyOf())

        // Normal path: set input then iterate network once.
        inputTensor.setActivations(inputRow.copyOf())
        net.update("test")
        val iterateOutput = outputArray.activationArray.copyOf()

        assertEquals(dialogOutput.size, iterateOutput.size)
        dialogOutput.indices.forEach { i ->
            assertEquals(dialogOutput[i], iterateOutput[i], 1e-10)
        }
    }

    @Test
    fun `normal iteration syncs flatten activations`() {
        val net = Network()

        val inputTensor = Tensor(TensorShape(2, 2, 1)).apply {
            isClamped = true
        }
        val flatArray = NeuronArray(4)
        val outputArray = NeuronArray(1)
        FlattenConnector(inputTensor, flatArray)
        WeightMatrix(flatArray, outputArray).apply {
            setWeights(doubleArrayOf(1.0, 1.0, 1.0, 1.0))
        }
        net.addConvolutionalNeuralNetwork(inputTensor, outputArray)

        inputTensor.setActivations(doubleArrayOf(1.0, 2.0, 3.0, 4.0))
        net.update("test")

        // Output path is updated...
        assertEquals(10.0, outputArray.activationArray[0], 1e-10)
        // ...and flatten node is synchronized for GUI/inspection.
        assertEquals(10.0, flatArray.activationArray.sum(), 1e-10)
    }
}
