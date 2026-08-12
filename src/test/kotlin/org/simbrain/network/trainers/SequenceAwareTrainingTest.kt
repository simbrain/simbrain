/**
 * Tests for training data that declares independent sequences rather than one continuous stream.
 *
 * The property under test is that a truncation window never spans a sequence boundary. A window that did
 * would have its memory cleared partway through and compute a gradient across a discontinuity nothing in
 * the unrolling can see.
 */
package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.getModelByLabel
import org.simbrain.network.core.getNetworkXStream
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.copy

class SequenceAwareTrainingTest {

    private fun datasetOf(rows: Int, sequenceLength: Int?) = TrainingDataset(
        inputs = MutableList(rows) { mutableListOf(1.0, 0.0) },
        targets = MutableList(rows) { mutableListOf(0.0, 1.0) },
        inputSize = 2,
        targetSize = 2,
        sequenceLength = sequenceLength
    )

    private fun trainerFor(rows: Int, sequenceLength: Int?, depth: Int, reset: Boolean = true): BPTTTrainer {
        val net = Network()
        val bptt = BPTTNetwork(2, 3, 2).apply { label = "BPTT" }
        net.addNetworkModelsAsync(bptt)
        bptt.trainingSet = datasetOf(rows, sequenceLength)
        bptt.trainerConfig.truncationDepth = depth
        bptt.trainerConfig.resetBetweenSequences = reset
        return BPTTTrainer(net, bptt)
    }

    @Test
    fun `undivided data is one sequence cut into windows`() {
        val windows = trainerFor(rows = 10, sequenceLength = null, depth = 4).trainingWindows((0 until 10).toList())
        assertEquals(1, windows.size) { "Data declaring no sequences should be read straight through" }
        assertEquals(listOf(listOf(0, 1, 2, 3), listOf(4, 5, 6, 7), listOf(8, 9)), windows.single())
    }

    @Test
    fun `windows restart at every sequence boundary`() {
        val windows = trainerFor(rows = 10, sequenceLength = 5, depth = 3).trainingWindows((0 until 10).toList())
        // Each sequence is cut on its own, so the depth-3 window that would otherwise have run from row 3
        // to row 5 is instead a short window ending at row 4.
        assertEquals(
            listOf(
                listOf(listOf(0, 1, 2), listOf(3, 4)),
                listOf(listOf(5, 6, 7), listOf(8, 9))
            ),
            windows
        )
    }

    @Test
    fun `no window spans a sequence boundary at any depth`() {
        val sequenceLength = 5
        (1..9).forEach { depth ->
            val windows = trainerFor(rows = 30, sequenceLength = sequenceLength, depth = depth)
                .trainingWindows((0 until 30).toList())
            windows.flatten().forEach { window ->
                val sequencesTouched = window.map { it / sequenceLength }.distinct()
                assertEquals(1, sequencesTouched.size) {
                    "At depth $depth the window $window covers more than one sequence"
                }
            }
        }
    }

    @Test
    fun `a depth beyond the sequence length is capped by it rather than reaching into the next`() {
        val windows = trainerFor(rows = 10, sequenceLength = 5, depth = 8).trainingWindows((0 until 10).toList())
        // Asking to unroll further than a sequence runs gives one window per sequence, not a window that
        // borrows rows from the sequence after it.
        assertTrue(windows.all { it.size == 1 && it.single().size == 5 }) { "Got $windows" }
    }

    @Test
    fun `reading straight through ignores the declared sequences`() {
        val windows = trainerFor(rows = 10, sequenceLength = 5, depth = 4, reset = false)
            .trainingWindows((0 until 10).toList())
        assertEquals(1, windows.size)
        assertTrue(windows.single().any { it.contains(4) && it.contains(5) }) {
            "Turning the reset off should let a window run across a boundary again"
        }
    }

    @Test
    fun `sequence length survives copying and a round trip`() {
        assertEquals(5, datasetOf(10, 5).copy().sequenceLength)
        assertNull(datasetOf(10, null).copy().sequenceLength)

        val net = Network()
        val bptt = BPTTNetwork(2, 3, 2).apply { label = "BPTT" }
        net.addNetworkModelsAsync(bptt)
        bptt.trainingSet = datasetOf(10, 5)
        bptt.trainerConfig.resetBetweenSequences = false

        val restored = (getNetworkXStream().fromXML(getNetworkXStream().toXML(net)) as Network)
            .getModelByLabel(BPTTNetwork::class.java, "BPTT")!!
        assertEquals(5, restored.trainingSet.sequenceLength) {
            "How the data is divided belongs to the data, so it has to be saved with it"
        }
        assertEquals(false, restored.trainerConfig.resetBetweenSequences)
    }

    @Test
    fun `copying a dataset keeps its column names`() {
        val named = TrainingDataset(
            inputs = mutableListOf(mutableListOf(1.0)),
            targets = mutableListOf(mutableListOf(1.0)),
            inputSize = 1,
            targetSize = 1,
            inputColumnNames = listOf("in"),
            targetColumnNames = listOf("out")
        )
        assertEquals(listOf("in"), named.copy().inputColumnNames)
        assertEquals(listOf("out"), named.copy().targetColumnNames)
    }
}
