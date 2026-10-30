/**
 * Top-k selects its candidates by running insertion, or by a sort when k is a large share of the vocabulary; these
 * tests pin both to a sort-based selection, including tie order. Top-k and top-p then draw a candidate in proportion
 * to its probability, which the distribution tests check over many seeded draws.
 */
package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TopKSamplingTest {

    private fun sortBasedTopK(k: Int, probabilities: DoubleArray) = probabilities.indices
        .sortedByDescending { probabilities[it] }
        .take(k)
        .toIntArray()

    private fun drawFrequencies(strategy: SamplingStrategy, probabilities: DoubleArray, draws: Int): DoubleArray {
        val random = Random(3)
        val counts = IntArray(probabilities.size)
        repeat(draws) { counts[strategy.sample(probabilities, random)]++ }
        return DoubleArray(counts.size) { counts[it].toDouble() / draws }
    }

    @Test
    fun `top k picks the same candidates as a sort based selection`() {
        val source = Random(11)
        for (size in listOf(1, 3, 10, 500)) {
            for (k in listOf(1, 2, 5, 40, 1000)) {
                repeat(20) {
                    val raw = DoubleArray(size) { source.nextDouble() }
                    val probabilities = raw.map { it / raw.sum() }.toDoubleArray()
                    assertArrayEquals(sortBasedTopK(k, probabilities), topKIndices(probabilities, k), "size=$size k=$k")
                }
            }
        }
    }

    @Test
    fun `tied probabilities keep the lower index first`() {
        val probabilities = doubleArrayOf(0.1, 0.3, 0.1, 0.3, 0.1, 0.1)
        for (k in 1..6) {
            assertArrayEquals(sortBasedTopK(k, probabilities), topKIndices(probabilities, k), "k=$k")
        }
    }

    @Test
    fun `top k over a whole vocabulary orders it like a sort`() {
        // Increasing probabilities are the worst case for a running insertion: every entry moves all the others
        val size = 65536
        val probabilities = DoubleArray(size) { (it + 1.0) / (size * (size + 1.0) / 2) }
        assertArrayEquals(sortBasedTopK(size, probabilities), topKIndices(probabilities, size))
    }

    @Test
    fun `top k draws each candidate in proportion to its probability`() {
        val probabilities = doubleArrayOf(0.1, 0.25, 0.05, 0.15, 0.45)
        val frequencies = drawFrequencies(SamplingStrategy.TopK(3), probabilities, 100_000)
        // The top three, 0.45, 0.25 and 0.15, renormalized over their total of 0.85
        val expected = doubleArrayOf(0.0, 0.25 / 0.85, 0.0, 0.15 / 0.85, 0.45 / 0.85)
        assertArrayEquals(expected, frequencies, 0.01)
    }

    @Test
    fun `top p draws each nucleus token in proportion to its probability`() {
        val probabilities = doubleArrayOf(0.1, 0.25, 0.05, 0.15, 0.45)
        // 0.45 + 0.25 = 0.7 falls short of 0.8, so the nucleus also takes 0.15
        val frequencies = drawFrequencies(SamplingStrategy.TopP(0.8), probabilities, 100_000)
        val expected = doubleArrayOf(0.0, 0.25 / 0.85, 0.0, 0.15 / 0.85, 0.45 / 0.85)
        assertArrayEquals(expected, frequencies, 0.01)
    }

    @Test
    fun `a zero weight is never drawn`() {
        val random = Random(5)
        repeat(10_000) {
            assertEquals(7, drawFromWeights(intArrayOf(3, 7, 9), doubleArrayOf(0.0, 1.0, 0.0), random))
        }
    }
}
