/**
 * Top-k sampling selects its candidates by running insertion, or by a sort when k is a large share of the
 * vocabulary; these tests pin both to the sort-based selection, including tie order, so seeded runs stay identical.
 */
package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TopKSamplingTest {

    private fun sortBasedTopK(k: Int, probabilities: DoubleArray, random: Random): Int {
        val indexed = probabilities.mapIndexed { index, prob -> index to prob }
            .sortedByDescending { it.second }
            .take(k)
        val sum = indexed.sumOf { it.second }
        var cumulative = 0.0
        for ((index, prob) in indexed) {
            cumulative += prob / sum
            if (random.nextDouble() <= cumulative) return index
        }
        return indexed.last().first
    }

    @Test
    fun `top k samples the same tokens as a sort based selection`() {
        val source = Random(11)
        for (size in listOf(1, 3, 10, 500)) {
            for (k in listOf(1, 2, 5, 40, 1000)) {
                repeat(20) {
                    val raw = DoubleArray(size) { source.nextDouble() }
                    val probabilities = raw.map { it / raw.sum() }.toDoubleArray()
                    val seed = source.nextInt()
                    assertEquals(
                        sortBasedTopK(k, probabilities, Random(seed)),
                        SamplingStrategy.TopK(k).sample(probabilities, Random(seed)),
                        "size=$size k=$k",
                    )
                }
            }
        }
    }

    @Test
    fun `tied probabilities keep the lower index first`() {
        val probabilities = doubleArrayOf(0.1, 0.3, 0.1, 0.3, 0.1, 0.1)
        for (seed in 0 until 50) {
            for (k in 1..6) {
                assertEquals(
                    sortBasedTopK(k, probabilities, Random(seed)),
                    SamplingStrategy.TopK(k).sample(probabilities, Random(seed)),
                    "seed=$seed k=$k",
                )
            }
        }
    }

    @Test
    fun `top k over a whole vocabulary takes about as long as a sort`() {
        // Increasing probabilities are the worst case for a running insertion: every entry moves all the others
        val size = 65536
        val probabilities = DoubleArray(size) { (it + 1.0) / (size * (size + 1.0) / 2) }
        val strategy = SamplingStrategy.TopK(size)
        repeat(3) { strategy.sample(probabilities, Random(it)) }
        val start = System.nanoTime()
        val sampled = strategy.sample(probabilities, Random(7))
        val millis = (System.nanoTime() - start) / 1e6
        assertEquals(sortBasedTopK(size, probabilities, Random(7)), sampled)
        assertTrue(millis < 500, "took $millis ms")
    }
}
