/** Tests the fixed-length sequence contract used by BPTT training. */
package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BPTTNetwork

class FixedSequenceTrainingTest {

    private fun trainerFor(rows: Int, sequenceLength: Int): BPTTTrainer {
        val network = Network()
        val bptt = BPTTNetwork(2, 3, 2)
        network.addNetworkModelsAsync(bptt)
        bptt.trainingSet = TrainingDataset(
            inputs = MutableList(rows) { mutableListOf(1.0, 0.0) },
            targets = MutableList(rows) { mutableListOf(0.0, 1.0) },
            inputSize = 2,
            targetSize = 2
        )
        bptt.trainerConfig.sequenceLength = sequenceLength
        return BPTTTrainer(network, bptt)
    }

    @Test
    fun `training data is divided into complete fixed length sequences`() {
        val sequences = trainerFor(rows = 12, sequenceLength = 4).trainingSequences((0 until 12).toList())
        assertEquals(listOf(listOf(0, 1, 2, 3), listOf(4, 5, 6, 7), listOf(8, 9, 10, 11)), sequences)
    }

    @Test
    fun `partial sequences are rejected`() {
        val trainer = trainerFor(rows = 10, sequenceLength = 4)
        assertThrows(IllegalArgumentException::class.java) {
            trainer.trainingSequences((0 until 10).toList())
        }
    }
}
