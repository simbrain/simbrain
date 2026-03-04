package org.simbrain.network.core

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.simbrain.util.toDoubleArray

class FlattenConnectorTest {

    @Test
    fun `constructor requires exact flatten size match`() {
        val source = Tensor(TensorShape(2, 2, 1))
        val wrongTarget = NeuronArray(3)

        assertThrows<IllegalArgumentException> {
            FlattenConnector(source, wrongTarget)
        }
    }

    @Test
    fun `backward requires exact gradient size match`() {
        val source = Tensor(TensorShape(2, 2, 1))
        val target = NeuronArray(4)
        val flatten = FlattenConnector(source, target)

        assertThrows<IllegalArgumentException> {
            flatten.backward(doubleArrayOf(1.0, 2.0, 3.0))
        }
    }

    @Test
    fun `propagate and backward preserve full tensor ordering`() {
        val source = Tensor(TensorShape(2, 2, 1))
        val target = NeuronArray(4)
        val flatten = FlattenConnector(source, target)

        source.setActivations(doubleArrayOf(1.0, 2.0, 3.0, 4.0))
        flatten.propagate()
        assertArrayEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0), target.inputs.toDoubleArray(), 1e-12)

        flatten.backward(doubleArrayOf(0.4, 0.3, 0.2, 0.1))
        assertArrayEquals(doubleArrayOf(0.4, 0.3, 0.2, 0.1), source.gradients, 1e-12)
    }
}
