package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TensorLayerTest {

    @Test
    fun `basic creation and size`() {
        val tensorLayer = TensorLayer(TensorShape(4, 5, 3))
        assertEquals(60, tensorLayer.activations.size)
        assertEquals(60, tensorLayer.inputs.size)
        assertEquals(60, tensorLayer.biases.size)
    }

    @Test
    fun `indexed access round-trip`() {
        val tensorLayer = TensorLayer(TensorShape(3, 3, 2))
        tensorLayer[1, 2, 0] = 42.0
        tensorLayer[1, 2, 1] = 99.0
        assertEquals(42.0, tensorLayer[1, 2, 0])
        assertEquals(99.0, tensorLayer[1, 2, 1])
    }

    @Test
    fun `update applies activation function`() {
        val net = Network()
        val tensorLayer = TensorLayer(TensorShape(2, 2, 1))
        tensorLayer.activationFunction = TensorActivation.RELU
        // Set inputs: mix of positive and negative
        tensorLayer.inputs[0] = 5.0
        tensorLayer.inputs[1] = -3.0
        tensorLayer.inputs[2] = 0.0
        tensorLayer.inputs[3] = 7.0

        with(net) { tensorLayer.update() }

        assertEquals(5.0, tensorLayer.activations[0])
        assertEquals(0.0, tensorLayer.activations[1]) // ReLU clips negative
        assertEquals(0.0, tensorLayer.activations[2])
        assertEquals(7.0, tensorLayer.activations[3])
        // Inputs cleared after update
        assertTrue(tensorLayer.inputs.all { it == 0.0 })
    }

    @Test
    fun `update with biases`() {
        val net = Network()
        val tensorLayer = TensorLayer(TensorShape(2, 2, 1))
        tensorLayer.activationFunction = TensorActivation.IDENTITY
        tensorLayer.biases[0] = 1.0
        tensorLayer.biases[1] = 2.0
        tensorLayer.inputs[0] = 3.0
        tensorLayer.inputs[1] = 4.0

        with(net) { tensorLayer.update() }

        assertEquals(4.0, tensorLayer.activations[0]) // 3 + 1
        assertEquals(6.0, tensorLayer.activations[1]) // 4 + 2
    }

    @Test
    fun `clamped tensor ignores inputs`() {
        val net = Network()
        val tensorLayer = TensorLayer(TensorShape(2, 2, 1))
        tensorLayer.isClamped = true
        tensorLayer.activations[0] = 42.0
        tensorLayer.inputs[0] = 100.0

        with(net) { tensorLayer.update() }

        assertEquals(42.0, tensorLayer.activations[0]) // Unchanged
    }

    @Test
    fun `setActivations coupling`() {
        val tensorLayer = TensorLayer(TensorShape(2, 2, 1))
        val source = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        tensorLayer.setActivations(source)
        assertArrayEquals(source, tensorLayer.activationArray, 1e-10)
    }

    @Test
    fun `getChannel extracts correct data`() {
        val tensorLayer = TensorLayer(TensorShape(2, 3, 2))
        // Set channel 1 to all 7s
        for (h in 0 until 2) {
            for (w in 0 until 3) {
                tensorLayer[h, w, 0] = 1.0
                tensorLayer[h, w, 1] = 7.0
            }
        }
        val ch1 = tensorLayer.getChannel(1)
        assertEquals(6, ch1.size)
        assertTrue(ch1.all { it == 7.0 })

        val ch0 = tensorLayer.getChannel(0)
        assertTrue(ch0.all { it == 1.0 })
    }

    @Test
    fun `clear resets everything`() {
        val tensorLayer = TensorLayer(TensorShape(3, 3, 1))
        tensorLayer.activations.fill(5.0)
        tensorLayer.inputs.fill(3.0)

        tensorLayer.clear()

        assertTrue(tensorLayer.activations.all { it == 0.0 })
        assertTrue(tensorLayer.inputs.all { it == 0.0 })
    }

    @Test
    fun `sigmoid activation function`() {
        val net = Network()
        val tensorLayer = TensorLayer(TensorShape(1, 1, 1))
        tensorLayer.activationFunction = TensorActivation.SIGMOID
        tensorLayer.inputs[0] = 0.0

        with(net) { tensorLayer.update() }

        assertEquals(0.5, tensorLayer.activations[0], 1e-10) // sigmoid(0) = 0.5
    }

    @Test
    fun `tanh activation function`() {
        val net = Network()
        val tensorLayer = TensorLayer(TensorShape(1, 1, 1))
        tensorLayer.activationFunction = TensorActivation.TANH
        tensorLayer.inputs[0] = 0.0

        with(net) { tensorLayer.update() }

        assertEquals(0.0, tensorLayer.activations[0], 1e-10) // tanh(0) = 0
    }
}
