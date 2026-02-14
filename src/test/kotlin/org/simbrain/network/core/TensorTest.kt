package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TensorTest {

    @Test
    fun `basic creation and size`() {
        val tensor = Tensor(TensorShape(4, 5, 3))
        assertEquals(60, tensor.activations.size)
        assertEquals(60, tensor.inputs.size)
        assertEquals(60, tensor.biases.size)
    }

    @Test
    fun `indexed access round-trip`() {
        val tensor = Tensor(TensorShape(3, 3, 2))
        tensor[1, 2, 0] = 42.0
        tensor[1, 2, 1] = 99.0
        assertEquals(42.0, tensor[1, 2, 0])
        assertEquals(99.0, tensor[1, 2, 1])
    }

    @Test
    fun `update applies activation function`() {
        val net = Network()
        val tensor = Tensor(TensorShape(2, 2, 1))
        tensor.activationFunction = TensorActivation.RELU
        // Set inputs: mix of positive and negative
        tensor.inputs[0] = 5.0
        tensor.inputs[1] = -3.0
        tensor.inputs[2] = 0.0
        tensor.inputs[3] = 7.0

        with(net) { tensor.update() }

        assertEquals(5.0, tensor.activations[0])
        assertEquals(0.0, tensor.activations[1]) // ReLU clips negative
        assertEquals(0.0, tensor.activations[2])
        assertEquals(7.0, tensor.activations[3])
        // Inputs cleared after update
        assertTrue(tensor.inputs.all { it == 0.0 })
    }

    @Test
    fun `update with biases`() {
        val net = Network()
        val tensor = Tensor(TensorShape(2, 2, 1))
        tensor.activationFunction = TensorActivation.IDENTITY
        tensor.biases[0] = 1.0
        tensor.biases[1] = 2.0
        tensor.inputs[0] = 3.0
        tensor.inputs[1] = 4.0

        with(net) { tensor.update() }

        assertEquals(4.0, tensor.activations[0]) // 3 + 1
        assertEquals(6.0, tensor.activations[1]) // 4 + 2
    }

    @Test
    fun `clamped tensor ignores inputs`() {
        val net = Network()
        val tensor = Tensor(TensorShape(2, 2, 1))
        tensor.isClamped = true
        tensor.activations[0] = 42.0
        tensor.inputs[0] = 100.0

        with(net) { tensor.update() }

        assertEquals(42.0, tensor.activations[0]) // Unchanged
    }

    @Test
    fun `setActivations coupling`() {
        val tensor = Tensor(TensorShape(2, 2, 1))
        val source = doubleArrayOf(1.0, 2.0, 3.0, 4.0)
        tensor.setActivations(source)
        assertArrayEquals(source, tensor.activationArray, 1e-10)
    }

    @Test
    fun `getChannel extracts correct data`() {
        val tensor = Tensor(TensorShape(2, 3, 2))
        // Set channel 1 to all 7s
        for (h in 0 until 2) {
            for (w in 0 until 3) {
                tensor[h, w, 0] = 1.0
                tensor[h, w, 1] = 7.0
            }
        }
        val ch1 = tensor.getChannel(1)
        assertEquals(6, ch1.size)
        assertTrue(ch1.all { it == 7.0 })

        val ch0 = tensor.getChannel(0)
        assertTrue(ch0.all { it == 1.0 })
    }

    @Test
    fun `clear resets everything`() {
        val tensor = Tensor(TensorShape(3, 3, 1))
        tensor.activations.fill(5.0)
        tensor.inputs.fill(3.0)

        tensor.clear()

        assertTrue(tensor.activations.all { it == 0.0 })
        assertTrue(tensor.inputs.all { it == 0.0 })
    }

    @Test
    fun `sigmoid activation function`() {
        val net = Network()
        val tensor = Tensor(TensorShape(1, 1, 1))
        tensor.activationFunction = TensorActivation.SIGMOID
        tensor.inputs[0] = 0.0

        with(net) { tensor.update() }

        assertEquals(0.5, tensor.activations[0], 1e-10) // sigmoid(0) = 0.5
    }

    @Test
    fun `tanh activation function`() {
        val net = Network()
        val tensor = Tensor(TensorShape(1, 1, 1))
        tensor.activationFunction = TensorActivation.TANH
        tensor.inputs[0] = 0.0

        with(net) { tensor.update() }

        assertEquals(0.0, tensor.activations[0], 1e-10) // tanh(0) = 0
    }
}
