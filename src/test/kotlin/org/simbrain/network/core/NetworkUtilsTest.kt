package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NetworkUtilsTest {

    val net = Network()

    @Test
    fun `test energy function`() {
        val neuron1 = Neuron()
        val neuron2 = Neuron()
        val weight = Synapse(neuron1, neuron2)
        neuron1.activation = 1.0
        neuron2.activation = 1.0
        weight.strength = 1.0
        // Energy is -.5 * neuron1 activation * neuron 2 activaton * weight strength
        assertEquals(-.5, listOf(neuron1, neuron2).getEnergy())

        neuron1.activation = -1.0
        assertEquals(.5, listOf(neuron1, neuron2).getEnergy(), .01)

        neuron2.activation = 0.0
        assertEquals(0.0, listOf(neuron1, neuron2).getEnergy(), .01)
    }

    @Test
    fun `test clamp neurons`() {
        val neuron1 = Neuron()
        val neuron2 = Neuron()
        listOf(neuron1, neuron2).clamp(true)
        assertEquals(true, neuron1.clamped)
        assertEquals(true, neuron2.clamped)
        listOf(neuron1, neuron2).clamp(false)
        assertEquals(false, neuron1.clamped)
        assertEquals(false, neuron2.clamped)
    }


    @Test
    fun `test single source and target with weight`() {
        val src = Neuron()
        val tar = Neuron()
        val syn = Synapse(src, tar).apply {
            strength = 2.0
        }
        val weights = getWeightMatrix(listOf(src), listOf(tar))
        // Should be a 1x1 matrix with one entry
        assertEquals(1, weights.ncol())
        assertEquals(2.0, weights.get(0, 0))
    }

    @Test
    fun `test multiple sources and targets`() {
        val src = Neuron()
        val tar = Neuron()

        Synapse(src, tar).apply { strength = 0.1 }
        Synapse(tar, src).apply { strength = 0.2 }

        val weights = getWeightMatrix(listOf(src, tar), listOf(src, tar))

        assertEquals(0.0, weights[0, 0])
        assertEquals(0.1, weights[0, 1])
        assertEquals(0.2, weights[1, 0])
        assertEquals(0.0, weights[1, 1])
    }

    @Test
    fun `test missing connections correspond to 0 entries`() {
        val s1 = Neuron()
        val s2 = Neuron()
        val t1 = Neuron()
        val t2 = Neuron()

        Synapse(s1, t1).apply { strength = 0.1 }
        Synapse(s1, t2).apply { strength = 0.2 }
        Synapse(s2, t1).apply { strength = 0.3 }
        Synapse(s2, t2).apply { strength = 0.4 }

        val weights = getWeightMatrix(listOf(s1, s2), listOf(t1, t2))

        assertEquals(0.1, weights[0, 0])
        assertEquals(0.2, weights[0, 1])
        assertEquals(0.3, weights[1, 0])
        assertEquals(0.4, weights[1, 1])
    }

    @Test
    fun `addConvolutionalNeuralNetwork adds wrapper and adopts pipeline`() {
        val network = Network()
        val inputTensor = Tensor(TensorShape(2, 2, 1))
        val flatArray = NeuronArray(4)
        val outputArray = NeuronArray(1)
        FlattenConnector(inputTensor, flatArray)
        WeightMatrix(flatArray, outputArray)

        val cnn = network.addConvolutionalNeuralNetwork(inputTensor, outputArray) {
            label = "CNN"
        }

        assertTrue(cnn in network.allModels)
        assertEquals(cnn, network.childToParentMap[inputTensor])
        assertEquals(cnn, network.childToParentMap[outputArray])
    }

}
