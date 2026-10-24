package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class TrainingDatasetTest {

    @Test
    fun `rows given as lists are copied into arrays`() {
        val inputs = mutableListOf(mutableListOf(1.0, 2.0), mutableListOf(3.0, 4.0))
        val targets = mutableListOf(mutableListOf(0.0), mutableListOf(1.0))
        val dataset = TrainingDataset(inputs = inputs, targets = targets)
        inputs[0][0] = 99.0
        assertArrayEquals(doubleArrayOf(1.0, 2.0), dataset.inputs[0])
        assertArrayEquals(doubleArrayOf(1.0), dataset.targets[1])
        assertEquals(2, dataset.inputSize)
        assertEquals(1, dataset.targetSize)
    }

    @Test
    fun `rows of the wrong width are rejected in either form`() {
        assertThrows(IllegalArgumentException::class.java) {
            TrainingDataset(inputs = listOf(listOf(1.0, 2.0), listOf(3.0)), targets = listOf(listOf(0.0), listOf(1.0)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            TrainingDataset(inputs = mutableListOf(doubleArrayOf(1.0)), targets = mutableListOf(doubleArrayOf(0.0), doubleArrayOf(1.0)))
        }
    }

    @Test
    fun `iteration yields each input with its target`() {
        val dataset = TrainingDataset(
            inputs = mutableListOf(doubleArrayOf(1.0), doubleArrayOf(2.0)),
            targets = mutableListOf(doubleArrayOf(10.0), doubleArrayOf(20.0))
        )
        assertEquals(listOf(1.0 to 10.0, 2.0 to 20.0), dataset.map { (input, target) -> input[0] to target[0] })
    }
}
