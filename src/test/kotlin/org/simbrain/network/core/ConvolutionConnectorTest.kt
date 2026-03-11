package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConvolutionConnectorTest {

    @Test
    fun `propagate produces output`() {
        val source = TensorLayer(TensorShape(4, 4, 1))
        source.activations.fill(1.0)

        val outputShape = source.shape.convOutputShape(3, 1, Padding.SAME, 2)
        val target = TensorLayer(outputShape)

        val conv = ConvolutionConnector(source, target, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)
        // Set all kernel weights to 1 and biases to 0
        conv.kernels.fill(1.0)
        conv.filterBiases.fill(0.0)

        conv.propagate()

        // Center pixel should have full 3x3 sum = 9 (all inputs are 1, kernel is 1)
        val centerIdx = target.shape.index(2, 2, 0)
        assertEquals(9.0, target.inputs[centerIdx], 1e-10)
    }

    @Test
    fun `shape mismatch throws`() {
        val source = TensorLayer(TensorShape(4, 4, 1))
        val wrongTarget = TensorLayer(TensorShape(10, 10, 1)) // Wrong shape

        assertThrows<IllegalArgumentException> {
            ConvolutionConnector(source, wrongTarget, kernelSize = 3, numFilters = 1, stride = 1, padding = Padding.VALID)
        }
    }

    @Test
    fun `invalid stride throws IllegalArgumentException`() {
        val source = TensorLayer(TensorShape(4, 4, 1))
        val target = TensorLayer(TensorShape(4, 4, 1))

        assertThrows<IllegalArgumentException> {
            ConvolutionConnector(source, target, kernelSize = 3, numFilters = 1, stride = 0, padding = Padding.SAME)
        }
    }

    @Test
    fun `he initialization produces non-zero weights`() {
        val source = TensorLayer(TensorShape(8, 8, 3))
        val outputShape = source.shape.convOutputShape(3, 1, Padding.SAME, 4)
        val target = TensorLayer(outputShape)
        val conv = ConvolutionConnector(source, target, kernelSize = 3, numFilters = 4, stride = 1, padding = Padding.SAME)

        // He initialization should produce non-zero weights
        assertTrue(conv.kernels.any { it != 0.0 })
        // Biases should be 0
        assertTrue(conv.filterBiases.all { it == 0.0 })
    }

    @Test
    fun `randomize changes weights`() {
        val source = TensorLayer(TensorShape(4, 4, 1))
        val outputShape = source.shape.convOutputShape(3, 1, Padding.SAME, 2)
        val target = TensorLayer(outputShape)
        val conv = ConvolutionConnector(source, target, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)

        val before = conv.kernels.copyOf()
        conv.randomize()
        // Very unlikely to be identical after re-randomization
        assertFalse(before.contentEquals(conv.kernels))
    }

    @Test
    fun `delete unwires connectors`() {
        val source = TensorLayer(TensorShape(4, 4, 1))
        val outputShape = source.shape.convOutputShape(3, 1, Padding.SAME, 2)
        val target = TensorLayer(outputShape)
        val conv = ConvolutionConnector(source, target, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)

        assertEquals(1, source.outgoingTensorConnectors.size)
        assertEquals(1, target.incomingTensorConnectors.size)

        // Delete is suspend, use blocking for test
        kotlinx.coroutines.runBlocking { conv.delete() }

        assertEquals(0, source.outgoingTensorConnectors.size)
        assertEquals(0, target.incomingTensorConnectors.size)
    }

    @Test
    fun `hardClear sets kernels to zero but leaves biases unchanged`() {
        val source = TensorLayer(TensorShape(4, 4, 1))
        val outputShape = source.shape.convOutputShape(3, 1, Padding.SAME, 2)
        val target = TensorLayer(outputShape)
        val conv = ConvolutionConnector(source, target, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)

        conv.kernels.fill(1.0)
        conv.filterBiases.fill(0.5)

        assertTrue(conv.kernels.all { it == 1.0 })
        assertTrue(conv.filterBiases.all { it == 0.5 })

        conv.hardClear()

        // Kernels should be zero
        assertTrue(conv.kernels.all { it == 0.0 })
        // Biases should be unchanged (following WeightMatrix pattern)
        assertTrue(conv.filterBiases.all { it == 0.5 })
    }

    @Test
    fun `increment and decrement change kernel weights`() {
        val source = TensorLayer(TensorShape(4, 4, 1))
        val outputShape = source.shape.convOutputShape(3, 1, Padding.SAME, 2)
        val target = TensorLayer(outputShape)
        val conv = ConvolutionConnector(source, target, kernelSize = 3, numFilters = 2, stride = 1, padding = Padding.SAME)

        conv.kernels.fill(1.0)
        val before = conv.kernels.copyOf()
        
        conv.increment()
        assertTrue(conv.kernels.all { it > 1.0 })
        
        conv.decrement()
        assertTrue(conv.kernels.contentEquals(before))
    }
}
